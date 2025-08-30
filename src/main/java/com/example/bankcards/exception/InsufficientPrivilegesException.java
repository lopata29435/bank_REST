package com.example.bankcards.exception;

public class InsufficientPrivilegesException extends RuntimeException {
    public InsufficientPrivilegesException(String message) {
        super(message);
    }

    public static InsufficientPrivilegesException forEndpoint(String endpoint) {
        return new InsufficientPrivilegesException("Access denied to endpoint: " + endpoint);
    }

    public static InsufficientPrivilegesException forResource(String resource) {
        return new InsufficientPrivilegesException("Access denied to resource: " + resource);
    }

    public static InsufficientPrivilegesException insufficientRole(String requiredRole) {
        return new InsufficientPrivilegesException("Insufficient privileges. Required role: " + requiredRole);
    }

    public static InsufficientPrivilegesException forAction(String action) {
        return new InsufficientPrivilegesException("Access denied for action: " + action);
    }
}
