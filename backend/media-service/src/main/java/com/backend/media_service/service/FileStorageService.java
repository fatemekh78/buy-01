package com.backend.media_service.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.backend.common.exception.CustomException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileStorageService {

    private final Path root = Paths.get("uploads");

    /**
     * Safely initializes the uploads directory after the Spring bean is fully constructed.
     */
    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
                log.info("Initialized storage folder at: {}", root.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Could not initialize folder for upload!", e);
            throw new CustomException("Could not initialize storage volume", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String save(MultipartFile file) {
        try {
            // 1. Sanitize the filename to prevent Directory Traversal Attacks
            String originalFilename = StringUtils.cleanPath(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown_file"
            );
            
            if (originalFilename.contains("..")) {
                throw new CustomException("Filename contains invalid path sequence", HttpStatus.BAD_REQUEST);
            }

            // 2. Generate a unique filename
            String filename = UUID.randomUUID().toString() + "-" + originalFilename;
            
            // 3. Save to disk
            Files.copy(file.getInputStream(), this.root.resolve(filename));
            
            log.info("Successfully saved physical file: {}", filename);
            return filename;
            
        } catch (Exception e) {
            log.error("Could not save file: {}", e.getMessage(), e);
            throw new CustomException("Could not store the file physically", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Resource load(String filename) {
        try {
            Path file = root.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                log.warn("File not found or not readable on disk: {}", filename);
                throw new CustomException("File not found", HttpStatus.NOT_FOUND);
            }
        } catch (MalformedURLException e) {
            log.error("Malformed URL for file: {}", filename, e);
            throw new CustomException("Error reading file stream", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void delete(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isBlank()) {
                log.warn("Attempted to delete a media file, but the provided URL was null or empty.");
                return;
            }

            // Extract just the filename from the URL path
            String filename = Path.of(fileUrl).getFileName().toString();
            Path file = root.resolve(filename).normalize();

            // Safely delete if it exists
            boolean deleted = Files.deleteIfExists(file);
            
            if (deleted) {
                log.info("Successfully deleted physical file: {}", filename);
            } else {
                log.warn("Physical file '{}' did not exist on disk during deletion attempt. (Already deleted?)", filename);
            }

        } catch (IOException e) {
            log.error("Could not delete the file: {}", fileUrl, e);
            throw new CustomException("Could not delete the physical file", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}