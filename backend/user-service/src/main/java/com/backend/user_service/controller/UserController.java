package com.backend.user_service.controller;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.backend.common.dto.InfoUserDTO;
import com.backend.user_service.dto.UpdateUserDTO;
import com.backend.user_service.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management API", description = "Endpoints for managing core user identities, avatars, and account lifecycles")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info", description = "Retrieves the identity details of the currently authenticated user (Client, Seller, or Admin).")
    public ResponseEntity<InfoUserDTO> getMe(@RequestHeader("X-User-ID") String userId) {
        InfoUserDTO user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user", description = "Partially updates user details. May issue a new JWT cookie if the email address was changed.")
    public ResponseEntity<Map<String, String>> updateMe(
            @Valid @RequestBody UpdateUserDTO userUpdatedInfo,
            @RequestHeader("X-User-ID") String userId,
            HttpServletResponse response) {
        
        UserService.UserUpdateResult result = userService.updateUserInfo(userId, userUpdatedInfo);
        
        if (result.newJwtNeeded()) {
            Cookie jwtCookie = userService.generateCookie(result.userEmail());
            response.addCookie(jwtCookie);
        }
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    @DeleteMapping
    @Operation(summary = "Delete account", description = "Permanently deletes the user's account and publishes a Kafka event to wipe associated data.")
    public ResponseEntity<Map<String, String>> deleteUser(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam String password) {
        
        userService.deleteUser(userId, password);
        return ResponseEntity.ok(Map.of("message", "User account deleted successfully"));
    }

    @DeleteMapping("/avatar")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN', 'CLIENT')")
    @Operation(summary = "Delete user avatar", description = "Removes the user's current avatar and triggers a deletion event in the media-service.")
    public ResponseEntity<String> deleteAvatar(@RequestHeader("X-User-ID") String userId) {
        userService.deleteAvatar(userId);
        return ResponseEntity.ok("Avatar deleted successfully");
    }

    @GetMapping("/email")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Find user by email (Admin)", description = "Allows administrators to securely look up user profiles via email.")
    public ResponseEntity<InfoUserDTO> getUsersByEmail(@RequestParam String email) {
        InfoUserDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
}