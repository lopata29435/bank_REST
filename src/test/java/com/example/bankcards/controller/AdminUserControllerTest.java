package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.security.JwtProvider;
import com.example.bankcards.service.UserManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserResponse testUser;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }

        @Bean
        public UserManagementService userManagementService() {
            return mock(UserManagementService.class);
        }

        @Bean
        JwtProvider jwtProvider() {
            return mock(JwtProvider.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return mock(JwtAuthenticationFilter.class);
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                    .anyRequest().permitAll()
                );
            return http.build();
        }
    }

    @BeforeEach
    void setUp() {
        testUser = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();
    }

    @Test
    void getAllUsers_ValidRequest_ReturnsUsersList() throws Exception {
        // Arrange
        Page<UserResponse> page = new PageImpl<>(List.of(testUser));
        when(userManagementService.getAllUsers(any(PageRequest.class)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/admin/users")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "username")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].username").value("testuser"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(userManagementService).getAllUsers(any(PageRequest.class));
    }

    @Test
    void getUserByUsername_ValidUsername_ReturnsUser() throws Exception {
        // Arrange
        when(userManagementService.getUserByUsername("testuser"))
                .thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(get("/admin/users/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.enabled").value(true));

        verify(userManagementService).getUserByUsername("testuser");
    }

    @Test
    void createUser_ValidRequest_ReturnsCreatedUser() throws Exception {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setPassword("password");

        UserResponse createdUser = UserResponse.builder()
                .id(2L)
                .username("newuser")
                .enabled(true)
                .roles(Set.of("USER"))
                .build();

        when(userManagementService.createUser(any(CreateUserRequest.class)))
                .thenReturn(createdUser);

        // Act & Assert
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("newuser"));

        verify(userManagementService).createUser(any(CreateUserRequest.class));
    }

    @Test
    void updateUserRoles_ValidRequest_ReturnsUpdatedUser() throws Exception {
        // Arrange
        UpdateRolesRequest request = new UpdateRolesRequest();
        request.setRoles(Set.of("USER", "ADMIN"));

        UserResponse updatedUser = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .enabled(true)
                .roles(Set.of("USER", "ADMIN"))
                .build();

        when(userManagementService.updateUserRoles(any(UserRolesRequest.class)))
                .thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/admin/users/testuser/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles.length()").value(2));

        verify(userManagementService).updateUserRoles(any(UserRolesRequest.class));
    }

    @Test
    void toggleUserStatus_ValidUsername_ReturnsToggledUser() throws Exception {
        // Arrange
        UserResponse disabledUser = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .enabled(false)
                .roles(Set.of("USER"))
                .build();

        when(userManagementService.toggleUserStatus("testuser"))
                .thenReturn(disabledUser);

        // Act & Assert
        mockMvc.perform(patch("/admin/users/testuser/toggle-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.enabled").value(false));

        verify(userManagementService).toggleUserStatus("testuser");
    }

    @Test
    void deleteUser_ValidUsername_ReturnsSuccessMessage() throws Exception {
        // Arrange
        doNothing().when(userManagementService).deleteUser("testuser");

        // Act & Assert
        mockMvc.perform(delete("/admin/users/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deleted successfully"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.status").value("success"));

        verify(userManagementService).deleteUser("testuser");
    }

    @Test
    void getAllUsers_InvalidSortParameter_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/admin/users")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "invalidField")
                        .param("sortDirection", "asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsers_InvalidSortDirection_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/admin/users")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "username")
                        .param("sortDirection", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        // Не заполняем обязательные поля

        // Act & Assert
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserRoles_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Arrange
        UpdateRolesRequest request = new UpdateRolesRequest();
        // Не заполняем обязательные поля

        // Act & Assert
        mockMvc.perform(put("/admin/users/testuser/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_UserAlreadyExists_ReturnsConflict() throws Exception {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existinguser");
        request.setPassword("password");

        when(userManagementService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new com.example.bankcards.exception.UserAlreadyExistsException("User already exists"));

        // Act & Assert
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllUsers_WithFiltering_ReturnsFilteredUsers() throws Exception {
        // Arrange
        Page<UserResponse> page = new PageImpl<>(List.of(testUser));
        when(userManagementService.getAllUsers(any(PageRequest.class)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/admin/users")
                        .param("page", "0")
                        .param("size", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("testuser"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));

        verify(userManagementService, atLeast(1)).getAllUsers(any(PageRequest.class));
    }
} 