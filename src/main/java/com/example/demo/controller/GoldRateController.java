package com.example.demo.controller;

import com.example.demo.model.GoldRate;
import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.GoldRateService;
import com.example.demo.service.PriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class GoldRateController {

    @Autowired
    private GoldRateService goldRateService;

    @Autowired
    private PriceService priceService;

    @Autowired
    private ProductRepository productRepository;

    // ⚡ GET CURRENT GOLD RATE (FAST CACHE)
    @GetMapping("/gold-rate")
    public GoldRate getRate() {
        return goldRateService.getLatestRate();
    }

    // 🔥 FORCE REFRESH LIVE API
    @GetMapping("/gold-rate/refresh")
    public GoldRate refreshRate() {
        return goldRateService.updateGoldRate();
    }

    // 💎 GET PRODUCT FINAL PRICE (JEWELLERY LOGIC)
//    @GetMapping("/product-price/{id}")
//    public double getProductPrice(@PathVariable Long id) {
//
//        Product product = productRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Product not found"));
//
//        GoldRate rate = goldRateService.getLatestRate();
//
//        return priceService.calculatePrice(product, rate);
//    }
}