package com.backend.common.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * Centralized exception interception for all microservices.
 * Formats all backend exceptions into a predictable JSON schema for the Angular
 * frontend.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP = "timestamp";
    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";

    /**
     * Handles manually thrown business logic exceptions.
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustom(CustomException ex) {
        log.warn("Business rule exception ({}): {}", ex.getStatus(), ex.getMessage());
        return buildResponse(ex.getStatus(), ex.getMessage());
    }

    /**
     * Handles Spring validation errors (e.g., @NotBlank, @Email in DTOs).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new HashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Payload validation failed: {}", validationErrors);
        return buildFieldErrorResponse(HttpStatus.BAD_REQUEST, validationErrors);
    }

    /**
     * Handles file size limit breaches in the media-service.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.warn("File upload blocked: Max upload size exceeded.");
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file exceeds the maximum allowed size.");
    }

    /**
     * Handles Spring Security authentication failures from Gateway/Services.
     */
    @ExceptionHandler({ AuthenticationException.class, UsernameNotFoundException.class })
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(RuntimeException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials or token.");
    }

    /**
     * Handles requests made to endpoints that do not exist.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoHandlerFoundException ex) {
        log.warn("Endpoint not found: {}", ex.getRequestURL());
        return buildResponse(HttpStatus.NOT_FOUND, "The requested resource was not found.");
    }

    /**
     * Catch-All for unhandled system crashes or NullPointerExceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("CRITICAL: Unhandled internal server error occurred.", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred on the server.");
    }

    /**
     * Prevent the global exception handler from swallowing security exceptions.
     * Re-throwing them allows Spring Security's ExceptionTranslationFilter to step
     * in
     * and correctly route to your CustomAuthEntryPoint (401) or AccessDeniedHandler
     * (403).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDeniedException(AccessDeniedException ex) {
        throw ex;
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper Methods (Frontend JSON Contract)
    // ─────────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, status.value());
        body.put(ERROR, status.getReasonPhrase());
        body.put(MESSAGE, message);
        return new ResponseEntity<>(body, status);
    }

    private ResponseEntity<Map<String, Object>> buildFieldErrorResponse(HttpStatus status, Map<String, String> errors) {
        Map<String, Object> body = new HashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, status.value());
        body.put(ERROR, status.getReasonPhrase());
        body.put("errors", errors);
        return new ResponseEntity<>(body, status);
    }
}