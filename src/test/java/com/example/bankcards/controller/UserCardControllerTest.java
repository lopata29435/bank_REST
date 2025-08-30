package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.enums.BlockRequestStatus;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.security.JwtProvider;
import com.example.bankcards.security.UserPrincipal;
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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserCardController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserCardControllerTest {

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
    private BlockRequestResponse testBlockRequest;
    private TransferResponse testTransferResponse;
    private Authentication mockAuthentication;

    @TestConfiguration
    @EnableWebSecurity
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }

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
        // Настройка мока Authentication
        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        when(userPrincipal.getUsername()).thenReturn("testuser");
        
        mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getPrincipal()).thenReturn(userPrincipal);
        when(mockAuthentication.isAuthenticated()).thenReturn(true);

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

        testBlockRequest = new BlockRequestResponse();
        testBlockRequest.setId(1L);
        testBlockRequest.setCardId(1L);
        testBlockRequest.setCardMaskedNumber("**** **** **** 3456");
        testBlockRequest.setReason("Lost card");
        testBlockRequest.setStatus(BlockRequestStatus.PENDING);
        testBlockRequest.setCreatedAt(Instant.now());

        testTransferResponse = new TransferResponse(
                "TXN123456",
                "**** **** **** 3456",
                "**** **** **** 4321",
                new BigDecimal("100.00"),
                new BigDecimal("900.00"),
                new BigDecimal("1100.00"),
                "Transfer between cards",
                LocalDateTime.now()
        );
    }

    @Test
    void getMyCards_ValidRequest_ReturnsUserCards() throws Exception {
        // Arrange
        when(cardService.getUserCards(anyString(), any(CardFilterRequest.class)))
                .thenReturn(testPageResponse);

        // Act & Assert
        mockMvc.perform(get("/user/cards")
                        .with(authentication(mockAuthentication))
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "id")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].maskedCardNumber").value("**** **** **** 3456"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));

        verify(cardService).getUserCards(eq("testuser"), any(CardFilterRequest.class));
    }

    @Test
    void getCardById_ValidId_ReturnsCard() throws Exception {
        // Arrange
        when(cardService.getUserCardById(anyString(), anyLong()))
                .thenReturn(testCard);

        // Act & Assert
        mockMvc.perform(get("/user/cards/1")
                        .with(authentication(mockAuthentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.maskedCardNumber").value("**** **** **** 3456"))
                .andExpect(jsonPath("$.cardHolderName").value("Test User"));

        verify(cardService).getUserCardById(eq("testuser"), eq(1L));
    }

    @Test
    void requestCardBlock_ValidRequest_ReturnsBlockRequest() throws Exception {
        // Arrange
        BlockRequestCreateRequest request = new BlockRequestCreateRequest();
        request.setReason("Lost card");

        when(blockRequestService.createBlockRequest(anyString(), anyLong(), any(BlockRequestCreateRequest.class)))
                .thenReturn(testBlockRequest);

        // Act & Assert
        mockMvc.perform(post("/user/cards/1/block-request")
                        .with(authentication(mockAuthentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardMaskedNumber").value("**** **** **** 3456"))
                .andExpect(jsonPath("$.reason").value("Lost card"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(blockRequestService).createBlockRequest(eq("testuser"), eq(1L), any(BlockRequestCreateRequest.class));
    }

    @Test
    void getMyBlockRequests_ValidRequest_ReturnsBlockRequests() throws Exception {
        // Arrange
        Page<BlockRequestResponse> page = new PageImpl<>(List.of(testBlockRequest), PageRequest.of(0, 10), 1);

        when(blockRequestService.getUserBlockRequests(anyString(), any(PageRequest.class)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/user/cards/block-requests")
                        .with(authentication(mockAuthentication))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].cardMaskedNumber").value("**** **** **** 3456"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));

        verify(blockRequestService).getUserBlockRequests(eq("testuser"), any(PageRequest.class));
    }

    @Test
    void transferBetweenCards_ValidRequest_ReturnsTransferResponse() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromCardNumber("1234567890123456");
        request.setToCardNumber("6543210987654321");
        request.setAmount(new BigDecimal("100.00"));

        when(cardService.transferBetweenCards(anyString(), any(TransferRequest.class)))
                .thenReturn(testTransferResponse);

        // Act & Assert
        mockMvc.perform(post("/user/cards/transfer")
                        .with(authentication(mockAuthentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value("TXN123456"))
                .andExpect(jsonPath("$.fromMaskedCardNumber").value("**** **** **** 3456"))
                .andExpect(jsonPath("$.toMaskedCardNumber").value("**** **** **** 4321"))
                .andExpect(jsonPath("$.amount").value(100.00));

        verify(cardService).transferBetweenCards(eq("testuser"), any(TransferRequest.class));
    }

    @Test
    void getTotalBalance_ValidRequest_ReturnsTotalBalance() throws Exception {
        // Arrange
        CardResponse card1 = new CardResponse();
        card1.setId(1L);
        card1.setMaskedCardNumber("**** **** **** 3456");
        card1.setBalance(new BigDecimal("1000.00"));
        card1.setStatus(CardStatus.ACTIVE);

        CardResponse card2 = new CardResponse();
        card2.setId(2L);
        card2.setMaskedCardNumber("**** **** **** 4321");
        card2.setBalance(new BigDecimal("500.00"));
        card2.setStatus(CardStatus.ACTIVE);

        PageResponse<CardResponse> cardsResponse = new PageResponse<>(
                List.of(card1, card2),
                0,
                1000,
                2L,
                1,
                true
        );

        when(cardService.getUserCards(anyString(), any(CardFilterRequest.class)))
                .thenReturn(cardsResponse);

        // Act & Assert
        mockMvc.perform(get("/user/cards/balance")
                        .with(authentication(mockAuthentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(1500.00))
                .andExpect(jsonPath("$.cardsCount").value(2))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(cardService).getUserCards(eq("testuser"), any(CardFilterRequest.class));
    }

    @Test
    void getMyCards_InvalidSortParameter_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/user/cards")
                        .with(authentication(mockAuthentication))
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "invalidField")
                        .param("sortDirection", "asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestCardBlock_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Arrange
        BlockRequestCreateRequest request = new BlockRequestCreateRequest();
        // Не заполняем обязательные поля

        // Act & Assert
        mockMvc.perform(post("/user/cards/1/block-request")
                        .with(authentication(mockAuthentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferBetweenCards_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest();
        // Не заполняем обязательные поля

        // Act & Assert
        mockMvc.perform(post("/user/cards/transfer")
                        .with(authentication(mockAuthentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyBlockRequests_InvalidPageSize_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/user/cards/block-requests")
                        .with(authentication(mockAuthentication))
                        .param("page", "0")
                        .param("size", "101")) // Превышаем максимальный размер
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyBlockRequests_NegativePage_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/user/cards/block-requests")
                        .with(authentication(mockAuthentication))
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferBetweenCards_SameCardNumbers_ReturnsBadRequest() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromCardNumber("1234567890123456");
        request.setToCardNumber("1234567890123456"); // Та же карта
        request.setAmount(new BigDecimal("100.00"));

        // Act & Assert
        mockMvc.perform(post("/user/cards/transfer")
                        .with(authentication(mockAuthentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferBetweenCards_NegativeAmount_ReturnsBadRequest() throws Exception {
        // Arrange
        TransferRequest request = new TransferRequest();
        request.setFromCardNumber("1234567890123456");
        request.setToCardNumber("6543210987654321");
        request.setAmount(new BigDecimal("-100.00")); // Отрицательная сумма

        // Act & Assert
        mockMvc.perform(post("/user/cards/transfer")
                        .with(authentication(mockAuthentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCardById_NonExistentCard_ReturnsNotFound() throws Exception {
        // Arrange
        when(cardService.getUserCardById(anyString(), anyLong()))
                .thenThrow(new com.example.bankcards.exception.CardNotFoundException("Card not found"));

        // Act & Assert
        mockMvc.perform(get("/user/cards/999")
                        .with(authentication(mockAuthentication)))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestCardBlock_NonExistentCard_ReturnsNotFound() throws Exception {
        // Arrange
        BlockRequestCreateRequest request = new BlockRequestCreateRequest();
        request.setReason("Lost card");

        when(blockRequestService.createBlockRequest(anyString(), anyLong(), any(BlockRequestCreateRequest.class)))
                .thenThrow(new com.example.bankcards.exception.CardNotFoundException("Card not found"));

        // Act & Assert
        mockMvc.perform(post("/user/cards/999/block-request")
                        .with(authentication(mockAuthentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
} 