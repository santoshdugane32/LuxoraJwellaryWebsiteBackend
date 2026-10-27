package com.example.demo.service;

import com.example.demo.model.Cart;
import com.example.demo.model.GoldRate;
import com.example.demo.model.Product;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final GoldRateService goldRateService;
    private final PriceService priceService;

    public CartService(
            CartRepository cartRepository,
            ProductRepository productRepository,
            GoldRateService goldRateService,
            PriceService priceService) {

        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.goldRateService = goldRateService;
        this.priceService = priceService;
    }

    // ADD ITEM TO CART
    @Transactional
    public Cart addToCart(
            Long productId,
            int quantity,
            String userEmail) {

        if (productId == null || quantity <= 0 || quantity > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid product ID or quantity"
            );
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        if (product.getStock() == null
                || product.getStock() < quantity) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient stock"
            );
        }

        GoldRate rate = goldRateService.getLatestRate();

        if (rate == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gold rate is currently unavailable"
            );
        }

        long unitPrice = priceService.calculatePrice(product, rate);

        if (unitPrice <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid product price"
            );
        }

        Cart cart = new Cart();
        cart.setUserEmail(userEmail);
        cart.setProductId(product.getId());
        cart.setProductName(product.getName());
        cart.setPrice((double) unitPrice);
        cart.setImage(product.getImageUrl());
        cart.setQuantity(quantity);

        return cartRepository.save(cart);
    }

    // GET THE LOGGED-IN USER'S CART
    public List<Cart> getCartItems(String userEmail) {
        return cartRepository.findByUserEmail(userEmail);
    }

    // REMOVE AN ITEM FROM THE LOGGED-IN USER'S CART
    @Transactional
    public void removeItem(Long cartId, String userEmail) {

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cart item not found"
                ));

        if (!userEmail.equals(cart.getUserEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot remove another user's cart item"
            );
        }

        cartRepository.delete(cart);
    }
}