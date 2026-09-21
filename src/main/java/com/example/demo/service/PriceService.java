package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.model.GoldRate;
import org.springframework.stereotype.Service;

@Service
public class PriceService {

    public double calculatePrice(Product p, GoldRate rate) {

        double baseGoldPrice = rate.getGold24k();

        double goldCost = baseGoldPrice * p.getWeightInGrams();

        double makingCharge = goldCost * (p.getMakingChargePercent() / 100);

        double subtotal = goldCost + makingCharge;

        double gst = subtotal * 0.03;

        return subtotal + gst;
    }
}
