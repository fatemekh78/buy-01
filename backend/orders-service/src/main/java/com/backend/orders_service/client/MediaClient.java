package com.backend.orders_service.client;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaClient {

    private final WebClient.Builder webClientBuilder;

    /**
     * Get the first image URL for a product by calling the media-service /urls
     * endpoint.
     */
    public String getFirstImageUrl(String productId) {
        try {
            // 🚨 FIX: Changed from HTTPS to HTTP to match media-service configuration
            String url = "http://media-service/api/media/product/" + productId + "/urls?limit=1";

            List<String> urls = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {
                    })
                    .block();

            if (urls != null && !urls.isEmpty()) {
                return urls.get(0);
            }

            return null;

        } catch (Exception e) {
            // 🚨 FIX: Replaced System.err with proper non-blocking SLF4J logging
            log.warn("Error fetching image URL for product {}: {}", productId, e.getMessage());
            return null;
        }
    }
}