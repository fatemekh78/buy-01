package com.backend.product_service.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.backend.product_service.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletionListener {

    private final ProductService productService;

    @KafkaListener(topics = "user-deleted-topic", groupId = "product-service-group")
    public void handleUserDeleted(String userId) {
        log.info("Received user deletion event for ID: {}", userId);
        // Triggers the cascading deletion of all products owned by this user
        productService.DeleteProductsOfUser(userId);
    }
}