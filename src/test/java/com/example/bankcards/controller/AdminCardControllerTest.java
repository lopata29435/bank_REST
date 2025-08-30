package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.enums.BlockRequestStatus;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.security.JwtProvider;
import com.example.bankcards.service.BlockRequestService;
import com.example.bankcards.service.CardService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminCardController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardService cardService;

    @Autowired
    private BlockRequestService blockRequestService;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private CardResponse testCard;
    private PageResponse<CardResponse> testPageResponse;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CardService cardService() {
            return mock(CardService.class);
        }

        @Bean
        public BlockRequestService blockRequestService() {
            return mock(BlockRequestService.class);
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
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        testCard = new CardResponse();
        testCard.setId(1L);
        testCard.setMaskedCardNumber("**** **** **** 3456");
        testCard.setCardHolderName("Test User");
        testCard.setBalance(new BigDecimal("1000.00"));
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setExpirationMonth(12);
        testCard.setExpirationYear(2027);

        testPageResponse = new PageResponse<>(
                List.of(testCard),
                0,
                20,
                1L,
                1,
                true
        );

        BlockRequestResponse testBlockRequest = new BlockRequestResponse();
        testBlockRequest.setId(1L);
        testBlockRequest.setCardId(1L);
        testBlockRequest.setCardMaskedNumber("**** **** **** 3456");
        testBlockRequest.setReason("Lost card");
        testBlockRequest.setStatus(BlockRequestStatus.PENDING);
        testBlockRequest.setCreatedAt(Instant.now());

    }

    @Test
    void getAllCards_ValidRequest_ReturnsCardsList() throws Exception {
        // Arrange
        when(cardService.getAllCards(any(CardFilterRequest.class)))
                .thenReturn(testPageResponse);

        // Act & Assert
        mockMvc.perform(get("/admin/cards")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "id")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].maskedCardNumber").value("**** **** **** 3456"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(cardService).getAllCards(any(CardFilterRequest.class));
    }

    @Test
    void createCard_ValidRequest_ReturnsCreatedCard() throws Exception {
        // Arrange
        AdminCreateCardRequest request = new AdminCreateCardRequest();
        request.setUsername("testuser");
        request.setCardNumber("1234567890123456");
        request.setCardHolderName("TEST USER");
        request.setExpirationMonth(12);
        request.setExpirationYear(2027);
        request.setInitialBalance(new BigDecimal("500.00"));

        when(cardService.createCardForUser(anyString(), any(AdminCreateCardRequest.class)))
                .thenReturn(testCard);

        // Act & Assert
        mockMvc.perform(post("/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.maskedCardNumber").value("**** **** **** 3456"))
                .andExpect(jsonPath("$.cardHolderName").value("Test User"));

        verify(cardService).createCardForUser(eq("testuser"), any(AdminCreateCardRequest.class));
    }

    @Test
    void getCardById_ValidId_ReturnsCard() throws Exception {
        // Arrange
        when(cardService.getCardById(1L))
                .thenReturn(testCard);

        // Act & Assert
        mockMvc.perform(get("/admin/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.maskedCardNumber").value("**** **** **** 3456"));

        verify(cardService).getCardById(1L);
    }

    @Test
    void activateCard_ValidId_ReturnsActivatedCard() throws Exception {
        // Arrange
        CardResponse activatedCard = new CardResponse();
        activatedCard.setId(1L);
        activatedCard.setMaskedCardNumber("**** **** **** 3456");
        activatedCard.setCardHolderName("Test User");
        activatedCard.setBalance(new BigDecimal("1000.00"));
        activatedCard.setStatus(CardStatus.ACTIVE);
        activatedCard.setExpirationMonth(12);
        activatedCard.setExpirationYear(2027);

        when(cardService.activateCard(1L))
                .thenReturn(activatedCard);

        // Act & Assert
        mockMvc.perform(post("/admin/cards/1/activate").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(cardService).activateCard(1L);
    }

    @Test
    void blockCard_ValidId_ReturnsBlockedCard() throws Exception {
        // Arrange
        CardResponse blockedCard = new CardResponse();
        blockedCard.setId(1L);
        blockedCard.setMaskedCardNumber("**** **** **** 3456");
        blockedCard.setCardHolderName("Test User");
        blockedCard.setBalance(new BigDecimal("1000.00"));
        blockedCard.setStatus(CardStatus.BLOCKED);
        blockedCard.setExpirationMonth(12);
        blockedCard.setExpirationYear(2027);

        when(cardService.blockCard(1L))
                .thenReturn(blockedCard);

        // Act & Assert
        mockMvc.perform(post("/admin/cards/1/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        verify(cardService).blockCard(1L);
    }

    @Test
    void deleteCard_ValidId_ReturnsSuccessMessage() throws Exception {
        // Arrange
        doNothing().when(cardService).deleteCard(1L);

        // Act & Assert
        mockMvc.perform(delete("/admin/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Card deleted successfully"))
                .andExpect(jsonPath("$.status").value(200));

        verify(cardService).deleteCard(1L);
    }

    @Test
    void updateCardBalance_ValidRequest_ReturnsUpdatedCard() throws Exception {
        // Arrange
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.setNewBalance(new BigDecimal("2000.00"));

        CardResponse updatedCard = new CardResponse();
        updatedCard.setId(1L);
        updatedCard.setMaskedCardNumber("**** **** **** 3456");
        updatedCard.setCardHolderName("Test User");
        updatedCard.setBalance(new BigDecimal("2000.00"));
        updatedCard.setStatus(CardStatus.ACTIVE);
        updatedCard.setExpirationMonth(12);
        updatedCard.setExpirationYear(2027);

        when(cardService.updateCardBalance(1L, new BigDecimal("2000.00")))
                .thenReturn(updatedCard);

        // Act & Assert
        mockMvc.perform(put("/admin/cards/1/balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(2000.00));

        verify(cardService).updateCardBalance(1L, new BigDecimal("2000.00"));
    }

    @Test
    void getCardsStatistics_ValidRequest_ReturnsStatistics() throws Exception {
        // Arrange
        Map<String, Object> statistics = Map.of(
                "totalCards", 100,
                "activeCards", 80,
                "blockedCards", 20,
                "totalBalance", new BigDecimal("50000.00")
        );

        when(cardService.getCardsStatistics())
                .thenReturn(statistics);

        // Act & Assert
        mockMvc.perform(get("/admin/cards/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCards").value(100))
                .andExpect(jsonPath("$.activeCards").value(80))
                .andExpect(jsonPath("$.blockedCards").value(20));

        verify(cardService).getCardsStatistics();
    }

    @Test
    void getUserCards_ValidUsername_ReturnsUserCards() throws Exception {
        // Arrange
        Page<CardResponse> page = new PageImpl<>(List.of(testCard), PageRequest.of(0, 20), 1);
        when(cardService.getUserCards(anyString(), any(CardFilterRequest.class)))
                .thenReturn(testPageResponse);

        // Act & Assert
        mockMvc.perform(get("/admin/cards/user/testuser")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "id")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(cardService).getUserCards(eq("testuser"), any(CardFilterRequest.class));
    }

    @Test
    void getBlockRequestsStatistics_ValidRequest_ReturnsStatistics() throws Exception {
        // Arrange
        Map<String, Object> statistics = Map.of(
                "totalRequests", 50,
                "pendingRequests", 10,
                "approvedRequests", 30,
                "rejectedRequests", 10
        );

        when(blockRequestService.getBlockRequestsStatistics())
                .thenReturn(statistics);

        // Act & Assert
        mockMvc.perform(get("/admin/cards/block-requests/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(50))
                .andExpect(jsonPath("$.pendingRequests").value(10))
                .andExpect(jsonPath("$.approvedRequests").value(30))
                .andExpect(jsonPath("$.rejectedRequests").value(10));

        verify(blockRequestService).getBlockRequestsStatistics();
    }

    @Test
    void getAllCards_InvalidSortParameter_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/admin/cards")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "invalidField")
                        .param("sortDirection", "asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Arrange
        AdminCreateCardRequest request = new AdminCreateCardRequest();
        // Не заполняем обязательные поля

        // Act & Assert
        mockMvc.perform(post("/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
} 