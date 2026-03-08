package com.ace.taskapi;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    // ── doFilterInternal ──────────────────────────────────────
    // Runs once per request. Checks the Authorization header for a Bearer token.
    // If valid, marks the request as authenticated so Spring Security lets it through.
    // If missing or invalid, the request continues unauthenticated —
    // SecurityConfig then decides whether to allow or reject it.
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No token present — pass the request along, SecurityConfig handles the rest
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the token — strip the "Bearer " prefix
        final String token = authHeader.substring(7);

        try {
            final String username = jwtService.extractUsername(token);

            // Only authenticate if we have a username and no authentication exists yet
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails.getUsername())) {
                    // Token is valid — create an authentication object and set it in the context
                    // This tells Spring Security the request is authenticated
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    Collections.emptyList()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Token expired — continue unauthenticated, SecurityConfig will reject protected routes
        } catch (Exception e) {
            // Any other token parsing error — treat as unauthenticated
        }

        // Continue the request — SecurityConfig will allow or reject based on auth status
        filterChain.doFilter(request, response);
    }
}