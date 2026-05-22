package com.backend.product_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.product_service.dto.ProductCardDTO;
import com.backend.product_service.model.Product;
import com.backend.product_service.repository.ProductRepository;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSearchService Unit Tests")
class ProductSearchServiceUnitTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private ProductSearchService productSearchService;

    private Pageable pageable;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        product1 = Product.builder()
                .id("prod1")
                .name("Laptop")
                .description("Gaming Laptop")
                .price(1500.0)
                .quantity(5)
                .sellerID("sellerA")
                .build();

        product2 = Product.builder()
                .id("prod2")
                .name("Mouse")
                .description("Wireless Mouse")
                .price(50.0)
                .quantity(20)
                .sellerID("sellerB")
                .build();

        // Leniently configure standard web client builder to avoid UnnecessaryStubbing exceptions
        lenient().when(webClientBuilder.build()).thenReturn(webClient);
    }

    @Test
    @DisplayName("Should return empty page directly without calling WebClient if repository finds no products")
    void searchAndFilter_EmptyResult() {
        // Arrange
        when(productRepository.searchAndFilterProducts(
                any(), any(), any(), any(), any(), any(), any(), eq(pageable)
        )).thenReturn(Page.empty(pageable));

        // Act
        Page<ProductCardDTO> result = productSearchService.searchAndFilter(
                "nonexistent", null, null, null, null, null, null, "sellerA", pageable
        );

        // Assert
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalElements()).isEqualTo(0);
        
        // Ensure WebClient is NEVER called when the database result is empty
        verify(webClient, never()).get();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Should successfully map products, fetch images, and assign correct isCreator flags")
    void searchAndFilter_Success_WithCreatorLogic() {
        // Arrange
        Page<Product> dbPage = new PageImpl<>(List.of(product1, product2), pageable, 2);
        
        when(productRepository.searchAndFilterProducts(
                any(), any(), any(), any(), any(), any(), any(), eq(pageable)
        )).thenReturn(dbPage);

        // Mock WebClient chain
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(List.of("http://image1.jpg", "http://image2.jpg")));

        // Act - Requesting as 'sellerA'
        Page<ProductCardDTO> result = productSearchService.searchAndFilter(
                null, null, null, null, null, null, null, "sellerA", pageable
        );

        // Assert
        assertThat(result.getContent()).hasSize(2);
        
        // Check Product 1 (Owned by sellerA)
        ProductCardDTO dto1 = result.getContent().get(0);
        assertThat(dto1.getId()).isEqualTo("prod1");
        assertThat(dto1.isCreatedByMe()).isTrue(); // sellerA == sellerA
        assertThat(dto1.getImageUrls()).containsExactly("http://image1.jpg", "http://image2.jpg");

        // Check Product 2 (Owned by sellerB)
        ProductCardDTO dto2 = result.getContent().get(1);
        assertThat(dto2.getId()).isEqualTo("prod2");
        assertThat(dto2.isCreatedByMe()).isFalse(); // sellerB != sellerA
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Should recover gracefully and return empty image list if MEDIA-SERVICE throws an error")
    void searchAndFilter_WebClientFallback() {
        // Arrange
        Page<Product> dbPage = new PageImpl<>(List.of(product1), pageable, 1);
        
        when(productRepository.searchAndFilterProducts(
                any(), any(), any(), any(), any(), any(), any(), eq(pageable)
        )).thenReturn(dbPage);

        // Mock WebClient chain to simulate an HTTP error (e.g., 500 Internal Server Error)
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Media service is down!")));

        // Act
        Page<ProductCardDTO> result = productSearchService.searchAndFilter(
                null, null, null, null, null, null, null, null, pageable
        );

        // Assert
        assertThat(result.getContent()).hasSize(1);
        ProductCardDTO dto1 = result.getContent().get(0);
        
        // The service should not crash! It should just return an empty image list.
        assertThat(dto1.getImageUrls()).isEmpty();
    }

    @Test
    @DisplayName("Should accurately map BigDecimal inputs to Double for the repository layer")
    void searchAndFilter_BigDecimalMapping() {
        // Arrange
        when(productRepository.searchAndFilterProducts(
                any(), any(), any(), any(), any(), any(), any(), eq(pageable)
        )).thenReturn(Page.empty(pageable));

        BigDecimal minPrice = new BigDecimal("100.50");
        BigDecimal maxPrice = new BigDecimal("500.99");
        Instant start = Instant.now();

        // Act
        productSearchService.searchAndFilter(
                "phone", minPrice, maxPrice, 1, 10, start, null, null, pageable
        );

        // Assert
        // Verify that the repository was called with the exact Double equivalents
        verify(productRepository).searchAndFilterProducts(
                eq("phone"),
                eq(100.5D),
                eq(500.99D),
                eq(1),
                eq(10),
                eq(start),
                isNull(),
                eq(pageable)
        );
    }
}