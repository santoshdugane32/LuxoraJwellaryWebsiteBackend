
package com.example.demo.controller;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.VerifyPaymentRequest;
import com.example.demo.service.PaymentService;
import com.razorpay.Order;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger logger =
            LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    // CREATE RAZORPAY ORDER
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication auth
    ) {
        if (!isAuthenticated(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "Authentication required"
                    ));
        }

        if (request == null || request.getOrderId() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "Order ID is required"
                    ));
        }

        try {
            // The backend reads the saved order amount.
            // Never trust an amount submitted by React.
            Order razorpayOrder = paymentService.createOrder(
                    request.getOrderId(),
                    auth.getName()
            );

            // Preserve the response structure expected by Checkout.jsx.
            return ResponseEntity.ok(razorpayOrder.toJson().toMap());

        } catch (ResponseStatusException ex) {
            logger.warn(
                    "Razorpay order request rejected: {}",
                    ex.getStatusCode()
            );

            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of(
                            "success", false,
                            "message", ex.getReason() != null
                                    ? ex.getReason()
                                    : "Payment order request rejected"
                    ));

        } catch (Exception ex) {
            logger.error("Failed to create Razorpay order", ex);

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(Map.of(
                    "success", false,
                    "message", "Unable to create payment order"
            ));
        }
    }

    // VERIFY RAZORPAY PAYMENT
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            Authentication auth
    ) {
        if (!isAuthenticated(auth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "message", "Authentication required"
                    ));
        }

        if (request == null
                || request.getRazorpayOrderId() == null
                || request.getRazorpayOrderId().isBlank()
                || request.getRazorpayPaymentId() == null
                || request.getRazorpayPaymentId().isBlank()
                || request.getRazorpaySignature() == null
                || request.getRazorpaySignature().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message",
                            "Payment verification details are required"
                    ));
        }

        try {
            boolean verified = paymentService.verifyPayment(
                    request,
                    auth.getName()
            );

            if (!verified) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "Payment verification failed"
                        ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment verified successfully"
            ));

        } catch (ResponseStatusException ex) {
            logger.warn(
                    "Payment verification request rejected: {}",
                    ex.getStatusCode()
            );

            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of(
                            "success", false,
                            "message", ex.getReason() != null
                                    ? ex.getReason()
                                    : "Payment verification request rejected"
                    ));

        } catch (Exception ex) {
            logger.error("Payment verification failed", ex);

            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR
            ).body(Map.of(
                    "success", false,
                    "message", "Unable to verify payment"
            ));
        }
    }
}
