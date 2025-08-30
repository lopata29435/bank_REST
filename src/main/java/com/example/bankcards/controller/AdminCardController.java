package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.enums.BlockRequestStatus;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.InvalidParameterException;
import com.example.bankcards.security.UserPrincipal;
import com.example.bankcards.service.BlockRequestService;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/admin/cards")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin Cards", description = "Administrative card management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class AdminCardController {

    private final CardService cardService;
    private final BlockRequestService blockRequestService;

    private static final int MAX_PAGE_SIZE = 100;
    private static final List<String> VALID_SORT_FIELDS = Arrays.asList("id", "cardNumber", "cardHolderName", "balance", "status", "createdAt");
    private static final List<String> VALID_SORT_DIRECTIONS = Arrays.asList("asc", "desc");

    @GetMapping
    @Operation(
        summary = "Get all cards (Admin)",
        description = "Retrieve list of all cards in the system (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content)
    })
    public ResponseEntity<PageResponse<CardResponse>> getAllCards(@Valid CardFilterParamsRequest filterParams) {
        validateSortParameters(filterParams.getSortBy(), filterParams.getSortDirection());
        CardFilterRequest filter = buildCardFilter(filterParams);
        PageResponse<CardResponse> cards = cardService.getAllCards(filter);
        return ResponseEntity.ok(cards);
    }

    @PostMapping
    @Operation(
        summary = "Create card for user (Admin)",
        description = "Create a new card for a specific user (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Card created successfully",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content)
    })
    public ResponseEntity<CardResponse> createCard(
            @Parameter(description = "Card creation request details", required = true)
            @Valid @RequestBody AdminCreateCardRequest request) {
        CardResponse card = cardService.createCardForUser(request.getUsername(), request);
        log.info("Admin created card for user: {}, cardId: {}", request.getUsername(), card.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    @GetMapping("/{cardId}")
    @Operation(
        summary = "Get card by ID (Admin)",
        description = "Retrieve specific card details by ID (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Card not found", content = @Content)
    })
    public ResponseEntity<CardResponse> getCardById(
            @Parameter(description = "Card ID", required = true)
            @PathVariable Long cardId) {

        CardResponse card = cardService.getCardById(cardId);
        return ResponseEntity.ok(card);
    }

    @PostMapping("/{cardId}/activate")
    @Operation(
        summary = "Activate card (Admin)",
        description = "Activate a specific card (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card activated successfully",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Card not found", content = @Content)
    })
    public ResponseEntity<CardResponse> activateCard(
            @Parameter(description = "Card ID to activate", required = true)
            @PathVariable Long cardId) {
        CardResponse card = cardService.activateCard(cardId);
        log.info("Admin activated card: {}", cardId);
        return ResponseEntity.ok(card);
    }

    @PostMapping("/{cardId}/block")
    @Operation(
        summary = "Block card (Admin)",
        description = "Block a specific card (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card blocked successfully",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Card not found", content = @Content)
    })
    public ResponseEntity<CardResponse> blockCard(
            @Parameter(description = "Card ID to block", required = true)
            @PathVariable Long cardId) {
        CardResponse card = cardService.blockCard(cardId);
        log.info("Admin blocked card: {}", cardId);
        return ResponseEntity.ok(card);
    }

    @DeleteMapping("/{cardId}")
    @Operation(
        summary = "Delete card (Admin)",
        description = "Delete a specific card (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card deleted successfully", content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Card not found", content = @Content)
    })
    public ResponseEntity<ApiMessageResponse> deleteCard(
            @Parameter(description = "Card ID to delete", required = true)
            @PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        log.info("Admin deleted card: {}", cardId);

        ApiMessageResponse response = ApiMessageResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .message("Card deleted successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{cardId}/balance")
    @Operation(
        summary = "Update card balance (Admin)",
        description = "Update the balance of a specific card (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card balance updated successfully",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid balance value", content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Card not found", content = @Content)
    })
    public ResponseEntity<CardResponse> updateCardBalance(
            @Parameter(description = "Card ID", required = true)
            @PathVariable Long cardId,
            @Parameter(description = "New balance details", required = true)
            @Valid @RequestBody UpdateBalanceRequest request) {

        CardResponse card = cardService.updateCardBalance(cardId, request.getNewBalance());
        log.info("Admin updated card {} balance to {}", cardId, request.getNewBalance());
        return ResponseEntity.ok(card);
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get cards statistics (Admin)",
        description = "Get system-wide card statistics (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> getCardsStatistics() {
        Map<String, Object> stats = cardService.getCardsStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/user/{username}")
    @Operation(
        summary = "Get user cards (Admin)",
        description = "Get all cards for a specific user (Admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User cards retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<PageResponse<CardResponse>> getUserCards(
            @Parameter(description = "Username", required = true)
            @PathVariable String username,
            @Valid CardFilterParamsRequest filterParams) {

        validateSortParameters(filterParams.getSortBy(), filterParams.getSortDirection());
        CardFilterRequest filter = buildCardFilter(filterParams);
        PageResponse<CardResponse> cards = cardService.getUserCards(username, filter);
        log.info("Admin requested cards for user: {}", username);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/block-requests")
    @Operation(
        summary = "Get all block requests",
        description = "Retrieve all block requests with optional status filtering"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Block requests retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    public ResponseEntity<PageResponse<BlockRequestResponse>> getAllBlockRequests(
            @Parameter(description = "Filter by status (PENDING, APPROVED, REJECTED)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        BlockRequestStatus requestStatus = parseBlockRequestStatus(status);
        Pageable pageable = PageRequest.of(page, size);
        Page<BlockRequestResponse> requests = blockRequestService.getAllBlockRequests(requestStatus, pageable);

        return ResponseEntity.ok(createPageResponse(requests));
    }

    @PostMapping("/block-requests/{requestId}/process")
    @Operation(
        summary = "Process block request",
        description = "Approve or reject a block request"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Block request processed successfully",
                    content = @Content(schema = @Schema(implementation = BlockRequestResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or already processed", content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "Block request not found", content = @Content)
    })
    public ResponseEntity<BlockRequestResponse> processBlockRequest(
            @Parameter(description = "Block request ID", required = true)
            @PathVariable Long requestId,
            @Parameter(description = "Processing decision", required = true)
            @Valid @RequestBody BlockRequestProcessRequest request,
            Authentication authentication) {

        String adminUsername = ((UserPrincipal) authentication.getPrincipal()).getUsername();
        BlockRequestResponse processedRequest = blockRequestService.processBlockRequest(adminUsername, requestId, request);

        log.info("Admin {} processed block request {}: {}", adminUsername, requestId, request.getDecision());
        return ResponseEntity.ok(processedRequest);
    }

    @GetMapping("/block-requests/statistics")
    @Operation(
        summary = "Get block requests statistics",
        description = "Get statistics about block requests"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> getBlockRequestsStatistics() {
        Map<String, Object> statistics = blockRequestService.getBlockRequestsStatistics();
        return ResponseEntity.ok(statistics);
    }

    private void validateSortParameters(String sortBy, String sortDirection) {
        if (!VALID_SORT_FIELDS.contains(sortBy)) {
            throw InvalidParameterException.invalidParameter("sortBy", sortBy + ". Valid values: " + VALID_SORT_FIELDS);
        }
        if (!VALID_SORT_DIRECTIONS.contains(sortDirection.toLowerCase())) {
            throw InvalidParameterException.invalidParameter("sortDirection", sortDirection + ". Valid values: " + VALID_SORT_DIRECTIONS);
        }
    }

    private CardFilterRequest buildCardFilter(CardFilterParamsRequest params) {
        CardFilterRequest filter = new CardFilterRequest();
        filter.setCardNumber(params.getCardNumber());
        filter.setCardHolderName(params.getCardHolderName());
        filter.setStatus(parseCardStatus(params.getStatus()));
        filter.setMinBalance(params.getMinBalance());
        filter.setMaxBalance(params.getMaxBalance());
        filter.setPage(params.getPage());
        filter.setSize(params.getSize());
        filter.setSortBy(params.getSortBy());
        filter.setSortDirection(params.getSortDirection());
        return filter;
    }

    private CardStatus parseCardStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return CardStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw InvalidParameterException.invalidStatus(status);
        }
    }

    private BlockRequestStatus parseBlockRequestStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return BlockRequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw InvalidParameterException.invalidParameter("block request status", status);
        }
    }

    private PageResponse<BlockRequestResponse> createPageResponse(Page<BlockRequestResponse> requests) {
        return new PageResponse<>(
                requests.getContent(),
                requests.getNumber(),
                requests.getSize(),
                requests.getTotalElements(),
                requests.getTotalPages(),
                requests.isLast()
        );
    }
}
