package com.example.demo.controller;

import com.example.demo.model.Cart;
import com.example.demo.repository.CartRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "http://localhost:5173")
public class CartController {

    private final CartRepository repository;

    public CartController(CartRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/add")
    public Cart addToCart(
            @RequestBody Cart cart) {

        return repository.save(cart);
    }

    @GetMapping
    public List<Cart> getCartItems() {

        return repository.findAll();
    }

    @DeleteMapping("/{id}")
    public void removeItem(
            @PathVariable Long id) {

        repository.deleteById(id);
    }
}