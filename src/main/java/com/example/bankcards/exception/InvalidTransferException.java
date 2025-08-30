package com.example.bankcards.exception;

public class InvalidTransferException extends RuntimeException {
    public InvalidTransferException(String message) {
        super(message);
    }

    public static InvalidTransferException withReason(String reason) {
        return new InvalidTransferException("Transfer operation is invalid: " + reason);
    }

    public static InvalidTransferException insufficientFunds(String cardNumber, double amount) {
        return new InvalidTransferException("Insufficient funds on card " + cardNumber + " for amount: " + amount);
    }

    public static InvalidTransferException sameCard() {
        return new InvalidTransferException("Cannot transfer to the same card");
    }

    public static InvalidTransferException invalidAmount(double amount) {
        return new InvalidTransferException("Invalid transfer amount: " + amount);
    }

    public static InvalidTransferException cardBlocked(String cardNumber) {
        return new InvalidTransferException("Card " + cardNumber + " is blocked");
    }
}
