package com.example.bankcards.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException byUsername(String username) {
        return new UserNotFoundException("User not found: " + username);
    }

    public static UserNotFoundException byId(Long userId) {
        return new UserNotFoundException("User not found with ID: " + userId);
    }

    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException("User not found with email: " + email);
    }
}
