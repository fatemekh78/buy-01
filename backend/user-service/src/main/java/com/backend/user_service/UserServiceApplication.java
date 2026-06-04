package com.backend.user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Main entry point for the User Microservice.
 * Responsible for managing user identities, profiles, authentication generation,
 * and seller platform registrations.
 * * Component scans include the 'com.backend.common' module to import shared
 * DTOs, Exceptions, and utilities.
 */
@SpringBootApplication
@ComponentScan(basePackages = { "com.backend.user_service", "com.backend.common" })
@EnableMongoRepositories(basePackages = { "com.backend.user_service.repository", "com.backend.common.repository" })
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}