package com.example.demo.repository;

import com.example.demo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Search products by name
    @Query("""
            SELECT p FROM Product p
            WHERE LOWER(p.name)
            LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    List<Product> searchByName(@Param("keyword") String keyword);

    // Search products by category and name
    @Query("""
            SELECT p FROM Product p
            WHERE LOWER(p.category) = LOWER(:category)
            AND LOWER(p.name)
            LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    List<Product> searchByCategoryAndName(
            @Param("category") String category,
            @Param("keyword") String keyword
    );

    // Find products by category
    List<Product> findByCategoryIgnoreCase(String category);

    // Search products by partial name
    List<Product> findByNameContainingIgnoreCase(String name);
}