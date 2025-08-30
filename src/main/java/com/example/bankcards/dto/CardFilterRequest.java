package com.example.bankcards.dto;

import com.example.bankcards.enums.CardStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardFilterRequest {
    private String cardNumber;
    private String cardHolderName;
    private CardStatus status;
    private BigDecimal minBalance;
    private BigDecimal maxBalance;

    private int page = 0;
    private int size = 10;
    private String sortBy = "id";
    private String sortDirection = "desc";
}
