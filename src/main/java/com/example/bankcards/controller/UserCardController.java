package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user/cards")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
@Tag(name = "User Cards", description = "User card management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class UserCardController {

    private final CardService cardService;
    private final BlockRequestService blockRequestService;

    private static final List<String> VALID_SORT_FIELDS = Arrays.asList("id", "cardNumber", "cardHolderName", "balance", "status", "createdAt");
    private static final List<String> VALID_SORT_DIRECTIONS = Arrays.asList("asc", "desc");

    @GetMapping
    @Operation(
        summary = "Get user cards",
        description = "Retrieve paginated list of cards for the authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    public ResponseEntity<PageResponse<CardResponse>> getMyCards(
            Authentication authentication,
            @Valid CardFilterParamsRequest filterParams) {

        validateSortParameters(filterParams.getSortBy(), filterParams.getSortDirection());
        String username = getUsernameFromAuthentication(authentication);
        CardFilterRequest filter = buildCardFilter(filterParams);
        PageResponse<CardResponse> cards = cardService.getUserCards(username, filter);

        log.info("User {} requested cards, page: {}, size: {}", username, filterParams.getPage(), filterParams.getSize());
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{cardId}")
    @Operation(
        summary = "Get card by ID",
        description = "Retrieve specific card details for the authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CardResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "Card not found", content = @Content)
    })
    public ResponseEntity<CardResponse> getCardById(
            @Parameter(description = "Card ID", required = true)
            @PathVariable Long cardId,
            Authentication authentication) {

        String username = getUsernameFromAuthentication(authentication);
        CardResponse card = cardService.getUserCardById(username, cardId);
        return ResponseEntity.ok(card);
    }

    @PostMapping("/{cardId}/block-request")
    @Operation(
        summary = "Request card blocking",
        description = "Create a request to block a specific card (requires admin approval)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Block request created successfully",
                    content = @Content(schema = @Schema(implementation = BlockRequestResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or card already has pending block request", content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
        @ApiResponse(responseCode = "404", description = "Card not found", content = @Content)
    })
    public ResponseEntity<BlockRequestResponse> requestCardBlock(
            @Parameter(description = "Card ID to request blocking", required = true)
            @PathVariable Long cardId,
            @Parameter(description = "Block request details", required = true)
            @Valid @RequestBody BlockRequestCreateRequest request,
            Authentication authentication) {

        String username = getUsernameFromAuthentication(authentication);
        BlockRequestResponse blockRequest = blockRequestService.createBlockRequest(username, cardId, request);

        log.info("User {} requested blocking for card {}", username, cardId);
        return ResponseEntity.status(HttpStatus.CREATED).body(blockRequest);
    }

    @GetMapping("/block-requests")
    @Operation(
        summary = "Get my block requests",
        description = "Retrieve paginated list of block requests for the authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Block requests retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    public ResponseEntity<PageResponse<BlockRequestResponse>> getMyBlockRequests(
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            Authentication authentication) {

        String username = getUsernameFromAuthentication(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<BlockRequestResponse> requests = blockRequestService.getUserBlockRequests(username, pageable);

        return ResponseEntity.ok(createPageResponse(requests));
    }

    @PostMapping("/transfer")
    @Operation(
        summary = "Transfer between cards",
        description = "Transfer money between user's cards"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transfer completed successfully",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transfer request", content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    public ResponseEntity<TransferResponse> transferBetweenCards(
            @Parameter(description = "Transfer request details", required = true)
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {

        String username = getUsernameFromAuthentication(authentication);
        TransferResponse transfer = cardService.transferBetweenCards(username, request);

        log.info("User {} made transfer: {} -> {}, amount: {}",
                username, request.getFromCardNumber(), request.getToCardNumber(), request.getAmount());

        return ResponseEntity.ok(transfer);
    }

    @GetMapping("/balance")
    @Operation(
        summary = "Get total balance",
        description = "Get the total balance across all user's cards"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Total balance retrieved successfully", content = @Content),
        @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> getTotalBalance(Authentication authentication) {
        String username = getUsernameFromAuthentication(authentication);

        CardFilterRequest filter = new CardFilterRequest();
        filter.setSize(1000);
        PageResponse<CardResponse> cards = cardService.getUserCards(username, filter);

        BigDecimal totalBalance = cards.getContent().stream()
                .map(CardResponse::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = Map.of(
                "totalBalance", totalBalance,
                "cardsCount", cards.getTotalElements(),
                "username", username
        );

        return ResponseEntity.ok(response);
    }

    private String getUsernameFromAuthentication(Authentication authentication) {
        return ((UserPrincipal) authentication.getPrincipal()).getUsername();
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
