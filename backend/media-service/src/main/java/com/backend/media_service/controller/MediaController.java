package com.backend.media_service.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.media_service.model.Media;
import com.backend.media_service.service.FileStorageService;
import com.backend.media_service.service.MediaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media API", description = "Endpoints for file uploading, static file serving, and media lifecycle management")
public class MediaController {

    private final FileStorageService fileStorageService;
    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload product media", description = "Uploads a media file associated with a product and saves its metadata.")
    public ResponseEntity<MediaUploadResponseDTO> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("productId") String productId) {

        Media savedMedia = mediaService.uploadFile(file, productId);
        String fileUrl = "/api/media/files/" + savedMedia.getImagePath();

        MediaUploadResponseDTO response = new MediaUploadResponseDTO(savedMedia.getId(), fileUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload user avatar", description = "Uploads a profile avatar and returns the raw file URL.")
    public ResponseEntity<String> uploadFileForAvatar(
            @RequestParam("file") MultipartFile file) {

        String fileName = mediaService.uploadFileAvatar(file);
        String fileUrl = "/api/media/files/" + fileName;

        log.info("Avatar uploaded successfully. Accessible at: {}", fileUrl);
        return ResponseEntity.ok(fileUrl);
    }

    @GetMapping("/files/{filename}")
    @Operation(summary = "Serve media file", description = "Retrieves a stored media file by its filename and probes its content type.")
    public ResponseEntity<Resource> getFile(@PathVariable("filename") String filename) {
        Resource file = fileStorageService.load(filename);
        String contentType = "application/octet-stream"; // Default fallback

        try {
            contentType = Files.probeContentType(file.getFile().toPath());
        } catch (IOException e) {
            log.warn("Could not determine file type for: {}. Defaulting to application/octet-stream", filename);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(file);
    }

    @GetMapping("/product/{productId}/urls")
    @Operation(summary = "Get limited product image URLs", description = "Retrieves a limited list of image URLs for a given product (Used for Catalog preview).")
    public ResponseEntity<List<String>> getLimitedProductImageUrls(
            @PathVariable("productId") String productId,
            @RequestParam(value = "limit", defaultValue = "3") int limit) {

        List<String> urls = mediaService.getLimitedImageUrlsForProduct(productId, limit);
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/batch")
    @Operation(summary = "Get all product media", description = "Retrieves a complete list of media assets for a given product.")
    public ResponseEntity<List<MediaUploadResponseDTO>> getMediaByIds(
            @Parameter(description = "Exact parameter name 'productID' required by internal services") @RequestParam("productID") String productId) {

        List<MediaUploadResponseDTO> mediaList = mediaService.findMediaByProductId(productId);
        return ResponseEntity.ok(mediaList);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete media by ID", description = "Permanently removes a media asset and its corresponding file.")
    public ResponseEntity<String> deleteMediaById(@PathVariable String id) {
        log.info("Initiating deletion for media ID: {}", id);
        mediaService.deleteMediaById(id);
        return ResponseEntity.ok("Delete media successfully");
    }

    // --------------------------------------------------------
    // Kafka Event Listeners
    // --------------------------------------------------------

    @KafkaListener(topics = "product-deleted-topic", groupId = "media-service-group")
    public void handleProductDeleted(String productId) {
        log.info("Kafka Event Received: Product deletion for ID '{}'. Triggering media cleanup.", productId);
        mediaService.deleteMediaByProductId(productId);
    }

    @KafkaListener(topics = "user-avatar-deleted-topic", groupId = "media-service-group")
    public void handleAvatarDeleted(String avatarUrl) {
        log.info("Kafka Event Received: Avatar deletion for URL '{}'. Triggering file cleanup.", avatarUrl);
        mediaService.deleteMediaByAvatarUrl(avatarUrl);
    }
}