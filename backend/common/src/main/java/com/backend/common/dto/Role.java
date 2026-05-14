package com.backend.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Defines the access levels and permissions for users across the platform.
 */
@Schema(description = "Defines the access levels and permissions for users across the platform")
public enum Role {
    @Schema(description = "Standard customer account")
    CLIENT, 
    
    @Schema(description = "Merchant account with store management privileges")
    SELLER, 
    
    @Schema(description = "System administrator with global access")
    ADMIN
}