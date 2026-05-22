package com.backend.product_service.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.common.exception.CustomException;
import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.service.ProductSearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for product search and filtering
 * Single endpoint handles all cases: all products, search, and filters
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Search API", description = "Advanced endpoints for querying, searching, and filtering the product catalog")
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    @GetMapping("/search")
    @Operation(
        summary = "Search and filter products", 
        description = "Performs a dynamic search against the product catalog. All parameters are optional. If no parameters are provided, it returns all products paginated."
    )
    public ResponseEntity<Page<ProductCardDTO>> searchProducts(
            @Parameter(description = "Keyword search (matches against name and description)") 
            @RequestParam(name = "q", required = false) String keyword,
            
            @Parameter(description = "Minimum price bound") 
            @RequestParam(required = false) BigDecimal minPrice,
            
            @Parameter(description = "Maximum price bound") 
            @RequestParam(required = false) BigDecimal maxPrice,
            
            @Parameter(description = "Minimum inventory quantity bound") 
            @RequestParam(required = false) Integer minQuantity,
            
            @Parameter(description = "Maximum inventory quantity bound") 
            @RequestParam(required = false) Integer maxQuantity,
            
            @Parameter(description = "Filter products created after this date (ISO 8601 or yyyy-MM-dd)") 
            @RequestParam(required = false) String startDate,
            
            @Parameter(description = "Filter products created before this date (ISO 8601 or yyyy-MM-dd)") 
            @RequestParam(required = false) String endDate,
            
            @Parameter(description = "Optional seller ID to flag products owned by the requester") 
            @RequestHeader(value = "X-User-ID", required = false) String sellerId,
            
            @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Search request - keyword: {}, minPrice: {}, maxPrice: {}, minQty: {}, maxQty: {}",
                keyword, minPrice, maxPrice, minQuantity, maxQuantity);

        // Standardized Error Handling mapping to GlobalExceptionHandler
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)) {
            throw new CustomException("Price bounds cannot be negative", HttpStatus.BAD_REQUEST);
        }
        
        if ((minQuantity != null && minQuantity < 0) || (maxQuantity != null && maxQuantity < 0)) {
            throw new CustomException("Quantity bounds cannot be negative", HttpStatus.BAD_REQUEST);
        }
        
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new CustomException("Minimum price cannot be greater than maximum price", HttpStatus.BAD_REQUEST);
        }
        
        if (minQuantity != null && maxQuantity != null && minQuantity > maxQuantity) {
            throw new CustomException("Minimum quantity cannot be greater than maximum quantity", HttpStatus.BAD_REQUEST);
        }

        Instant parsedStartDate = parseDate(startDate);
        Instant parsedEndDate = parseDate(endDate);

        if (parsedStartDate != null && parsedEndDate != null && parsedStartDate.isAfter(parsedEndDate)) {
            throw new CustomException("Start date cannot be after end date", HttpStatus.BAD_REQUEST);
        }

        Page<ProductCardDTO> results = productSearchService.searchAndFilter(
                keyword, minPrice, maxPrice, minQuantity, maxQuantity,
                parsedStartDate, parsedEndDate, sellerId, pageable);

        log.info("Search returned {} results on page {}", results.getNumberOfElements(), pageable.getPageNumber());
        return ResponseEntity.ok(results);
    }

    /**
     * Parse an ISO-8601 instant or a plain yyyy-MM-dd (HTML date input) string to
     * Instant at UTC start of day.
     */
    private Instant parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(date);
        } catch (Exception ignored) {
            // Try simple date without time
        }
        try {
            LocalDate localDate = LocalDate.parse(date);
            return localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception ex) {
            // Throw CustomException so the frontend gets a clean 400 Bad Request JSON response
            throw new CustomException("Invalid date format: " + date + ". Expected ISO-8601 or yyyy-MM-dd", HttpStatus.BAD_REQUEST);
        }
    }
}