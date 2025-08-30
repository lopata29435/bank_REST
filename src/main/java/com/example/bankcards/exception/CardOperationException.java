package com.example.bankcards.exception;

public class CardOperationException extends RuntimeException {
    public CardOperationException(String message) {
        super(message);
    }

    public CardOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public static CardOperationException alreadyBlocked() {
        return new CardOperationException("Card is already blocked");
    }

    public static CardOperationException positiveBalance() {
        return new CardOperationException("Cannot delete card with positive balance");
    }

    public static CardOperationException cardNumberExists() {
        return new CardOperationException("Card number already exists");
    }
}
