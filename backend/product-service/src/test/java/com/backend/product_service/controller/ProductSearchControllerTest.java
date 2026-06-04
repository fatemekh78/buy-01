package com.backend.product_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.backend.common.exception.CustomException;
import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.service.ProductSearchService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSearchController Tests")
class ProductSearchControllerTest {

    @Mock
    private ProductSearchService productSearchService;

    @InjectMocks
    private ProductSearchController productSearchController;

    private Pageable pageable;
    private List<ProductCardDTO> sampleProducts;
    private Page<ProductCardDTO> samplePage;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 20);
        sampleProducts = List.of(
                new ProductCardDTO("p1", "Laptop", "High-end laptop", 999.99, 5, false, null),
                new ProductCardDTO("p2", "Mouse", "Wireless mouse", 29.99, 50, false, null));
        samplePage = new PageImpl<>(sampleProducts, pageable, 2);
    }

    // =========================================================================
    // Happy-path scenarios
    // =========================================================================

    @Nested
    @DisplayName("Happy path – valid requests")
    class HappyPath {

        @Test
        @DisplayName("No params – returns all products paginated")
        void searchProducts_NoParams_ReturnsAllProducts() {
            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, null, null, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(2, result.getBody().getTotalElements());
            verify(productSearchService).searchAndFilter(
                    null, null, null, null, null, null, null, null, pageable);
        }

        @Test
        @DisplayName("Keyword only – filters by name/description")
        void searchProducts_KeywordOnly_ReturnsMatchingProducts() {
            Page<ProductCardDTO> keywordPage = new PageImpl<>(List.of(sampleProducts.get(0)), pageable, 1);

            when(productSearchService.searchAndFilter(
                    eq("laptop"), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(keywordPage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    "laptop", null, null, null, null, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(1, result.getBody().getTotalElements());
            assertEquals("Laptop", result.getBody().getContent().get(0).getName());
        }

        @Test
        @DisplayName("Price range only – filters by minPrice and maxPrice")
        void searchProducts_PriceRange_ReturnsProductsWithinRange() {
            BigDecimal minPrice = BigDecimal.valueOf(10);
            BigDecimal maxPrice = BigDecimal.valueOf(100);

            when(productSearchService.searchAndFilter(
                    isNull(), eq(minPrice), eq(maxPrice), isNull(), isNull(),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleProducts.get(1)), pageable, 1));

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, minPrice, maxPrice, null, null, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(1, result.getBody().getContent().size());
        }

        @Test
        @DisplayName("Quantity range only – filters by minQuantity and maxQuantity")
        void searchProducts_QuantityRange_ReturnsFilteredProducts() {
            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), eq(1), eq(10),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, 1, 10, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertNotNull(result.getBody());
        }

        @Test
        @DisplayName("Date range with ISO-8601 format – parses correctly")
        void searchProducts_DateRange_Iso8601Format_ParsedAndForwarded() {
            String startDate = "2024-01-01T00:00:00Z";
            String endDate = "2024-12-31T23:59:59Z";
            Instant expectedStart = Instant.parse(startDate);
            Instant expectedEnd = Instant.parse(endDate);

            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), isNull(), isNull(),
                    eq(expectedStart), eq(expectedEnd), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, null, null, startDate, endDate, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            verify(productSearchService).searchAndFilter(
                    null, null, null, null, null, expectedStart, expectedEnd, null, pageable);
        }

        @Test
        @DisplayName("Date range with yyyy-MM-dd format – parses to UTC start of day")
        void searchProducts_DateRange_SimpleFormat_ParsedToUtcStartOfDay() {
            String startDate = "2024-01-01";
            String endDate = "2024-12-31";
            Instant expectedStart = LocalDate.parse(startDate).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant expectedEnd = LocalDate.parse(endDate).atStartOfDay().toInstant(ZoneOffset.UTC);

            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), isNull(), isNull(),
                    eq(expectedStart), eq(expectedEnd), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, null, null, startDate, endDate, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            verify(productSearchService).searchAndFilter(
                    null, null, null, null, null, expectedStart, expectedEnd, null, pageable);
        }

        @Test
        @DisplayName("With sellerId header – forwards sellerId to service")
        void searchProducts_WithSellerId_ForwardsToService() {
            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), eq("seller123"), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, null, null, null, null, "seller123", pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            verify(productSearchService).searchAndFilter(
                    null, null, null, null, null, null, null, "seller123", pageable);
        }

        @Test
        @DisplayName("All params combined – passes every argument to service")
        void searchProducts_AllParamsCombined_ForwardsAll() {
            String startDate = "2024-01-01";
            String endDate = "2024-06-30";
            Instant expectedStart = LocalDate.parse(startDate).atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant expectedEnd = LocalDate.parse(endDate).atStartOfDay().toInstant(ZoneOffset.UTC);
            BigDecimal minPrice = BigDecimal.valueOf(50);
            BigDecimal maxPrice = BigDecimal.valueOf(500);

            when(productSearchService.searchAndFilter(
                    eq("laptop"), eq(minPrice), eq(maxPrice), eq(1), eq(20),
                    eq(expectedStart), eq(expectedEnd), eq("seller123"), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    "laptop", minPrice, maxPrice, 1, 20, startDate, endDate, "seller123", pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(2, result.getBody().getTotalElements());
        }

        @Test
        @DisplayName("Empty result – returns 200 with empty page")
        void searchProducts_NoMatches_ReturnsEmptyPage() {
            when(productSearchService.searchAndFilter(
                    eq("nonexistent"), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    "nonexistent", null, null, null, null, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals(0, result.getBody().getTotalElements());
        }

        @Test
        @DisplayName("Exact same minPrice and maxPrice (boundary) – valid and allowed")
        void searchProducts_MinPriceEqualMaxPrice_IsValid() {
            BigDecimal price = BigDecimal.valueOf(100);

            when(productSearchService.searchAndFilter(
                    isNull(), eq(price), eq(price), isNull(), isNull(),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, price, price, null, null, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
        }

        @Test
        @DisplayName("Exact same minQuantity and maxQuantity (boundary) – valid and allowed")
        void searchProducts_MinQuantityEqualMaxQuantity_IsValid() {
            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), eq(5), eq(5),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, 5, 5, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
        }

        @Test
        @DisplayName("Zero minPrice – valid edge case (price can be zero)")
        void searchProducts_ZeroMinPrice_IsValid() {
            BigDecimal zero = BigDecimal.ZERO;
            BigDecimal maxPrice = BigDecimal.valueOf(100);

            when(productSearchService.searchAndFilter(
                    isNull(), eq(zero), eq(maxPrice), isNull(), isNull(),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, zero, maxPrice, null, null, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
        }

        @Test
        @DisplayName("Zero minQuantity – valid edge case (quantity can be zero)")
        void searchProducts_ZeroMinQuantity_IsValid() {
            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), eq(0), eq(10),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, 0, 10, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
        }

        @Test
        @DisplayName("Exact same startDate and endDate (boundary) – valid and allowed")
        void searchProducts_SameStartAndEndDate_IsValid() {
            String date = "2024-06-15";
            Instant expectedInstant = LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC);

            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), isNull(), isNull(),
                    eq(expectedInstant), eq(expectedInstant), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, null, null, date, date, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
        }
    }

    // =========================================================================
    // Validation – price guards
    // =========================================================================

    @Nested
    @DisplayName("Validation – price bounds")
    class PriceValidation {

        @Test
        @DisplayName("Negative minPrice – throws 400 CustomException")
        void searchProducts_NegativeMinPrice_ThrowsCustomException() {
            CustomException ex = assertThrows(CustomException.class, () -> productSearchController.searchProducts(
                    null, BigDecimal.valueOf(-1), null, null, null,
                    null, null, null, pageable));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals("Price bounds cannot be negative", ex.getMessage());
            verify(productSearchService, never()).searchAndFilter(
                    any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Negative maxPrice – throws 400 CustomException")
        void searchProducts_NegativeMaxPrice_ThrowsCustomException() {
            CustomException ex = assertThrows(CustomException.class, () -> productSearchController.searchProducts(
                    null, null, BigDecimal.valueOf(-5), null, null,
                    null, null, null, pageable));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals("Price bounds cannot be negative", ex.getMessage());
        }

        @Test
        @DisplayName("Both negative prices – throws 400 CustomException")
        void searchProducts_BothNegativePrices_ThrowsCustomException() {
            CustomException ex = assertThrows(CustomException.class, () -> productSearchController.searchProducts(
                    null, BigDecimal.valueOf(-10), BigDecimal.valueOf(-1), null, null,
                    null, null, null, pageable));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        }

        @Test
        @DisplayName("minPrice greater than maxPrice – throws 400 CustomException")
        void searchProducts_MinPriceGreaterThanMaxPrice_ThrowsCustomException() {
            CustomException ex = assertThrows(CustomException.class, () -> productSearchController.searchProducts(
                    null, BigDecimal.valueOf(500), BigDecimal.valueOf(100), null, null,
                    null, null, null, pageable));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals("Minimum price cannot be greater than maximum price", ex.getMessage());
        }

        @Test
        @DisplayName("Only minPrice provided (no maxPrice) – valid, passes through")
        void searchProducts_OnlyMinPrice_IsValid() {
            BigDecimal minPrice = BigDecimal.valueOf(50);

            when(productSearchService.searchAndFilter(
                    isNull(), eq(minPrice), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, minPrice, null, null, null, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
        }

        @Test
        @DisplayName("Only maxPrice provided (no minPrice) – valid, passes through")
        void searchProducts_OnlyMaxPrice_IsValid() {
            BigDecimal maxPrice = BigDecimal.valueOf(200);

            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), eq(maxPrice), isNull(), isNull(),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, maxPrice, null, null, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
        }
    }

    // =========================================================================
    // Validation – quantity guards
    // =========================================================================

    @Nested
    @DisplayName("Validation – quantity bounds")
    class QuantityValidation {

        @Test
        @DisplayName("Negative minQuantity – throws 400 CustomException")
        void searchProducts_NegativeMinQuantity_ThrowsCustomException() {
            CustomException ex = assertThrows(CustomException.class, () -> productSearchController.searchProducts(
                    null, null, null, -1, null,
                    null, null, null, pageable));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals("Quantity bounds cannot be negative", ex.getMessage());
            verify(productSearchService, never()).searchAndFilter(
                    any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Negative maxQuantity – throws 400 CustomException")
        void searchProducts_NegativeMaxQuantity_ThrowsCustomException() {
            CustomException ex = assertThrows(CustomException.class, () -> productSearchController.searchProducts(
                    null, null, null, null, -3,
                    null, null, null, pageable));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals("Quantity bounds cannot be negative", ex.getMessage());
        }

        @Test
        @DisplayName("minQuantity greater than maxQuantity – throws 400 CustomException")
        void searchProducts_MinQuantityGreaterThanMaxQuantity_ThrowsCustomException() {
            CustomException ex = assertThrows(CustomException.class, () -> productSearchController.searchProducts(
                    null, null, null, 20, 5,
                    null, null, null, pageable));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals("Minimum quantity cannot be greater than maximum quantity", ex.getMessage());
        }

        @Test
        @DisplayName("Only minQuantity provided (no maxQuantity) – valid, passes through")
        void searchProducts_OnlyMinQuantity_IsValid() {
            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), eq(5), isNull(),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, 5, null, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
        }

        @Test
        @DisplayName("Only maxQuantity provided (no minQuantity) – valid, passes through")
        void searchProducts_OnlyMaxQuantity_IsValid() {
            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), isNull(), eq(100),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, null, 100, null, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
        }
    }

    // =========================================================================
    // Validation – date guards
    // =========================================================================

    @Nested
    @DisplayName("Validation – date bounds")
    class DateValidation {

        @Test
        @DisplayName("startDate after endDate – throws 400 CustomException")
        void searchProducts_StartDateAfterEndDate_ThrowsCustomException() {
            CustomException ex = assertThrows(CustomException.class, () -> productSearchController.searchProducts(
                    null, null, null, null, null,
                    "2024-12-31", "2024-01-01", null, pageable));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals("Start date cannot be after end date", ex.getMessage());
            verify(productSearchService, never()).searchAndFilter(
                    any(), any(), any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Invalid date string – throws 400 CustomException with descriptive message")
        void searchProducts_InvalidDateFormat_ThrowsCustomException() {
            CustomException ex = assertThrows(CustomException.class, () -> productSearchController.searchProducts(
                    null, null, null, null, null,
                    "not-a-date", null, null, pageable));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals(
                    "Invalid date format: not-a-date. Expected ISO-8601 or yyyy-MM-dd",
                    ex.getMessage());
        }

        @Test
        @DisplayName("Invalid endDate string – throws 400 CustomException")
        void searchProducts_InvalidEndDateFormat_ThrowsCustomException() {
            CustomException ex = assertThrows(CustomException.class, () -> productSearchController.searchProducts(
                    null, null, null, null, null,
                    "2024-01-01", "31/12/2024", null, pageable));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals(
                    "Invalid date format: 31/12/2024. Expected ISO-8601 or yyyy-MM-dd",
                    ex.getMessage());
        }

        @Test
        @DisplayName("Blank startDate string – treated as null, no exception thrown")
        void searchProducts_BlankStartDate_TreatedAsNull() {
            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, null, null, "   ", null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
            // Blank string must resolve to null — service receives null, not a parsed
            // Instant
            verify(productSearchService).searchAndFilter(
                    null, null, null, null, null, null, null, null, pageable);
        }

        @Test
        @DisplayName("Only startDate provided (no endDate) – valid, passes through")
        void searchProducts_OnlyStartDate_IsValid() {
            String startDate = "2024-01-01";
            Instant expectedStart = LocalDate.parse(startDate).atStartOfDay().toInstant(ZoneOffset.UTC);

            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), isNull(), isNull(),
                    eq(expectedStart), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, null, null, startDate, null, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
        }

        @Test
        @DisplayName("Only endDate provided (no startDate) – valid, passes through")
        void searchProducts_OnlyEndDate_IsValid() {
            String endDate = "2024-12-31";
            Instant expectedEnd = LocalDate.parse(endDate).atStartOfDay().toInstant(ZoneOffset.UTC);

            when(productSearchService.searchAndFilter(
                    isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), eq(expectedEnd), isNull(), any(Pageable.class)))
                    .thenReturn(samplePage);

            ResponseEntity<Page<ProductCardDTO>> result = productSearchController.searchProducts(
                    null, null, null, null, null, null, endDate, null, pageable);

            assertEquals(HttpStatus.OK, result.getStatusCode());
        }
    }

    // =========================================================================
    // Validation – multi-guard interaction (guard order matters)
    // =========================================================================

    @Nested
    @DisplayName("Validation – guard evaluation order")
    class GuardOrder {

        @Test
        @DisplayName("Negative price is caught before min>max price check")
        void searchProducts_NegativePriceCheckedBeforeMinMaxOrder() {
            // minPrice is negative AND minPrice > maxPrice — negative check fires first
            CustomException ex = assertThrows(CustomException.class, () -> productSearchController.searchProducts(
                    null, BigDecimal.valueOf(-1), BigDecimal.valueOf(-10), null, null,
                    null, null, null, pageable));

            assertEquals("Price bounds cannot be negative", ex.getMessage());
        }

        @Test
        @DisplayName("Negative quantity is caught before min>max quantity check")
        void searchProducts_NegativeQuantityCheckedBeforeMinMaxOrder() {
            CustomException ex = assertThrows(CustomException.class, () -> productSearchController.searchProducts(
                    null, null, null, -1, -10,
                    null, null, null, pageable));

            assertEquals("Quantity bounds cannot be negative", ex.getMessage());
        }
    }
}