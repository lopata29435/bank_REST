package com.example.bankcards.exception;

public class CardAccessDeniedException extends RuntimeException {
    public CardAccessDeniedException(String message) {
        super(message);
    }

    public CardAccessDeniedException(Long cardId, String username) {
        super("Access denied to card " + cardId + " for user " + username);
    }
}
