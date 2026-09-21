package com.example.demo.repository;

import com.example.demo.model.GoldRate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface GoldRateRepository extends JpaRepository<GoldRate, Long> {
    Optional<GoldRate> findTopByOrderByIdDesc();
}
