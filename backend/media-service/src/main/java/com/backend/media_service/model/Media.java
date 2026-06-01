package com.backend.media_service.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a media asset (e.g., product image or avatar) stored in the
 * system.
 * Maps directly to the "media" collection in MongoDB.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "media")
public class Media {

    @Id
    private String id;

    @NotBlank(message = "Image path cannot be blank")
    private String imagePath;

    @NotBlank(message = "Product ID cannot be blank")
    @Field("productID") // Ensures backward compatibility with existing MongoDB records
    private String productId;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}