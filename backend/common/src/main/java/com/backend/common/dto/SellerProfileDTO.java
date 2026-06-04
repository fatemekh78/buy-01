package com.backend.common.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing the public and statistical profile of a
 * Seller.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comprehensive profile and performance metrics for a Seller")
public class SellerProfileDTO {

    @NotBlank
    @Schema(description = "Unique identifier for the seller", example = "sel_112233")
    private String sellerId;

    @NotBlank
    @Schema(description = "Display name of the seller's shop", example = "Tech Haven")
    private String sellerName;

    @Schema(description = "URL to the shop's logo", example = "https://storage.buy-01.com/logos/sel_112233.png")
    private String shopLogoUrl;

    @Size(max = 1000)
    @Schema(description = "Biography or description of the shop", example = "Premium electronics and gadgets.")
    private String shopDescription;

    // Business Statistics
    @PositiveOrZero
    @Schema(description = "Total lifetime revenue generated", example = "15000.50")
    private BigDecimal totalRevenue;

    @PositiveOrZero
    @Schema(description = "Total number of items sold", example = "450")
    private Integer totalSales;

    @PositiveOrZero
    @Schema(description = "Total number of distinct orders processed", example = "320")
    private Integer totalOrders;

    @PositiveOrZero
    @Schema(description = "Total number of unique customers", example = "290")
    private Integer totalCustomers;

    // Best Selling Product
    @Schema(description = "ID of the most popular product")
    private String bestSellingProductId;

    @Schema(description = "Name of the most popular product")
    private String bestSellingProductName;

    @PositiveOrZero
    @Schema(description = "Units sold of the best selling product")
    private Integer bestSellingProductCount;

    // Ratings & Reviews
    @Min(0)
    @Max(5)
    @Schema(description = "Aggregate average rating from all reviews", example = "4.8")
    private Double averageRating;

    @PositiveOrZero
    @Schema(description = "Total count of reviews received", example = "125")
    private Integer totalReviews;

    @PositiveOrZero
    @Schema(description = "Total count of 5-star reviews", example = "110")
    private Integer totalFiveStarReviews;

    // Shop Info
    @Schema(description = "Indicates if the shop has passed platform verification")
    private Boolean isVerified;

    @Schema(description = "Indicates if the shop is currently accepting orders")
    private Boolean isActive;

    // Performance Metrics
    @Min(0)
    @Max(5)
    @Schema(description = "Rating specifically for delivery speed/quality", example = "4.9")
    private Double deliveryRating;

    @Min(0)
    @Max(5)
    @Schema(description = "Rating specifically for seller communication", example = "4.7")
    private Double communicationRating;

    @Min(0)
    @Max(100)
    @Schema(description = "Percentage of orders returned", example = "2")
    private Integer returnRate;

    @Min(0)
    @Max(100)
    @Schema(description = "Percentage of orders cancelled prior to fulfillment", example = "1")
    private Integer cancellationRate;

    // Dates
    @Schema(description = "Timestamp when the seller joined the platform")
    private Instant joinDate;

    @Schema(description = "Timestamp of the most recent sale")
    private Instant lastSaleDate;

    // Metadata
    @Schema(description = "List of product categories the seller operates in", example = "[\"Electronics\", \"Accessories\"]")
    private List<String> categories;

    @PositiveOrZero
    @Schema(description = "Number of users following the shop", example = "1500")
    private Integer followerCount;
}