package com.example.demo.Scheduler;

import com.example.demo.service.GoldRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GoldRateScheduler {

    @Autowired
    private GoldRateService service;

    // 🔥 Runs every 1 hour 0th minutes(production safe)
    @Scheduled(cron = "0 0 * * * *")
    public void updateRate() {

        try {
            service.updateGoldRate();
            System.out.println("✅ Gold rate updated successfully!");
        } catch (Exception e) {
            System.out.println("❌ Error updating gold rate: " + e.getMessage());
        }
    }
}