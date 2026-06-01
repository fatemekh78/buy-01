package com.backend.orders_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OrderSearchValidation Unit Tests")
class OrderSearchValidationTest {

    @Nested
    @DisplayName("Tests for validatePriceRange()")
    class ValidatePriceRangeTests {

        @Test
        @DisplayName("Should return true when both prices are null")
        void testValidPriceRangeBothNull() {
            assertThat(OrderSearchValidation.validatePriceRange(null, null)).isTrue();
        }

        @Test
        @DisplayName("Should return true for valid price ranges (min <= max)")
        void testValidPriceRange() {
            assertThat(OrderSearchValidation.validatePriceRange(10.0, 50.0)).isTrue();
            assertThat(OrderSearchValidation.validatePriceRange(20.0, 20.0)).isTrue(); // Equal is valid
            assertThat(OrderSearchValidation.validatePriceRange(15.5, null)).isTrue(); // Only min
            assertThat(OrderSearchValidation.validatePriceRange(null, 99.99)).isTrue(); // Only max
        }

        @Test
        @DisplayName("Should return false when prices are negative")
        void testInvalidPriceRangeNegative() {
            assertThat(OrderSearchValidation.validatePriceRange(-5.0, 50.0)).isFalse();
            assertThat(OrderSearchValidation.validatePriceRange(10.0, -10.0)).isFalse();
            assertThat(OrderSearchValidation.validatePriceRange(-20.0, -10.0)).isFalse();
        }

        @Test
        @DisplayName("Should return false when minPrice > maxPrice")
        void testInvalidPriceRangeMinGreaterThanMax() {
            assertThat(OrderSearchValidation.validatePriceRange(50.0, 10.0)).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests for parseDate()")
    class ParseDateTests {

        @Test
        @DisplayName("Should successfully parse full ISO 8601 date-time string")
        void testParseDateIso8601() {
            String isoString = "2026-05-19T18:00:00Z";
            Instant result = OrderSearchValidation.parseDate(isoString);
            
            assertThat(result).isEqualTo(Instant.parse(isoString));
        }

        @Test
        @DisplayName("Should successfully parse simple yyyy-MM-dd date string (Defaults to start of day UTC)")
        void testParseDateSimpleFormat() {
            String dateString = "2026-05-19";
            Instant result = OrderSearchValidation.parseDate(dateString);
            
            Instant expected = LocalDate.of(2026, 5, 19).atStartOfDay(ZoneOffset.UTC).toInstant();
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid date formats")
        void testParseDateInvalidFormat() {
            assertThatThrownBy(() -> OrderSearchValidation.parseDate("invalid-date-string"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid date format: invalid-date-string");
            
            assertThatThrownBy(() -> OrderSearchValidation.parseDate("19-05-2026")) // Wrong order
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Tests for parseDateRange()")
    class ParseDateRangeTests {

        @Test
        @DisplayName("Should return array of nulls when inputs are null or blank")
        void testParseDateRangeNullOrBlank() {
            Instant[] result1 = OrderSearchValidation.parseDateRange(null, null);
            assertThat(result1).containsExactly(null, null);

            Instant[] result2 = OrderSearchValidation.parseDateRange("", "   ");
            assertThat(result2).containsExactly(null, null);
        }

        @Test
        @DisplayName("Should parse valid date ranges correctly")
        void testParseDateRangeValid() {
            String minDate = "2026-01-01";
            String maxDate = "2026-12-31";
            
            Instant[] result = OrderSearchValidation.parseDateRange(minDate, maxDate);
            
            assertThat(result).hasSize(2);
            assertThat(result[0]).isEqualTo(OrderSearchValidation.parseDate(minDate));
            assertThat(result[1]).isEqualTo(OrderSearchValidation.parseDate(maxDate));
        }

        @Test
        @DisplayName("Should return null when minDate > maxDate")
        void testParseDateRangeMinGreaterThanMax() {
            // maxDate is chronologically BEFORE minDate
            Instant[] result = OrderSearchValidation.parseDateRange("2026-12-31", "2026-01-01");
            
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null and catch exception if parsing fails")
        void testParseDateRangeInvalidFormat() {
            Instant[] result = OrderSearchValidation.parseDateRange("bad-date", "2026-12-31");
            
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Tests for validateSearchParameters()")
    class ValidateSearchParametersTests {

        @Test
        @DisplayName("Should return true when all parameters are completely valid")
        void testAllValid() {
            boolean result = OrderSearchValidation.validateSearchParameters(
                    10.0, 50.0, 
                    "2026-01-01", "2026-12-31"
            );
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when all parameters are null/empty")
        void testAllNull() {
            boolean result = OrderSearchValidation.validateSearchParameters(
                    null, null, 
                    null, ""
            );
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when price validation fails")
        void testInvalidPrice() {
            boolean result = OrderSearchValidation.validateSearchParameters(
                    100.0, 10.0, // Invalid: min > max
                    "2026-01-01", "2026-12-31"
            );
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when date validation fails (chronology error)")
        void testInvalidDatesOrder() {
            boolean result = OrderSearchValidation.validateSearchParameters(
                    10.0, 50.0, 
                    "2026-12-31", "2026-01-01" // Invalid: min > max
            );
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when date parsing fails (format error)")
        void testInvalidDateFormat() {
            boolean result = OrderSearchValidation.validateSearchParameters(
                    10.0, 50.0, 
                    "invalid-date", null
            );
            assertThat(result).isFalse();
        }
    }
}