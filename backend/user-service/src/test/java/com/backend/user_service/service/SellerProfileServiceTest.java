package com.backend.user_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.backend.common.dto.SellerProfileDTO;
import com.backend.common.entity.SellerProfile;
import com.backend.common.exception.CustomException;
import com.backend.common.repository.SellerProfileRepository;
import com.backend.user_service.model.User;
import com.backend.user_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerProfileService Unit Tests")
class SellerProfileServiceTest {

    @Mock private SellerProfileRepository sellerProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private SellerProfileService sellerProfileService;

    private SellerProfile mockProfile;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockProfile = SellerProfile.builder()
                .sellerId("seller123")
                .sellerName("Tech Store")
                .shopDescription("Selling gadgets")
                .totalSales(10)
                .totalRevenue(new BigDecimal("500.00"))
                .joinDate(Instant.now())
                .build();

        mockUser = User.builder()
                .id("seller123")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    // ==========================================
    // GET PROFILE & INITIALIZATION TESTS
    // ==========================================

    @Test
    @DisplayName("Should return existing profile directly")
    void getSellerProfile_ExistingProfile() {
        when(sellerProfileRepository.findBySellerId("seller123")).thenReturn(Optional.of(mockProfile));

        SellerProfileDTO result = sellerProfileService.getSellerProfile("seller123");

        assertThat(result.getSellerName()).isEqualTo("Tech Store");
        verify(sellerProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create default profile if none exists (User Found)")
    void getSellerProfile_NewProfile_UserExists() {
        when(sellerProfileRepository.findBySellerId("seller123")).thenReturn(Optional.empty());
        when(userRepository.findById("seller123")).thenReturn(Optional.of(mockUser));
        
        // Mock the save to return whatever is passed into it
        when(sellerProfileRepository.save(any(SellerProfile.class))).thenAnswer(i -> i.getArguments()[0]);

        SellerProfileDTO result = sellerProfileService.getSellerProfile("seller123");

        assertThat(result.getSellerName()).isEqualTo("John Doe"); // Pulled from User repo
        verify(sellerProfileRepository).save(any(SellerProfile.class));
    }

    @Test
    @DisplayName("Should create default profile if none exists (User Not Found)")
    void getSellerProfile_NewProfile_UserNotFound() {
        when(sellerProfileRepository.findBySellerId("seller123")).thenReturn(Optional.empty());
        when(userRepository.findById("seller123")).thenReturn(Optional.empty());
        
        when(sellerProfileRepository.save(any(SellerProfile.class))).thenAnswer(i -> i.getArguments()[0]);

        SellerProfileDTO result = sellerProfileService.getSellerProfile("seller123");

        assertThat(result.getSellerName()).isEqualTo("New Shop"); // Fallback name
        verify(sellerProfileRepository).save(any(SellerProfile.class));
    }

    // ==========================================
    // STATISTICS & REST_TEMPLATE TESTS
    // ==========================================

    @Test
    @DisplayName("Should throw 404 when getting stats for non-existent profile")
    void getSellerStatistics_ProfileNotFound() {
        when(sellerProfileRepository.findBySellerId("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sellerProfileService.getSellerStatistics("invalid"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Seller profile not found");
    }

    @Test
    @DisplayName("Should fetch live stats from orders-service and update DTO")
    void getSellerStatistics_SuccessWithLiveStats() {
        when(sellerProfileRepository.findBySellerId("seller123")).thenReturn(Optional.of(mockProfile));

        // Mock the RestTemplate exchange
        Map<String, Object> liveStats = Map.of(
            "totalSales", 50,
            "totalRevenue", 2500.75
        );
        ResponseEntity<Map<String, Object>> mockResponse = new ResponseEntity<>(liveStats, HttpStatus.OK);
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponse);

        SellerProfileDTO result = sellerProfileService.getSellerStatistics("seller123");

        // Assert it combined base stats with the live fetched stats
        assertThat(result.getSellerName()).isEqualTo("Tech Store");
        assertThat(result.getTotalSales()).isEqualTo(50);
        assertThat(result.getTotalRevenue()).isEqualTo(new BigDecimal("2500.75"));
    }

    @Test
    @DisplayName("Should fail silently and return base stats if orders-service is down")
    void getSellerStatistics_ExternalServiceFails() {
        when(sellerProfileRepository.findBySellerId("seller123")).thenReturn(Optional.of(mockProfile));

        // Force RestTemplate to throw an exception
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenThrow(new RestClientException("Connection refused"));

        SellerProfileDTO result = sellerProfileService.getSellerStatistics("seller123");

        // Assert it gracefully returned the stale/base stats without crashing
        assertThat(result.getTotalSales()).isEqualTo(10);
        assertThat(result.getTotalRevenue()).isEqualTo(new BigDecimal("500.00"));
    }

    // ==========================================
    // UPDATE PROFILE TESTS
    // ==========================================

    @Test
    @DisplayName("Should partially update an existing profile")
    void updateProfile_ExistingProfile_PartialUpdate() {
        when(sellerProfileRepository.findBySellerId("seller123")).thenReturn(Optional.of(mockProfile));
        when(sellerProfileRepository.save(any(SellerProfile.class))).thenAnswer(i -> i.getArguments()[0]);

        SellerProfileDTO updateRequest = SellerProfileDTO.builder()
                .shopDescription("New amazing description") // Only updating this
                .build();

        SellerProfileDTO result = sellerProfileService.updateProfile("seller123", updateRequest);

        assertThat(result.getSellerName()).isEqualTo("Tech Store"); // Unchanged
        assertThat(result.getShopDescription()).isEqualTo("New amazing description"); // Changed
        verify(sellerProfileRepository).save(mockProfile);
    }

    @Test
    @DisplayName("Should create default profile then update if none exists initially")
    void updateProfile_NewProfile() {
        when(sellerProfileRepository.findBySellerId("seller123")).thenReturn(Optional.empty());
        when(userRepository.findById("seller123")).thenReturn(Optional.of(mockUser));
        
        // This simulates the save inside createDefaultProfile AND the save inside updateProfile
        when(sellerProfileRepository.save(any(SellerProfile.class))).thenAnswer(i -> i.getArguments()[0]);

        SellerProfileDTO updateRequest = SellerProfileDTO.builder()
                .sellerName("My Brand New Store")
                .build();

        SellerProfileDTO result = sellerProfileService.updateProfile("seller123", updateRequest);

        assertThat(result.getSellerName()).isEqualTo("My Brand New Store");
        // Verify save was called twice (once for init, once for update)
        verify(sellerProfileRepository, times(2)).save(any(SellerProfile.class));
    }
}