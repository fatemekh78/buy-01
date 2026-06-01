package com.backend.media_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.backend.common.dto.MediaUploadResponseDTO;
import com.backend.common.exception.CustomException;
import com.backend.media_service.model.Media;
import com.backend.media_service.service.FileStorageService;
import com.backend.media_service.service.MediaService;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaController Unit Tests")
class MediaControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MediaService mediaService;

    @Mock
    private Resource resource;

    @InjectMocks
    private MediaController mediaController;

    private MockMultipartFile mockFile;
    private Media mockMedia;

    @BeforeEach
    void setUp() {
        mockFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes());

        // Utilizing the new @Builder pattern from the refactored Media model
        mockMedia = Media.builder()
                .id("test-media-id")
                .imagePath("test-image.jpg")
                .productId("test-product-id")
                .build();
    }

    @Test
    @DisplayName("Should successfully upload a product file")
    void testUploadFile_Success() {
        // Arrange
        String productId = "test-product-id";
        when(mediaService.uploadFile(any(MultipartFile.class), anyString())).thenReturn(mockMedia);

        // Act
        ResponseEntity<MediaUploadResponseDTO> response = mediaController.uploadFile(mockFile, productId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test-media-id", response.getBody().getFileId());
        verify(mediaService, times(1)).uploadFile(mockFile, productId);
    }

    @Test
    @DisplayName("Should successfully upload a user avatar")
    void testUploadFileForAvatar_Success() {
        // Arrange
        String expectedFileName = "avatar-test.jpg";
        when(mediaService.uploadFileAvatar(any(MultipartFile.class))).thenReturn(expectedFileName);

        // Act
        ResponseEntity<String> response = mediaController.uploadFileForAvatar(mockFile);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains(expectedFileName));
        verify(mediaService, times(1)).uploadFileAvatar(mockFile);
    }

    @Test
    @DisplayName("Should successfully serve a file resource")
    void testGetFile_Success() throws Exception {
        // Arrange
        String filename = "test-image.jpg";
        java.io.File physicalMockFile = mock(java.io.File.class);
        java.nio.file.Path mockPath = mock(java.nio.file.Path.class);

        when(fileStorageService.load(anyString())).thenReturn(resource);
        when(resource.getFile()).thenReturn(physicalMockFile);
        when(physicalMockFile.toPath()).thenReturn(mockPath);

        // Act
        ResponseEntity<Resource> response = mediaController.getFile(filename);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(fileStorageService, times(1)).load(filename);
    }

    @Test
    @DisplayName("Should throw CustomException when file is invalid/empty")
    void testUploadFile_InvalidFile() {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]);
        String productId = "test-product-id";

        // Replaced generic RuntimeException with your CustomException
        when(mediaService.uploadFile(any(MultipartFile.class), anyString()))
                .thenThrow(new CustomException("File is empty", HttpStatus.BAD_REQUEST));

        // Act & Assert
        assertThrows(CustomException.class, () -> {
            mediaController.uploadFile(emptyFile, productId);
        });
    }

    @Test
    @DisplayName("Should throw CustomException when file is not found on disk")
    void testGetFile_FileNotFound() {
        // Arrange
        String filename = "non-existent.jpg";
        when(fileStorageService.load(anyString()))
                .thenThrow(new CustomException("File not found", HttpStatus.NOT_FOUND));

        // Act & Assert
        assertThrows(CustomException.class, () -> {
            mediaController.getFile(filename);
        });
    }

    @Test
    @DisplayName("Should successfully delete media by ID")
    void testDeleteMediaById_Success() {
        // Arrange
        String mediaId = "test-media-id";

        // Note: Using the updated camelCase method name
        doNothing().when(mediaService).deleteMediaById(anyString());

        // Act
        ResponseEntity<String> response = mediaController.deleteMediaById(mediaId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Delete media successfully", response.getBody());
        verify(mediaService, times(1)).deleteMediaById(mediaId);
    }

    @Test
    @DisplayName("Should fetch all media IDs/URLs for a product")
    void testGetMediaByIds_Success() {
        // Arrange
        String productId = "test-product-id";

        // Note: Using the updated camelCase method name
        when(mediaService.findMediaByProductId(anyString())).thenReturn(List.of());

        // Act
        ResponseEntity<List<MediaUploadResponseDTO>> response = mediaController.getMediaByIds(productId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(mediaService, times(1)).findMediaByProductId(productId);
    }

    @Test
    @DisplayName("Should fetch limited product image URLs")
    void testGetLimitedProductImageUrls_Success() {
        // Arrange
        String productId = "test-product-id";
        int limit = 3;
        List<String> mockUrls = List.of("url1", "url2", "url3");
        when(mediaService.getLimitedImageUrlsForProduct(anyString(), anyInt())).thenReturn(mockUrls);

        // Act
        ResponseEntity<List<String>> response = mediaController.getLimitedProductImageUrls(productId, limit);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().size());
        verify(mediaService, times(1)).getLimitedImageUrlsForProduct(productId, limit);
    }

    @Test
    @DisplayName("Should fetch limited product image URLs using default limit")
    void testGetLimitedProductImageUrls_WithDefaultLimit() {
        // Arrange
        String productId = "test-product-id";
        List<String> mockUrls = List.of("url1", "url2", "url3");
        when(mediaService.getLimitedImageUrlsForProduct(anyString(), anyInt())).thenReturn(mockUrls);

        // Act
        ResponseEntity<List<String>> response = mediaController.getLimitedProductImageUrls(productId, 3);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(mediaService, times(1)).getLimitedImageUrlsForProduct(productId, 3);
    }
}