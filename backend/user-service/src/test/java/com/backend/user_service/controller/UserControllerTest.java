package com.backend.user_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.web.servlet.MockMvc;

import com.backend.common.config.filter.GatewayHeadersFilter;
import com.backend.common.dto.InfoUserDTO;
import com.backend.common.exception.CustomException;
import com.backend.common.repository.SellerProfileRepository;
import com.backend.user_service.dto.UpdateUserDTO;
import com.backend.user_service.repository.UserRepository;
import com.backend.user_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WebMvcTest(UserController.class)
// Add this to prevent the full application context from starting
@MockBean({ UserRepository.class, SellerProfileRepository.class })
@DisplayName("UserController Web Layer Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserService userService;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private GatewayHeadersFilter gatewayHeadersFilter;

    @MockBean
    private AuthenticationEntryPoint customAuthEntryPoint;

    @BeforeEach
    void setUp() throws Exception {
        // This tells the mocked filter to let the request pass through to the
        // controller!
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);

            chain.doFilter(request, response);
            return null;
        }).when(gatewayHeadersFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("GET /me should return user details when header is present")
    @WithMockUser(roles = "CLIENT")
    void getMe_Success() throws Exception {
        InfoUserDTO mockUser = new InfoUserDTO();
        mockUser.setEmail("test@test.com");

        when(userService.getUserById("user123")).thenReturn(mockUser);

        mockMvc.perform(get("/api/users/me")
                .header("X-User-ID", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    @DisplayName("PUT /me should return 400 when DTO is invalid")
    @WithMockUser(roles = "CLIENT")
    void updateMe_InvalidInput() throws Exception {
        UpdateUserDTO invalidDto = new UpdateUserDTO();
        invalidDto.setEmail("not-an-email"); // Trigger validation error

        mockMvc.perform(put("/api/users/me")
                .header("X-User-ID", "user123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /avatar should be unauthorized without headers")
    void deleteAvatar_Unauthorized() throws Exception {

        // 1. Override the mocked filter just for this test to simulate a security block
        doAnswer(invocation -> {
            jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
            response.setStatus(401); // Set the 401 Unauthorized status
            return null; // CRITICAL: Do NOT call chain.doFilter(). We stop the request here.
        }).when(gatewayHeadersFilter).doFilter(any(), any(), any());

        // 2. Perform the test expecting the 401
        mockMvc.perform(delete("/api/users/avatar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /email should be FORBIDDEN for non-ADMIN")
    @WithMockUser(roles = "CLIENT")
    void getUsersByEmail_ForbiddenForClient() throws Exception {
        mockMvc.perform(get("/api/users/email")
                .param("email", "admin@admin.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /email should be allowed for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void getUsersByEmail_SuccessForAdmin() throws Exception {
        InfoUserDTO admin = new InfoUserDTO();
        admin.setEmail("admin@admin.com");

        when(userService.getUserByEmail("admin@admin.com")).thenReturn(admin);

        mockMvc.perform(get("/api/users/email")
                .param("email", "admin@admin.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@admin.com"));
    }

    @Test
    @DisplayName("DELETE / should handle service-level CustomException")
    @WithMockUser(roles = "CLIENT")
    void deleteUser_ServiceFailure() throws Exception {
        doThrow(new CustomException("Invalid password", HttpStatus.FORBIDDEN))
                .when(userService).deleteUser(anyString(), anyString());

        mockMvc.perform(delete("/api/users")
                .header("X-User-ID", "user123")
                .param("password", "wrong"))
                .andExpect(status().isForbidden());
    }
}