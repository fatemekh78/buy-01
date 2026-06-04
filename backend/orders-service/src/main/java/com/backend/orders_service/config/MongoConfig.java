package com.backend.orders_service.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Configuration for MongoDB.
 * Enables automatic population of @CreatedDate and @LastModifiedDate fields on documents.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("system");
    }
}