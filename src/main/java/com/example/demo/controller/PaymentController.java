package com.example.demo.controller;

import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.VerifyPaymentRequest;
import com.example.demo.service.PaymentService;
import com.razorpay.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ==========================
    // CREATE RAZORPAY ORDER
    // ==========================
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestBody CreateOrderRequest request,
            Authentication auth
    ) {

        try {

            System.out.println("\n======================================");
            System.out.println("CREATE PAYMENT REQUEST");
            System.out.println("======================================");

            if (auth != null) {
                System.out.println("USER      : " + auth.getName());
            } else {
                System.out.println("USER      : NULL");
            }

            System.out.println("ORDER ID  : " + request.getOrderId());
            System.out.println("AMOUNT    : " + request.getAmount());

            Order razorpayOrder = paymentService.createOrder(
                    request.getOrderId(),
                    request.getAmount()
            );

            System.out.println("RAZORPAY ORDER CREATED SUCCESSFULLY");
            System.out.println("ORDER ID : " + razorpayOrder.get("id"));

            // Return JSON instead of Razorpay Order object
            Map<String, Object> response = razorpayOrder.toJson().toMap();

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            System.out.println("\n======================================");
            System.out.println("CREATE PAYMENT FAILED");
            System.out.println("======================================");

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }

    // ==========================
    // VERIFY PAYMENT
    // ==========================
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestBody VerifyPaymentRequest request,
            Authentication auth
    ) {

        try {

            System.out.println("\n======================================");
            System.out.println("VERIFY PAYMENT");
            System.out.println("======================================");

            if (auth != null) {
                System.out.println("USER : " + auth.getName());
            } else {
                System.out.println("USER : NULL");
            }

            System.out.println("ORDER ID          : " + request.getOrderId());
            System.out.println("RAZORPAY ORDER ID : " + request.getRazorpayOrderId());
            System.out.println("PAYMENT ID        : " + request.getRazorpayPaymentId());

            boolean verified = paymentService.verifyPayment(request);

            System.out.println("VERIFIED : " + verified);

            if (verified) {

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Payment Verified Successfully"
                ));
            }

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Payment Verification Failed"
            ));

        } catch (Exception e) {

            System.out.println("\n======================================");
            System.out.println("VERIFY PAYMENT FAILED");
            System.out.println("======================================");

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }
}