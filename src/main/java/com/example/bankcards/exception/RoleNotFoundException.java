package com.example.bankcards.exception;

public class RoleNotFoundException extends RuntimeException {
    public RoleNotFoundException(String message) {
        super(message);
    }

    public static RoleNotFoundException withName(String roleName) {
        return new RoleNotFoundException("Role not found: " + roleName);
    }
}
