package com.backend.orders_service.client;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.backend.common.exception.CustomException;
import com.backend.orders_service.model.OrderItem;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductInventoryClient {

    private final WebClient.Builder webClientBuilder;

    // 🚨 FIX: Standardized internal Eureka HTTP routing
    private static final String PRODUCT_SERVICE_URL = "http://product-service";

    public void decreaseStock(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<StockAdjustmentRequest> payload = items.stream()
                .map(item -> new StockAdjustmentRequest(item.getProductId(), item.getQuantity()))
                .toList();

        try {
            webClientBuilder.build()
                    .post()
                    .uri(PRODUCT_SERVICE_URL + "/api/products/adjust-stock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Successfully decreased stock for {} distinct items", payload.size());

        } catch (WebClientResponseException e) {
            log.error("Failed to decrease stock: {}", e.getResponseBodyAsString());
            // Re-throw so the OrderService can catch and format the exact product service
            // error
            throw e;
        }
    }

    public void increaseStock(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<StockAdjustmentRequest> payload = items.stream()
                .map(item -> new StockAdjustmentRequest(item.getProductId(), item.getQuantity()))
                .toList();

        try {
            webClientBuilder.build()
                    .post()
                    .uri(PRODUCT_SERVICE_URL + "/api/products/restock")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            log.info("Successfully restocked {} distinct items", payload.size());

        } catch (WebClientResponseException e) {
            log.error("Failed to increase stock during cancellation: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * Get product details including current quantity available.
     */
    public ProductDetail getProductDetails(String productId) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(PRODUCT_SERVICE_URL + "/api/products/{productId}", productId)
                    .header("X-User-ID", "system-service") // Bypasses auth requirements for internal traffic
                    .retrieve()
                    .bodyToMono(ProductDetail.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Failed to get details for product {}: {}", productId, e.getResponseBodyAsString());
            throw new CustomException("Failed to get product details for ID: " + productId,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ────────────────────────────────────────────────────────────────
    // INNER DTOs
    // ────────────────────────────────────────────────────────────────

    private record StockAdjustmentRequest(String productId, int quantity) {
    }

    @Data
    public static class ProductDetail {
        @JsonProperty("productId")
        private String productId;

        private String name;
        private String description;
        private Double price;
        private Integer quantity;
        private String sellerFirstName;
        private String sellerLastName;
        private String sellerEmail;
    }
}