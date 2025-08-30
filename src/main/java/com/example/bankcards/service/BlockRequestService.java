package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.BlockRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.BlockRequestStatus;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.InvalidDecisionException;
import com.example.bankcards.exception.BlockRequestException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.BlockRequestRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockRequestService {

    private static final String DECISION_APPROVE = "approve";
    private static final String DECISION_REJECT = "reject";

    private final BlockRequestRepository blockRequestRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @Transactional
    public BlockRequestResponse createBlockRequest(String username, Long cardId, BlockRequestCreateRequest request) {
        User user = findUserByUsername(username);
        Card card = findCardByIdAndUser(cardId, username);

        validateCardForBlockRequest(card);

        BlockRequest blockRequest = BlockRequest.builder()
                .card(card)
                .user(user)
                .reason(request.getReason())
                .status(BlockRequestStatus.PENDING)
                .build();

        BlockRequest savedRequest = blockRequestRepository.save(blockRequest);
        log.info("Block request created: id={}, cardId={}, user={}", savedRequest.getId(), cardId, username);

        return mapToBlockRequestResponse(savedRequest);
    }

    @Transactional(readOnly = true)
    public Page<BlockRequestResponse> getUserBlockRequests(String username, Pageable pageable) {
        User user = findUserByUsername(username);
        Page<BlockRequest> requests = blockRequestRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return requests.map(this::mapToBlockRequestResponse);
    }

    @Transactional(readOnly = true)
    public Page<BlockRequestResponse> getAllBlockRequests(BlockRequestStatus status, Pageable pageable) {
        Page<BlockRequest> requests = (status != null)
                ? blockRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : blockRequestRepository.findAll(pageable);

        return requests.map(this::mapToBlockRequestResponse);
    }

    @Transactional
    public BlockRequestResponse processBlockRequest(String adminUsername, Long requestId, BlockRequestProcessRequest request) {
        User admin = findUserByUsername(adminUsername);
        BlockRequest blockRequest = findBlockRequestById(requestId);

        validateBlockRequestForProcessing(blockRequest);

        updateBlockRequestWithDecision(blockRequest, admin, request);
        BlockRequest savedRequest = blockRequestRepository.save(blockRequest);

        log.info("Block request processed: id={}, status={}, admin={}",
                savedRequest.getId(), savedRequest.getStatus(), adminUsername);

        return mapToBlockRequestResponse(savedRequest);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBlockRequestsStatistics() {
        long pendingCount = blockRequestRepository.countByStatus(BlockRequestStatus.PENDING);
        long approvedCount = blockRequestRepository.countByStatus(BlockRequestStatus.APPROVED);
        long rejectedCount = blockRequestRepository.countByStatus(BlockRequestStatus.REJECTED);
        long totalCount = pendingCount + approvedCount + rejectedCount;

        return Map.of(
                "totalRequests", totalCount,
                "pendingRequests", pendingCount,
                "approvedRequests", approvedCount,
                "rejectedRequests", rejectedCount
        );
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> UserNotFoundException.byUsername(username));
    }

    private Card findCardByIdAndUser(Long cardId, String username) {
        return cardRepository.findByIdAndUserUsername(cardId, username)
                .orElseThrow(() -> new CardNotFoundException("Card not found or does not belong to user"));
    }

    private BlockRequest findBlockRequestById(Long requestId) {
        return blockRequestRepository.findById(requestId)
                .orElseThrow(() -> new BlockRequestException("Block request not found"));
    }

    private void validateCardForBlockRequest(Card card) {
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BlockRequestException("Card is already blocked");
        }

        if (blockRequestRepository.existsByCardAndStatus(card, BlockRequestStatus.PENDING)) {
            throw new BlockRequestException("Block request for this card is already pending");
        }
    }

    private void validateBlockRequestForProcessing(BlockRequest blockRequest) {
        if (blockRequest.getStatus() != BlockRequestStatus.PENDING) {
            throw new BlockRequestException("Block request has already been processed");
        }
    }

    private void updateBlockRequestWithDecision(BlockRequest blockRequest, User admin, BlockRequestProcessRequest request) {
        blockRequest.setProcessedAt(Instant.now());
        blockRequest.setProcessedByAdmin(admin);
        blockRequest.setAdminComment(request.getAdminComment());

        String decision = request.getDecision();

        if (DECISION_APPROVE.equalsIgnoreCase(decision)) {
            blockRequest.setStatus(BlockRequestStatus.APPROVED);
            blockCardIfNotAlreadyBlocked(blockRequest.getCard(), admin.getUsername());
        } else if (DECISION_REJECT.equalsIgnoreCase(decision)) {
            blockRequest.setStatus(BlockRequestStatus.REJECTED);
        } else {
            throw new InvalidDecisionException("Invalid decision. Use '" + DECISION_APPROVE + "' or '" + DECISION_REJECT + "'");
        }
    }

    private void blockCardIfNotAlreadyBlocked(Card card, String adminUsername) {
        if (card.getStatus() != CardStatus.BLOCKED) {
            card.setStatus(CardStatus.BLOCKED);
            cardRepository.save(card);
            log.info("Card blocked by admin: cardId={}, admin={}", card.getId(), adminUsername);
        }
    }

    private BlockRequestResponse mapToBlockRequestResponse(BlockRequest blockRequest) {
        BlockRequestResponse response = new BlockRequestResponse();
        response.setId(blockRequest.getId());
        response.setCardId(blockRequest.getCard().getId());

        try {
            response.setCardMaskedNumber(blockRequest.getCard().getMaskedNumber());
        } catch (Exception e) {
            log.error("Error getting masked card number for card ID {}: {}", blockRequest.getCard().getId(), e.getMessage());
            response.setCardMaskedNumber("**** **** **** ****");
        }

        response.setReason(blockRequest.getReason());
        response.setStatus(blockRequest.getStatus());
        response.setCreatedAt(blockRequest.getCreatedAt());
        response.setProcessedAt(blockRequest.getProcessedAt());

        if (blockRequest.getProcessedByAdmin() != null) {
            response.setProcessedByAdmin(blockRequest.getProcessedByAdmin().getUsername());
        }

        response.setAdminComment(blockRequest.getAdminComment());
        return response;
    }
}
