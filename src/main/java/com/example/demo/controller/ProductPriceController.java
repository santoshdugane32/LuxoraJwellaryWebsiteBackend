
package com.example.demo.controller;

import com.example.demo.model.GoldRate;
import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.GoldRateService;
import com.example.demo.service.PriceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/product-price")
@CrossOrigin(origins = "*")
public class ProductPriceController {

    private final ProductRepository productRepo;
    private final GoldRateService goldRateService;
    private final PriceService priceService;

    public ProductPriceController(
            ProductRepository productRepo,
            GoldRateService goldRateService,
            PriceService priceService
    ) {
        this.productRepo = productRepo;
        this.goldRateService = goldRateService;
        this.priceService = priceService;
    }

    @GetMapping("/{id}")
    public long getPrice(@PathVariable Long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"
                ));

        GoldRate rate = goldRateService.getLatestRate();

        if (rate == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Gold rate is currently unavailable"
            );
        }

        try {
            return priceService.calculatePrice(product, rate);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    ex.getMessage()
            );
        }
    }
}
