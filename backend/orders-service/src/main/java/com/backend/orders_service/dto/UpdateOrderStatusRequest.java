package com.backend.orders_service.dto;

import com.backend.orders_service.model.OrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for transitioning an order from one state to another
 * (e.g., PENDING -> SHIPPED).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "Target order status must be provided")
    private OrderStatus status;
}