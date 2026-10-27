
package com.example.demo.service;

import com.example.demo.model.GoldRate;
import com.example.demo.model.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PriceService {

    public long calculatePrice(Product product, GoldRate rate) {
        if (product == null || rate == null) {
            throw new IllegalArgumentException(
                    "Product and gold rate are required"
            );
        }

        if (product.getWeightInGrams() <= 0
                || product.getMakingChargePercent() < 0
                || rate.getGold24k() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid product weight, making charges, or gold rate"
            );
        }

        BigDecimal goldRate = BigDecimal.valueOf(rate.getGold24k());
        BigDecimal weight = BigDecimal.valueOf(
                product.getWeightInGrams()
        );
        BigDecimal makingPercent = BigDecimal.valueOf(
                product.getMakingChargePercent()
        );

        BigDecimal goldCost = goldRate.multiply(weight);

        BigDecimal makingCharge = goldCost
                .multiply(makingPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal subtotal = goldCost.add(makingCharge);

        BigDecimal gst = subtotal
                .multiply(new BigDecimal("0.03"));

        return subtotal.add(gst)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
