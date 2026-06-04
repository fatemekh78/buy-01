package com.backend.user_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.common.dto.SellerProfileDTO;
import com.backend.common.entity.SellerProfile;
import com.backend.common.repository.SellerProfileRepository;
import com.backend.user_service.repository.UserRepository;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerProfileService Unit Tests")
@SuppressWarnings({ "unchecked", "rawtypes" })
class SellerProfileServiceTest {

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private SellerProfileService sellerProfileService;

    private SellerProfile mockProfile;

    // --- WebClient Chain Mocks ---
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    @Mock
    private Mono<Map<String, Object>> monoResponse;

    @BeforeEach
    void setUp() {
        mockProfile = new SellerProfile();
        mockProfile.setSellerId("seller123");
        mockProfile.setSellerName("Tech Store");
        mockProfile.setTotalRevenue(BigDecimal.valueOf(500.50));
        mockProfile.setTotalSales(20);
    }

    /**
     * Helper method to mock the complex WebClient builder chain
     */
    private void setupWebClientMock() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(monoResponse);
    }

    @Test
    @DisplayName("Should successfully fetch basic profile from DB")
    void getSellerProfile_Success() {
        when(sellerProfileRepository.findBySellerId("seller123")).thenReturn(Optional.of(mockProfile));

        SellerProfileDTO result = sellerProfileService.getSellerProfile("seller123");

        assertThat(result.getSellerId()).isEqualTo("seller123");
        assertThat(result.getSellerName()).isEqualTo("Tech Store");
    }

    @Test
    @DisplayName("Should create default profile if not found in DB")
    void getSellerProfile_NotFound_CreatesDefault() {
        when(sellerProfileRepository.findBySellerId("seller999")).thenReturn(Optional.empty());

        SellerProfile defaultProfile = new SellerProfile();
        defaultProfile.setSellerId("seller999");
        defaultProfile.setSellerName("New Seller");

        when(sellerProfileRepository.save(any(SellerProfile.class))).thenReturn(defaultProfile);

        SellerProfileDTO result = sellerProfileService.getSellerProfile("seller999");

        assertThat(result.getSellerId()).isEqualTo("seller999");
        verify(sellerProfileRepository).save(any(SellerProfile.class));
    }

    @Test
    @DisplayName("Should fetch seller stats from orders-service successfully")
    void getSellerStatistics_SuccessWithLiveStats() {
        setupWebClientMock();

        Map<String, Object> mockStats = Map.of(
                "totalRevenue", 1500.50,
                "totalItemsSold", 45);

        when(monoResponse.block()).thenReturn(mockStats);

        SellerProfileDTO result = sellerProfileService.getSellerStatistics("seller123");

        assertThat(result.getSellerId()).isEqualTo("seller123");
        assertThat(result.getTotalRevenue()).isEqualTo(BigDecimal.valueOf(1500.50));
        assertThat(result.getTotalSales()).isEqualTo(45);
    }

    @Test
    @DisplayName("Should return fallback DTO when external service throws error")
    void getSellerStatistics_ExternalServiceFails() {
        setupWebClientMock();

        // Simulates the orders-service being offline or throwing a 500 error
        when(monoResponse.block()).thenThrow(new RuntimeException("Orders service down"));

        SellerProfileDTO result = sellerProfileService.getSellerStatistics("seller123");

        // Verify it caught the error and returned the fallback DTO safely
        assertThat(result).isNotNull();
        assertThat(result.getSellerId()).isEqualTo("seller123");
        assertThat(result.getTotalRevenue()).isNull();
    }

    @Test
    @DisplayName("Should handle OK response from orders-service with null body safely")
    void getSellerStatistics_EmptyBody() {
        setupWebClientMock();

        // Simulates a 200 OK but with no data body
        when(monoResponse.block()).thenReturn(null);

        SellerProfileDTO result = sellerProfileService.getSellerStatistics("seller123");

        assertThat(result).isNotNull();
        assertThat(result.getSellerId()).isEqualTo("seller123");
        assertThat(result.getTotalRevenue()).isNull();
    }
}