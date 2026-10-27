package com.example.demo.controller;

import com.example.demo.model.Cart;
import com.example.demo.repository.CartRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartRepository repository;

    public CartController(CartRepository repository) {
        this.repository = repository;
    }

    private String requireAuthentication(Authentication auth) {
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in to access your cart"
            );
        }

        return auth.getName();
    }

    // ADD ITEM TO CART
    @PostMapping("/add")
    public Cart addToCart(
            @RequestBody Cart cart,
            Authentication auth) {

        String email = requireAuthentication(auth);

        if (cart == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cart item is required"
            );
        }

        /*
         * IMPORTANT:
         * Set the cart owner's email using the authenticated user.
         * This assumes your Cart entity has a userEmail field.
         */
        cart.setId(null);
        cart.setUserEmail(email);

        return repository.save(cart);
    }

    // GET THE LOGGED-IN USER'S CART
    @GetMapping
    public List<Cart> getCartItems(Authentication auth) {
        String email = requireAuthentication(auth);

        /*
         * This assumes CartRepository has:
         * List<Cart> findByUserEmail(String userEmail);
         */
        return repository.findByUserEmail(email);
    }

    // REMOVE ITEM FROM THE LOGGED-IN USER'S CART
    @DeleteMapping("/{id}")
    @Transactional
    public void removeItem(
            @PathVariable Long id,
            Authentication auth) {

        String email = requireAuthentication(auth);

        Cart cart = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cart item not found"
                ));

        if (!email.equals(cart.getUserEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot remove another user's cart item"
            );
        }

        repository.delete(cart);
    }
}
