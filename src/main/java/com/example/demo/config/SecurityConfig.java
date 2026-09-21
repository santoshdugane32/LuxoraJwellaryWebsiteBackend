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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // ==========================
                        // PUBLIC APIs
                        // ==========================
                        .requestMatchers(
                                "/error",
                                "/api/users/login",
                                "/api/users/register",
                                "/api/products/**",
                                "/api/product-price/**",
                                "/api/gold-rate/**"
                        ).permitAll()

                        // ==========================
                        // TEMPORARY (DEBUG ONLY)
                        // ==========================
                        .requestMatchers("/api/payment/create-order")
                        .permitAll()

                        // ==========================
                        // ADMIN APIs
                        // ==========================
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // ==========================
                        // AUTHENTICATED APIs
                        // ==========================
                        .requestMatchers(
                                "/api/orders/**",
                                "/api/cart/**",
                                "/api/payment/**"
                        )
                        .authenticated()

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}