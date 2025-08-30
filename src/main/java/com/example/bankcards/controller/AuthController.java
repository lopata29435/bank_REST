package com.example.bankcards.controller;

import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RefreshTokenRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.dto.TokenResponse;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.RefreshTokenNotFoundException;
import com.example.bankcards.security.JwtProvider;
import com.example.bankcards.security.RefreshTokenProvider;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user authentication and token management")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final UserManagementService userManagementService;

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticate user and return access and refresh tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request format", content = @Content)
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for username: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userPrincipal.getUser();

        String accessToken = jwtProvider.generateAccessToken(user.getUsername(), createClaimsForUser(user));
        String refreshToken = refreshTokenProvider.createRefreshToken(user.getUsername());

        TokenResponse tokenResponse = new TokenResponse(accessToken, refreshToken);
        log.info("Login successful for user: {}", loginRequest.getUsername());
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Generate new access token using refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(responseCode = "401", description = "Refresh token expired", content = @Content),
        @ApiResponse(responseCode = "404", description = "Refresh token not found", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request format", content = @Content)
    })
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenProvider.findByToken(requestRefreshToken)
                .map(refreshTokenProvider::verifyExpiration)
                .map(refreshToken -> {
                    User user = refreshToken.getUser();

                    String accessToken = jwtProvider.generateAccessToken(user.getUsername(), createClaimsForUser(user));

                    return ResponseEntity.ok(new TokenResponse(accessToken, requestRefreshToken));
                })
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token not found or has been revoked"));
    }

    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Revoke the provided refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenProvider.revokeToken(request.getRefreshToken());

        Map<String, Object> response = Map.of(
                "message", "Logged out successfully",
                "timestamp", LocalDateTime.now(),
                "status", "success"
        );

        log.info("User logged out successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout-all")
    @Operation(
        summary = "Logout from all devices",
        description = "Revoke all refresh tokens for the authenticated user",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All sessions logged out successfully", content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> logoutAll(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            User user = userPrincipal.getUser();
            refreshTokenProvider.revokeAllUserTokens(user);

            Map<String, Object> response = Map.of(
                    "message", "All sessions logged out successfully",
                    "username", user.getUsername(),
                    "timestamp", LocalDateTime.now(),
                    "status", "success"
            );

            log.info("All refresh tokens revoked for user: {}", user.getUsername());
            return ResponseEntity.ok(response);
        }

        Map<String, Object> errorResponse = Map.of(
                "message", "User not authenticated",
                "timestamp", LocalDateTime.now(),
                "status", "error"
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @GetMapping("/sessions")
    @Operation(
        summary = "Get active sessions",
        description = "Get the count of active sessions for the authenticated user",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active sessions retrieved successfully", content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> getActiveSessions(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            User user = userPrincipal.getUser();
            int activeSessionsCount = refreshTokenProvider.getActiveSessionsCount(user);

            Map<String, Object> response = Map.of(
                    "activeSessionsCount", activeSessionsCount,
                    "username", user.getUsername(),
                    "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(response);
        }

        Map<String, Object> errorResponse = Map.of(
                "message", "User not authenticated",
                "timestamp", LocalDateTime.now(),
                "status", "error"
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @PostMapping("/register")
    @Operation(
        summary = "User registration",
        description = "Register a new user account"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "409", description = "User already exists", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request format", content = @Content)
    })
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration request for username: {}", registerRequest.getUsername());

        UserResponse userResponse = userManagementService.registerUser(registerRequest);

        log.info("User {} registered successfully", userResponse.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    private Map<String, Object> createClaimsForUser(User user) {
        String roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.joining(","));
        return Map.of("roles", roles);
    }
}
