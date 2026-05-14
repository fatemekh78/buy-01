package com.backend.common.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a Seller's statistical and performance profile in
 * MongoDB.
 * Maintains a 1:1 relationship with a User entity possessing the SELLER role.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "seller_profiles")
public class SellerProfile {

    @Id
    private String id;

    /**
     * Unique identifier linking to the primary User account.
     * Indexed as unique to enforce the 1:1 relationship and optimize query
     * performance.
     */
    @Indexed(unique = true)
    private String sellerId;

    /**
     * Display name of the shop for quick access without querying the User database.
     */
    private String sellerName;

    // --- Business Statistics ---

    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Builder.Default
    private Integer totalSales = 0;

    @Builder.Default
    private Integer totalOrders = 0;

    @Builder.Default
    private Integer totalCustomers = 0;

    // --- Best Performing Products ---

    private String bestSellingProductId;
    private String bestSellingProductName;

    @Builder.Default
    private Integer bestSellingProductCount = 0;

    // --- Ratings & Reviews ---

    @Builder.Default
    private Double averageRating = 0.0;

    @Builder.Default
    private Integer totalReviews = 0;

    @Builder.Default
    private Integer totalFiveStarReviews = 0;

    @Builder.Default
    private Integer totalOneStarReviews = 0;

    // --- Shop Info ---

    private String shopDescription;
    private String shopLogoUrl;

    @Builder.Default
    private Boolean isVerified = false;

    @Builder.Default
    private Boolean isActive = true;

    // --- Performance Metrics ---

    @Builder.Default
    private Double deliveryRating = 0.0;

    @Builder.Default
    private Double communicationRating = 0.0;

    @Builder.Default
    private Integer returnRate = 0;

    @Builder.Default
    private Integer cancellationRate = 0;

    // --- Dates ---

    /** Timestamp of seller registration. */
    private Instant joinDate;

    /** Timestamp of the most recently fulfilled order. */
    private Instant lastSaleDate;

    /** Automatically populated by Spring Data MongoDB upon document creation. */
    @CreatedDate
    private Instant createdAt;

    /**
     * Automatically updated by Spring Data MongoDB upon any document modification.
     */
    @LastModifiedDate
    private Instant updatedAt;

    // --- Metadata ---

    @Builder.Default
    private List<String> categories = new ArrayList<>();

    @Builder.Default
    private Integer followerCount = 0;
}