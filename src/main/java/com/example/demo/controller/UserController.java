package com.example.demo.controller;

import com.example.demo.Security.JwtUtil;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public UserController(
            UserRepository userRepository,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder,
            UserService userService) {

        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {

        User savedUser = userService.register(user);

        // Never return the password hash to the frontend.
        savedUser.setPassword(null);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedUser);
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        // 1. Validate required fields
        if (request == null
                || request.getEmail() == null
                || request.getEmail().isBlank()
                || request.getPassword() == null
                || request.getPassword().isEmpty()
                || request.getRole() == null
                || request.getRole().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email, password and role are required"
            );
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        String selectedRole = request.getRole()
                .trim()
                .toUpperCase(Locale.ROOT);

        // 2. Accept only supported roles
        if (!selectedRole.equals("USER")
                && !selectedRole.equals("ADMIN")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid role selected"
            );
        }

        // 3. Find account by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        // 4. Verify password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        // 5. Verify selected role against the database role
        String actualRole = user.getRole();

        if (actualRole == null
                || !selectedRole.equalsIgnoreCase(actualRole.trim())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Selected role does not match this account"
            );
        }

        // 6. Generate JWT only after all checks pass
        String token = jwtUtil.generateToken(user.getEmail());

        // 7. Return the actual database role
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
