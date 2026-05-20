package com.backend.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.common.dto.SellerProfileDTO;
import com.backend.user_service.service.SellerProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
@Tag(name = "Seller Profile API", description = "Endpoints dedicated to managing seller-specific metadata and performance statistics")
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get current seller profile", description = "Retrieves the authenticated seller's shop profile based on their Gateway user ID.")
    public ResponseEntity<SellerProfileDTO> getAuthenticatedSellerProfile(@RequestHeader("X-User-ID") String userId) {
        SellerProfileDTO profile = sellerProfileService.getSellerProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{sellerId}/statistics")
    @Operation(summary = "Get public seller statistics", description = "Retrieves the public-facing performance statistics of a specific seller.")
    public ResponseEntity<SellerProfileDTO> getSellerStatistics(@PathVariable String sellerId) {
        SellerProfileDTO statistics = sellerProfileService.getSellerStatistics(sellerId);
        return ResponseEntity.ok(statistics);
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update seller profile", description = "Allows a seller to update their shop description, logo, and metadata.")
    public ResponseEntity<SellerProfileDTO> updateSellerProfile(
            @RequestBody SellerProfileDTO profileDTO,
            @RequestHeader("X-User-ID") String userId) {

        SellerProfileDTO updated = sellerProfileService.updateProfile(userId, profileDTO);
        return ResponseEntity.ok(updated);
    }
}