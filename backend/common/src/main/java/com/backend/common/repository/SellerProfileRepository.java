package com.backend.common.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.backend.common.entity.SellerProfile;

/**
 * Data access layer for managing SellerProfile documents within MongoDB.
 * Provides out-of-the-box CRUD operations and custom derived query methods.
 */
@Repository
public interface SellerProfileRepository extends MongoRepository<SellerProfile, String> {

    /**
     * Retrieves a seller's statistical and performance profile using their linked
     * User ID.
     * Wrapped in an Optional to safely handle scenarios where the profile has not
     * yet been initialized.
     *
     * @param sellerId The unique identifier of the User possessing the SELLER role.
     * @return An Optional containing the SellerProfile if found, or empty if it
     *         does not exist.
     */
    Optional<SellerProfile> findBySellerId(String sellerId);

    /**
     * Removes a seller's profile from the database based on their linked User ID.
     * Designed to be triggered during account closure or GDPR data deletion
     * workflows.
     *
     * @param sellerId The unique identifier of the User whose profile should be
     *                 deleted.
     */
    void deleteBySellerId(String sellerId);
}