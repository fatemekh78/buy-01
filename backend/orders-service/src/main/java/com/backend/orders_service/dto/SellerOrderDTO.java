package com.backend.orders_service.dto;

import java.time.Instant;
import java.util.List;

import com.backend.orders_service.model.OrderItem;
import com.backend.orders_service.model.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A filtered view of an Order, specific to a single Seller.
 * Excludes items from other sellers that may have been part of the same checkout cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerOrderDTO {
    
    @JsonProperty("id")
    private String id;
    
    private String orderId;
    private String userId;
    private List<OrderItem> items;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Quick preview image (usually the first item in the seller's portion of the order)
    private String imageUrl; 
}