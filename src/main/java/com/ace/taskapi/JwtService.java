package com.ace.taskapi;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    // Reads JWT_SECRET from environment variable (Railway) or application.properties (local)
    @Value("${jwt.secret}")
    private String secret;

    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    // ── Generate token ────────────────────────────────────────
    // Called on successful login — creates a signed token containing the username.
    // The token expires after 24 hours.
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Extract username ──────────────────────────────────────
    // Reads the username out of a valid token.
    // Called by JwtFilter to identify who is making the request.
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    // ── Validate token ────────────────────────────────────────
    // Returns true if the token belongs to the given username and has not expired.
    public boolean isTokenValid(String token, String username) {
        try {
            String extractedUsername = extractUsername(token);
            return extractedUsername.equals(username) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // ── Private helpers ───────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}