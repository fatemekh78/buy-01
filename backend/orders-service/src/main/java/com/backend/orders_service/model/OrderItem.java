package com.backend.orders_service.model;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single line item within an Order.
 * Contains historical snapshots of the product details at the time of purchase.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @NotBlank(message = "Product ID cannot be blank")
    private String productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // --- Historical Snapshot Data ---
    // We store these values here instead of fetching them from the Product Service
    // because product prices, names, and images can change over time.
    // An invoice must reflect exactly what the user saw at the time of purchase.
    // ... inside OrderItem.java ...

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;
    private String sellerId;
    private String productName;
    private String imageUrl;
}