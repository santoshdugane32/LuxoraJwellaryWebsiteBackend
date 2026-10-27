
package com.example.demo.repository;

import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // USER ORDERS
    List<Order> findByUserEmail(String email);

    Page<Order> findByUserEmail(String email, Pageable pageable);

    Page<Order> findByUserEmailAndStatus(
            String email,
            OrderStatus status,
            Pageable pageable
    );

    // SEARCH BY ORDER ID OR PRODUCT NAME
    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN o.items i
        WHERE o.userEmail = :email
        AND (
            CAST(o.id AS string) LIKE CONCAT('%', :search, '%')
            OR LOWER(i.productName) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
    Page<Order> searchByUserAndKeyword(
            @Param("email") String email,
            @Param("search") String search,
            Pageable pageable
    );

    // SEARCH BY ORDER ID OR PRODUCT NAME + STATUS
    @Query("""
        SELECT DISTINCT o FROM Order o
        JOIN o.items i
        WHERE o.userEmail = :email
        AND o.status = :status
        AND (
            CAST(o.id AS string) LIKE CONCAT('%', :search, '%')
            OR LOWER(i.productName) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
    Page<Order> searchByUserStatusAndKeyword(
            @Param("email") String email,
            @Param("status") OrderStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
