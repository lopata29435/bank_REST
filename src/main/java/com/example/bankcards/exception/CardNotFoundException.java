package com.example.bankcards.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String message) {
        super(message);
    }

    public CardNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static CardNotFoundException byNumber(String cardNumber) {
        return new CardNotFoundException("Card not found: " + cardNumber);
    }

    public static CardNotFoundException accessDenied(String cardNumber) {
        return new CardNotFoundException("Card not found or access denied: " + cardNumber);
    }
}
