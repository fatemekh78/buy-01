package com.backend.media_service.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.backend.media_service.model.Media;

/**
 * Repository interface for performing CRUD operations on Media documents.
 */
@Repository
public interface MediaRepository extends MongoRepository<Media, String> {

    /**
     * Retrieves a paginated/limited list of media associated with a specific
     * product.
     * * @param productId The unique identifier of the product.
     * 
     * @param pageable Pagination and sorting configuration (e.g., limit to 3
     *                 images).
     * @return A list of media assets matching the criteria.
     */
    List<Media> findByProductId(String productId, Pageable pageable);

    /**
     * Retrieves all media assets associated with a specific product.
     * * @param productId The unique identifier of the product.
     * 
     * @return A complete list of media assets for the product.
     */
    List<Media> findByProductId(String productId);
}