package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPaymentRequest {

    // Internal order ID
    private Long orderId;

    // Razorpay order ID
    @NotBlank(message = "Razorpay order ID is required")
    private String razorpayOrderId;

    // Razorpay payment ID
    @NotBlank(message = "Razorpay payment ID is required")
    private String razorpayPaymentId;

    // Razorpay signature
    @NotBlank(message = "Razorpay signature is required")
    private String razorpaySignature;
}
