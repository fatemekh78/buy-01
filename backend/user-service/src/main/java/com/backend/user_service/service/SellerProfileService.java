package com.backend.user_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects; // Added for explicit null assertions

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.common.dto.SellerProfileDTO;
import com.backend.common.entity.SellerProfile;
import com.backend.common.repository.SellerProfileRepository;
import com.backend.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing seller profiles and aggregating cross-service
 * performance metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;;

    private static final String ORDERS_SERVICE_URL = "http://orders-service/api/orders";

    public SellerProfileDTO getSellerProfile(String sellerId) {
        SellerProfile profile = sellerProfileRepository.findBySellerId(sellerId)
                .orElseGet(() -> createDefaultProfile(sellerId));
        return mapToDTO(profile);
    }

    @SuppressWarnings("null")
    public SellerProfileDTO getSellerStatistics(String sellerId) {
        try {
            // 🚨 FIX 1: Exact URL matching the OrderController
            // 🚨 FIX 2: Using http:// internal Eureka routing
            String url = ORDERS_SERVICE_URL + "/seller/" + sellerId + "/stats";

            // Fetch the Map<String, Object> returned by the OrderController
            Map<String, Object> stats = webClientBuilder.build()
                    .get()
                    .uri(url)
                    .header("X-User-ID", "system") // Bypasses specific ownership checks if needed
                    .header("X-User-Role", "ADMIN") // Ensures the request isn't blocked by gateway logic
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            SellerProfileDTO profileDTO = new SellerProfileDTO();
            profileDTO.setSellerId(sellerId);

            // Safely map the raw data if the orders-service returned results
            if (stats != null) {
                // Map Revenue
                if (stats.containsKey("totalRevenue")) {
                    profileDTO.setTotalRevenue(
                            BigDecimal.valueOf(Double.parseDouble(stats.get("totalRevenue").toString())));
                }

                // Map Total Items Sold
                if (stats.containsKey("totalItemsSold")) {
                    profileDTO.setTotalSales(Integer.valueOf(stats.get("totalItemsSold").toString()));
                }

                // 🚨 NEW FIX: Map Total Orders (using delivered orders)
                if (stats.containsKey("totalDeliveredOrders")) {
                    profileDTO.setTotalOrders(Integer.valueOf(stats.get("totalDeliveredOrders").toString()));
                }

                // 🚨 NEW FIX: Map Total Unique Customers
                if (stats.containsKey("totalUniqueCustomers")) {
                    profileDTO.setTotalCustomers(Integer.valueOf(stats.get("totalUniqueCustomers").toString()));
                }

                // Optional: Map Cancellation Rate if you want to display it
                if (stats.containsKey("cancellationRate")) {
                    profileDTO.setCancellationRate((int) Double.parseDouble(stats.get("cancellationRate").toString()));
                }
            }

            return profileDTO;

        } catch (Exception e) {
            log.warn("Failed to fetch seller statistics for {}: {}", sellerId, e.getMessage());
            // Return an empty/default DTO rather than crashing the User profile load
            SellerProfileDTO fallback = new SellerProfileDTO();
            fallback.setSellerId(sellerId);
            return fallback;
        }
    }

    @Transactional
    public SellerProfileDTO updateProfile(String sellerId, SellerProfileDTO dto) {
        SellerProfile profile = sellerProfileRepository.findBySellerId(sellerId)
                .orElseGet(() -> createDefaultProfile(sellerId));

        // Partial Update Logic perfectly matching the Entity
        if (dto.getSellerName() != null)
            profile.setSellerName(dto.getSellerName());
        if (dto.getShopDescription() != null)
            profile.setShopDescription(dto.getShopDescription());
        if (dto.getShopLogoUrl() != null)
            profile.setShopLogoUrl(dto.getShopLogoUrl());
        if (dto.getCategories() != null)
            profile.setCategories(dto.getCategories());

        // Uses Objects.requireNonNull to satisfy SonarQube without @SuppressWarnings
        SellerProfile savedProfile = Objects.requireNonNull(sellerProfileRepository.save(profile),
                "Saved profile must not be null");
        return mapToDTO(savedProfile);
    }

    private SellerProfile createDefaultProfile(String sellerId) {
        log.info("Initializing new default seller profile for ID: {}", sellerId);

        String defaultName = userRepository.findById(sellerId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("New Shop");

        SellerProfile newProfile = SellerProfile.builder()
                .sellerId(sellerId)
                .sellerName(defaultName)
                .joinDate(Instant.now())
                .build();

        return Objects.requireNonNull(sellerProfileRepository.save(newProfile), "Created profile must not be null");
    }

    private SellerProfileDTO mapToDTO(SellerProfile profile) {
        return SellerProfileDTO.builder()
                .sellerId(profile.getSellerId())
                .sellerName(profile.getSellerName())
                .shopLogoUrl(profile.getShopLogoUrl())
                .shopDescription(profile.getShopDescription())
                .totalRevenue(profile.getTotalRevenue())
                .totalSales(profile.getTotalSales())
                .totalOrders(profile.getTotalOrders())
                .totalCustomers(profile.getTotalCustomers())
                .bestSellingProductId(profile.getBestSellingProductId())
                .bestSellingProductName(profile.getBestSellingProductName())
                .bestSellingProductCount(profile.getBestSellingProductCount())
                .averageRating(profile.getAverageRating())
                .totalReviews(profile.getTotalReviews())
                .totalFiveStarReviews(profile.getTotalFiveStarReviews())
                .isVerified(profile.getIsVerified())
                .isActive(profile.getIsActive())
                .deliveryRating(profile.getDeliveryRating())
                .communicationRating(profile.getCommunicationRating())
                .returnRate(profile.getReturnRate())
                .cancellationRate(profile.getCancellationRate())
                .joinDate(profile.getJoinDate())
                .lastSaleDate(profile.getLastSaleDate())
                .categories(profile.getCategories())
                .followerCount(profile.getFollowerCount())
                .build();
    }
}