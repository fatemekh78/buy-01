package com.backend.user_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.backend.common.config.filter.GatewayHeadersFilter;

/**
 * Core security configuration for the User Service.
 * Note: This service sits strictly behind the API Gateway. The Gateway handles
 * all JWT
 * cryptographic validation. This configuration simply enforces that the
 * Gateway's injected
 * headers (X-User-ID, etc.) are parsed into a Spring Security Context via the
 * GatewayHeadersFilter.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final GatewayHeadersFilter gatewayHeadersFilter;
    private final AuthenticationEntryPoint customAuthEntryPoint;

    // Constructor Injection (Enterprise Standard)
    public SecurityConfig(GatewayHeadersFilter gatewayHeadersFilter, AuthenticationEntryPoint customAuthEntryPoint) {
        this.gatewayHeadersFilter = gatewayHeadersFilter;
        this.customAuthEntryPoint = customAuthEntryPoint;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint(customAuthEntryPoint))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(gatewayHeadersFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}