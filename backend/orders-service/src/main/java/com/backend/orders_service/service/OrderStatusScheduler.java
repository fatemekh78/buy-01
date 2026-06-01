package com.backend.orders_service.service;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.backend.orders_service.model.OrderStatus;
import com.backend.orders_service.repository.OrderRepository;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Simulates order fulfillment by automatically transitioning orders
 * to DELIVERED after a configurable delay.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusScheduler {

    // Configurable delays via application properties (in milliseconds)
    @Value("${app.order.status.min-delay-ms:30000}") // Default 30 seconds
    private long minDelayMs;

    @Value("${app.order.status.max-delay-ms:120000}") // Default 2 minutes
    private long maxDelayMs;

    private final TaskScheduler taskScheduler;
    private final OrderRepository orderRepository;

    /**
     * Schedules a post-checkout status update with random delay jitter.
     * Delay is configurable via app.order.status.min-delay-ms and max-delay-ms.
     */
    @SuppressWarnings("java:S2245") // ThreadLocalRandom is safe here for non-security jitter
    public void schedulePostCheckoutUpdate(@NotNull String orderId) {
        long delay = ThreadLocalRandom.current().nextLong(minDelayMs, maxDelayMs + 1);
        Instant runAt = Instant.now().plusMillis(delay);

        taskScheduler.schedule(() -> processOrder(orderId), runAt);

        log.debug("Scheduled delivery update for order {} in {} ms", orderId, delay);
    }

    private void processOrder(@NotNull String orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            if (order.getStatus() != OrderStatus.SHIPPING) {
                log.debug("Skipping scheduled update for order {} because status is {}", orderId, order.getStatus());
                return;
            }

            // Transition to DELIVERED.
            // Note: @LastModifiedDate (MongoDB Auditing) automatically tracks the exact
            // time of this update.
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);

            log.info("Simulated Fulfillment: Order {} has automatically transitioned to DELIVERED", orderId);
        });
    }
}