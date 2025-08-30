package com.example.bankcards.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TransferResponse {
    private String transactionId;
    private String fromMaskedCardNumber;
    private String toMaskedCardNumber;
    private BigDecimal amount;
    private BigDecimal fromCardBalance;
    private BigDecimal toCardBalance;
    private String description;
    private LocalDateTime transferredAt;
}
