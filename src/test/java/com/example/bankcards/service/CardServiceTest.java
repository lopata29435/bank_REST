package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.Role;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.TransferException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardEncryptor cardEncryptor;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private Card testCard;
    private AdminCreateCardRequest createCardRequest;
    private CardFilterRequest filterRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Initialize static encryptor for Card entity
        Card.setEncryptor(cardEncryptor);

        // Setup mocks for CardEncryptor
        when(cardEncryptor.decrypt("encrypted123")).thenReturn("1234567890123456");
        when(cardEncryptor.decrypt("encrypted456")).thenReturn("6543210987654321");
        when(cardEncryptor.encrypt("1234567890123456")).thenReturn("encrypted123");
        when(cardEncryptor.encrypt("6543210987654321")).thenReturn("encrypted456");

        // Setup test user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .passwordHash("hashedpassword")
                .enabled(true)
                .roles(Set.of(Role.builder().id(1L).roleName("USER").build()))
                .build();

        // Setup test card
        testCard = Card.builder()
                .id(1L)
                .encryptedNumber("encrypted123")
                .cardHolderName("Test User")
                .expirationMonth(12)
                .expirationYear(2025)
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .user(testUser)
                .build();

        // Setup request DTOs
        createCardRequest = new AdminCreateCardRequest();
        createCardRequest.setUsername("testuser");
        createCardRequest.setCardNumber("1234567890123456");
        createCardRequest.setCardHolderName("Test User");
        createCardRequest.setExpirationMonth(12);
        createCardRequest.setExpirationYear(2025);
        createCardRequest.setInitialBalance(new BigDecimal("1000.00"));

        filterRequest = new CardFilterRequest();
        filterRequest.setPage(0);
        filterRequest.setSize(10);
        filterRequest.setSortBy("id");
        filterRequest.setSortDirection("asc");
    }

    @Test
    void createCardForUser_Success() throws Exception {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardEncryptor.encrypt("1234567890123456")).thenReturn("encrypted123");
        when(cardRepository.existsByEncryptedNumber("encrypted123")).thenReturn(false);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);

        // When
        CardResponse result = cardService.createCardForUser("testuser", createCardRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test User", result.getCardHolderName());
        assertEquals(new BigDecimal("1000.00"), result.getBalance());
        assertEquals(CardStatus.ACTIVE, result.getStatus());

        verify(userRepository).findByUsername("testuser");
        verify(cardRepository).existsByEncryptedNumber("encrypted123");
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCardForUser_UserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
            () -> cardService.createCardForUser("nonexistent", createCardRequest));
    }

    @Test
    void createCardForUser_CardNumberAlreadyExists() throws Exception {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardEncryptor.encrypt("1234567890123456")).thenReturn("encrypted123");
        when(cardRepository.existsByEncryptedNumber("encrypted123")).thenReturn(true);

        // When & Then
        assertThrows(CardOperationException.class,
            () -> cardService.createCardForUser("testuser", createCardRequest));
    }

    @Test
    void getUserCards_Success() {
        // Given
        List<Card> cards = Arrays.asList(testCard);
        Page<Card> cardPage = new PageImpl<>(cards, PageRequest.of(0, 10), 1);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(cardPage);

        // When
        PageResponse<CardResponse> result = cardService.getUserCards("testuser", filterRequest);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());

        verify(userRepository).findByUsername("testuser");
        verify(cardRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllCards_Success() {
        // Given
        List<Card> cards = Arrays.asList(testCard);
        Page<Card> cardPage = new PageImpl<>(cards, PageRequest.of(0, 10), 1);

        when(cardRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(cardPage);

        // When
        PageResponse<CardResponse> result = cardService.getAllCards(filterRequest);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(cardRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getCardById_Success() {
        // Given
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        // When
        CardResponse result = cardService.getCardById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(cardRepository).findById(1L);
    }

    @Test
    void getCardById_NotFound() {
        // Given
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CardNotFoundException.class, () -> cardService.getCardById(1L));
    }

    @Test
    void getUserCardById_Success() {
        // Given
        when(cardRepository.findByIdAndUserUsername(1L, "testuser")).thenReturn(Optional.of(testCard));

        // When
        CardResponse result = cardService.getUserCardById("testuser", 1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(cardRepository).findByIdAndUserUsername(1L, "testuser");
    }

    @Test
    void getUserCardById_NotFound() {
        // Given
        when(cardRepository.findByIdAndUserUsername(1L, "testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CardNotFoundException.class,
            () -> cardService.getUserCardById("testuser", 1L));
    }

    @Test
    void blockCard_Success() {
        // Given
        testCard.setStatus(CardStatus.ACTIVE);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(testCard)).thenReturn(testCard);

        // When
        CardResponse result = cardService.blockCard(1L);

        // Then
        assertNotNull(result);
        assertEquals(CardStatus.BLOCKED, testCard.getStatus());
        verify(cardRepository).save(testCard);
    }

    @Test
    void blockCard_AlreadyBlocked() {
        // Given
        testCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        // When & Then
        assertThrows(CardOperationException.class, () -> cardService.blockCard(1L));
    }

    @Test
    void activateCard_Success() {
        // Given
        testCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(testCard)).thenReturn(testCard);

        // When
        CardResponse result = cardService.activateCard(1L);

        // Then
        assertNotNull(result);
        assertEquals(CardStatus.ACTIVE, testCard.getStatus());
        verify(cardRepository).save(testCard);
    }

    @Test
    void deleteCard_Success() {
        // Given
        testCard.setBalance(BigDecimal.ZERO);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        // When
        cardService.deleteCard(1L);

        // Then
        verify(cardRepository).delete(testCard);
    }

    @Test
    void deleteCard_PositiveBalance() {
        // Given
        testCard.setBalance(new BigDecimal("100.00"));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        // When & Then
        assertThrows(CardOperationException.class, () -> cardService.deleteCard(1L));
    }

    @Test
    void updateCardBalance_Success() {
        // Given
        BigDecimal newBalance = new BigDecimal("2000.00");
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(testCard)).thenReturn(testCard);

        // When
        CardResponse result = cardService.updateCardBalance(1L, newBalance);

        // Then
        assertNotNull(result);
        assertEquals(newBalance, testCard.getBalance());
        verify(cardRepository).save(testCard);
    }

    @Test
    void transferBetweenCards_Success() throws Exception {
        // Given
        Card fromCard = Card.builder()
                .id(1L)
                .encryptedNumber("encrypted123")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .user(testUser)
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .encryptedNumber("encrypted456")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .user(testUser)
                .build();

        TransferRequest request = new TransferRequest();
        request.setFromCardNumber("1234567890123456");
        request.setToCardNumber("6543210987654321");
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Test transfer");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByUser(testUser)).thenReturn(Arrays.asList(fromCard, toCard));

        // When
        TransferResponse result = cardService.transferBetweenCards("testuser", request);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        assertEquals(new BigDecimal("900.00"), fromCard.getBalance());
        assertEquals(new BigDecimal("600.00"), toCard.getBalance());
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void transferBetweenCards_InsufficientFunds() throws Exception {
        // Given
        Card fromCard = Card.builder()
                .id(1L)
                .encryptedNumber("encrypted123")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("50.00"))
                .user(testUser)
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .encryptedNumber("encrypted456")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .user(testUser)
                .build();

        TransferRequest request = new TransferRequest();
        request.setFromCardNumber("1234567890123456");
        request.setToCardNumber("6543210987654321");
        request.setAmount(new BigDecimal("100.00"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByUser(testUser)).thenReturn(Arrays.asList(fromCard, toCard));

        // When & Then
        assertThrows(TransferException.class,
            () -> cardService.transferBetweenCards("testuser", request));
    }

    @Test
    void transferBetweenCards_SameCard() throws Exception {
        // Given
        Card card = Card.builder()
                .id(1L)
                .encryptedNumber("encrypted123")
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .user(testUser)
                .build();

        TransferRequest request = new TransferRequest();
        request.setFromCardNumber("1234567890123456");
        request.setToCardNumber("1234567890123456");
        request.setAmount(new BigDecimal("100.00"));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByUser(testUser)).thenReturn(Arrays.asList(card));

        // When & Then
        assertThrows(TransferException.class,
            () -> cardService.transferBetweenCards("testuser", request));
    }

    @Test
    void getCardsStatistics_Success() {
        // Given
        Card activeCard = Card.builder()
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        Card blockedCard = Card.builder()
                .status(CardStatus.BLOCKED)
                .balance(new BigDecimal("500.00"))
                .build();

        when(cardRepository.findAll()).thenReturn(Arrays.asList(activeCard, blockedCard));

        // When
        Map<String, Object> result = cardService.getCardsStatistics();

        // Then
        assertNotNull(result);
        assertEquals(2L, result.get("totalCards"));
        assertEquals(1L, result.get("activeCards"));
        assertEquals(1L, result.get("blockedCards"));
        assertEquals(new BigDecimal("1500.00"), result.get("totalBalance"));
        assertEquals(new BigDecimal("750.00"), result.get("averageBalance"));
    }

    @Test
    void getCardsStatistics_EmptyList() {
        // Given
        when(cardRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        Map<String, Object> result = cardService.getCardsStatistics();

        // Then
        assertNotNull(result);
        assertEquals(0L, result.get("totalCards"));
        assertEquals(0L, result.get("activeCards"));
        assertEquals(0L, result.get("blockedCards"));
        assertEquals(BigDecimal.ZERO, result.get("totalBalance"));
        assertEquals(BigDecimal.ZERO, result.get("averageBalance"));
    }
}
