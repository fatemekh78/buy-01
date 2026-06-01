package com.backend.media_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Configuration for MongoDB.
 * Enables auditing to automatically populate @CreatedDate and @LastModifiedDate
 * fields on documents.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // Configuration intentionally left blank; annotation handles auditing setup.
}