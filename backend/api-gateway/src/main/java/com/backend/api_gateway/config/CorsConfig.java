package com.backend.api_gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * Centralized Cross-Origin Resource Sharing (CORS) configuration.
 * Ensures the Angular frontend can securely communicate with the Gateway.
 */
@Slf4j
@Configuration
public class CorsConfig {

    /**
     * Registers the CORS web filter with the highest precedence to ensure 
     * preflight OPTIONS requests are handled before authentication checks.
     *
     * @return A configured CorsWebFilter instance.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        log.info("Configuring CORS policy for allowed origins and headers.");

        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Arrays.asList(
                "https://localhost:4200",
                "http://localhost:4200"
        ));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Ensure custom X-User headers are permitted to pass through the gateway boundaries
        corsConfig.setAllowedHeaders(Arrays.asList(
                "Origin", "Content-Type", "Accept", "Authorization",
                "X-Requested-With", "X-User-Email", "X-User-ID", "X-User-Role"
        ));
        corsConfig.setExposedHeaders(Arrays.asList(
                "Authorization", "X-User-Email", "X-User-ID", "X-User-Role"
        ));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}