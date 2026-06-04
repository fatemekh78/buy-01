package com.backend.api_gateway;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 * Integration test to verify that the Spring Cloud API Gateway context 
 * successfully bootstraps without dependency injection failures or configuration errors.
 */
@SpringBootTest
class ApiGatewayApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Verifies that the Spring application context loads correctly.
     * The assertion ensures SonarQube registers this as a valid test execution.
     */
    @Test
    void contextLoads() {
        Assertions.assertNotNull(applicationContext, "The application context should not be null.");
    }
}