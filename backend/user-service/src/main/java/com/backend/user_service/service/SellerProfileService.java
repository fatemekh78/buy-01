package com.backend.user_service.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects; // Added for explicit null assertions

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.backend.common.dto.SellerProfileDTO;
import com.backend.common.entity.SellerProfile;
import com.backend.common.exception.CustomException;
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
    private final RestTemplate restTemplate;

    private static final String ORDERS_SERVICE_URL = "http://orders-service";

    public SellerProfileDTO getSellerProfile(String sellerId) {
        SellerProfile profile = sellerProfileRepository.findBySellerId(sellerId)
                .orElseGet(() -> createDefaultProfile(sellerId));
        return mapToDTO(profile);
    }

    @SuppressWarnings("null")
    public SellerProfileDTO getSellerStatistics(String sellerId) {
        SellerProfile profile = sellerProfileRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new CustomException("Seller profile not found", HttpStatus.NOT_FOUND));

        SellerProfileDTO dto = mapToDTO(profile);

        // Fault Tolerance: Try to fetch live stats, but don't crash if orders-service
        // is down.
        try {
            String url = ORDERS_SERVICE_URL + "/api/orders/seller/" + sellerId + "/stats";

            // Cleaned up: Passed HttpMethod.GET directly.
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> stats = response.getBody();

            if (stats != null) {
                dto.setTotalSales(stats.get("totalSales") != null ? ((Number) stats.get("totalSales")).intValue()
                        : dto.getTotalSales());
                dto.setTotalRevenue(
                        stats.get("totalRevenue") != null ? new BigDecimal(stats.get("totalRevenue").toString())
                                : dto.getTotalRevenue());
            }

        } catch (RestClientException e) {
            log.warn("Could not fetch live statistics for seller {} from orders-service: {}", sellerId, e.getMessage());
        }

        return dto;
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