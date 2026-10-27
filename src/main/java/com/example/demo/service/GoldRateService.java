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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GoldRateService {

    private static final BigDecimal TROY_OUNCE_GRAMS =
            new BigDecimal("31.1034768");

    private static final BigDecimal PURITY_22K =
            new BigDecimal("22")
                    .divide(new BigDecimal("24"), 8, RoundingMode.HALF_UP);

    private final GoldRateRepository repo;
    private WebClient webClient;

    private volatile GoldRate cachedRate;
    private volatile LocalDateTime cacheTime;

    @Value("${goldapi.key:}")
    private String apiKey;

    public GoldRateService(GoldRateRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void init() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> conn
                        .addHandlerLast(
                                new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                        .addHandlerLast(
                                new WriteTimeoutHandler(5, TimeUnit.SECONDS)));

        this.webClient = WebClient.builder()
                .baseUrl("https://www.goldapi.io/api")
                .clientConnector(
                        new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/json")
                .build();
    }

    // Fetch a fresh rate and save it to PostgreSQL.
    public synchronized GoldRate updateGoldRate() {
        try {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException(
                        "GoldAPI key is missing.");
            }

            Map<?, ?> response = webClient.get()
                    .uri("/price/XAU/INR")
                    .header("x-access-token", apiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new IllegalStateException(
                        "GoldAPI returned an empty response.");
            }

            BigDecimal gold24k = extract24kPricePerGram(response);

            // 22K is calculated from 24K purity.
            BigDecimal gold22k = gold24k
                    .multiply(PURITY_22K);

            long gold24kRounded = gold24k
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();

            long gold22kRounded = gold22k
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();

            GoldRate rate = new GoldRate();
            rate.setGold24k(gold24kRounded);
            rate.setGold22k(gold22kRounded);
            rate.setUpdatedAt(LocalDateTime.now());

            GoldRate saved = repo.save(rate);

            cachedRate = saved;
            cacheTime = LocalDateTime.now();

            System.out.println(
                    "Gold market rate saved. 24K: ₹"
                            + gold24kRounded
                            + "/gram, 22K: ₹"
                            + gold22kRounded + "/gram");

            return saved;

        } catch (WebClientResponseException e) {
            System.err.println(
                    "GoldAPI HTTP error: " + e.getStatusCode());

            return getDatabaseFallback(e);

        } catch (Exception e) {
            System.err.println(
                    "Gold rate update failed: "
                            + e.getClass().getSimpleName()
                            + ": " + e.getMessage());

            return getDatabaseFallback(e);
        }
    }

    private BigDecimal extract24kPricePerGram(Map<?, ?> response) {

        // Format 1: Direct price per gram.
        Object directPrice = response.get("price_gram_24k");

        if (directPrice != null) {
            return positivePrice(directPrice);
        }

        // Format 2: price_per_unit.gram.
        Object perUnit = response.get("price_per_unit");

        if (perUnit instanceof Map<?, ?> unitMap) {
            Object gramPrice = unitMap.get("gram");

            if (gramPrice != null) {
                return positivePrice(gramPrice);
            }
        }

        // Format 3: melt_price_per_gram.24k.
        Object meltPrice = response.get("melt_price_per_gram");

        if (meltPrice instanceof Map<?, ?> meltMap) {
            Object karat24 = meltMap.get("24k");

            if (karat24 == null) {
                karat24 = meltMap.get("24K");
            }

            if (karat24 != null) {
                return positivePrice(karat24);
            }
        }

        // Format 4: Convert a troy-ounce price to a gram price.
        Object ouncePrice = response.get("price");
        Object unit = response.get("unit");

        if (ouncePrice != null && unit != null
                && unit.toString().toLowerCase().contains("ounce")) {

            return positivePrice(ouncePrice)
                    .divide(
                            TROY_OUNCE_GRAMS,
                            8,
                            RoundingMode.HALF_UP);
        }

        throw new IllegalStateException(
                "No recognized 24K price per gram found. "
                        + "Check GoldAPI response fields and unit.");
    }

    private BigDecimal positivePrice(Object value) {
        BigDecimal price = new BigDecimal(value.toString());

        if (price.signum() <= 0) {
            throw new IllegalStateException(
                    "GoldAPI returned a non-positive price.");
        }

        return price;
    }

    private GoldRate getDatabaseFallback(Exception cause) {
        return repo.findTopByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException(
                        "GoldAPI failed and no historical rate exists.",
                        cause));
    }

    // Return the latest saved rate without calling GoldAPI every time.
    public GoldRate getLatestRate() {
        GoldRate rate = cachedRate;
        LocalDateTime time = cacheTime;

        if (rate != null && time != null
                && time.isAfter(
                LocalDateTime.now().minusMinutes(10))) {
            return rate;
        }

        return repo.findTopByOrderByIdDesc()
                .orElseGet(this::updateGoldRate);
    }
}