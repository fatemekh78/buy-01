package com.backend.user_service.dto;

import com.backend.common.dto.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for creating a new user account.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for registering a new user account")
public class RegisterUserDTO {

    @NotBlank(message = "First name is required")
    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(description = "User's unique email address", example = "john.doe@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 5, max = 100, message = "Password must be between 5 and 100 characters")
    @Schema(description = "Raw password for the account", example = "SecurePass123!")
    private String password;

    @NotNull(message = "Role assignment is required")
    @Schema(description = "Access level role being requested", example = "CLIENT")
    private Role role;
}