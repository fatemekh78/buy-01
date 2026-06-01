package com.backend.user_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.backend.common.dto.InfoUserDTO;
import com.backend.common.exception.CustomException;
import com.backend.user_service.dto.UpdateUserDTO;
import com.backend.user_service.service.UserService;
import com.backend.user_service.service.UserService.UserUpdateResult;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Tests (Pure Mockito)")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private InfoUserDTO mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new InfoUserDTO();
        mockUser.setEmail("test@example.com");
        mockUser.setFirstName("John");
        mockUser.setLastName("Doe");
    }

    // ────────────────────────────────────────────────────────────────
    // GET /me
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("getMe - Should return user details successfully")
    void getMe_Success() {
        when(userService.getUserById("user123")).thenReturn(mockUser);

        ResponseEntity<InfoUserDTO> response = userController.getMe("user123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("test@example.com", response.getBody().getEmail());
        verify(userService).getUserById("user123");
    }

    // ────────────────────────────────────────────────────────────────
    // PUT /me
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("updateMe - Success WITHOUT email change (No new cookie)")
    void updateMe_NoEmailChange_Success() {
        UpdateUserDTO updateDto = new UpdateUserDTO();
        updateDto.setFirstName("Johnny");
        
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        UserUpdateResult mockResult = new UserUpdateResult(false, "test@example.com");

        when(userService.updateUserInfo("user123", updateDto)).thenReturn(mockResult);

        ResponseEntity<Map<String, String>> response = userController.updateMe(updateDto, "user123", mockResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Profile updated successfully", response.getBody().get("message"));
        
        // Verify no cookie was added since email didn't change
        verifyNoInteractions(mockResponse); 
    }

    @Test
    @DisplayName("updateMe - Success WITH email change (Generates new cookie)")
    void updateMe_WithEmailChange_Success() {
        UpdateUserDTO updateDto = new UpdateUserDTO();
        updateDto.setEmail("new@example.com");
        
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        UserUpdateResult mockResult = new UserUpdateResult(true, "new@example.com");
        Cookie mockCookie = new Cookie("jwt", "fake-token");

        when(userService.updateUserInfo("user123", updateDto)).thenReturn(mockResult);
        when(userService.generateCookie("new@example.com")).thenReturn(mockCookie);

        ResponseEntity<Map<String, String>> response = userController.updateMe(updateDto, "user123", mockResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Profile updated successfully", response.getBody().get("message"));
        
        // Verify the controller added the new cookie to the response
        verify(mockResponse).addCookie(mockCookie);
    }

    // ────────────────────────────────────────────────────────────────
    // DELETE /
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("deleteUser - Should return success message on valid deletion")
    void deleteUser_Success() {
        doNothing().when(userService).deleteUser("user123", "myPassword123");

        ResponseEntity<Map<String, String>> response = userController.deleteUser("user123", "myPassword123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User account deleted successfully", response.getBody().get("message"));
        verify(userService).deleteUser("user123", "myPassword123");
    }

    @Test
    @DisplayName("deleteUser - Should bubble up exception on invalid password")
    void deleteUser_InvalidPassword_ThrowsException() {
        doThrow(new CustomException("Invalid password", HttpStatus.FORBIDDEN))
                .when(userService).deleteUser("user123", "wrongPassword");

        assertThrows(CustomException.class, () -> {
            userController.deleteUser("user123", "wrongPassword");
        });
    }

    // ────────────────────────────────────────────────────────────────
    // DELETE /avatar
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("deleteAvatar - Should return success string")
    void deleteAvatar_Success() {
        doNothing().when(userService).deleteAvatar("user123");

        ResponseEntity<String> response = userController.deleteAvatar("user123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Avatar deleted successfully", response.getBody());
        verify(userService).deleteAvatar("user123");
    }

    // ────────────────────────────────────────────────────────────────
    // GET /seller
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("getSellerById - Should return seller details")
    void getSellerById_Success() {
        when(userService.getUserById("seller123")).thenReturn(mockUser);

        ResponseEntity<InfoUserDTO> response = userController.getSellerById("seller123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("test@example.com", response.getBody().getEmail());
        verify(userService).getUserById("seller123");
    }

    // ────────────────────────────────────────────────────────────────
    // GET /email
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("getUsersByEmail - Should return user details")
    void getUsersByEmail_Success() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(mockUser);

        ResponseEntity<InfoUserDTO> response = userController.getUsersByEmail("test@example.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("test@example.com", response.getBody().getEmail());
        verify(userService).getUserByEmail("test@example.com");
    }

    // ────────────────────────────────────────────────────────────────
    // POST /newAvatar
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("handleUserNewAvatar - Should accept file and return success map")
    void handleUserNewAvatar_Success() {
        MultipartFile mockFile = mock(MultipartFile.class);

        doNothing().when(userService).updateUserAvatar(eq("user123"), any(MultipartFile.class));

        ResponseEntity<Map<String, String>> response = userController.handleUserNewAvatar(mockFile, "user123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Avatar updated successfully", response.getBody().get("message"));
        verify(userService).updateUserAvatar("user123", mockFile);
    }
}