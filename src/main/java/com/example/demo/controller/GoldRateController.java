package com.example.demo.controller;

import com.example.demo.model.GoldRate;
import com.example.demo.service.GoldRateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GoldRateController {

    private final GoldRateService goldRateService;

    public GoldRateController(GoldRateService goldRateService) {
        this.goldRateService = goldRateService;
    }

    // Return the latest saved gold rate.
    // Fast response: does not force a GoldAPI request.
    @GetMapping("/gold-rate")
    public GoldRate getRate() {
        return goldRateService.getLatestRate();
    }

    // Fetch a fresh rate and save it to PostgreSQL.
    // Only administrators can trigger this operation.
    @PostMapping("/gold-rate/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public GoldRate refreshRate() {
        return goldRateService.updateGoldRate();
    }
}
