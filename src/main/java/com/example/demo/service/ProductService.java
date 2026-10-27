package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // GET ALL PRODUCTS
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // GET PRODUCT BY ID
    public Product getProductById(Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid product ID"
            );
        }

        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));
    }

    // GET PRODUCTS BY CATEGORY
    public List<Product> getProductsByCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Category is required"
            );
        }

        return productRepository.findByCategoryIgnoreCase(
                category.trim()
        );
    }

    // SEARCH PRODUCTS BY NAME
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllProducts();
        }

        return productRepository
                .findByNameContainingIgnoreCase(keyword.trim());
    }

    // VALIDATE PRODUCT INPUT
    private void validateProduct(Product product) {
        if (product == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product details are required"
            );
        }

        if (product.getName() == null || product.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product name is required"
            );
        }

        if (product.getCategory() == null
                || product.getCategory().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product category is required"
            );
        }

        if (product.getStock() == null || product.getStock() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Stock cannot be negative"
            );
        }

        if (product.getWeightInGrams() == null
                || !Double.isFinite(product.getWeightInGrams())
                || product.getWeightInGrams() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Weight must be greater than zero"
            );
        }

        if (product.getMakingChargePercent() == null
                || !Double.isFinite(product.getMakingChargePercent())
                || product.getMakingChargePercent() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Making charge cannot be negative"
            );
        }

        if (product.getPurity() == null
                || product.getPurity() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Purity must be greater than zero"
            );
        }
    }

    // ADD PRODUCT
    @Transactional
    public Product addProduct(Product product) {
        validateProduct(product);

        // Ensure a new product is created.
        product.setId(null);

        return productRepository.save(product);
    }

    // UPDATE PRODUCT
    @Transactional
    public Product updateProduct(Long id, Product updatedProduct) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid product ID"
            );
        }

        validateProduct(updatedProduct);

        Product existingProduct = getProductById(id);

        existingProduct.setName(updatedProduct.getName().trim());
        existingProduct.setCategory(updatedProduct.getCategory().trim());
        existingProduct.setImageUrl(updatedProduct.getImageUrl());
        existingProduct.setStock(updatedProduct.getStock());
        existingProduct.setPurity(updatedProduct.getPurity());
        existingProduct.setWeightInGrams(
                updatedProduct.getWeightInGrams()
        );
        existingProduct.setMakingChargePercent(
                updatedProduct.getMakingChargePercent()
        );

        return productRepository.save(existingProduct);
    }

    // DELETE PRODUCT
    @Transactional
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }
}
