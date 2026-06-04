package com.backend.orders_service.model;

/**
 * Represents the lifecycle state of an order.
 */
public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}