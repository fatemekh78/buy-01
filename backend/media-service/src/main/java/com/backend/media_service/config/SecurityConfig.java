package com.backend.media_service.config;

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
 * Security configuration for the Media Service.
 * Relies on the API Gateway for primary JWT validation and uses 
 * GatewayHeadersFilter to establish local Spring Security context.
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
                
                // All endpoints are permitted here because the API Gateway acts as the secure front door.
                // Method-level security (@PreAuthorize) will still apply where specified in controllers.
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // Inject the custom filter to translate Gateway headers into the Security Context
                .addFilterBefore(gatewayHeadersFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}