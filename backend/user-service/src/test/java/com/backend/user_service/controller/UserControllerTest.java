package com.backend.user_service.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.exception.CustomException;
import com.backend.common.repository.SellerProfileRepository;
import com.backend.user_service.dto.UpdateUserDTO;
import com.backend.user_service.repository.UserRepository;
import com.backend.user_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;


@WebMvcTest(UserController.class)
// Add this to prevent the full application context from starting
@MockBean({UserRepository.class, SellerProfileRepository.class})
@DisplayName("UserController Web Layer Tests")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private UserService userService;
    @Autowired private ObjectMapper objectMapper;

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
    @DisplayName("DELETE /avatar should be forbidden for unauthenticated")
    void deleteAvatar_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/users/avatar")
                .header("X-User-ID", "user123"))
                .andExpect(status().isUnauthorized()); // Security filter catches it
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