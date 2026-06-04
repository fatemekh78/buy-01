package com.backend.api_gateway.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import lombok.extern.slf4j.Slf4j;

/**
 * Global reactive security configuration for the API Gateway.
 */
@Slf4j
@Configuration
public class SecurityConfig {

    /**
     * Configures the primary WebFlux security chain.
     * Note: Authorization is purposefully delegated to the custom
     * AuthenticationGatewayFilterFactory
     * on a per-route basis via application.yml, allowing the main chain to permit
     * exchanges through.
     *
     * @param http The ServerHttpSecurity builder.
     * @return The configured SecurityWebFilterChain.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        log.info("Initializing reactive SecurityWebFilterChain for API Gateway.");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(withDefaults())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/api/auth/**").permitAll()
                        .anyExchange().permitAll())
                .build();
    }
}