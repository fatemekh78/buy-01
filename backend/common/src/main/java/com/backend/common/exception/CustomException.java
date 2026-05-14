package com.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base custom exception for the platform.
 * Allows microservices to throw business-logic exceptions with specific HTTP
 * status codes
 * that are seamlessly intercepted and formatted by the GlobalExceptionHandler.
 */
public class CustomException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Constructs a new CustomException.
     *
     * @param message The descriptive error message meant to be read by the
     *                client/frontend.
     * @param status  The specific HTTP status code (e.g., BAD_REQUEST, NOT_FOUND).
     */
    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * Retrieves the HTTP status associated with this exception.
     *
     * @return HttpStatus
     */
    public HttpStatus getStatus() {
        return this.status;
    }
}