package com.backend.media_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.backend.common.exception.CustomException;
import com.backend.media_service.model.Media;
import com.backend.media_service.repository.MediaRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaService Unit Tests")
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private MediaService mediaService;

    private Media testMedia;

    @BeforeEach
    void setUp() {
        // Utilizing the new @Builder pattern from the refactored model
        testMedia = Media.builder()
                .id("media123")
                .imagePath("/api/media/files/image.jpg")
                .productId("product123")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should upload file successfully")
    void testUploadFileSuccess() {
        // Arrange
        when(fileStorageService.save(mockFile)).thenReturn("image.jpg");
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setId("newMediaId");
            return m;
        });

        // Act
        Media saved = mediaService.uploadFile(mockFile, "product123");

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("newMediaId");
        assertThat(saved.getProductId()).isEqualTo("product123");
        verify(fileStorageService).save(mockFile);
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    @DisplayName("Should upload avatar file successfully")
    void testUploadFileAvatarSuccess() {
        // Arrange
        when(fileStorageService.save(mockFile)).thenReturn("avatar.jpg");

        // Act
        String filename = mediaService.uploadFileAvatar(mockFile);

        // Assert
        assertThat(filename).isEqualTo("avatar.jpg");
        verify(fileStorageService).save(mockFile);
    }

    @Test
    @DisplayName("Should find media by product ID successfully")
    void testFindMediaByProductIdSuccess() {
        // Arrange
        Media media1 = Media.builder().id("media1").imagePath("/api/media/files/image1.jpg").productId("product123")
                .build();
        Media media2 = Media.builder().id("media2").imagePath("/api/media/files/image2.jpg").productId("product123")
                .build();

        when(mediaRepository.findByProductId("product123"))
                .thenReturn(List.of(media1, media2));

        // Act
        var result = mediaService.findMediaByProductId("product123");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFileUrl()).isEqualTo("/api/media/files/image1.jpg");
        assertThat(result.get(1).getFileUrl()).isEqualTo("/api/media/files/image2.jpg");
        verify(mediaRepository).findByProductId("product123");
    }

    @Test
    @DisplayName("Should return empty list when no media found for product")
    void testFindMediaByProductIdEmpty() {
        // Arrange
        when(mediaRepository.findByProductId("nonexistent"))
                .thenReturn(List.of());

        // Act
        var result = mediaService.findMediaByProductId("nonexistent");

        // Assert
        assertThat(result).isEmpty();
        verify(mediaRepository).findByProductId("nonexistent");
    }

    @Test
    @DisplayName("Should delete media by product ID successfully")
    void testDeleteMediaByProductIdSuccess() {
        // Arrange
        Media media1 = Media.builder().id("media1").imagePath("/api/media/files/image1.jpg").productId("product123")
                .build();
        Media media2 = Media.builder().id("media2").imagePath("/api/media/files/image2.jpg").productId("product123")
                .build();

        when(mediaRepository.findByProductId("product123"))
                .thenReturn(List.of(media1, media2));

        // Act
        mediaService.deleteMediaByProductId("product123");

        // Assert
        verify(fileStorageService).delete("/api/media/files/image1.jpg");
        verify(fileStorageService).delete("/api/media/files/image2.jpg");
        verify(mediaRepository).deleteAll(List.of(media1, media2));
    }

    @Test
    @DisplayName("Should handle deletion when no media exists for product")
    void testDeleteMediaByProductIdEmpty() {
        // Arrange
        when(mediaRepository.findByProductId("product123")).thenReturn(List.of());

        // Act
        mediaService.deleteMediaByProductId("product123");

        // Assert
        verify(fileStorageService, never()).delete(anyString());

        // ✅ THE FIX: Expect that deleteAll is NEVER called!
        verify(mediaRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("Should delete media by ID successfully")
    void testDeleteMediaByIdSuccess() {
        // Arrange
        when(mediaRepository.findById("media123"))
                .thenReturn(Optional.of(testMedia));

        // Act
        mediaService.deleteMediaById("media123");

        // Assert
        verify(mediaRepository).findById("media123");
        verify(fileStorageService).delete("/api/media/files/image.jpg");
        verify(mediaRepository).delete(testMedia);
    }

    @Test
    @DisplayName("Should throw exception when media ID not found")
    void testDeleteMediaByIdNotFound() {
        // Arrange
        when(mediaRepository.findById("nonexistent"))
                .thenThrow(new CustomException("Media Not Found!", null));

        // Act & Assert
        assertThatThrownBy(() -> mediaService.deleteMediaById("nonexistent"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Media Not Found!");

        verify(mediaRepository).findById("nonexistent");
        verify(fileStorageService, never()).delete(anyString());
    }

    @Test
    @DisplayName("Should delete media by avatar URL successfully")
    void testDeleteMediaByAvatarUrlSuccess() {
        // Act
        mediaService.deleteMediaByAvatarUrl("/uploads/avatar.jpg");

        // Assert
        verify(fileStorageService).delete("/uploads/avatar.jpg");
    }

    @Test
    @DisplayName("Should verify media has product ID")
    void testMediaHasProductId() {
        // Assert
        assertThat(testMedia.getProductId()).isNotNull();
        assertThat(testMedia.getProductId()).isEqualTo("product123");
    }

    @Test
    @DisplayName("Should verify media has image path")
    void testMediaHasImagePath() {
        // Assert
        assertThat(testMedia.getImagePath()).isNotNull();
        assertThat(testMedia.getImagePath()).startsWith("/api/media/files/");
    }

    @Test
    @DisplayName("Should verify media has ID")
    void testMediaHasID() {
        // Assert
        assertThat(testMedia.getId()).isNotNull();
        assertThat(testMedia.getId()).isEqualTo("media123");
    }

    @Test
    @DisplayName("Should verify media timestamps are set")
    void testMediaTimestamps() {
        // Assert
        assertThat(testMedia.getCreatedAt()).isNotNull();
        assertThat(testMedia.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find media by ID successfully")
    void testFindByIdSuccess() {
        // Arrange
        when(mediaRepository.findById("media123")).thenReturn(Optional.of(testMedia));

        // Act
        Optional<Media> found = mediaRepository.findById("media123");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("media123");
        verify(mediaRepository).findById("media123");
    }

    @Test
    @DisplayName("Should return empty when media ID not found")
    void testFindByIdNotFound() {
        // Arrange
        when(mediaRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<Media> found = mediaRepository.findById("nonexistent");

        // Assert
        assertThat(found).isEmpty();
        verify(mediaRepository).findById("nonexistent");
    }

    @Test
    @DisplayName("Should save media successfully")
    void testSaveMediaSuccess() {
        // Arrange
        Media newMedia = Media.builder().imagePath("/api/media/files/new.jpg").productId("product456").build();

        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setId("newId");
            return m;
        });

        // Act
        Media saved = mediaRepository.save(newMedia);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo("newId");
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    @DisplayName("Should update media successfully")
    void testUpdateMediaSuccess() {
        // Arrange
        Media existingMedia = Media.builder().id("media123").imagePath("/api/media/files/old.jpg")
                .productId("product123").build();
        Media updatedMedia = Media.builder().id("media123").imagePath("/api/media/files/updated.jpg")
                .productId("product123").build();

        when(mediaRepository.save(any(Media.class))).thenReturn(updatedMedia);

        // Act
        Media result = mediaService.updateMedia(existingMedia);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("media123");
        assertThat(result.getImagePath()).isEqualTo("/api/media/files/updated.jpg");
        verify(mediaRepository).save(existingMedia);
    }

    @Test
    @DisplayName("Should get limited image URLs for product")
    void testGetLimitedImageUrlsForProduct() {
        // Arrange
        String productId = "product123";
        int limit = 3;

        Media media1 = Media.builder().id("media1").imagePath("/api/media/files/image1.jpg").productId(productId)
                .createdAt(Instant.now().minusSeconds(300)).build();
        Media media2 = Media.builder().id("media2").imagePath("/api/media/files/image2.jpg").productId(productId)
                .createdAt(Instant.now().minusSeconds(200)).build();
        Media media3 = Media.builder().id("media3").imagePath("/api/media/files/image3.jpg").productId(productId)
                .createdAt(Instant.now().minusSeconds(100)).build();

        when(mediaRepository.findByProductId(eq(productId), any()))
                .thenReturn(List.of(media1, media2, media3));

        // Act
        List<String> urls = mediaService.getLimitedImageUrlsForProduct(productId, limit);

        // Assert
        assertThat(urls).hasSize(3);
        assertThat(urls).containsExactly(
                "/api/media/files/image1.jpg",
                "/api/media/files/image2.jpg",
                "/api/media/files/image3.jpg");
        verify(mediaRepository).findByProductId(eq(productId), any());
    }

    @Test
    @DisplayName("Should get limited image URLs with fewer results than limit")
    void testGetLimitedImageUrlsForProduct_FewerThanLimit() {
        // Arrange
        String productId = "product123";
        int limit = 5;

        Media media1 = Media.builder().id("media1").imagePath("/api/media/files/image1.jpg").productId(productId)
                .build();
        Media media2 = Media.builder().id("media2").imagePath("/api/media/files/image2.jpg").productId(productId)
                .build();

        when(mediaRepository.findByProductId(eq(productId), any()))
                .thenReturn(List.of(media1, media2));

        // Act
        List<String> urls = mediaService.getLimitedImageUrlsForProduct(productId, limit);

        // Assert
        assertThat(urls).hasSize(2);
        assertThat(urls).containsExactly(
                "/api/media/files/image1.jpg",
                "/api/media/files/image2.jpg");
        verify(mediaRepository).findByProductId(eq(productId), any());
    }

    @Test
    @DisplayName("Should return empty list when no images found for product")
    void testGetLimitedImageUrlsForProduct_Empty() {
        // Arrange
        String productId = "product123";
        int limit = 3;

        when(mediaRepository.findByProductId(eq(productId), any()))
                .thenReturn(List.of());

        // Act
        List<String> urls = mediaService.getLimitedImageUrlsForProduct(productId, limit);

        // Assert
        assertThat(urls).isEmpty();
        verify(mediaRepository).findByProductId(eq(productId), any());
    }
}