
package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    // PUBLIC API - GET ALL PRODUCTS
    @GetMapping
    public List<Product> getAll() {
        return repository.findAll();
    }

    // PUBLIC API - EXPLORE PRODUCTS
    @GetMapping("/explore")
    public List<Product> explore(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        boolean hasCategory =
                category != null && !category.isBlank();

        boolean hasKeyword =
                keyword != null && !keyword.isBlank();

        if (hasCategory && hasKeyword) {
            return repository.searchByCategoryAndName(
                    category.trim(), keyword.trim()
            );
        }

        if (hasKeyword) {
            return repository.searchByName(keyword.trim());
        }

        if (hasCategory) {
            return repository.findAll()
                    .stream()
                    .filter(product ->
                            category.trim().equalsIgnoreCase(
                                    product.getCategory()
                            )
                    )
                    .toList();
        }

        return repository.findAll();
    }

    // PUBLIC API - GET PRODUCT BY ID
    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );
    }

    // ADMIN API - ADD PRODUCT
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Product addProduct(@RequestBody Product product) {
        validateProduct(product);
        return repository.save(product);
    }

    // ADMIN API - UPDATE PRODUCT
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Product updateProduct(
            @PathVariable Long id,
            @RequestBody Product product
    ) {
        validateProduct(product);

        Product existing = repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );

        existing.setName(product.getName());
        existing.setCategory(product.getCategory());
        existing.setImageUrl(product.getImageUrl());
        existing.setStock(product.getStock());
        existing.setPurity(product.getPurity());
        existing.setWeightInGrams(product.getWeightInGrams());
        existing.setMakingChargePercent(
                product.getMakingChargePercent()
        );

        // Preserve the existing price unless your pricing system
        // explicitly allows administrators to update it.
        return repository.save(existing);
    }

    // ADMIN API - DELETE PRODUCT
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Product not found"
            );
        }

        repository.deleteById(id);
    }

    private void validateProduct(Product product) {
        if (product == null
                || product.getName() == null
                || product.getName().isBlank()
                || product.getCategory() == null
                || product.getCategory().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product name and category are required"
            );
        }

        if (product.getStock() < 0
                || product.getWeightInGrams() <= 0
                || product.getMakingChargePercent() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid stock, weight, or making charge"
            );
        }
    }
}
