
package com.example.demo.Scheduler;

import com.example.demo.service.GoldRateService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GoldRateScheduler {

    private final GoldRateService service;

    public GoldRateScheduler(GoldRateService service) {
        this.service = service;
    }

    // Runs every day at midnight Indian Standard Time
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    public void updateRate() {
        try {
            service.updateGoldRate();
            System.out.println("Gold rate scheduler executed.");
        } catch (Exception e) {
            System.err.println(
                    "Gold rate update failed: " + e.getMessage()
            );
        }
    }
}
