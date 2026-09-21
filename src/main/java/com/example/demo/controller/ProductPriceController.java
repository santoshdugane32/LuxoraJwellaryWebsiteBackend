package com.example.demo.controller;

import com.example.demo.model.GoldRate;
import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.GoldRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-price")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductPriceController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private GoldRateService goldRateService;

    @GetMapping("/{id}")
    public Long getPrice(@PathVariable Long id) {

        Product p = productRepo.findById(id).orElseThrow();

        GoldRate rate = goldRateService.getLatestRate();

        double goldCost =
                rate.getGold24k() * p.getWeightInGrams();

        double makingCharge =
                goldCost * p.getMakingChargePercent() / 100;

        double subtotal =
                goldCost + makingCharge;

        double gst =
                subtotal * 0.03;

        return Math.round(subtotal + gst);
    }

}
