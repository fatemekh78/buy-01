package com.backend.orders_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderStatsService Unit Tests")
class OrderStatsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderStatsService orderStatsService;

    private Order deliveredOrder;

    @BeforeEach
    void setUp() {
        OrderItem item1 = OrderItem.builder()
                .productId("prod-1")
                .quantity(2)
                .price(new BigDecimal("50.00"))
                .sellerId("seller-1")
                .productName("Keyboard")
                .build();

        OrderItem item2 = OrderItem.builder()
                .productId("prod-2")
                .quantity(1)
                .price(new BigDecimal("100.00"))
                .sellerId("seller-1")
                .productName("Monitor")
                .build();

        deliveredOrder = Order.builder()
                .id("order-1")
                .userId("client-1")
                .status(OrderStatus.DELIVERED)
                .orderDate(Instant.now())
                .items(List.of(item1, item2))
                .build();
    }

    @Test
    @DisplayName("Should accurately calculate user statistics")
    void testCalculateUserStats() {
        // Arrange
        when(orderRepository.findByUserIdAndStatusOrderByOrderDateDesc("client-1", OrderStatus.DELIVERED))
                .thenReturn(List.of(deliveredOrder));

        // Act
        Map<String, Object> stats = orderStatsService.calculateUserStats("client-1");

        // Assert
        assertThat(stats.get("totalOrders")).isEqualTo(1);
        assertThat(stats.get("totalQuantityBought")).isEqualTo(3); // 2 keyboards + 1 monitor

        // 2 * 50.00 + 1 * 100.00 = 200.00
        BigDecimal expectedTotal = new BigDecimal("200.00");
        assertThat((BigDecimal) stats.get("totalSpent")).isEqualByComparingTo(expectedTotal);

        assertThat(stats.get("mostPurchasedProductId")).isEqualTo("prod-1");
        assertThat(stats.get("mostPurchasedProductCount")).isEqualTo(2);
    }

    @Test
    @DisplayName("Should accurately calculate seller statistics")
    void testCalculateSellerStats() {
        // Arrange
        when(orderRepository.findAll()).thenReturn(List.of(deliveredOrder));

        // Act
        Map<String, Object> stats = orderStatsService.calculateSellerStats("seller-1");

        // Assert
        assertThat(stats.get("totalDeliveredOrders")).isEqualTo(1);
        assertThat(stats.get("totalCancelledOrders")).isEqualTo(0);
        assertThat(stats.get("totalItemsSold")).isEqualTo(3);
        assertThat(stats.get("totalUniqueCustomers")).isEqualTo(1);

        BigDecimal expectedRevenue = new BigDecimal("200.00");
        assertThat((BigDecimal) stats.get("totalRevenue")).isEqualByComparingTo(expectedRevenue);
    }
}