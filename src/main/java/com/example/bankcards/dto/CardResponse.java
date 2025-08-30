package com.example.bankcards.dto;

import com.example.bankcards.enums.CardStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardResponse {
    private Long id;
    private String maskedCardNumber;
    private String cardHolderName;
    private Integer expirationMonth;
    private Integer expirationYear;
    private BigDecimal balance;
    private CardStatus status;
}
