package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.exception.InvalidParameterException;
import com.example.bankcards.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin User Management", description = "Administrative endpoints for user management")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class AdminUserController {

    private final UserManagementService userManagementService;
    
    private static final List<String> VALID_SORT_FIELDS = Arrays.asList("id", "username", "email", "firstName", "lastName", "enabled", "createdAt");
    private static final List<String> VALID_SORT_DIRECTIONS = Arrays.asList("asc", "desc");

    @GetMapping
    @Operation(
        summary = "Get all users (Admin)",
        description = "Retrieve paginated list of all users in the system (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content)
    })
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(@Valid UserFilterParamsRequest filterParams) {
        validateSortParameters(filterParams.getSortBy(), filterParams.getSortDirection());

        Sort.Direction direction = filterParams.getSortDirection().equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(filterParams.getPage(), filterParams.getSize(),
                Sort.by(direction, filterParams.getSortBy()));
        Page<UserResponse> users = userManagementService.getAllUsers(pageable);

        PageResponse<UserResponse> response = new PageResponse<>(
                users.getContent(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.isLast()
        );

        log.info("Admin requested all users, page: {}, size: {}, sort: {} {}",
                filterParams.getPage(), filterParams.getSize(), filterParams.getSortBy(), filterParams.getSortDirection());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{username}")
    @Operation(
        summary = "Get user by username (Admin)",
        description = "Retrieve specific user details by username (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserResponse> getUserByUsername(
            @Parameter(description = "Username", required = true)
            @PathVariable String username) {

        UserResponse user = userManagementService.getUserByUsername(username);
        log.info("Admin requested user details for: {}", username);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @Operation(
        summary = "Create user (Admin)",
        description = "Create a new user account (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "409", description = "User already exists", content = @Content)
    })
    public ResponseEntity<UserResponse> createUser(
            @Parameter(description = "User creation request details", required = true)
            @Valid @RequestBody CreateUserRequest request) {

        UserResponse userResponse = userManagementService.createUser(request);
        log.info("Admin created user: {}", userResponse.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @PutMapping("/{username}/roles")
    @Operation(
        summary = "Update user roles (Admin)",
        description = "Update roles for a specific user (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User roles updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "User or role not found", content = @Content)
    })
    public ResponseEntity<UserResponse> updateUserRoles(
            @Parameter(description = "Username", required = true)
            @PathVariable String username,
            @Parameter(description = "Roles to assign to user", required = true)
            @Valid @RequestBody UpdateRolesRequest request) {

        UserRolesRequest userRolesRequest = new UserRolesRequest();
        userRolesRequest.setUsername(username);
        userRolesRequest.setRoles(request.getRoles());

        UserResponse userResponse = userManagementService.updateUserRoles(userRolesRequest);
        log.info("Admin updated roles for user: {} to {}", username, request.getRoles());
        return ResponseEntity.ok(userResponse);
    }

    @PatchMapping("/{username}/toggle-status")
    @Operation(
        summary = "Toggle user status (Admin)",
        description = "Enable or disable a user account (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User status toggled successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<UserResponse> toggleUserStatus(
            @Parameter(description = "Username", required = true)
            @PathVariable String username) {

        UserResponse userResponse = userManagementService.toggleUserStatus(username);
        log.info("Admin toggled status for user: {} to {}", username,
                userResponse.isEnabled() ? "enabled" : "disabled");
        return ResponseEntity.ok(userResponse);
    }

    @DeleteMapping("/{username}")
    @Operation(
        summary = "Delete user (Admin)",
        description = "Permanently delete a user account and all associated data (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User deleted successfully", content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> deleteUser(
            @Parameter(description = "Username to delete", required = true)
            @PathVariable String username) {

        userManagementService.deleteUser(username);

        Map<String, Object> response = Map.of(
                "message", "User deleted successfully",
                "username", username,
                "timestamp", LocalDateTime.now(),
                "status", "success"
        );

        log.info("Admin deleted user: {}", username);
        return ResponseEntity.ok(response);
    }

    private void validateSortParameters(String sortBy, String sortDirection) {
        if (!VALID_SORT_FIELDS.contains(sortBy)) {
            throw InvalidParameterException.invalidParameter("sortBy", sortBy + ". Valid values: " + VALID_SORT_FIELDS);
        }
        if (!VALID_SORT_DIRECTIONS.contains(sortDirection.toLowerCase())) {
            throw InvalidParameterException.invalidParameter("sortDirection", sortDirection + ". Valid values: " + VALID_SORT_DIRECTIONS);
        }
    }
}
