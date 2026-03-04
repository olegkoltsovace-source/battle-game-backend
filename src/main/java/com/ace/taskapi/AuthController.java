package com.ace.taskapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    // ── Register ──────────────────────────────────────────────
    // Validates input, checks for duplicate username, hashes the password
    // with BCrypt, saves the user, and returns a JWT so the user is
    // immediately logged in after registering.
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (password == null || password.length() < 3) {
            throw new IllegalArgumentException("Password must be at least 3 characters");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Hash the password before saving — the plain text password never touches the database
        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(username, hashedPassword);
        userRepository.save(user);

        // Generate JWT so the user is logged in immediately after registering
        String token = jwtService.generateToken(username);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("message", "Registration successful");

        return ResponseEntity.ok(response);
    }

    // ── Login ─────────────────────────────────────────────────
    // Finds the user by username, verifies the password against the BCrypt hash,
    // and returns a JWT on success.
    // We return the same error message for both "user not found" and "wrong password"
    // intentionally — telling an attacker which one is correct is a security risk.
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        // BCrypt compares the typed password against the stored hash
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtService.generateToken(username);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("totalWins", user.getTotalWins());
        response.put("totalLosses", user.getTotalLosses());
        response.put("message", "Login successful");

        return ResponseEntity.ok(response);
    }
}