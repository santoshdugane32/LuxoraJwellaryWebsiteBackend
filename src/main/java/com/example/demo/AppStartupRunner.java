package com.example.demo;

import com.example.demo.service.GoldRateService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppStartupRunner implements CommandLineRunner {

    private static final Logger logger =
            LoggerFactory.getLogger(AppStartupRunner.class);

    private final GoldRateService service;

    public AppStartupRunner(GoldRateService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        try {
            service.updateGoldRate();

            logger.info("Gold rate warmed up successfully at startup.");

        } catch (Exception e) {
            logger.error(
                    "Gold rate warm-up failed at startup: {}",
                    e.getMessage(),
                    e
            );
        }
    }
}
