package com.example.demo.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key.id}")
    private String key;

    @Value("${razorpay.key.secret}")
    private String secret;

    // ==========================
    // Razorpay Client Bean
    // ==========================
    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {

        return new RazorpayClient(key, secret);
    }
}