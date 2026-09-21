package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPaymentRequest {

    // Internal Order ID
    private Long orderId;

    // Razorpay Order ID
    private String razorpayOrderId;

    // Razorpay Payment ID
    private String razorpayPaymentId;

    // Razorpay Signature
    private String razorpaySignature;

}