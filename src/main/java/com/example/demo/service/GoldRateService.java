package com.example.demo.service;

import com.example.demo.model.GoldRate;
import com.example.demo.repository.GoldRateRepository;
import jakarta.annotation.PostConstruct;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GoldRateService {

    private final GoldRateRepository repo;
    private WebClient webClient;

    private volatile GoldRate cachedRate;
    private volatile LocalDateTime cacheTime;

    @Value("${goldapi.key}")
    private String apiKey;

    // Dynamically injected external configuration values from application.properties
    @Value("${gold.rate.import-duty:0.06}")
    private BigDecimal importDuty;

    @Value("${gold.rate.hedging-cost:0.02}")
    private BigDecimal hedgingCost;

    @Value("${gold.rate.jeweller-margin:0.04}")
    private BigDecimal jewellerMargin;

    @Value("${gold.rate.demand-spread:0.05}")
    private BigDecimal demandSpread;

    @Value("${gold.rate.making-charges:0.15}")
    private BigDecimal makingCharges;

    @Value("${gold.rate.gst:0.03}")
    private BigDecimal gst;

    public GoldRateService(GoldRateRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void init() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));

        this.webClient = WebClient.builder()
                .baseUrl("https://www.goldapi.io/api")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public synchronized GoldRate updateGoldRate() {
        try {
            Map<?, ?> response = webClient.get()
                    .uri("/XAU/INR")
                    .header("x-access-token", apiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.get("price_gram_24k") == null) {
                throw new RuntimeException("Empty or invalid API response payload.");
            }

            BigDecimal baseGold24kInr = new BigDecimal(response.get("price_gram_24k").toString());

            // ========================================================
            // ADDITIVE MULTIPLIER ENGINE using injected properties
            // ========================================================
            BigDecimal internalStructuralMultiplier = BigDecimal.ONE
                    .add(importDuty)
                    .add(hedgingCost)
                    .add(jewellerMargin)
                    .add(demandSpread)
                    .add(makingCharges);

            // Calculate 24K Subtotal before tax
            BigDecimal subTotal24k = baseGold24kInr.multiply(internalStructuralMultiplier);

            // Apply consumption-side GST (3%) from application.properties
            BigDecimal final24k = subTotal24k.multiply(BigDecimal.ONE.add(gst));

            // Calculate 22K via Indian 916 Bullion standard (Derived from the final 24K rate)
            BigDecimal final22k = final24k.multiply(BigDecimal.valueOf(0.916));

            long gold24k = final24k.setScale(0, RoundingMode.HALF_UP).longValue();
            long gold22k = final22k.setScale(0, RoundingMode.HALF_UP).longValue();

            GoldRate rate = new GoldRate();
            rate.setGold24k(gold24k);
            rate.setGold22k(gold22k);
            rate.setUpdatedAt(LocalDateTime.now());

            GoldRate saved = repo.save(rate);

            this.cachedRate = saved;
            this.cacheTime = LocalDateTime.now();

            return saved;

        } catch (Exception e) {
            System.err.println("WARN: Network connection timeout or API outage encountered. Reverting to fallback. Error: " + e.getMessage());
            return repo.findTopByOrderByIdDesc()
                    .orElseThrow(() -> new RuntimeException("API Connection failed and no historical fallback records found within database.", e));
        }
    }

    public GoldRate getLatestRate() {
        GoldRate tempRate = this.cachedRate;
        LocalDateTime tempTime = this.cacheTime;

        if (tempRate != null && tempTime != null && tempTime.isAfter(LocalDateTime.now().minusMinutes(10))) {
            return tempRate;
        }

        return repo.findTopByOrderByIdDesc()
                .orElseGet(this::updateGoldRate);
    }
}
