package com.backend.product_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for product search and filtering
 * Uses MongoDB queries for efficient filtering at database level
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final WebClient.Builder webClientBuilder;

    /**
     * Search and filter products with optional criteria
     * If all parameters are null, returns all products
     */
    public Page<ProductCardDTO> searchAndFilter(
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minQuantity,
            Integer maxQuantity,
            Instant startDate,
            Instant endDate,
            String sellerId,
            Pageable pageable) {

        log.info(
                "Searching products - keyword: {}, priceRange: {}-{}, quantityRange: {}-{}, dateRange: {} to {}",
                keyword, minPrice, maxPrice, minQuantity, maxQuantity, startDate, endDate);

        // Convert BigDecimal to Double for MongoDB query
        Double minPriceDouble = minPrice != null ? minPrice.doubleValue() : null;
        Double maxPriceDouble = maxPrice != null ? maxPrice.doubleValue() : null;

        // Use repository query method - filtering happens at database level
        Page<Product> products = productRepository.searchAndFilterProducts(
                keyword,
                minPriceDouble,
                maxPriceDouble,
                minQuantity,
                maxQuantity,
                startDate,
                endDate,
                pageable);

        log.info("Found {} products matching criteria", products.getTotalElements());

        // Convert Product entities to ProductCardDTO using concurrent network calls
        return convertToCardDTOPage(products, sellerId);
    }

    /**
     * Convert Product page to ProductCardDTO page with limited images.
     * Uses Flux.flatMapSequential to fetch images concurrently while preserving sort order.
     */
    private Page<ProductCardDTO> convertToCardDTOPage(Page<Product> productPage, String sellerId) {
        if (productPage.isEmpty()) {
            return Page.empty(productPage.getPageable());
        }

        List<ProductCardDTO> dtoList = Flux.fromIterable(productPage.getContent())
                .flatMapSequential(product -> {
                    boolean isCreator = sellerId != null && product.getSellerID().equals(sellerId);
                    
                    // Return a Mono that combines the product info with the asynchronously fetched images
                    return getLimitedImageUrlsReactive(product.getId(), 3)
                            .map(images -> new ProductCardDTO(
                                    product.getId(),
                                    product.getName(),
                                    product.getDescription(),
                                    product.getPrice(),
                                    product.getQuantity(),
                                    isCreator,
                                    images
                            ));
                })
                .collectList()
                .block(); // Block exactly once for the entire batch of concurrent calls

        return new PageImpl<>(dtoList, productPage.getPageable(), productPage.getTotalElements());
    }

    /**
     * Fetch limited image URLs from Media Service reactively
     * Returns a Mono<List<String>> instead of blocking immediately
     */
    private Mono<List<String>> getLimitedImageUrlsReactive(String productId, int limit) {
        ParameterizedTypeReference<List<String>> listType = new ParameterizedTypeReference<>() {};

        return webClientBuilder.build().get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("MEDIA-SERVICE")
                        .path("/api/media/product/{productId}/urls")
                        .queryParam("limit", limit)
                        .build(productId))
                .retrieve()
                .bodyToMono(listType)
                .onErrorResume(e -> {
                    // Fallback mechanism: if media service fails, log and return empty list
                    log.error("Failed to fetch media URLs for product {}: {}", productId, e.getMessage());
                    return Mono.just(List.of());
                });
    }
}