package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Internal Order ID
    private Long orderId;

    // Razorpay Order ID
    @Column(unique = true)
    private String razorpayOrderId;

    // Razorpay Payment ID
    @Column(unique = true)
    private String razorpayPaymentId;

    // Razorpay Signature
    @Column(length = 500)
    private String razorpaySignature;

    // Amount in INR
    private Long amount;

    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime paymentTime;
}