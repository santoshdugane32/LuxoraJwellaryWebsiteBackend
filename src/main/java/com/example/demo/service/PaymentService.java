
package com.example.demo.service;

import com.example.demo.dto.VerifyPaymentRequest;
import com.example.demo.model.Order;
import com.example.demo.model.OrderStatus;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

    public PaymentService(
            RazorpayClient razorpayClient,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository
    ) {
        this.razorpayClient = razorpayClient;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public com.razorpay.Order createOrder(
            Long orderId,
            String userEmail
    ) throws RazorpayException {

        if (orderId == null || isBlank(userEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order details"
            );
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));

        validateOwnership(order, userEmail);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order is not awaiting payment"
            );
        }

        if (order.getTotalAmount() == null
                || !Double.isFinite(order.getTotalAmount())
                || order.getTotalAmount() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order amount"
            );
        }

        long amountInRupees;

        try {
            amountInRupees = BigDecimal
                    .valueOf(order.getTotalAmount())
                    .longValueExact();

            Math.multiplyExact(amountInRupees, 100L);

        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order total must be a whole number of rupees"
            );
        }

        Payment existingPayment = paymentRepository
                .findByOrderId(orderId)
                .orElse(null);

        if (existingPayment != null
                && existingPayment.getStatus() == PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order is already paid"
            );
        }

        // Reuse an existing Razorpay order if it is still active.
        if (existingPayment != null
                && existingPayment.getStatus() == PaymentStatus.CREATED
                && !isBlank(existingPayment.getRazorpayOrderId())) {

            com.razorpay.Order existingRazorpayOrder =
                    razorpayClient.orders.fetch(
                            existingPayment.getRazorpayOrderId()
                    );

            Object statusValue = existingRazorpayOrder.get("status");

            if (statusValue != null
                    && "created".equalsIgnoreCase(
                    statusValue.toString())) {
                return existingRazorpayOrder;
            }

            if (statusValue != null
                    && "paid".equalsIgnoreCase(
                    statusValue.toString())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Payment may already be completed. Please check your order status."
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The previous payment attempt is no longer active. Please contact support."
            );
        }

        JSONObject options = new JSONObject();
        options.put(
                "amount",
                Math.multiplyExact(amountInRupees, 100L)
        );
        options.put("currency", "INR");
        options.put("receipt", "ORDER_" + orderId);

        com.razorpay.Order razorpayOrder =
                razorpayClient.orders.create(options);

        Payment payment = existingPayment != null
                ? existingPayment
                : new Payment();

        payment.setOrderId(orderId);
        payment.setRazorpayOrderId(
                razorpayOrder.get("id").toString()
        );
        payment.setRazorpayPaymentId(null);
        payment.setRazorpaySignature(null);
        payment.setAmount(amountInRupees);
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setPaymentTime(null);

        paymentRepository.save(payment);

        return razorpayOrder;
    }

    @Transactional
    public boolean verifyPayment(
            VerifyPaymentRequest request,
            String userEmail
    ) throws RazorpayException {

        if (request == null
                || isBlank(request.getRazorpayOrderId())
                || isBlank(request.getRazorpayPaymentId())
                || isBlank(request.getRazorpaySignature())
                || isBlank(userEmail)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment verification details"
            );
        }

        Payment payment = paymentRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment record not found"
                ));

        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"
                ));

        validateOwnership(order, userEmail);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            if (request.getRazorpayPaymentId().equals(
                    payment.getRazorpayPaymentId())) {
                return true;
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A different payment has already been recorded"
            );
        }

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This payment cannot be verified"
            );
        }

        // Verify Razorpay's signature.
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

        boolean verified;

        try {
            verified = Utils.verifyPaymentSignature(
                    attributes,
                    razorpaySecret
            );
        } catch (RazorpayException | IllegalArgumentException ex) {
            verified = false;
        }

        if (!verified) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment signature verification failed"
            );
        }

        // Fetch the actual payment status from Razorpay.
        com.razorpay.Payment razorpayPayment =
                razorpayClient.payments.fetch(
                        request.getRazorpayPaymentId()
                );

        Object orderIdValue = razorpayPayment.get("order_id");
        Object statusValue = razorpayPayment.get("status");
        Object currencyValue = razorpayPayment.get("currency");
        Object amountValue = razorpayPayment.get("amount");

        if (orderIdValue == null
                || statusValue == null
                || currencyValue == null
                || !(amountValue instanceof Number)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment details returned by Razorpay"
            );
        }

        String razorpayOrderId = orderIdValue.toString();
        String razorpayStatus = statusValue.toString();
        String razorpayCurrency = currencyValue.toString();

        long razorpayAmount =
                ((Number) amountValue).longValue();

        long expectedAmount;

        try {
            expectedAmount = Math.multiplyExact(
                    payment.getAmount(),
                    100L
            );
        } catch (ArithmeticException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid stored payment amount"
            );
        }

        if (!request.getRazorpayOrderId().equals(razorpayOrderId)
                || razorpayAmount != expectedAmount
                || !"INR".equalsIgnoreCase(razorpayCurrency)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment details do not match the order"
            );
        }

        if ("authorized".equalsIgnoreCase(razorpayStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment is authorized but not captured yet"
            );
        }

        if (!"captured".equalsIgnoreCase(razorpayStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment has not been captured successfully"
            );
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order is no longer awaiting payment"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        payment.setRazorpayPaymentId(
                request.getRazorpayPaymentId()
        );
        payment.setRazorpaySignature(
                request.getRazorpaySignature()
        );
        payment.setPaymentTime(now);
        payment.setStatus(PaymentStatus.SUCCESS);

        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(now);

        paymentRepository.save(payment);
        orderRepository.save(order);

        return true;
    }

    private void validateOwnership(
            Order order,
            String userEmail
    ) {
        if (order.getUserEmail() == null
                || isBlank(userEmail)
                || !order.getUserEmail().equalsIgnoreCase(userEmail)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to access this order"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
