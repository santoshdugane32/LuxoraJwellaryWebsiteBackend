package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    @Autowired
    private ProductRepository repository;

    // ==========================
    // PUBLIC APIs
    // ==========================

    @GetMapping
    public List<Product> getAll() {
        return repository.findAll();
    }

    @GetMapping("/explore")
    public List<Product> explore(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {

        if (category != null && keyword != null && !keyword.isEmpty()) {
            return repository.searchByCategoryAndName(category, keyword);
        }

        if (keyword != null && !keyword.isEmpty()) {
            return repository.searchByName(keyword);
        }

        if (category != null && !category.isEmpty()) {
            return repository.findAll()
                    .stream()
                    .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                    .toList();
        }

        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    // ==========================
    // ADMIN APIs
    // ==========================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Product addProduct(@RequestBody Product product) {
        return repository.save(product);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Product updateProduct(
            @PathVariable Long id,
            @RequestBody Product product
    ) {

        Product existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        existing.setName(product.getName());
        existing.setCategory(product.getCategory());
        existing.setImageUrl(product.getImageUrl());
        existing.setStock(product.getStock());
        existing.setPurity(product.getPurity());
        existing.setWeightInGrams(product.getWeightInGrams());
        existing.setMakingChargePercent(product.getMakingChargePercent());

        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(@PathVariable Long id) {
        repository.deleteById(id);
    }
}