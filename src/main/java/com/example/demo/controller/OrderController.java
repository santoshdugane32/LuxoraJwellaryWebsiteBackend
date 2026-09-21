package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.Product;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderService orderService;

    // =========================
    // USER - PLACE ORDER
    // =========================
    @PostMapping
    public Order placeOrder(@RequestBody Order order, Authentication auth) {

        String email = auth.getName();

        order.setUserEmail(email);
        order.setStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());

        if (order.getItems() != null) {

            for (OrderItem item : order.getItems()) {

                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Product not found"
                                )
                        );

                if (product.getStock() < item.getQuantity()) {

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Not enough stock for " + product.getName()
                    );

                }

                product.setStock(product.getStock() - item.getQuantity());

                productRepository.save(product);

                item.setProductName(product.getName());

                item.setOrder(order);
            }
        }

        return repository.save(order);
    }

    // =========================
    // USER - PAGINATED ORDERS
    // =========================
    @GetMapping("/user")
    public Page<Order> getUserOrders(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search
    ) {

        String email = auth.getName();

        Pageable pageable = PageRequest.of(page, size);

        if (search != null && !search.isBlank() && status != null) {

            return repository.searchByUserStatusAndKeyword(
                    email,
                    status,
                    search,
                    pageable
            );
        }

        if (search != null && !search.isBlank()) {

            return repository.searchByUserAndKeyword(
                    email,
                    search,
                    pageable
            );
        }

        if (status != null) {

            return repository.findByUserEmailAndStatus(
                    email,
                    status,
                    pageable
            );
        }

        return repository.findByUserEmail(email, pageable);
    }

    // =========================
    // USER - ALL MY ORDERS
    // =========================
    @GetMapping("/my-orders")
    public List<Order> getMyOrders(Authentication auth) {

        return orderService.getOrdersByEmail(auth.getName());
    }
}