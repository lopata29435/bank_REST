package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.BlockRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.Role;
import com.example.bankcards.enums.BlockRequestStatus;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.BlockRequestException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InvalidDecisionException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.BlockRequestRepository;
import com.example.bankcards.repository.CardRepository;
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

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockRequestServiceTest {

    @Mock
    private BlockRequestRepository blockRequestRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BlockRequestService blockRequestService;

    private User testUser;
    private User adminUser;
    private Card testCard;
    private BlockRequest testBlockRequest;
    private BlockRequestCreateRequest createRequest;
    private BlockRequestProcessRequest processRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .passwordHash("hashedpassword")
                .enabled(true)
                .roles(Set.of(Role.builder().id(1L).roleName("USER").build()))
                .build();

        // Setup admin user
        adminUser = User.builder()
                .id(2L)
                .username("admin")
                .passwordHash("hashedpassword")
                .enabled(true)
                .roles(Set.of(Role.builder().id(2L).roleName("ADMIN").build()))
                .build();

        // Setup test card
        testCard = Card.builder()
                .id(1L)
                .encryptedNumber("encrypted123")
                .cardHolderName("Test User")
                .expirationMonth(12)
                .expirationYear(2025)
                .status(CardStatus.ACTIVE)
                .user(testUser)
                .build();

        // Setup test block request
        testBlockRequest = BlockRequest.builder()
                .id(1L)
                .card(testCard)
                .user(testUser)
                .reason("Suspicious activity")
                .status(BlockRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        // Setup request DTOs
        createRequest = new BlockRequestCreateRequest();
        createRequest.setReason("Suspicious activity");

        processRequest = new BlockRequestProcessRequest();
        processRequest.setDecision("approve");
        processRequest.setAdminComment("Approved by admin");
    }

    @Test
    void createBlockRequest_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndUserUsername(1L, "testuser")).thenReturn(Optional.of(testCard));
        when(blockRequestRepository.existsByCardAndStatus(testCard, BlockRequestStatus.PENDING)).thenReturn(false);
        when(blockRequestRepository.save(any(BlockRequest.class))).thenReturn(testBlockRequest);

        // When
        BlockRequestResponse result = blockRequestService.createBlockRequest("testuser", 1L, createRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Suspicious activity", result.getReason());
        assertEquals(BlockRequestStatus.PENDING, result.getStatus());

        verify(userRepository).findByUsername("testuser");
        verify(cardRepository).findByIdAndUserUsername(1L, "testuser");
        verify(blockRequestRepository).save(any(BlockRequest.class));
    }

    @Test
    void createBlockRequest_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
            () -> blockRequestService.createBlockRequest("nonexistent", 1L, createRequest));
    }

    @Test
    void createBlockRequest_CardNotFound() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndUserUsername(1L, "testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CardNotFoundException.class,
            () -> blockRequestService.createBlockRequest("testuser", 1L, createRequest));
    }

    @Test
    void createBlockRequest_CardAlreadyBlocked() {
        // Given
        testCard.setStatus(CardStatus.BLOCKED);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndUserUsername(1L, "testuser")).thenReturn(Optional.of(testCard));

        // When & Then
        assertThrows(BlockRequestException.class,
            () -> blockRequestService.createBlockRequest("testuser", 1L, createRequest));
    }

    @Test
    void createBlockRequest_PendingRequestExists() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndUserUsername(1L, "testuser")).thenReturn(Optional.of(testCard));
        when(blockRequestRepository.existsByCardAndStatus(testCard, BlockRequestStatus.PENDING)).thenReturn(true);

        // When & Then
        assertThrows(BlockRequestException.class,
            () -> blockRequestService.createBlockRequest("testuser", 1L, createRequest));
    }

    @Test
    void getUserBlockRequests_Success() {
        // Given
        List<BlockRequest> requests = Arrays.asList(testBlockRequest);
        Page<BlockRequest> requestPage = new PageImpl<>(requests, PageRequest.of(0, 10), 1);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(blockRequestRepository.findByUserOrderByCreatedAtDesc(testUser, PageRequest.of(0, 10)))
                .thenReturn(requestPage);

        // When
        Page<BlockRequestResponse> result = blockRequestService.getUserBlockRequests("testuser", PageRequest.of(0, 10));

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());

        verify(userRepository).findByUsername("testuser");
        verify(blockRequestRepository).findByUserOrderByCreatedAtDesc(testUser, PageRequest.of(0, 10));
    }

    @Test
    void getAllBlockRequests_WithStatus() {
        // Given
        List<BlockRequest> requests = Arrays.asList(testBlockRequest);
        Page<BlockRequest> requestPage = new PageImpl<>(requests, PageRequest.of(0, 10), 1);

        when(blockRequestRepository.findByStatusOrderByCreatedAtDesc(BlockRequestStatus.PENDING, PageRequest.of(0, 10)))
                .thenReturn(requestPage);

        // When
        Page<BlockRequestResponse> result = blockRequestService.getAllBlockRequests(BlockRequestStatus.PENDING, PageRequest.of(0, 10));

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        verify(blockRequestRepository).findByStatusOrderByCreatedAtDesc(BlockRequestStatus.PENDING, PageRequest.of(0, 10));
    }

    @Test
    void getAllBlockRequests_WithoutStatus() {
        // Given
        List<BlockRequest> requests = Arrays.asList(testBlockRequest);
        Page<BlockRequest> requestPage = new PageImpl<>(requests, PageRequest.of(0, 10), 1);

        when(blockRequestRepository.findAll(PageRequest.of(0, 10))).thenReturn(requestPage);

        // When
        Page<BlockRequestResponse> result = blockRequestService.getAllBlockRequests(null, PageRequest.of(0, 10));

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());

        verify(blockRequestRepository).findAll(PageRequest.of(0, 10));
    }

    @Test
    void processBlockRequest_Approve() {
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(blockRequestRepository.findById(1L)).thenReturn(Optional.of(testBlockRequest));
        when(blockRequestRepository.save(testBlockRequest)).thenReturn(testBlockRequest);

        // When
        BlockRequestResponse result = blockRequestService.processBlockRequest("admin", 1L, processRequest);

        // Then
        assertNotNull(result);
        assertEquals(BlockRequestStatus.APPROVED, testBlockRequest.getStatus());
        assertEquals(adminUser, testBlockRequest.getProcessedByAdmin());
        assertEquals("Approved by admin", testBlockRequest.getAdminComment());

        verify(blockRequestRepository).save(testBlockRequest);
    }

    @Test
    void processBlockRequest_Reject() {
        // Given
        processRequest.setDecision("reject");
        processRequest.setAdminComment("Rejected by admin");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(blockRequestRepository.findById(1L)).thenReturn(Optional.of(testBlockRequest));
        when(blockRequestRepository.save(testBlockRequest)).thenReturn(testBlockRequest);

        // When
        BlockRequestResponse result = blockRequestService.processBlockRequest("admin", 1L, processRequest);

        // Then
        assertNotNull(result);
        assertEquals(BlockRequestStatus.REJECTED, testBlockRequest.getStatus());
        assertEquals(adminUser, testBlockRequest.getProcessedByAdmin());
        assertEquals("Rejected by admin", testBlockRequest.getAdminComment());

        verify(blockRequestRepository).save(testBlockRequest);
    }

    @Test
    void processBlockRequest_InvalidDecision() {
        // Given
        processRequest.setDecision("invalid");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(blockRequestRepository.findById(1L)).thenReturn(Optional.of(testBlockRequest));

        // When & Then
        assertThrows(InvalidDecisionException.class,
            () -> blockRequestService.processBlockRequest("admin", 1L, processRequest));
    }

    @Test
    void processBlockRequest_AlreadyProcessed() {
        // Given
        testBlockRequest.setStatus(BlockRequestStatus.APPROVED);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(blockRequestRepository.findById(1L)).thenReturn(Optional.of(testBlockRequest));

        // When & Then
        assertThrows(BlockRequestException.class,
            () -> blockRequestService.processBlockRequest("admin", 1L, processRequest));
    }

    @Test
    void processBlockRequest_RequestNotFound() {
        // Given
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(blockRequestRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(BlockRequestException.class,
            () -> blockRequestService.processBlockRequest("admin", 1L, processRequest));
    }

    @Test
    void getBlockRequestsStatistics_Success() {
        // Given
        when(blockRequestRepository.countByStatus(BlockRequestStatus.PENDING)).thenReturn(5L);
        when(blockRequestRepository.countByStatus(BlockRequestStatus.APPROVED)).thenReturn(10L);
        when(blockRequestRepository.countByStatus(BlockRequestStatus.REJECTED)).thenReturn(3L);

        // When
        Map<String, Object> result = blockRequestService.getBlockRequestsStatistics();

        // Then
        assertNotNull(result);
        assertEquals(18L, result.get("totalRequests"));
        assertEquals(5L, result.get("pendingRequests"));
        assertEquals(10L, result.get("approvedRequests"));
        assertEquals(3L, result.get("rejectedRequests"));

        verify(blockRequestRepository).countByStatus(BlockRequestStatus.PENDING);
        verify(blockRequestRepository).countByStatus(BlockRequestStatus.APPROVED);
        verify(blockRequestRepository).countByStatus(BlockRequestStatus.REJECTED);
    }
}
