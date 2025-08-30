package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request to create a new card for a specific user (Admin only)")
public class AdminCreateCardRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username of the card owner",
            example = "john_doe")
    private String username;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "Card number must be 16 digits")
    @Schema(description = "Card number (16 digits)",
            example = "4532123456789012")
    private String cardNumber;

    @NotBlank(message = "Card holder name is required")
    @Pattern(regexp = "^[A-Z ]+$", message = "Card holder name must contain only uppercase letters and spaces")
    @Schema(description = "Card holder name (uppercase letters and spaces only)",
            example = "JOHN SMITH")
    private String cardHolderName;

    @NotNull(message = "Expiration month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    @Schema(description = "Card expiration month (1-12)",
            example = "12")
    private Integer expirationMonth;

    @NotNull(message = "Expiration year is required")
    @Min(value = 2025, message = "Year must be in the future")
    @Schema(description = "Card expiration year",
            example = "2027")
    private Integer expirationYear;

    @NotNull(message = "Initial balance is required")
    @Schema(description = "Initial card balance",
            example = "1000.00",
            minimum = "0")
    private BigDecimal initialBalance = BigDecimal.ZERO;
}
