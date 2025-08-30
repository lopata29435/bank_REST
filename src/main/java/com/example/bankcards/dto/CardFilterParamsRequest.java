package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardFilterParamsRequest {

    @Parameter(description = "Card number (partial search)", example = "1234")
    private String cardNumber;

    @Parameter(description = "Card holder name (partial search)", example = "John")
    private String cardHolderName;

    @Parameter(description = "Card status", example = "ACTIVE")
    private String status;

    @Parameter(description = "Minimum balance", example = "0")
    private BigDecimal minBalance;

    @Parameter(description = "Maximum balance", example = "100000")
    private BigDecimal maxBalance;

    @Parameter(description = "Page number", example = "0")
    @Min(0)
    private int page = 0;

    @Parameter(description = "Page size", example = "20")
    @Min(1)
    @Max(100)
    private int size = 20;

    @Parameter(description = "Sort by field", example = "id")
    private String sortBy = "id";

    @Parameter(description = "Sort direction", example = "desc")
    private String sortDirection = "desc";
}
