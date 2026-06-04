package com.backend.orders_service.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The root aggregate representing a customer's purchase.
 * Maps to the "orders" collection in MongoDB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {
    
    @Id
    private String id;

    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotNull(message = "Order status cannot be null")
    @Builder.Default // ✅ GUARANTEES new orders start as PENDING
    private OrderStatus status = OrderStatus.PENDING;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private PaymentMethod paymentMethod;

    // Represents the moment the checkout process was initiated/completed
    private Instant orderDate;

    // MongoDB Auditing fields (Managed by MongoConfig)
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // Soft delete flag (If true, the user "deleted" this order from their history)
    @Builder.Default
    private boolean isRemoved = false;
}