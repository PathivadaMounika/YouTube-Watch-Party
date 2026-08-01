package com.watchparty.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the Authorization header if present and, when the token is valid,
 * populates the SecurityContext so controllers can see who's calling via
 * @AuthenticationPrincipal / SecurityContextHolder.
 *
 * Deliberately permissive: a missing or invalid token is NOT rejected here.
 * Most of this app's endpoints (create/join room) are open to guests: it's
 * SecurityConfig's job to decide which specific endpoints require a
 * logged-in principal, not this filter's.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            jwtService.parseToken(token).ifPresent(principal -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        filterChain.doFilter(request, response);
    }
}
