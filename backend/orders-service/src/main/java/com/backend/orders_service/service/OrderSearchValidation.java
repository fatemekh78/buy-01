package com.backend.orders_service.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared validation methods for order search and filtering.
 * Contains pure static utility functions.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 🚨 FIX: Prevents accidental instantiation of this utility class
public class OrderSearchValidation {

    /**
     * Validate price range - ensures both are non-negative and min <= max.
     * * @return false if validation fails, true if valid or both null
     */
    public static boolean validatePriceRange(Double minPrice, Double maxPrice) {
        if ((minPrice != null && minPrice < 0) || (maxPrice != null && maxPrice < 0)) {
            log.warn("Rejecting search: Negative price bounds - minPrice: {}, maxPrice: {}", minPrice, maxPrice);
            return false;
        }

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            log.warn("Rejecting search: minPrice > maxPrice - minPrice: {}, maxPrice: {}", minPrice, maxPrice);
            return false;
        }

        return true;
    }

    /**
     * Parse and validate date range.
     * * @return Instant array [minDate, maxDate] or null on error
     */
    public static Instant[] parseDateRange(String minUpdateDate, String maxUpdateDate) {
        Instant parsedMinDate = null;
        Instant parsedMaxDate = null;

        try {
            // 🚨 FIX: Replaced !isEmpty() with modern Java .isBlank()
            if (minUpdateDate != null && !minUpdateDate.isBlank()) {
                parsedMinDate = parseDate(minUpdateDate);
            }
            if (maxUpdateDate != null && !maxUpdateDate.isBlank()) {
                parsedMaxDate = parseDate(maxUpdateDate);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Failed to parse date range parameters: {}", e.getMessage());
            return null;
        }

        // Date ordering check
        if (parsedMinDate != null && parsedMaxDate != null && parsedMinDate.isAfter(parsedMaxDate)) {
            log.warn("Rejecting search: minDate > maxDate - minDate: {}, maxDate: {}", parsedMinDate, parsedMaxDate);
            return null;
        }

        return new Instant[] { parsedMinDate, parsedMaxDate };
    }

    /**
     * Parse date string in multiple formats (ISO 8601 or yyyy-MM-dd).
     */
    public static Instant parseDate(String dateString) {
        try {
            // Try full ISO 8601 format first (e.g. 2026-05-19T18:00:00Z)
            return Instant.parse(dateString);
        } catch (DateTimeParseException e1) {
            try {
                // Try yyyy-MM-dd format (e.g. 2026-05-19)
                LocalDate localDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
                return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException e2) {
                log.warn("Could not parse date: {}", dateString);
                throw new IllegalArgumentException("Invalid date format: " + dateString);
            }
        }
    }

    /**
     * Validate all search parameters.
     * * @return true if all parameters are valid
     */
    public static boolean validateSearchParameters(
            Double minPrice,
            Double maxPrice,
            String minUpdateDate,
            String maxUpdateDate) {

        // Validate price range
        if (!validatePriceRange(minPrice, maxPrice)) {
            return false;
        }

        // Validate and parse date range
        Instant[] dateRange = parseDateRange(minUpdateDate, maxUpdateDate);

        // If dates were provided but parsing failed (dateRange is null)
        boolean datesProvided = (minUpdateDate != null && !minUpdateDate.isBlank()) ||
                (maxUpdateDate != null && !maxUpdateDate.isBlank());

        if (datesProvided && dateRange == null) {
            return false;
        }

        return true;
    }
}