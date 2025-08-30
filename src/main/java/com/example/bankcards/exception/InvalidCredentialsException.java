package com.example.bankcards.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }

    public static InvalidCredentialsException invalidUsernameOrPassword() {
        return new InvalidCredentialsException("Invalid username or password");
    }

    public static InvalidCredentialsException userNotFound(String username) {
        return new InvalidCredentialsException("User not found: " + username);
    }

    public static InvalidCredentialsException userDisabled(String username) {
        return new InvalidCredentialsException("User account is disabled: " + username);
    }

    public static InvalidCredentialsException passwordMismatch() {
        return new InvalidCredentialsException("Password does not match");
    }
}
