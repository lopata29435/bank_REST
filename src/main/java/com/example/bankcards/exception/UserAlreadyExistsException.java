package com.example.bankcards.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }

    public static UserAlreadyExistsException withUsername(String username) {
        return new UserAlreadyExistsException("User with username '" + username + "' already exists");
    }
}
