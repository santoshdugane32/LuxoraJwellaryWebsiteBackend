package com.example.demo.repository;

import com.example.demo.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // Fetch cart items belonging to a specific user
    List<Cart> findByUserEmail(String userEmail);
}
