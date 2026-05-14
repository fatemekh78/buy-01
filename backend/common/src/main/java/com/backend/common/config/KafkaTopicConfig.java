package com.backend.common.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class responsible for setting up Kafka administration and
 * defining application topics.
 */
@Slf4j
@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    /**
     * Initializes the KafkaAdmin client required to programmatically manage topics.
     *
     * @return KafkaAdmin instance configured with the bootstrap server address.
     * @throws RuntimeException if the Kafka properties cannot be mapped.
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        try {
            Map<String, Object> configs = new HashMap<>();
            configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
            log.info("Initializing KafkaAdmin with bootstrap servers: {}", bootstrapAddress);
            return new KafkaAdmin(configs);
        } catch (Exception e) {
            log.error("Failed to initialize KafkaAdmin: {}", e.getMessage(), e);
            throw new RuntimeException("KafkaAdmin initialization failed", e);
        }
    }

    /**
     * Creates the 'product-deleted-topic' if it does not already exist.
     *
     * @return NewTopic defining the product deletion event stream.
     */
    @Bean
    public NewTopic productDeletedTopic() {
        log.info("Configuring topic: product-deleted-topic");
        return new NewTopic("product-deleted-topic", 1, (short) 1);
    }

    /**
     * Creates the 'user-deleted-topic' if it does not already exist.
     *
     * @return NewTopic defining the user deletion event stream.
     */
    @Bean
    public NewTopic userDeletedTopic() {
        log.info("Configuring topic: user-deleted-topic");
        return new NewTopic("user-deleted-topic", 1, (short) 1);
    }

    /**
     * Creates the 'user-avatar-deleted-topic' if it does not already exist.
     *
     * @return NewTopic defining the user avatar deletion event stream.
     */
    @Bean
    public NewTopic userAvatarDeletedTopic() {
        log.info("Configuring topic: user-avatar-deleted-topic");
        return new NewTopic("user-avatar-deleted-topic", 1, (short) 1);
    }
}