package com.backend.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing core user information.
 * Used for safely transmitting user details without exposing internal database
 * structures.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Core user profile information")
public class InfoUserDTO {

    @NotBlank(message = "User ID cannot be blank")
    @Schema(description = "Unique identifier of the user", example = "usr_5f8a9b2c")
    private String id;

    @NotBlank(message = "First name is required")
    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User's registered email address", example = "john.doe@example.com")
    private String email;

    @NotBlank(message = "Avatar URL is required")
    @Schema(description = "URL pointing to the user's profile picture", example = "https://storage.buy-01.com/avatars/usr_5f8a9b2c.png")
    private String avatarUrl;

    @NotNull(message = "Role is required")
    @Schema(description = "System role assigned to the user")
    private Role role;
}