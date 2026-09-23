package com.example.demo.Security;

import com.example.demo.service.CustomUserDetailsService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {


        System.out.println("\n==============================");
        System.out.println(
                "REQUEST : "
                        + request.getMethod()
                        + " "
                        + request.getRequestURI()
        );


        // ==========================================
        // GET AUTHORIZATION HEADER
        // ==========================================

        String authHeader =
                request.getHeader("Authorization");

        System.out.println(
                "AUTH HEADER : " + authHeader
        );


        // ==========================================
        // CHECK BEARER TOKEN
        // ==========================================

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            System.out.println(
                    "No Bearer Token Found"
            );

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }


        // ==========================================
        // EXTRACT TOKEN
        // ==========================================

        String token =
                authHeader.substring(7);

        System.out.println(
                "TOKEN : " + token
        );


        try {


            // ==========================================
            // EXTRACT EMAIL
            // ==========================================

            String email =
                    jwtUtil.extractEmail(token);

            System.out.println(
                    "EMAIL FROM TOKEN : " + email
            );


            // ==========================================
            // CHECK SECURITY CONTEXT
            // ==========================================

            if (email != null &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {


                // ==========================================
                // LOAD USER FROM DATABASE
                // ==========================================

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(email);


                System.out.println(
                        "USER FOUND : "
                                + userDetails.getUsername()
                );


                System.out.println(
                        "AUTHORITIES : "
                                + userDetails.getAuthorities()
                );


                // ==========================================
                // VALIDATE JWT
                // ==========================================

                if (jwtUtil.validateToken(
                        token,
                        userDetails
                )) {


                    // ==========================================
                    // CREATE AUTHENTICATION
                    // ==========================================

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );


                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );


                    // ==========================================
                    // SET SECURITY CONTEXT
                    // ==========================================

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authentication
                            );


                    System.out.println(
                            "JWT AUTHENTICATION SUCCESS"
                    );
                }
            }


            filterChain.doFilter(
                    request,
                    response
            );


        } catch (ExpiredJwtException ex) {

            ex.printStackTrace();

            SecurityContextHolder.clearContext();

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );

            response.getWriter().write("""
                    {
                      "message": "JWT Token Expired"
                    }
                    """);


        } catch (JwtException ex) {

            ex.printStackTrace();

            SecurityContextHolder.clearContext();

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );

            response.getWriter().write("""
                    {
                      "message": "Invalid JWT Token"
                    }
                    """);


        } catch (Exception ex) {

            ex.printStackTrace();

            SecurityContextHolder.clearContext();

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );

            response.getWriter().write("""
                    {
                      "message": "Authentication Failed"
                    }
                    """);
        }
    }
}