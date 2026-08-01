package com.watchparty.backend.config;

import com.watchparty.backend.security.JsonAuthEntryPoint;
import com.watchparty.backend.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Login is now REQUIRED to create or join a room - /api/rooms/** demands a
 * valid JWT. Only /api/auth/** (register/login), /api/health/**, and the
 * WebSocket handshake itself stay open. Once connected over WebSocket,
 * identity is still tracked via the userId issued at join time (see
 * RoomWebSocketController) rather than the JWT, but that userId can now
 * only ever have been handed out to a logged-in account, since REST
 * create/join is the only place userIds are minted.
 *
 * AuthService checks passwords directly against the stored BCrypt hash
 * (see AuthService), so we don't need Spring's UserDetailsService /
 * AuthenticationManager machinery here - just the encoder and the filter
 * chain.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JsonAuthEntryPoint jsonAuthEntryPoint;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, JsonAuthEntryPoint jsonAuthEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jsonAuthEntryPoint = jsonAuthEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jsonAuthEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/ws/**").permitAll() // STOMP handshake
                        .requestMatchers("/api/rooms/**").authenticated() // login required
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
