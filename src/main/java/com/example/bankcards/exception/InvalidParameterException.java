package com.example.bankcards.exception;

public class InvalidParameterException extends RuntimeException {
    public InvalidParameterException(String message) {
        super(message);
    }

    public InvalidParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public static InvalidParameterException invalidStatus(String status) {
        return new InvalidParameterException("Invalid status: '" + status + "'. Valid values are: ACTIVE, BLOCKED");
    }

    public static InvalidParameterException invalidParameter(String parameterName, String value) {
        return new InvalidParameterException("Invalid " + parameterName + ": '" + value + "'");
    }
}
