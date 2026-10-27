package com.example.demo.controller;

import com.example.demo.model.GoldRate;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.Product;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.GoldRateService;
import com.example.demo.service.OrderService;
import com.example.demo.service.PriceService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository repository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final GoldRateService goldRateService;
    private final PriceService priceService;

    public OrderController(
            OrderRepository repository,
            ProductRepository productRepository,
            OrderService orderService,
            GoldRateService goldRateService,
            PriceService priceService) {

        this.repository = repository;
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.goldRateService = goldRateService;
        this.priceService = priceService;
    }

    private void requireAuthentication(Authentication auth) {
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }
    }

    // USER - PLACE ORDER
    @PostMapping
    @Transactional
    public Order placeOrder(
            @RequestBody Order order,
            Authentication auth) {

        requireAuthentication(auth);

        if (order == null
                || order.getItems() == null
                || order.getItems().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order must contain at least one item"
            );
        }

        GoldRate rate = goldRateService.getLatestRate();

        if (rate == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gold rate is currently unavailable"
            );
        }

        long totalAmount = 0L;
        List<OrderItem> validatedItems = new ArrayList<>();

        for (OrderItem requestedItem : order.getItems()) {

            if (requestedItem == null
                    || requestedItem.getProductId() == null
                    || requestedItem.getQuantity() == null
                    || requestedItem.getQuantity() <= 0
                    || requestedItem.getQuantity() > 100) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid product ID or quantity"
                );
            }

            Product product = productRepository
                    .findById(requestedItem.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Product not found"
                    ));

            if (product.getStock() == null
                    || product.getStock() < requestedItem.getQuantity()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Not enough stock for " + product.getName()
                );
            }

            long unitPrice = priceService.calculatePrice(product, rate);

            if (unitPrice <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid price for " + product.getName()
                );
            }

            try {
                long itemTotal = Math.multiplyExact(
                        unitPrice,
                        requestedItem.getQuantity().longValue()
                );

                totalAmount = Math.addExact(totalAmount, itemTotal);

            } catch (ArithmeticException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Order amount is too large"
                );
            }

            // Create a clean item using trusted server-side product data.
            OrderItem item = new OrderItem();

            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setQuantity(requestedItem.getQuantity());

            // Store the unit-price snapshot.
            item.setPrice((double) unitPrice);

            item.setOrder(order);

            validatedItems.add(item);
        }

        // Use only validated items.
        order.setItems(validatedItems);

        // Use the authenticated user's email.
        order.setUserEmail(auth.getName());

        // Calculate the total on the backend.
        order.setTotalAmount((double) totalAmount);

        // Await successful payment verification.
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        // Prevent a client-supplied order ID.
        order.setId(null);

        return repository.save(order);
    }

    // USER - PAGINATED ORDERS
    @GetMapping("/user")
    public Page<Order> getUserOrders(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search) {

        requireAuthentication(auth);

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid pagination values"
            );
        }

        String email = auth.getName();
        Pageable pageable = PageRequest.of(page, size);

        if (search != null && !search.isBlank() && status != null) {
            return repository.searchByUserStatusAndKeyword(
                    email, status, search.trim(), pageable
            );
        }

        if (search != null && !search.isBlank()) {
            return repository.searchByUserAndKeyword(
                    email, search.trim(), pageable
            );
        }

        if (status != null) {
            return repository.findByUserEmailAndStatus(
                    email, status, pageable
            );
        }

        return repository.findByUserEmail(email, pageable);
    }

    // USER - ALL MY ORDERS
    @GetMapping("/my-orders")
    public List<Order> getMyOrders(Authentication auth) {
        requireAuthentication(auth);
        return orderService.getOrdersByEmail(auth.getName());
    }
}
