package com.backend.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.backend.api_gateway.util.JwtUtil;

/**
 * Configuration class responsible for explicitly registering custom
 * GatewayFilterFactory beans.
 * Ensures that programmatic filters are properly injected into the Spring
 * context for route resolution.
 */
@Configuration
public class GatewayConfig {

    private final JwtUtil jwtUtil;

    public GatewayConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Registers the AuthenticationGatewayFilterFactory.
     * Spring Cloud Gateway dynamically maps this bean to the "Authentication"
     * filter specified in routing rules.
     *
     * @return The configured AuthenticationGatewayFilterFactory.
     */
    @Bean
    public AuthenticationGatewayFilterFactory authenticationGatewayFilterFactory() {
        return new AuthenticationGatewayFilterFactory(jwtUtil);
    }
}