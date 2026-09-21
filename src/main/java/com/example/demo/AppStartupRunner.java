package com.example.demo;

import com.example.demo.service.GoldRateService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppStartupRunner implements CommandLineRunner {

    private final GoldRateService service;

    public AppStartupRunner(GoldRateService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        try {
            service.updateGoldRate();
            System.out.println("🔥 Gold rate warmed up at startup");
        } catch (Exception e) {
            System.out.println("❌ Warm-up failed: " + e.getMessage());
        }
    }
}
