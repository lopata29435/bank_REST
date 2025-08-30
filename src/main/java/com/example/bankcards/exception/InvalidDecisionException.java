package com.example.bankcards.exception;

public class InvalidDecisionException extends RuntimeException {
    public InvalidDecisionException(String message) {
        super(message);
    }

    public InvalidDecisionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InvalidDecisionException invalidValue(String decision) {
        return new InvalidDecisionException("Invalid decision: '" + decision + "'. Use 'approve' or 'reject'");
    }
}
