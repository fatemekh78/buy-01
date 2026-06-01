package com.backend.orders_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for calculating user and seller statistics from orders.
 * Only counts DELIVERED orders for accurate user statistics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatsService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    private static final String PRODUCT_SERVICE_URL = "http://product-service";

    // 🚨 FIX: Upgraded to Thread-Safe ConcurrentHashMap
    private final Map<String, Map<String, Object>> productCache = new ConcurrentHashMap<>();

    // ────────────────────────────────────────────────────────────────
    // USER STATISTICS
    // ────────────────────────────────────────────────────────────────

    public Map<String, Object> calculateUserStats(String userId) {
        try {
            List<Order> userOrders = orderRepository.findByUserIdAndStatusOrderByOrderDateDesc(userId,
                    OrderStatus.DELIVERED);

            if (userOrders == null || userOrders.isEmpty()) {
                return initializeEmptyStats();
            }

            UserStatsAccumulator accumulator = new UserStatsAccumulator();

            for (Order order : userOrders) {
                accumulator.totalOrders++;
                accumulator.updateLastOrderDate(order.getOrderDate());

                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        processUserOrderItem(item, accumulator);
                    }
                }
            }

            return accumulator.toMap();

        } catch (Exception e) {
            log.error("Error calculating user stats for userId: {}", userId, e);
            return initializeEmptyStats();
        }
    }

    // 🚨 FIX: Replaced Reflection with direct object method calls
    private void processUserOrderItem(OrderItem item, UserStatsAccumulator acc) {
        String productId = item.getProductId();
        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;

        // Use historical price if available, fallback to product service
        BigDecimal price = item.getPrice() != null ? item.getPrice() : fetchItemPrice(productId);

        acc.totalSpent = acc.totalSpent.add(price.multiply(BigDecimal.valueOf(quantity)));
        acc.totalQuantityBought += quantity;

        String productName = item.getProductName();
        if (productName == null || productName.isBlank()) {
            productName = getProductName(productId);
        }

        acc.trackProduct(productId, productName, quantity);
    }

    private static class UserStatsAccumulator {
        BigDecimal totalSpent = BigDecimal.ZERO;
        int totalOrders = 0;
        Instant lastOrderDate = null;
        int totalQuantityBought = 0;

        Map<String, Integer> productQuantityMap = new HashMap<>();
        String mostPurchasedProductId = null;
        String mostPurchasedProductName = null;
        int mostPurchasedQuantity = 0;

        void updateLastOrderDate(Instant orderDate) {
            if (lastOrderDate == null || (orderDate != null && orderDate.isAfter(lastOrderDate))) {
                lastOrderDate = orderDate;
            }
        }

        void trackProduct(String productId, String productName, int quantity) {
            int newQuantity = productQuantityMap.getOrDefault(productId, 0) + quantity;
            productQuantityMap.put(productId, newQuantity);

            if (newQuantity > mostPurchasedQuantity) {
                mostPurchasedQuantity = newQuantity;
                mostPurchasedProductId = productId;
                mostPurchasedProductName = productName;
            }
        }

        Map<String, Object> toMap() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalOrders", totalOrders);
            stats.put("totalSpent", totalSpent);
            stats.put("lastOrderDate", lastOrderDate);
            stats.put("mostPurchasedProductId", mostPurchasedProductId);
            stats.put("mostPurchasedProductName", mostPurchasedProductName);
            stats.put("mostPurchasedProductCount", mostPurchasedQuantity);
            stats.put("totalQuantityBought", totalQuantityBought);
            return stats;
        }
    }

    // ────────────────────────────────────────────────────────────────
    // SELLER STATISTICS
    // ────────────────────────────────────────────────────────────────

    public Map<String, Object> calculateSellerStats(String sellerId) {
        try {
            List<Order> allOrders = orderRepository.findAll();

            if (allOrders == null || allOrders.isEmpty()) {
                return initializeEmptySellerStats();
            }

            SellerStatsAccumulator acc = new SellerStatsAccumulator();

            for (Order order : allOrders) {
                if (order.getItems() == null || order.getItems().isEmpty())
                    continue;

                for (OrderItem item : order.getItems()) {
                    processSellerOrderItem(item, order, sellerId, acc);
                }
            }

            return acc.toMap();

        } catch (Exception e) {
            log.error("Error calculating seller stats for sellerId: {}", sellerId, e);
            return initializeEmptySellerStats();
        }
    }

    private void processSellerOrderItem(OrderItem item, Order order, String targetSellerId,
            SellerStatsAccumulator acc) {
        acc.itemsChecked++;

        String itemSellerId = resolveItemSellerId(item);

        if (!targetSellerId.equals(itemSellerId)) {
            return;
        }

        acc.itemsMatched++;
        OrderStatus status = order.getStatus();

        if (status == OrderStatus.DELIVERED) {
            BigDecimal price = item.getPrice() != null ? item.getPrice() : fetchItemPrice(item.getProductId());
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;

            acc.totalRevenue = acc.totalRevenue.add(price.multiply(BigDecimal.valueOf(quantity)));
            acc.totalItemsSold += quantity;
            acc.deliveredOrdersSet.add(order.getId());
            acc.customersSet.add(order.getUserId());
            acc.updateLastDeliveredDate(order.getOrderDate());

        } else if (status == OrderStatus.CANCELLED) {
            acc.cancelledOrdersSet.add(order.getId());
        }
    }

    private String resolveItemSellerId(OrderItem item) {
        if (item.getSellerId() != null && !item.getSellerId().isBlank()) {
            return item.getSellerId();
        }
        return getProductSellerId(item.getProductId());
    }

    private static class SellerStatsAccumulator {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalItemsSold = 0;
        Set<String> deliveredOrdersSet = new HashSet<>();
        Set<String> cancelledOrdersSet = new HashSet<>();
        Set<String> customersSet = new HashSet<>();
        Instant lastDeliveredDate = null;
        int itemsChecked = 0;
        int itemsMatched = 0;

        void updateLastDeliveredDate(Instant orderDate) {
            if (lastDeliveredDate == null || (orderDate != null && orderDate.isAfter(lastDeliveredDate))) {
                lastDeliveredDate = orderDate;
            }
        }

        Map<String, Object> toMap() {
            Map<String, Object> stats = new HashMap<>();
            int totalOrders = deliveredOrdersSet.size() + cancelledOrdersSet.size();

            double deliveryRatePercent = totalOrders > 0 ? ((double) deliveredOrdersSet.size() / totalOrders * 100)
                    : 100.0;
            double cancellationRatePercent = totalOrders > 0 ? ((double) cancelledOrdersSet.size() / totalOrders * 100)
                    : 0.0;

            stats.put("totalRevenue", totalRevenue);
            stats.put("totalItemsSold", totalItemsSold);
            stats.put("totalDeliveredOrders", deliveredOrdersSet.size());
            stats.put("totalCancelledOrders", cancelledOrdersSet.size());
            stats.put("totalUniqueCustomers", customersSet.size());
            stats.put("lastDeliveredDate", lastDeliveredDate);
            stats.put("deliveryRate", deliveryRatePercent);
            stats.put("cancellationRate", cancellationRatePercent);
            return stats;
        }
    }

    // ────────────────────────────────────────────────────────────────
    // PRODUCT SERVICE DELEGATES & CACHING
    // ────────────────────────────────────────────────────────────────

    private BigDecimal fetchItemPrice(String productId) {
        Map<String, Object> product = getProductDetails(productId);
        if (product != null && product.containsKey("price")) {
            return new BigDecimal(product.get("price").toString());
        }
        return BigDecimal.ZERO;
    }

    private String getProductSellerId(String productId) {
        Map<String, Object> product = getProductDetails(productId);
        if (product != null && product.containsKey("sellerID")) {
            return product.get("sellerID").toString();
        }
        return null;
    }

    private String getProductName(String productId) {
        Map<String, Object> product = getProductDetails(productId);
        if (product != null && product.containsKey("name")) {
            return product.get("name").toString();
        }
        return "Unknown Product";
    }

    /**
     * Fetch ALL product details at once from cache or product service.
     * Uses computeIfAbsent for atomic, thread-safe cache population.
     */
    private Map<String, Object> getProductDetails(String productId) {
        if (productId == null)
            return Collections.emptyMap();

        // 🚨 FIX: computeIfAbsent handles the cache checking and network fetching in
        // one clean step
        return productCache.computeIfAbsent(productId, id -> {
            try {
                String url = PRODUCT_SERVICE_URL + "/api/products/simple/" + id;
                var response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        });
                return response.getBody() != null ? response.getBody() : Collections.emptyMap();
            } catch (Exception e) {
                log.warn("Could not fetch product details for productId: {} - Error: {}", id, e.getMessage());
                return Collections.emptyMap();
            }
        });
    }

    // ────────────────────────────────────────────────────────────────
    // INITIALIZERS
    // ────────────────────────────────────────────────────────────────

    private Map<String, Object> initializeEmptyStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", 0);
        stats.put("totalSpent", BigDecimal.ZERO);
        stats.put("lastOrderDate", null);
        stats.put("mostPurchasedProductId", null);
        stats.put("mostPurchasedProductName", null);
        stats.put("mostPurchasedProductCount", 0);
        stats.put("totalQuantityBought", 0);
        return stats;
    }

    private Map<String, Object> initializeEmptySellerStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", BigDecimal.ZERO);
        stats.put("totalItemsSold", 0);
        stats.put("totalDeliveredOrders", 0);
        stats.put("totalCancelledOrders", 0);
        stats.put("totalUniqueCustomers", 0);
        stats.put("lastDeliveredDate", null);
        stats.put("deliveryRate", 100.0);
        stats.put("cancellationRate", 0.0);
        return stats;
    }
}