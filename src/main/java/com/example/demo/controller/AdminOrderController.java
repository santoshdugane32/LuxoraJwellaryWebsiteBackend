package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.repository.OrderRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderRepository repository;

    // Constructor injection
    public AdminOrderController(OrderRepository repository) {
        this.repository = repository;
    }

    // GET ALL ORDERS
    @GetMapping
    public List<Order> getAllOrders() {
        return repository.findAll();
    }

    // UPDATE ORDER STATUS
    @PutMapping("/{id}/{status}")
    @Transactional
    public Order updateOrderStatus(
            @PathVariable Long id,
            @PathVariable OrderStatus status) {

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order ID"
            );
        }

        Order order = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));

        order.setStatus(status);

        LocalDateTime now = LocalDateTime.now();

        switch (status) {
            case PENDING -> {
                // No timestamp update
            }

            case CONFIRMED -> {
                if (order.getConfirmedAt() == null) {
                    order.setConfirmedAt(now);
                }
            }

            case QUALITY_CHECK -> {
                if (order.getQualityCheckedAt() == null) {
                    order.setQualityCheckedAt(now);
                }
            }

            case PACKED -> {
                if (order.getPackedAt() == null) {
                    order.setPackedAt(now);
                }
            }

            case SHIPPED -> {
                if (order.getShippedAt() == null) {
                    order.setShippedAt(now);
                }
            }

            case OUT_FOR_DELIVERY -> {
                if (order.getOutForDeliveryAt() == null) {
                    order.setOutForDeliveryAt(now);
                }
            }

            case DELIVERED -> {
                if (order.getDeliveredAt() == null) {
                    order.setDeliveredAt(now);
                }
            }

            case CANCELLED -> {
                if (order.getCancelledAt() == null) {
                    order.setCancelledAt(now);
                }
            }

            case RETURN_REQUESTED -> {
                if (order.getReturnRequestedAt() == null) {
                    order.setReturnRequestedAt(now);
                }
            }

            case RETURNED -> {
                if (order.getReturnedAt() == null) {
                    order.setReturnedAt(now);
                }
            }

            case REFUNDED -> {
                if (order.getRefundedAt() == null) {
                    order.setRefundedAt(now);
                }
            }
        }

        return repository.save(order);
    }

    // GET ORDER BY ID
    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order ID"
            );
        }

        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));
    }

    // DELETE ORDER
    @DeleteMapping("/{id}")
    @Transactional
    public String deleteOrder(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order ID"
            );
        }

        Order order = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));

        repository.delete(order);

        return "Order deleted successfully";
    }
}
