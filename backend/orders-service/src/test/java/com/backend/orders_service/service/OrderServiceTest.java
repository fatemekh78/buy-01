package com.backend.orders_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.backend.common.exception.CustomException;
import com.backend.orders_service.client.ProductInventoryClient;
import com.backend.orders_service.dto.CheckoutRequest;
import com.backend.orders_service.dto.CreateOrderRequest;
import com.backend.orders_service.dto.RedoOrderResponse;
import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.model.PaymentMethod;
import com.backend.orders_service.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductInventoryClient productInventoryClient;

    @Mock
    private OrderStatusScheduler orderStatusScheduler;

    @Mock
    private RestTemplate restTemplate; // Used for inner product calls

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private OrderItem testOrderItem;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        testOrderItem = OrderItem.builder()
                .productId("prod-123")
                .quantity(2)
                .price(new BigDecimal("29.99"))
                .sellerId("seller-456")
                .productName("Test Product")
                .build();

        testOrder = Order.builder()
                .id("order-123")
                .userId("user-456")
                .shippingAddress("123 Main St")
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>(List.of(testOrderItem)))
                .paymentMethod(PaymentMethod.CARD)
                .orderDate(Instant.now())
                .build();

        createOrderRequest = CreateOrderRequest.builder()
                .userId("user-456")
                .shippingAddress("123 Main St")
                .items(new ArrayList<>(List.of(testOrderItem)))
                .paymentMethod(PaymentMethod.CARD)
                .build();
    }

    @Nested
    class CreateOrderTests {
        @Test
        @DisplayName("Should create order successfully")
        void testCreateOrderSuccess() {
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId("new-order-id");
                return order;
            });

            Order created = orderService.createOrder(createOrderRequest);

            assertThat(created).isNotNull();
            assertThat(created.getUserId()).isEqualTo("user-456");
            assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING);
            verify(orderRepository).save(any(Order.class));
        }
    }

    @Nested
    class UpdateOrderStatusTests {
        @Test
        @DisplayName("Should update order status successfully")
        void testUpdateOrderStatusSuccess() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Order updated = orderService.updateOrderStatus("order-123", OrderStatus.SHIPPING);

            assertThat(updated.getStatus()).isEqualTo(OrderStatus.SHIPPING);
        }

        @Test
        @DisplayName("Should throw CustomException when order not found")
        void testUpdateOrderStatusNotFound() {
            when(orderRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateOrderStatus("nonexistent", OrderStatus.SHIPPING))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Order not found");
        }
    }

    @Nested
    class RedoOrderTests {
        private ProductInventoryClient.ProductDetail mockProductDetail;

        @BeforeEach
        void setUpProductDetail() {
            mockProductDetail = new ProductInventoryClient.ProductDetail();
            mockProductDetail.setProductId("prod-123");
            mockProductDetail.setName("Test Product");
            mockProductDetail.setQuantity(10);
            mockProductDetail.setPrice(29.99);
        }

        @Test
        @DisplayName("Should redo order with all items in stock")
        void testRedoOrderAllItemsInStock() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(productInventoryClient.getProductDetails("prod-123")).thenReturn(mockProductDetail);
            when(orderRepository.findFirstByUserIdAndStatusOrderByOrderDateDesc("user-456", OrderStatus.PENDING))
                    .thenReturn(Optional.empty());
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RedoOrderResponse response = orderService.redoOrder("order-123");

            assertThat(response.getMessage()).isEqualTo("All items successfully added to cart");
            assertThat(response.getOrder()).isNotNull();
            assertThat(response.getOutOfStockProducts()).isEmpty();
        }

        @Test
        @DisplayName("Should handle out of stock products")
        void testRedoOrderOutOfStock() {
            mockProductDetail.setQuantity(0);
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(productInventoryClient.getProductDetails("prod-123")).thenReturn(mockProductDetail);

            RedoOrderResponse response = orderService.redoOrder("order-123");

            assertThat(response.getMessage())
                    .isEqualTo("No items could be added to cart. All products are out of stock.");
            assertThat(response.getOrder()).isNull();
            assertThat(response.getOutOfStockProducts()).hasSize(1);
        }
    }

    @Nested
    class OrderItemManagementTests {

        @Test
        @DisplayName("Should update order item quantity AND preserve historical data")
        void testUpdateOrderItemSuccess() {
            // Frontend sends an item containing ONLY the ID and the new quantity
            OrderItem updatedItem = OrderItem.builder()
                    .productId("prod-123")
                    .quantity(5)
                    .build();

            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Order result = orderService.updateOrderItem("order-123", "prod-123", updatedItem);

            OrderItem savedItem = result.getItems().get(0);

            // Verifying the quantity updated but the historical data was kept!
            assertThat(savedItem.getQuantity()).isEqualTo(5);
            assertThat(savedItem.getPrice()).isEqualTo(new BigDecimal("29.99"));
            assertThat(savedItem.getSellerId()).isEqualTo("seller-456");
        }

        @Test
        @DisplayName("Should throw CustomException when order is not PENDING")
        void testUpdateItemInNonPendingOrder() {
            testOrder.setStatus(OrderStatus.DELIVERED);
            OrderItem updatedItem = OrderItem.builder().productId("prod-123").quantity(5).build();

            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));

            assertThatThrownBy(() -> orderService.updateOrderItem("order-123", "prod-123", updatedItem))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Cannot modify order");
        }
    }

    @Nested
    class CheckoutOrderTests {
        private CheckoutRequest checkoutRequest;

        @BeforeEach
        void setUpCheckout() {
            checkoutRequest = CheckoutRequest.builder()
                    .shippingAddress("456 New Address")
                    .paymentMethod(PaymentMethod.PAY_ON_DELIVERY)
                    .build();
        }

        @Test
        @DisplayName("Should checkout order and clear cart")
        void testCheckoutCreatesNewCart() {
            when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            orderService.checkoutOrder("order-123", checkoutRequest);

            // Should save twice: once to update current order to SHIPPING, once to create
            // empty PENDING cart
            verify(orderRepository, times(2)).save(any(Order.class));
            verify(productInventoryClient).decreaseStock(testOrder.getItems());
        }
    }
}