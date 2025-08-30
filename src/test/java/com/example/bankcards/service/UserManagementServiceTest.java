package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.*;
import com.example.bankcards.repository.RefreshTokenRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserManagementService userManagementService;

    private User testUser;
    private Role userRole;
    private Role adminRole;
    private RegisterRequest registerRequest;
    private CreateUserRequest createUserRequest;

    @BeforeEach
    void setUp() {
        // Setup roles
        userRole = Role.builder()
                .id(1L)
                .roleName("USER")
                .enabled(true)
                .build();

        adminRole = Role.builder()
                .id(2L)
                .roleName("ADMIN")
                .enabled(true)
                .build();

        // Setup test user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .passwordHash("hashedpassword")
                .enabled(true)
                .roles(Set.of(userRole))
                .build();

        // Setup request DTOs
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");

        createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("adminuser");
        createUserRequest.setPassword("password123");
        createUserRequest.setEnabled(true);
    }

    @Test
    void registerUser_Success() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedpassword");
        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse result = userManagementService.registerUser(registerRequest);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertTrue(result.isEnabled());

        verify(userRepository).findByUsername("newuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_UserAlreadyExists() {
        // Given
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(UserAlreadyExistsException.class,
            () -> userManagementService.registerUser(registerRequest));
    }

    @Test
    void getAllUsers_Success() {
        // Given
        List<User> users = Arrays.asList(testUser);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 10), 1);

        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        // When
        Page<UserResponse> result = userManagementService.getAllUsers(PageRequest.of(0, 10));

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("testuser", result.getContent().get(0).getUsername());

        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    void getUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserResponse result = userManagementService.getUserByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertTrue(result.isEnabled());

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_NotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
            () -> userManagementService.getUserByUsername("nonexistent"));
    }

    @Test
    void createUser_Success() {
        // Given
        when(userRepository.findByUsername("adminuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedpassword");
        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse result = userManagementService.createUser(createUserRequest);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());

        verify(userRepository).findByUsername("adminuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserRoles_Success() {
        // Given
        UserRolesRequest rolesRequest = new UserRolesRequest();
        rolesRequest.setUsername("testuser");
        rolesRequest.setRoles(Set.of("USER", "ADMIN"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(roleRepository.findByRoleName("USER")).thenReturn(Optional.of(userRole));
        when(roleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        UserResponse result = userManagementService.updateUserRoles(rolesRequest);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());

        verify(userRepository).findByUsername("testuser");
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserRoles_RoleNotFound() {
        // Given
        UserRolesRequest rolesRequest = new UserRolesRequest();
        rolesRequest.setUsername("testuser");
        rolesRequest.setRoles(Set.of("NONEXISTENT"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(roleRepository.findByRoleName("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RoleNotFoundException.class,
            () -> userManagementService.updateUserRoles(rolesRequest));
    }

    @Test
    void deleteUser_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        userManagementService.deleteUser("testuser");

        // Then
        verify(userRepository).findByUsername("testuser");
        verify(userRepository).delete(testUser);
    }

    @Test
    void deleteUser_NotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
            () -> userManagementService.deleteUser("nonexistent"));
    }

    @Test
    void toggleUserStatus_EnableToDisable() {
        // Given
        testUser.setEnabled(true);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        UserResponse result = userManagementService.toggleUserStatus("testuser");

        // Then
        assertNotNull(result);
        assertFalse(testUser.isEnabled());

        verify(userRepository).save(testUser);
        verify(refreshTokenRepository).revokeAllByUser(testUser);
    }

    @Test
    void toggleUserStatus_DisableToEnable() {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        UserResponse result = userManagementService.toggleUserStatus("testuser");

        // Then
        assertNotNull(result);
        assertTrue(testUser.isEnabled());

        verify(userRepository).save(testUser);
        verify(refreshTokenRepository, never()).revokeAllByUser(any());
    }
}
