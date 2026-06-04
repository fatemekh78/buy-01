package com.backend.orders_service.dto;

import com.backend.orders_service.model.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload representing a user's request to check out their current cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotBlank(message = "Shipping address is required to process checkout")
    private String shippingAddress;

    @NotNull(message = "A valid payment method must be selected")
    private PaymentMethod paymentMethod;
}