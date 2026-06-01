package com.backend.media_service.service;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.common.exception.CustomException;
import com.backend.media_service.model.Media;
import com.backend.media_service.repository.MediaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public Media uploadFile(MultipartFile file, String productId) {
        String filename = fileStorageService.save(file);
        
        // Utilizing the @Builder pattern from the refactored Media model
        Media media = Media.builder()
                .imagePath("/api/media/files/" + filename)
                .productId(productId)
                .build();
                
        log.info("Media metadata saved to database for product: {}", productId);
        return mediaRepository.save(media);
    }

    public String uploadFileAvatar(MultipartFile file) {
        String filename = fileStorageService.save(file);
        log.info("Avatar file saved physically: {}", filename);
        return filename;
    }

    public List<MediaUploadResponseDTO> findMediaByProductId(String productId) {
        List<Media> mediaList = mediaRepository.findByProductId(productId);
        
        if (mediaList.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Replaced standard 'for' loop with clean Java Streams API
        return mediaList.stream()
                .map(media -> MediaUploadResponseDTO.builder()
                        .fileId(media.getId())
                        .fileUrl(media.getImagePath())
                        .build())
                .toList();
    }

    @Transactional
    public void deleteMediaByProductId(String productId) {
        List<Media> mediaToDelete = mediaRepository.findByProductId(productId);
        
        if (mediaToDelete.isEmpty()) {
            return;
        }

        for (Media media : mediaToDelete) {
            fileStorageService.delete(media.getImagePath());
        }
        
        mediaRepository.deleteAll(mediaToDelete);
        log.info("Successfully deleted {} media items for product ID: {}", mediaToDelete.size(), productId);
    }

    @Transactional
    public void deleteMediaById(String id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new CustomException("Media Not Found!", HttpStatus.NOT_FOUND));
                
        fileStorageService.delete(media.getImagePath());
        mediaRepository.delete(media);
        log.info("Successfully deleted media with ID: {}", id);
    }

    public void deleteMediaByAvatarUrl(String avatarUrl) {
        fileStorageService.delete(avatarUrl);
        log.info("Successfully deleted avatar file associated with URL: {}", avatarUrl);
    }

    @Transactional
    public Media updateMedia(Media media) {
        return mediaRepository.save(media);
    }

    public List<String> getLimitedImageUrlsForProduct(String productId, int limit) {
        // Page 0, 'limit' items, sorted by createdAt ascending (oldest first)
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "createdAt"));
        List<Media> mediaList = mediaRepository.findByProductId(productId, pageable);

        return mediaList.stream()
                .map(Media::getImagePath)
                .toList();
    }
}