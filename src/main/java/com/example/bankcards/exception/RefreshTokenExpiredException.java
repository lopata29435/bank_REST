package com.example.bankcards.exception;

public class RefreshTokenExpiredException extends RuntimeException {
    public RefreshTokenExpiredException(String message) {
        super(message);
    }

    public RefreshTokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
