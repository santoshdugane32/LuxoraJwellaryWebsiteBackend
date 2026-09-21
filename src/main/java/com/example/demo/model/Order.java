package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;

    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // ==========================
    // Order Timeline
    // ==========================

    // Order Placed
    private LocalDateTime orderDate;

    // Admin Confirmed
    private LocalDateTime confirmedAt;

    // Jewellery Quality Check
    private LocalDateTime qualityCheckedAt;

    // Packed
    private LocalDateTime packedAt;

    // Shipped
    private LocalDateTime shippedAt;

    // Out For Delivery
    private LocalDateTime outForDeliveryAt;

    // Delivered
    private LocalDateTime deliveredAt;

    // Cancelled
    private LocalDateTime cancelledAt;

    // Return Requested
    private LocalDateTime returnRequestedAt;

    // Returned
    private LocalDateTime returnedAt;

    // Refunded
    private LocalDateTime refundedAt;

    // ==========================
    // Order Items
    // ==========================

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    // ==========================
    // Automatically executed before saving a new order
    // ==========================
    @PrePersist
    public void prePersist() {

        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }

        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }
}