package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "User roles management request")
public class UserRolesRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username", example = "testuser", required = true)
    private String username;

    @NotEmpty(message = "At least one role is required")
    @Schema(description = "User roles", example = "[\"USER\", \"ADMIN\"]", required = true)
    private Set<String> roles;
}
