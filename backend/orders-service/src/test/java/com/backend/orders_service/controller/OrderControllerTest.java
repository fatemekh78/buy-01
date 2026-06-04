package com.backend.orders_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.backend.orders_service.dto.CheckoutRequest;
import com.backend.orders_service.dto.CreateOrderRequest;
import com.backend.orders_service.dto.SellerOrderDTO;
import com.backend.orders_service.dto.UpdateOrderStatusRequest;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.model.PaymentMethod;
import com.backend.orders_service.service.OrderSearchService;
import com.backend.orders_service.service.OrderService;
import com.backend.orders_service.service.OrderStatsService;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController Unit Tests (Pure Mockito)")
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderStatsService orderStatsService;

    @Mock
    private OrderSearchService orderSearchService;

    @InjectMocks
    private OrderController orderController;

    private Order testOrder;
    private OrderItem testItem;
    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = mock(HttpServletRequest.class);

        testItem = OrderItem.builder()
                .productId("prod-123")
                .quantity(1)
                .price(new BigDecimal("99.99"))
                .sellerId("seller-1")
                .build();

        testOrder = Order.builder()
                .id("order-1")
                .userId("client-1")
                .status(OrderStatus.PENDING)
                .items(List.of(testItem))
                .orderDate(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("Create & Checkout Order Endpoints")
    class CreateAndCheckoutTests {

        @Test
        @DisplayName("Should create an order successfully")
        void testCreateOrder() {
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .userId("client-1")
                    .shippingAddress("123 Test St")
                    .paymentMethod(PaymentMethod.CARD)
                    .build();

            when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(testOrder);

            ResponseEntity<Order> response = orderController.createOrder(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("order-1", response.getBody().getId());
        }

        @Test
        @DisplayName("Should process checkout when requested by order owner")
        void testCheckoutSuccess() {
            CheckoutRequest req = CheckoutRequest.builder()
                    .shippingAddress("456 Done St")
                    .paymentMethod(PaymentMethod.CARD)
                    .build();

            testOrder.setStatus(OrderStatus.SHIPPING);
            
            // Mock headers for ownership verification
            when(mockRequest.getHeader("X-User-ID")).thenReturn("client-1");
            when(mockRequest.getHeader("X-User-Role")).thenReturn("CLIENT");
            
            when(orderService.getOrderById("order-1")).thenReturn(testOrder);
            when(orderService.checkoutOrder(eq("order-1"), any(CheckoutRequest.class))).thenReturn(testOrder);

            ResponseEntity<Order> response = orderController.checkout("order-1", req, mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(OrderStatus.SHIPPING, response.getBody().getStatus());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when checkout requested by non-owner")
        void testCheckoutForbidden() {
            CheckoutRequest req = CheckoutRequest.builder().build();

            // Mock headers for wrong owner
            when(mockRequest.getHeader("X-User-ID")).thenReturn("sneaky-client-99");
            when(mockRequest.getHeader("X-User-Role")).thenReturn("CLIENT");
            
            when(orderService.getOrderById("order-1")).thenReturn(testOrder);

            ResponseEntity<Order> response = orderController.checkout("order-1", req, mockRequest);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Get Order Details Endpoints")
    class GetOrderDetailsTests {

        @Test
        @DisplayName("Should return full order for the owning client")
        void testGetOrderByIdForClient() {
            when(mockRequest.getHeader("X-User-ID")).thenReturn("client-1");
            when(mockRequest.getHeader("X-User-Role")).thenReturn("CLIENT");
            
            when(orderService.getOrderById("order-1")).thenReturn(testOrder);

            ResponseEntity<Object> response = orderController.getOrderById("order-1", mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(testOrder, response.getBody());
        }

        @Test
        @DisplayName("Should return stripped SellerOrderDTO for authorized seller")
        void testGetOrderByIdForSeller() {
            SellerOrderDTO sellerDTO = SellerOrderDTO.builder()
                    .id("order-1")
                    .items(List.of(testItem))
                    .build();

            when(mockRequest.getHeader("X-User-ID")).thenReturn("seller-1");
            when(mockRequest.getHeader("X-User-Role")).thenReturn("SELLER");
            
            when(orderService.getOrderById("order-1")).thenReturn(testOrder);
            when(orderSearchService.getSellerOrderDetail("order-1", "seller-1")).thenReturn(sellerDTO);

            ResponseEntity<Object> response = orderController.getOrderById("order-1", mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(sellerDTO, response.getBody());
        }

        @Test
        @DisplayName("Should return 403 Forbidden if seller has no items in order")
        void testGetOrderByIdForUnauthorizedSeller() {
            when(mockRequest.getHeader("X-User-ID")).thenReturn("seller-2");
            when(mockRequest.getHeader("X-User-Role")).thenReturn("SELLER");
            
            when(orderService.getOrderById("order-1")).thenReturn(testOrder);
            when(orderSearchService.getSellerOrderDetail("order-1", "seller-2")).thenReturn(null);

            ResponseEntity<Object> response = orderController.getOrderById("order-1", mockRequest);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Status Update Endpoints")
    class UpdateStatusTests {

        @Test
        @DisplayName("Should allow ADMIN to update order status")
        void testUpdateStatusAsAdmin() {
            UpdateOrderStatusRequest req = new UpdateOrderStatusRequest(OrderStatus.SHIPPED);
            testOrder.setStatus(OrderStatus.SHIPPED);

            when(mockRequest.getHeader("X-User-Role")).thenReturn("ADMIN");
            when(orderService.updateOrderStatus("order-1", OrderStatus.SHIPPED)).thenReturn(testOrder);

            ResponseEntity<Order> response = orderController.updateStatus("order-1", req, mockRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(OrderStatus.SHIPPED, response.getBody().getStatus());
        }

        @Test
        @DisplayName("Should return 403 Forbidden when CLIENT tries to update status")
        void testUpdateStatusAsClient() {
            UpdateOrderStatusRequest req = new UpdateOrderStatusRequest(OrderStatus.SHIPPED);

            when(mockRequest.getHeader("X-User-Role")).thenReturn("CLIENT");

            ResponseEntity<Order> response = orderController.updateStatus("order-1", req, mockRequest);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Statistics Endpoints")
    class StatsTests {

        @Test
        @DisplayName("Should return user statistics")
        void testGetUserStats() {
            Map<String, Object> mockStats = Map.of("totalOrders", 5, "totalSpent", 500.0);
            when(orderStatsService.calculateUserStats("client-1")).thenReturn(mockStats);

            ResponseEntity<Map<String, Object>> response = orderController.getUserStats("client-1");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(5, response.getBody().get("totalOrders"));
            assertEquals(500.0, response.getBody().get("totalSpent"));
        }

        @Test
        @DisplayName("Should return seller statistics")
        void testGetSellerStats() {
            Map<String, Object> mockStats = Map.of("totalRevenue", 1500.0, "totalItemsSold", 20);
            when(orderStatsService.calculateSellerStats("seller-1")).thenReturn(mockStats);

            ResponseEntity<Map<String, Object>> response = orderController.getSellerStats("seller-1");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1500.0, response.getBody().get("totalRevenue"));
            assertEquals(20, response.getBody().get("totalItemsSold"));
        }
    }
}