package com.example.demo.config;

import com.example.demo.Security.JwtFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // ==========================================
                // CORS
                // ==========================================
                .cors(Customizer.withDefaults())

                // ==========================================
                // CSRF
                // ==========================================
                .csrf(csrf -> csrf.disable())

                // ==========================================
                // SESSION
                // ==========================================
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // ==========================================
                // AUTHORIZATION
                // ==========================================
                .authorizeHttpRequests(auth -> auth

                        // ----------------------------------
                        // PUBLIC APIs
                        // ----------------------------------
                        .requestMatchers(
                                "/error",
                                "/api/users/login",
                                "/api/users/register",
                                "/api/products/**",
                                "/api/product-price/**",
                                "/api/gold-rate/**"
                        )
                        .permitAll()

                        // ----------------------------------
                        // ADMIN APIs
                        // ----------------------------------
                        .requestMatchers(
                                "/api/admin/**"
                        )
                        .hasRole("ADMIN")

                        // ----------------------------------
                        // AUTHENTICATED APIs
                        // ----------------------------------
                        .requestMatchers(
                                "/api/orders/**",
                                "/api/cart/**",
                                "/api/payment/**"
                        )
                        .authenticated()

                        // ----------------------------------
                        // EVERYTHING ELSE
                        // ----------------------------------
                        .anyRequest()
                        .authenticated()
                )

                // ==========================================
                // JWT FILTER
                // ==========================================
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}