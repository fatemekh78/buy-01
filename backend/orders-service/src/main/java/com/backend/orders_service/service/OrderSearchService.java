package com.backend.orders_service.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.orders_service.dto.SellerOrderDTO;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified service for order search and filtering.
 * Handles both client (user) and seller order searches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSearchService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;

    private static final String PRODUCT_SERVICE_URL = "http://product-service";

    // 🚨 FIX: Upgraded to ConcurrentHashMap for Thread-Safe Singleton access
    private final Map<String, Map<String, Object>> productCache = new ConcurrentHashMap<>();

    // ==================== CLIENT ORDER SEARCH ====================

    public Page<Order> searchAndFilterOrders(
            String userId, String keyword, Double minPrice, Double maxPrice,
            String minUpdateDate, String maxUpdateDate, List<OrderStatus> statuses,
            int page, int size) {

        log.info(
                "Searching client orders for userId: {} - keyword: {}, priceRange: {}-{}, dateRange: {} to {}, statuses: {}",
                userId, keyword, minPrice, maxPrice, minUpdateDate, maxUpdateDate, statuses);

        if (!OrderSearchValidation.validateSearchParameters(minPrice, maxPrice, minUpdateDate, maxUpdateDate)) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")), 0);
        }

        Instant[] dateRange = OrderSearchValidation.parseDateRange(minUpdateDate, maxUpdateDate);
        Instant parsedMinDate = dateRange != null ? dateRange[0] : null;
        Instant parsedMaxDate = dateRange != null ? dateRange[1] : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        // Leverages the highly optimized MongoDB Aggregation Pipeline we built earlier
        Page<Order> filteredOrders = orderRepository.searchAndFilterOrdersByUser(
                userId, keyword, minPrice, maxPrice, parsedMinDate, parsedMaxDate, statuses, pageable);

        log.info("Found {} client orders matching criteria (out of {} total)",
                filteredOrders.getNumberOfElements(), filteredOrders.getTotalElements());

        return filteredOrders;
    }

    // ==================== SELLER ORDER SEARCH ====================

    public Page<SellerOrderDTO> searchAndFilterSellerOrders(
            String sellerId, String keyword, Double minPrice, Double maxPrice,
            String minUpdateDate, String maxUpdateDate, List<OrderStatus> statuses,
            int page, int size) {

        log.info("Searching seller orders for sellerId: {}, keyword: {}, price: {}-{}, dates: {} to {}, statuses: {}",
                sellerId, keyword, minPrice, maxPrice, minUpdateDate, maxUpdateDate, statuses);

        if (!OrderSearchValidation.validateSearchParameters(minPrice, maxPrice, minUpdateDate, maxUpdateDate)) {
            log.warn("Invalid search parameters - returning empty results");
            return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
        }

        Instant[] dateRange = OrderSearchValidation.parseDateRange(minUpdateDate, maxUpdateDate);
        Instant parsedMinDate = dateRange != null ? dateRange[0] : null;
        Instant parsedMaxDate = dateRange != null ? dateRange[1] : null;

        Pageable pageable = PageRequest.of(page, size);

        Page<Order> dbFilteredOrders = orderRepository.searchAndFilterOrdersForSeller(
                keyword, minPrice, maxPrice, parsedMinDate, parsedMaxDate, statuses, pageable);

        log.info("Database Query Result: {} orders fetched at page {}", dbFilteredOrders.getNumberOfElements(), page);

        // 🚨 FIX: Streamlined Java Streams for maximum readability and efficiency
        List<SellerOrderDTO> filteredOrders = dbFilteredOrders.getContent().stream()
                .map(order -> convertToSellerOrderDTO(order, sellerId))
                .filter(dto -> {
                    boolean hasItems = !dto.getItems().isEmpty();
                    if (!hasItems)
                        log.debug("Filtering out order {} - no items belong to seller {}", dto.getId(), sellerId);
                    return hasItems;
                })
                .filter(dto -> matchesSellerPriceRange(dto, minPrice, maxPrice))
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .toList();

        log.info("Final Result: {} seller orders returned after memory-level isolation", filteredOrders.size());

        // Note: The pagination leak warning still applies here, but this is the safest
        // approach without radically altering the database schema for multi-vendor
        // carts.
        return new PageImpl<>(filteredOrders, pageable, dbFilteredOrders.getTotalElements());
    }

    // ==================== SELLER ORDER HELPER METHODS ====================

    public SellerOrderDTO convertToSellerOrderDTO(Order order, String sellerId) {
        log.debug("Converting order {} for seller {}", order.getId(), sellerId);

        List<OrderItem> sellerItems = order.getItems().stream()
                .filter(item -> {
                    String resolvedItemSellerId = resolveItemSellerId(item);
                    return sellerId.equals(resolvedItemSellerId);
                })
                .collect(Collectors.toList());

        return SellerOrderDTO.builder()
                .id(order.getId())
                .orderId(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .items(sellerItems)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private String resolveItemSellerId(OrderItem item) {
        String itemSellerId = item.getSellerId();

        if (itemSellerId == null || itemSellerId.isBlank()) {
            log.debug("Item {} lacks stored sellerId. Falling back to Product Service API...", item.getProductId());
            return getProductSellerId(item.getProductId());
        }

        return itemSellerId;
    }

    private String getProductSellerId(String productId) {
        try {
            Map<String, Object> product = getProductDetails(productId);
            if (product != null && product.containsKey("sellerID")) {
                return product.get("sellerID").toString();
            }
        } catch (Exception e) {
            log.warn("Could not fetch sellerID for product {}: {}", productId, e.getMessage());
        }
        return null;
    }

    private Map<String, Object> getProductDetails(String productId) {
        if (productCache.containsKey(productId)) {
            log.debug("CACHE HIT: Found product {} in memory", productId);
            return productCache.get(productId);
        }

        try {
            log.debug("CACHE MISS: Fetching product {} from {}", productId, PRODUCT_SERVICE_URL);
            String url = PRODUCT_SERVICE_URL + "/api/products/simple/" + productId;

            // 🚨 FIX: Safely deserializing the JSON to a ParameterizedTypeReference
            Map<String, Object> response = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            if (response != null) {
                productCache.put(productId, response);
                return response;
            }
        } catch (Exception e) {
            log.warn("Product Service fetch failed for {}: {}", productId, e.getMessage());
        }
        return null;
    }

    private boolean matchesSellerPriceRange(SellerOrderDTO order, Double minPrice, Double maxPrice) {
        if (minPrice == null && maxPrice == null) {
            return true;
        }

        double sellerTotal = order.getItems().stream()
                .mapToDouble(item -> item.getPrice().doubleValue() * item.getQuantity())
                .sum();

        if (minPrice != null && sellerTotal < minPrice) {
            log.debug("Order {} total ({}) is below minimum ({})", order.getId(), sellerTotal, minPrice);
            return false;
        }

        if (maxPrice != null && sellerTotal > maxPrice) {
            log.debug("Order {} total ({}) is above maximum ({})", order.getId(), sellerTotal, maxPrice);
            return false;
        }

        return true;
    }

    // ==================== SELLER ORDER SERVICE METHODS ====================

    public Page<SellerOrderDTO> getSellerOrders(String sellerId, int page, int size) {
        log.info("Fetching unfiltered orders for seller: {}", sellerId);
        return searchAndFilterSellerOrders(sellerId, null, null, null, null, null, null, page, size);
    }

    public SellerOrderDTO getSellerOrderDetail(String orderId, String sellerId) {
        log.info("Fetching order detail {} for seller {}", orderId, sellerId);

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order {} not found", orderId);
            return null;
        }

        SellerOrderDTO sellerOrderDTO = convertToSellerOrderDTO(order, sellerId);

        if (sellerOrderDTO.getItems().isEmpty()) {
            log.warn("UNAUTHORIZED: Seller {} has NO items in order {}", sellerId, orderId);
            return null;
        }

        return sellerOrderDTO;
    }
}