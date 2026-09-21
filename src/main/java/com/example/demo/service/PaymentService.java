package com.example.demo.service;

import com.example.demo.dto.VerifyPaymentRequest;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

    public PaymentService(
            RazorpayClient razorpayClient,
            PaymentRepository paymentRepository
    ) {
        this.razorpayClient = razorpayClient;
        this.paymentRepository = paymentRepository;
    }

    // =====================================
    // CREATE RAZORPAY ORDER
    // =====================================
    public Order createOrder(Long orderId, Integer amount) throws Exception {

        System.out.println("\n========== PAYMENT SERVICE ==========");
        System.out.println("Order ID : " + orderId);
        System.out.println("Amount   : " + amount);

        JSONObject options = new JSONObject();

        options.put("amount", amount*100);          // Amount in paise
        options.put("currency", "INR");
        options.put("receipt", "ORDER_" + orderId);
        options.put("payment_capture", 1);

        System.out.println("\nSending to Razorpay...");
        System.out.println(options.toString(4));

        try {

            Order razorpayOrder = razorpayClient.orders.create(options);

            System.out.println("\n========== RAZORPAY RESPONSE ==========");
            System.out.println(razorpayOrder.toString());

            Payment payment = Payment.builder()
                    .orderId(orderId)
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .amount(Long.valueOf(amount))
                    .currency("INR")
                    .status(PaymentStatus.CREATED)
                    .build();

            paymentRepository.save(payment);

            return razorpayOrder;

        } catch (RazorpayException e) {

            System.out.println("\n========== RAZORPAY ERROR ==========");
            System.out.println("Message : " + e.getMessage());
            e.printStackTrace();

            throw e;

        } catch (Exception e) {

            System.out.println("\n========== UNKNOWN ERROR ==========");
            e.printStackTrace();

            throw e;
        }
    }

    // =====================================
    // VERIFY PAYMENT
    // =====================================
    public boolean verifyPayment(VerifyPaymentRequest request) throws Exception {

        JSONObject attributes = new JSONObject();

        attributes.put(
                "razorpay_order_id",
                request.getRazorpayOrderId()
        );

        attributes.put(
                "razorpay_payment_id",
                request.getRazorpayPaymentId()
        );

        attributes.put(
                "razorpay_signature",
                request.getRazorpaySignature()
        );

        boolean verified = Utils.verifyPaymentSignature(
                attributes,
                razorpaySecret
        );

        Payment payment = paymentRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() ->
                        new RuntimeException("Payment record not found"));

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setPaymentTime(LocalDateTime.now());

        if (verified) {
            payment.setStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentRepository.save(payment);

        return verified;
    }
}