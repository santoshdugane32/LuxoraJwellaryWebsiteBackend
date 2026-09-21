package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminOrderController {

    @Autowired
    private OrderRepository repository;

    // ==========================
    // GET ALL ORDERS
    // ==========================
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Order> getAllOrders() {
        return repository.findAll();
    }

    // ==========================
    // UPDATE ORDER STATUS
    // ==========================
    @PutMapping("/{id}/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public Order updateOrderStatus(
            @PathVariable Long id,
            @PathVariable OrderStatus status
    ) {

        Order order = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(status);

        switch (status) {

            case PENDING -> {
                // No changes
            }

            case CONFIRMED -> {
                if (order.getConfirmedAt() == null) {
                    order.setConfirmedAt(LocalDateTime.now());
                }
            }

            case QUALITY_CHECK -> {
                if (order.getQualityCheckedAt() == null) {
                    order.setQualityCheckedAt(LocalDateTime.now());
                }
            }

            case PACKED -> {
                if (order.getPackedAt() == null) {
                    order.setPackedAt(LocalDateTime.now());
                }
            }

            case SHIPPED -> {
                if (order.getShippedAt() == null) {
                    order.setShippedAt(LocalDateTime.now());
                }
            }

            case OUT_FOR_DELIVERY -> {
                if (order.getOutForDeliveryAt() == null) {
                    order.setOutForDeliveryAt(LocalDateTime.now());
                }
            }

            case DELIVERED -> {
                if (order.getDeliveredAt() == null) {
                    order.setDeliveredAt(LocalDateTime.now());
                }
            }

            case CANCELLED -> {
                if (order.getCancelledAt() == null) {
                    order.setCancelledAt(LocalDateTime.now());
                }
            }

            case RETURN_REQUESTED -> {
                if (order.getReturnRequestedAt() == null) {
                    order.setReturnRequestedAt(LocalDateTime.now());
                }
            }

            case RETURNED -> {
                if (order.getReturnedAt() == null) {
                    order.setReturnedAt(LocalDateTime.now());
                }
            }

            case REFUNDED -> {
                if (order.getRefundedAt() == null) {
                    order.setRefundedAt(LocalDateTime.now());
                }
            }
        }

        return repository.save(order);
    }

    // ==========================
    // GET ORDER BY ID
    // ==========================
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Order getOrder(@PathVariable Long id) {

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    // ==========================
    // DELETE ORDER
    // ==========================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteOrder(@PathVariable Long id) {

        repository.deleteById(id);

        return "Order Deleted Successfully";
    }
}