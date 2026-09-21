package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String category; // ring, chain, coin

    private String imageUrl;

    private Integer stock;

    // ⭐ NEW IMPORTANT FIELDS
    private Integer purity;
    private Double weightInGrams;          // e.g. 10g
    private Double makingChargePercent;    // e.g. 8% - 15%
}
