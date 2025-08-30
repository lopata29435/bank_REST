package com.example.bankcards.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String cardNumber, double requestedAmount, double availableBalance) {
        super(String.format("Insufficient funds on card %s. Requested: %.2f, Available: %.2f",
                cardNumber, requestedAmount, availableBalance));
    }
}
