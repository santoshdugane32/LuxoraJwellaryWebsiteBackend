package com.example.demo.repository;

import com.example.demo.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find payment using Razorpay Order ID
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    // Find payment using your internal Order ID
    Optional<Payment> findByOrderId(Long orderId);

    // (Optional) Get all payments of an order
    List<Payment> findAllByOrderId(Long orderId);

}