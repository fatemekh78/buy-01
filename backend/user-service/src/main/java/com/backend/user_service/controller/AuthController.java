package com.backend.user_service.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.backend.user_service.dto.LoginUserDTO;
import com.backend.user_service.dto.RegisterUserDTO;
import com.backend.user_service.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication API", description = "Endpoints for user registration, login, and secure session management")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register a new user", description = "Registers a new Client or Seller account and optionally uploads a profile avatar.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or validation error"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<Map<String, String>> handleUserRegistration(
            @Valid @RequestPart("userDto") RegisterUserDTO userDto,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile) {
        
        // DTO is passed directly to the service layer to be handled by MapStruct
        userService.registerUser(userDto, avatarFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates credentials and attaches a secure HttpOnly JWT cookie to the response.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    public ResponseEntity<Map<String, String>> handleUserLogin(
            @Valid @RequestBody LoginUserDTO loginUserDTO,
            HttpServletResponse response) {
        
        // Let BadCredentialsException bubble up to the GlobalExceptionHandler or CustomAuthEntryPoint
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginUserDTO.getEmail(), loginUserDTO.getPassword()));
        
        Cookie jwtCookie = userService.generateCookie(loginUserDTO.getEmail());
        response.addCookie(jwtCookie);
        
        return ResponseEntity.ok(Map.of("message", "Login successful"));
    }
    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates the user's active session by clearing the JWT cookie.")
    public ResponseEntity<Map<String, String>> handleUserLogout(HttpServletResponse response) {
        response.addCookie(userService.generateEmptyCookie());
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }
}