package com.example.bankcards.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========== AUTH & TOKEN EXCEPTIONS ==========

    @ExceptionHandler(InvalidCredentialsException.class)
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Invalid Credentials")
                .message(ex.getMessage())
                .build();

        log.warn("Invalid credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    @ApiResponse(responseCode = "401", description = "Refresh token expired")
    public ResponseEntity<ErrorResponse> handleRefreshTokenExpired(RefreshTokenExpiredException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Refresh Token Expired")
                .message(ex.getMessage())
                .build();

        log.warn("Refresh token expired: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(RefreshTokenNotFoundException.class)
    @ApiResponse(responseCode = "404", description = "Refresh token not found")
    public ResponseEntity<ErrorResponse> handleRefreshTokenNotFound(RefreshTokenNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Refresh Token Not Found")
                .message(ex.getMessage())
                .build();

        log.warn("Refresh token not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ApiResponse(responseCode = "401", description = "Authentication failed")
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Failed")
                .message("Invalid username or password")
                .build();

        log.warn("Authentication failed for request");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // ========== USER EXCEPTIONS ==========

    @ExceptionHandler(UserNotFoundException.class)
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("User Not Found")
                .message(ex.getMessage())
                .build();

        log.warn("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ApiResponse(responseCode = "409", description = "User already exists")
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("User Already Exists")
                .message(ex.getMessage())
                .build();

        log.warn("User already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    @ApiResponse(responseCode = "404", description = "Role not found")
    public ResponseEntity<ErrorResponse> handleRoleNotFound(RoleNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Role Not Found")
                .message(ex.getMessage())
                .build();

        log.warn("Role not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // ========== CARD EXCEPTIONS ==========

    @ExceptionHandler(CardNotFoundException.class)
    @ApiResponse(responseCode = "404", description = "Card not found")
    public ResponseEntity<ErrorResponse> handleCardNotFound(CardNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Card Not Found")
                .message(ex.getMessage())
                .build();

        log.warn("Card not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(CardAccessDeniedException.class)
    @ApiResponse(responseCode = "403", description = "Card access denied")
    public ResponseEntity<ErrorResponse> handleCardAccessDenied(CardAccessDeniedException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Card Access Denied")
                .message(ex.getMessage())
                .build();

        log.warn("Card access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(CardOperationException.class)
    @ApiResponse(responseCode = "400", description = "Card operation failed")
    public ResponseEntity<ErrorResponse> handleCardOperation(CardOperationException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Card Operation Failed")
                .message(ex.getMessage())
                .build();

        log.warn("Card operation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ========== TRANSACTION EXCEPTIONS ==========

    @ExceptionHandler(TransferException.class)
    @ApiResponse(responseCode = "400", description = "Transfer operation failed")
    public ResponseEntity<ErrorResponse> handleTransferException(TransferException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Transfer Failed")
                .message(ex.getMessage())
                .build();

        log.warn("Transfer failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ApiResponse(responseCode = "400", description = "Insufficient funds")
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Insufficient Funds")
                .message(ex.getMessage())
                .build();

        log.warn("Insufficient funds: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InvalidTransferException.class)
    @ApiResponse(responseCode = "400", description = "Invalid transfer operation")
    public ResponseEntity<ErrorResponse> handleInvalidTransfer(InvalidTransferException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Transfer")
                .message(ex.getMessage())
                .build();

        log.warn("Invalid transfer: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ========== DATABASE EXCEPTIONS ==========

    @ExceptionHandler(DatabaseOperationException.class)
    @ApiResponse(responseCode = "500", description = "Database operation failed")
    public ResponseEntity<ErrorResponse> handleDatabaseOperation(DatabaseOperationException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Database Operation Failed")
                .message("A database error occurred")
                .build();

        log.error("Database operation failed: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // ========== VALIDATION EXCEPTIONS ==========

    @ExceptionHandler(InvalidDecisionException.class)
    @ApiResponse(responseCode = "400", description = "Invalid decision value")
    public ResponseEntity<ErrorResponse> handleInvalidDecision(InvalidDecisionException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Decision")
                .message(ex.getMessage())
                .build();

        log.warn("Invalid decision: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InvalidParameterException.class)
    @ApiResponse(responseCode = "400", description = "Invalid parameter value")
    public ResponseEntity<ErrorResponse> handleInvalidParameter(InvalidParameterException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Parameter")
                .message(ex.getMessage())
                .build();

        log.warn("Invalid parameter: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(BlockRequestException.class)
    @ApiResponse(responseCode = "400", description = "Block request operation failed")
    public ResponseEntity<ErrorResponse> handleBlockRequestException(BlockRequestException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Block Request Error")
                .message(ex.getMessage())
                .build();

        log.warn("Block request error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder errorMessage = new StringBuilder("Validation failed: ");

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMsg = error.getDefaultMessage();
            errorMessage.append(fieldName).append(" - ").append(errorMsg).append("; ");
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message(errorMessage.toString())
                .build();

        log.warn("Validation error: {}", errorMessage.toString());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    // ========== SECURITY EXCEPTIONS ==========

    @ExceptionHandler(InsufficientPrivilegesException.class)
    @ApiResponse(responseCode = "403", description = "Insufficient privileges")
    public ResponseEntity<ErrorResponse> handleInsufficientPrivileges(InsufficientPrivilegesException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Insufficient Privileges")
                .message(ex.getMessage())
                .build();

        log.warn("Insufficient privileges: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ApiResponse(responseCode = "403", description = "Access denied")
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Access Denied")
                .message("You don't have permission to access this resource")
                .build();

        log.warn("Access denied for request");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    // ========== FALLBACK EXCEPTIONS ==========

    @ExceptionHandler(Exception.class)
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .build();

        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // ========== ERROR RESPONSE MODEL ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Error response format")
    public static class ErrorResponse {
        @Schema(description = "Timestamp when error occurred", example = "2024-08-29T10:15:30")
        private LocalDateTime timestamp;

        @Schema(description = "HTTP status code", example = "400")
        private int status;

        @Schema(description = "Error type", example = "Validation Failed")
        private String error;

        @Schema(description = "Error message", example = "Input validation failed")
        private String message;
    }
}
