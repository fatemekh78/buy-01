package com.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an uploaded file's MIME type or extension does not
 * match
 * the allowed formats configured in the application (e.g., uploading a PDF
 * instead of a JPEG).
 */
public class InvalidFileTypeException extends CustomException {

    /**
     * Constructs a new InvalidFileTypeException.
     * Automatically assigns an HTTP 415 (Unsupported Media Type) status.
     *
     * @param message The specific error message detailing the invalid file type.
     */
    public InvalidFileTypeException(String message) {
        super(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}