package com.example.demo.repository;

import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // =========================
    // USER ORDERS
    // =========================

    List<Order> findByUserEmail(String email);

    Page<Order> findByUserEmail(String email, Pageable pageable);

    Page<Order> findByUserEmailAndStatus(
            String email,
            OrderStatus status,
            Pageable pageable
    );

    // =========================
    // SEARCH ONLY
    // =========================

    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN o.items i
        WHERE o.userEmail = :email
        AND (
            CAST(o.id AS string) LIKE %:search%
            OR LOWER(i.productName) LIKE LOWER(CONCAT('%', :search, '%'))
        )
    """)
    Page<Order> searchByUserAndKeyword(
            String email,
            String search,
            Pageable pageable
    );

    // =========================
    // SEARCH + STATUS
    // =========================

    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN o.items i
        WHERE o.userEmail = :email
        AND o.status = :status
        AND (
            CAST(o.id AS string) LIKE %:search%
            OR LOWER(i.productName) LIKE LOWER(CONCAT('%', :search, '%'))
        )
    """)
    Page<Order> searchByUserStatusAndKeyword(
            String email,
            OrderStatus status,
            String search,
            Pageable pageable
    );

}