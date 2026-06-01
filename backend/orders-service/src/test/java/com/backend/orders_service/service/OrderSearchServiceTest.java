package com.backend.orders_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.orders_service.dto.SellerOrderDTO;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.repository.OrderRepository;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderSearchService Unit Tests")
class OrderSearchServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private OrderSearchService orderSearchService;

    private Order mixedOrder;
    private OrderItem seller1Item;
    private OrderItem seller2Item;

    @BeforeEach
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void setUp() {
        // Setup WebClient mock chain (Lenient because not all tests trigger the
        // WebClient fallback)
        lenient().when(webClientBuilder.build()).thenReturn(webClient);
        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Setup Test Data
        seller1Item = OrderItem.builder()
                .productId("prod-1")
                .sellerId("seller-1")
                .quantity(2)
                .price(new BigDecimal("50.00")) // Total: 100.00
                .build();

        seller2Item = OrderItem.builder()
                .productId("prod-2")
                .sellerId("seller-2")
                .quantity(1)
                .price(new BigDecimal("75.00")) // Total: 75.00
                .build();

        mixedOrder = Order.builder()
                .id("order-123")
                .userId("client-99")
                .status(OrderStatus.SHIPPED)
                .items(new ArrayList<>(List.of(seller1Item, seller2Item)))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Client Order Search Tests")
    class ClientSearchTests {

        @Test
        @DisplayName("Should successfully delegate valid client search to repository")
        void testValidClientSearch() {
            Page<Order> expectedPage = new PageImpl<>(List.of(mixedOrder));
            when(orderRepository.searchAndFilterOrdersByUser(
                    eq("client-99"), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<Order> result = orderSearchService.searchAndFilterOrders(
                    "client-99", null, null, null, null, null, null, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            verify(orderRepository).searchAndFilterOrdersByUser(
                    eq("client-99"), any(), any(), any(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page immediately on invalid parameters (negative price)")
        void testInvalidClientSearch() {
            Page<Order> result = orderSearchService.searchAndFilterOrders(
                    "client-99", null, -50.0, null, null, null, null, 0, 10);

            assertThat(result.getContent()).isEmpty();
            verify(orderRepository, never()).searchAndFilterOrdersByUser(any(), any(), any(), any(), any(), any(),
                    any(), any());
        }
    }

    @Nested
    @DisplayName("Seller Order Search & Filtering Tests")
    class SellerSearchTests {

        @Test
        @DisplayName("Should isolate seller items and filter out orders if no items match")
        void testSellerIsolationAndFiltering() {
            // Setup DB to return the mixed order containing items for seller-1 and seller-2
            Page<Order> dbPage = new PageImpl<>(List.of(mixedOrder));
            when(orderRepository.searchAndFilterOrdersForSeller(
                    any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(dbPage);

            // Act: Search for seller-1
            Page<SellerOrderDTO> result = orderSearchService.searchAndFilterSellerOrders(
                    "seller-1", null, null, null, null, null, null, 0, 10);

            // Assert: Order is returned, but ONLY contains seller-1's items
            assertThat(result.getContent()).hasSize(1);
            SellerOrderDTO dto = result.getContent().get(0);
            assertThat(dto.getItems()).hasSize(1);
            assertThat(dto.getItems().get(0).getSellerId()).isEqualTo("seller-1");
        }

        @Test
        @DisplayName("Should exclude order entirely if it fails the seller-specific price filter")
        void testSellerPriceFiltering() {
            Page<Order> dbPage = new PageImpl<>(List.of(mixedOrder));
            when(orderRepository.searchAndFilterOrdersForSeller(
                    any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(dbPage);

            // Act: Search for seller-1, but require a minimum price of 200.00
            // Seller-1's total in this order is only 100.00 (2 * 50.00)
            Page<SellerOrderDTO> result = orderSearchService.searchAndFilterSellerOrders(
                    "seller-1", null, 200.00, null, null, null, null, 0, 10);

            // Assert: Order is filtered out of the final page
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Single Seller Order Detail Tests")
    class SellerOrderDetailTests {

        @Test
        @DisplayName("Should return order detail stripped of other sellers' items")
        void testGetSellerOrderDetailSuccess() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(mixedOrder));

            SellerOrderDTO result = orderSearchService.getSellerOrderDetail("order-123", "seller-2");

            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getProductId()).isEqualTo("prod-2"); // seller-2's item
        }

        @Test
        @DisplayName("Should return null if seller has zero items in the order")
        void testGetSellerOrderDetailNoAuthorizedItems() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(mixedOrder));

            // Seller-99 has no items in mixedOrder
            SellerOrderDTO result = orderSearchService.getSellerOrderDetail("order-123", "seller-99");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should fetch missing sellerId from WebClient if item data is incomplete")
        void testGetSellerOrderDetailWithWebClientFallback() {
            // Setup an item with a missing seller ID
            OrderItem incompleteItem = OrderItem.builder()
                    .productId("prod-missing-seller")
                    .quantity(1)
                    .price(new BigDecimal("10.00"))
                    .sellerId(null) // MISSING!
                    .build();

            Order brokenOrder = Order.builder().id("order-999").items(List.of(incompleteItem)).build();
            when(orderRepository.findById("order-999")).thenReturn(Optional.of(brokenOrder));

            // Setup WebClient to mock the product-service returning the missing seller ID
            Map<String, Object> mockProductResponse = Map.of("sellerID", "seller-recovered");
            when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                    .thenReturn(Mono.just(mockProductResponse));

            // Act
            SellerOrderDTO result = orderSearchService.getSellerOrderDetail("order-999", "seller-recovered");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getProductId()).isEqualTo("prod-missing-seller");
        }
    }
}