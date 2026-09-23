package com.example.demo.controller;

import com.example.demo.Security.JwtUtil;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;


    // =====================================================
    // REGISTER
    // =====================================================

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        // -----------------------------------------
        // Check if email already exists
        // -----------------------------------------

        Optional<User> existingUser =
                userRepository.findByEmail(user.getEmail());

        if (existingUser.isPresent()) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Email already registered");
        }


        // -----------------------------------------
        // Public registration is ALWAYS USER
        // -----------------------------------------

        user.setRole("USER");


        // -----------------------------------------
        // Encrypt password
        // -----------------------------------------

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );


        // -----------------------------------------
        // Save user
        // -----------------------------------------

        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(savedUser);
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {


        // -----------------------------------------
        // Check email
        // -----------------------------------------

        Optional<User> optionalUser =
                userRepository.findByEmail(request.getEmail());


        if (optionalUser.isEmpty()) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid Email or Password");
        }


        User user = optionalUser.get();


        // -----------------------------------------
        // Check password
        // -----------------------------------------

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid Email or Password");
        }


        // -----------------------------------------
        // Check role
        // -----------------------------------------

        if (request.getRole() == null ||
                user.getRole() == null ||
                !request.getRole()
                        .equalsIgnoreCase(user.getRole())) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid Role");
        }


        // -----------------------------------------
        // Generate JWT
        // -----------------------------------------

        String token =
                jwtUtil.generateToken(user.getEmail());


        // -----------------------------------------
        // Login Response
        // -----------------------------------------

        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                token,
                "Login Successful"
        );


        return ResponseEntity.ok(response);
    }
}