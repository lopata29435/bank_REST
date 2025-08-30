package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());
        return createUserInternal(request.getUsername(), request.getPassword(), true, false);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = findUserByUsername(username);
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Admin creating new user: {}", request.getUsername());
        return createUserInternal(request.getUsername(), request.getPassword(), request.isEnabled(), true);
    }

    @Transactional
    public UserResponse updateUserRoles(UserRolesRequest request) {
        log.info("Updating roles for user: {} to {}", request.getUsername(), request.getRoles());

        User user = findUserByUsername(request.getUsername());
        Set<Role> newRoles = findRolesByNames(request.getRoles());
        user.setRoles(newRoles);

        User savedUser = saveUser(user);
        log.info("Roles updated for user: {}", user.getUsername());
        return mapToUserResponse(savedUser);
    }

    @Transactional
    public void deleteUser(String username) {
        log.info("Deleting user: {}", username);
        User user = findUserByUsername(username);

        try {
            userRepository.delete(user);
            log.info("User {} deleted successfully", username);
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to delete user", e);
        }
    }

    @Transactional
    public UserResponse toggleUserStatus(String username) {
        log.info("Toggling status for user: {}", username);

        User user = findUserByUsername(username);
        boolean wasEnabled = user.isEnabled();
        user.setEnabled(!wasEnabled);

        User savedUser = saveUser(user);

        if (wasEnabled && !savedUser.isEnabled()) {
            refreshTokenRepository.revokeAllByUser(savedUser);
            log.info("All refresh tokens revoked for disabled user: {}", username);
        }

        log.info("User {} status changed to: {}", username, savedUser.isEnabled() ? "enabled" : "disabled");
        return mapToUserResponse(savedUser);
    }

    private UserResponse createUserInternal(String username, String password, boolean enabled, boolean isAdminCreated) {
        validateUserDoesNotExist(username);
        Role userRole = findUserRole();

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .enabled(enabled)
                .roles(Set.of(userRole))
                .build();

        User savedUser = saveUser(user);

        String logMessage = isAdminCreated ?
            "User {} created by admin with ID: {}" :
            "User {} registered successfully with ID: {}";
        log.info(logMessage, savedUser.getUsername(), savedUser.getId());

        return mapToUserResponse(savedUser);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> UserNotFoundException.byUsername(username));
    }

    private void validateUserDoesNotExist(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw UserAlreadyExistsException.withUsername(username);
        }
    }

    private Role findUserRole() {
        return roleRepository.findByRoleName("USER")
                .orElseThrow(() -> RoleNotFoundException.withName("USER"));
    }

    private Set<Role> findRolesByNames(Set<String> roleNames) {
        return roleNames.stream()
                .map(roleName -> roleRepository.findByRoleName(roleName)
                        .orElseThrow(() -> RoleNotFoundException.withName(roleName)))
                .collect(Collectors.toSet());
    }

    private User saveUser(User user) {
        try {
            return userRepository.save(user);
        } catch (Exception e) {
            throw new DatabaseOperationException("Failed to save user", e);
        }
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream()
                        .map(Role::getRoleName)
                        .collect(Collectors.toSet()))
                .build();
    }
}
