package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateBalanceRequest {

    @NotNull(message = "New balance is required")
    @DecimalMin(value = "0.00", message = "Balance cannot be negative")
    private BigDecimal newBalance;
}
