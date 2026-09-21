package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    // Internal Order ID from your orders table
    private Long orderId;

    // Total amount in INR (Razorpay converts it to paise)
    private Integer amount;

}