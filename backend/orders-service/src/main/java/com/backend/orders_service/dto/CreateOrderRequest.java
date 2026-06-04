package com.backend.orders_service.dto;

import java.util.ArrayList;
import java.util.List;

import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload used internally to construct a new Order document in the database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
    
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}