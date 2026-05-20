package com.backend.user_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object for updating an existing user's profile information.
 * All fields are structurally optional to support partial (PATCH) updates.
 */
@Data
@Schema(description = "Payload for partially updating a user profile")
public class UpdateUserDTO {

    @Email(message = "Email should be valid")
    @Schema(description = "New email address", example = "john.new@example.com")
    private String email;

    @Schema(description = "Updated first name", example = "Johnny")
    private String firstName;

    @Schema(description = "Updated last name", example = "Doe")
    private String lastName;

    @Size(min = 5, max = 100, message = "Current password must be provided and valid length")
    @Schema(description = "Current password, required if setting a new password", example = "OldPass123!")
    private String currentPassword;

    @Size(min = 5, max = 100, message = "New password must be between 5 and 100 characters")
    @Schema(description = "New requested password", example = "NewPass456!")
    private String newPassword;
}