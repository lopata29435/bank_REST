package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "Request to update user roles")
public class UpdateRolesRequest {

    @NotEmpty(message = "At least one role is required")
    @Schema(description = "Roles to assign", example = "[\"USER\", \"ADMIN\"]", required = true)
    private Set<String> roles;
}
