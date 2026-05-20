package com.backend.user_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.backend.common.dto.InfoUserDTO;
import com.backend.user_service.dto.UpdateUserDTO;
import com.backend.user_service.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    @DisplayName("Get Me should return current user profile")
    void testGetMe_Success() {
        InfoUserDTO mockUser = new InfoUserDTO();
        mockUser.setEmail("test@test.com");

        when(userService.getUserById("user123")).thenReturn(mockUser);

        ResponseEntity<InfoUserDTO> result = userController.getMe("user123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("test@test.com", result.getBody().getEmail());
    }

    @Test
    @DisplayName("Update Me should attach new cookie if email changes")
    void testUpdateMe_WithEmailChange() {
        UpdateUserDTO updateDto = new UpdateUserDTO();
        HttpServletResponse response = mock(HttpServletResponse.class);

        UserService.UserUpdateResult serviceResult = new UserService.UserUpdateResult(true, "new@test.com");
        when(userService.updateUserInfo("user123", updateDto)).thenReturn(serviceResult);

        Cookie mockCookie = new Cookie("jwt", "new_token");
        when(userService.generateCookie("new@test.com")).thenReturn(mockCookie);

        ResponseEntity<Map<String, String>> result = userController.updateMe(updateDto, "user123", response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(response).addCookie(mockCookie);
    }

    @Test
    @DisplayName("Delete User should pass raw password to service")
    void testDeleteUser_Success() {
        doNothing().when(userService).deleteUser("user123", "myPassword");

        ResponseEntity<Map<String, String>> result = userController.deleteUser("user123", "myPassword");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userService).deleteUser("user123", "myPassword");
    }
}