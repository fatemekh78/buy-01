package com.backend.api_gateway.exception;

import org.springframework.http.HttpStatus;

/**
 * Base custom exception for the API Gateway.
 * Allows custom gateway filters to throw business-logic exceptions with
 * specific HTTP status codes,
 * which are then formatted into a standardized JSON response by the
 * WebFluxGlobalExceptionHandler.
 */
public class CustomException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Constructs a new gateway exception.
     *
     * @param message The descriptive error message.
     * @param status  The specific HTTP status code (e.g., UNAUTHORIZED, FORBIDDEN).
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