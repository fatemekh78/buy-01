package com.backend.orders_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import com.backend.orders_service.model.Order;
import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderStatusScheduler Unit Tests")
class OrderStatusSchedulerTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderStatusScheduler orderStatusScheduler;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @Captor
    private ArgumentCaptor<Instant> instantCaptor;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        // Manually inject the @Value properties for testing
        ReflectionTestUtils.setField(orderStatusScheduler, "minDelayMs", 1000L);
        ReflectionTestUtils.setField(orderStatusScheduler, "maxDelayMs", 5000L);

        testOrder = Order.builder()
                .id("order-123")
                .status(OrderStatus.SHIPPING)
                .build();
    }

    @Test
    @DisplayName("Should successfully schedule a task with random delay")
    void testSchedulePostCheckoutUpdate() {
        // Act
        orderStatusScheduler.schedulePostCheckoutUpdate("order-123");

        // Assert that the TaskScheduler received a Runnable and a scheduled Instant
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());

        Instant scheduledTime = instantCaptor.getValue();
        Instant now = Instant.now();

        // Verify the scheduled time is in the future, bounded by our min/max delays
        assertThat(scheduledTime).isAfterOrEqualTo(now.plusMillis(1000));
        assertThat(scheduledTime).isBeforeOrEqualTo(now.plusMillis(5000).plusSeconds(1)); // 1s buffer for execution
                                                                                          // time
    }

    @Test
    @DisplayName("Runnable should transition SHIPPING order to DELIVERED")
    void testProcessOrderSuccess() {
        // Arrange
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));

        // Act 1: Schedule the task
        orderStatusScheduler.schedulePostCheckoutUpdate("order-123");

        // Act 2: Catch the scheduled Runnable and execute it manually
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));
        Runnable capturedTask = runnableCaptor.getValue();
        capturedTask.run();

        // Assert
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Runnable should do nothing if order is not in SHIPPING status")
    void testProcessOrderWrongStatus() {
        // Arrange: Order is PENDING, not SHIPPING
        testOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById("order-123")).thenReturn(Optional.of(testOrder));

        // Act
        orderStatusScheduler.schedulePostCheckoutUpdate("order-123");
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));
        runnableCaptor.getValue().run();

        // Assert: Ensure it was NEVER saved and status remains unchanged
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Runnable should do nothing if order does not exist")
    void testProcessOrderNotFound() {
        // Arrange
        when(orderRepository.findById("order-123")).thenReturn(Optional.empty());

        // Act
        orderStatusScheduler.schedulePostCheckoutUpdate("order-123");
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));
        runnableCaptor.getValue().run();

        // Assert: Ensure it didn't crash and never called save
        verify(orderRepository, never()).save(any());
    }
}