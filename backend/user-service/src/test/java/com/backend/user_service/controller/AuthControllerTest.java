package com.backend.user_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.backend.common.dto.Role;
import com.backend.user_service.dto.LoginUserDTO;
import com.backend.user_service.dto.RegisterUserDTO;
import com.backend.user_service.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("Registration endpoint should return 201 Created")
    void testHandleUserRegistration_Success() {
        RegisterUserDTO reg = new RegisterUserDTO("John", "Doe", "test@test.com", "pass123", Role.CLIENT);
        doNothing().when(userService).registerUser(reg, null);

        ResponseEntity<Map<String, String>> result = authController.handleUserRegistration(reg, null);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("User registered successfully", result.getBody().get("message"));
    }

    @Test
    @DisplayName("Login endpoint should attach JWT cookie on success")
    void testHandleUserLogin_Success() {
        LoginUserDTO dto = new LoginUserDTO();
        dto.setEmail("test@test.com");
        dto.setPassword("pass123");

        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        Cookie mockCookie = new Cookie("jwt", "token123");
        when(userService.generateCookie("test@test.com")).thenReturn(mockCookie);

        ResponseEntity<Map<String, String>> result = authController.handleUserLogin(dto, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(response).addCookie(mockCookie);
    }

    @Test
    @DisplayName("Logout endpoint should attach empty cookie")
    void testHandleUserLogout_Success() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Cookie emptyCookie = new Cookie("jwt", null);

        when(userService.generateEmptyCookie()).thenReturn(emptyCookie);

        ResponseEntity<Map<String, String>> result = authController.handleUserLogout(response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(response).addCookie(emptyCookie);
    }
}