package com.backend.orders_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.backend.common.config.filter.GatewayHeadersFilter;

import lombok.RequiredArgsConstructor;

/**
 * Security configuration for the Orders Service.
 * Relies on the API Gateway for JWT validation and uses the GatewayHeadersFilter
 * to populate the local Spring Security context.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayHeadersFilter gatewayHeadersFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                
                // Endpoints permitted here because the API Gateway acts as the secure front door.
                // Method-level security (@PreAuthorize) will enforce specific role access in controllers.
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(gatewayHeadersFilter, UsernamePasswordAuthenticationFilter.class);
                
        return http.build();
    }
}