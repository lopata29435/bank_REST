package com.example.bankcards.service;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.TransferException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardEncryptor cardEncryptor;

    @Transactional
    public CardResponse createCardForUser(String username, AdminCreateCardRequest request) {
        User user = findUserByUsername(username);
        String cardNumber = request.getCardNumber().trim();

        validateCardNumberUniqueness(cardNumber);

        Card card = buildCardFromAdminRequest(request, user);
        Card savedCard = cardRepository.save(card);

        log.info("Created new card for user: {}, cardId: {}", username, savedCard.getId());
        return mapToCardResponse(savedCard);
    }

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getUserCards(String username, CardFilterRequest filter) {
        User user = findUserByUsername(username);
        return getCardsWithFilter(filter, user);
    }

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> getAllCards(CardFilterRequest filter) {
        return getCardsWithFilter(filter, null);
    }

    @Transactional
    public CardResponse blockCard(Long cardId) {
        Card card = findCardById(cardId);
        validateCardNotAlreadyBlocked(card);

        card.setStatus(CardStatus.BLOCKED);
        Card savedCard = cardRepository.save(card);

        log.info("Card blocked: cardId={}", cardId);
        return mapToCardResponse(savedCard);
    }

    @Transactional
    public CardResponse activateCard(Long cardId) {
        Card card = findCardById(cardId);
        card.setStatus(CardStatus.ACTIVE);
        Card savedCard = cardRepository.save(card);

        log.info("Card activated: cardId={}", cardId);
        return mapToCardResponse(savedCard);
    }

    @Transactional
    public void deleteCard(Long cardId) {
        Card card = findCardById(cardId);
        validateCardCanBeDeleted(card);

        cardRepository.delete(card);
        log.info("Card deleted: cardId={}", cardId);
    }

    @Transactional(readOnly = true)
    public CardResponse getCardById(Long cardId) {
        Card card = findCardById(cardId);
        log.debug("Retrieved card by ID: {}", cardId);
        return mapToCardResponse(card);
    }

    @Transactional(readOnly = true)
    public CardResponse getUserCardById(String username, Long cardId) {
        Card card = findCardByIdAndUser(cardId, username);
        log.debug("Retrieved user-scoped card by ID: {} for user {}", cardId, username);
        return mapToCardResponse(card);
    }

    @Transactional
    public CardResponse updateCardBalance(Long cardId, BigDecimal newBalance) {
        Card card = findCardById(cardId);
        card.setBalance(newBalance);
        Card savedCard = cardRepository.save(card);

        log.info("Card balance updated by admin: cardId={}, newBalance={}", cardId, newBalance);
        return mapToCardResponse(savedCard);
    }

    @Transactional
    public TransferResponse transferBetweenCards(String username, TransferRequest request) {
        User user = findUserByUsername(username);
        Card fromCard = findCardByNumberAndUser(request.getFromCardNumber(), user);
        Card toCard = findCardByNumberAndUser(request.getToCardNumber(), user);

        validateTransfer(fromCard, toCard, request.getAmount());

        performTransfer(fromCard, toCard, request.getAmount());
        String transactionId = UUID.randomUUID().toString();

        log.info("Transfer completed: transactionId={}, from={}, to={}, amount={}",
                transactionId, request.getFromCardNumber(), request.getToCardNumber(), request.getAmount());

        return createTransferResponse(transactionId, fromCard, toCard, request);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCardsStatistics() {
        List<Card> allCards = cardRepository.findAll();

        long totalCards = allCards.size();
        long activeCards = allCards.stream().filter(c -> c.getStatus() == CardStatus.ACTIVE).count();
        long blockedCards = allCards.stream().filter(c -> c.getStatus() == CardStatus.BLOCKED).count();

        BigDecimal totalBalance = allCards.stream()
                .map(Card::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageBalance = totalCards > 0
                ? totalBalance.divide(BigDecimal.valueOf(totalCards), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return Map.of(
                "totalCards", totalCards,
                "activeCards", activeCards,
                "blockedCards", blockedCards,
                "totalBalance", totalBalance,
                "averageBalance", averageBalance
        );
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> UserNotFoundException.byUsername(username));
    }

    private Card findCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found"));
    }

    private Card findCardByIdAndUser(Long cardId, String username) {
        return cardRepository.findByIdAndUserUsername(cardId, username)
                .orElseThrow(() -> new CardNotFoundException("Card not found"));
    }

    private Card findCardByNumberAndUser(String cardNumber, User user) {
        List<Card> userCards = cardRepository.findByUser(user);

        return userCards.stream()
                .filter(card -> isCardNumberMatch(card, cardNumber))
                .findFirst()
                .orElseThrow(() -> CardNotFoundException.accessDenied(cardNumber));
    }

    private boolean isCardNumberMatch(Card card, String cardNumber) {
        try {
            return card.getCardNumber().equals(cardNumber);
        } catch (Exception e) {
            log.error("Error decrypting card number for card {}: {}", card.getId(), e.getMessage());
            return false;
        }
    }

    private void validateCardNumberUniqueness(String cardNumber) {
        try {
            String encryptedNumber = cardEncryptor.encrypt(cardNumber);
            if (cardRepository.existsByEncryptedNumber(encryptedNumber)) {
                throw new CardOperationException("Card number already exists");
            }
        } catch (Exception e) {
            log.error("Error validating card number uniqueness: {}", e.getMessage());
            throw new CardOperationException("Error validating card number: " + e.getMessage());
        }
    }

    private void validateCardNotAlreadyBlocked(Card card) {
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new CardOperationException("Card is already blocked");
        }
    }

    private void validateCardCanBeDeleted(Card card) {
        if (card.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new CardOperationException("Cannot delete card with positive balance");
        }
    }

    private void validateTransfer(Card fromCard, Card toCard, BigDecimal amount) {
        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new TransferException("Source card is not active");
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new TransferException("Destination card is not active");
        }
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new TransferException("Insufficient funds");
        }
        if (fromCard.getId().equals(toCard.getId())) {
            throw new TransferException("Cannot transfer to the same card");
        }
    }

    private Card buildCardFromAdminRequest(AdminCreateCardRequest request, User user) {
        try {
            return Card.builder()
                    .cardNumber(request.getCardNumber())
                    .cardHolderName(request.getCardHolderName())
                    .expirationMonth(request.getExpirationMonth())
                    .expirationYear(request.getExpirationYear())
                    .status(CardStatus.ACTIVE)
                    .balance(request.getInitialBalance())
                    .user(user)
                    .build();
        } catch (Exception e) {
            log.error("Error building card from admin request: {}", e.getMessage());
            throw new CardOperationException("Failed to create card: " + e.getMessage());
        }
    }

    private PageResponse<CardResponse> getCardsWithFilter(CardFilterRequest filter, User user) {
        Specification<Card> spec = createSpecification(filter, user);
        Pageable pageable = createPageable(filter);

        Page<Card> cards = cardRepository.findAll(spec, pageable);
        List<CardResponse> content = cards.getContent().stream()
                .map(this::mapToCardResponse)
                .toList();

        return new PageResponse<>(
                content,
                cards.getNumber(),
                cards.getSize(),
                cards.getTotalElements(),
                cards.getTotalPages(),
                cards.isLast()
        );
    }

    private void performTransfer(Card fromCard, Card toCard, BigDecimal amount) {
        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));
        cardRepository.save(fromCard);
        cardRepository.save(toCard);
    }

    private TransferResponse createTransferResponse(String transactionId, Card fromCard, Card toCard, TransferRequest request) {
        try {
            return new TransferResponse(
                    transactionId,
                    fromCard.getMaskedNumber(),
                    toCard.getMaskedNumber(),
                    request.getAmount(),
                    fromCard.getBalance(),
                    toCard.getBalance(),
                    request.getDescription(),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("Error creating transfer response: {}", e.getMessage());
            return new TransferResponse(
                    transactionId,
                    "**** **** **** ****",
                    "**** **** **** ****",
                    request.getAmount(),
                    fromCard.getBalance(),
                    toCard.getBalance(),
                    request.getDescription(),
                    LocalDateTime.now()
            );
        }
    }

    private Specification<Card> createSpecification(CardFilterRequest filter, User user) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (user != null) {
                predicates.add(criteriaBuilder.equal(root.get("user"), user));
            }

            if (filter.getCardNumber() != null && !filter.getCardNumber().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("encryptedNumber")),
                    "%" + filter.getCardNumber().toLowerCase() + "%"
                ));
            }

            if (filter.getCardHolderName() != null && !filter.getCardHolderName().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("cardHolderName")),
                    "%" + filter.getCardHolderName().toLowerCase() + "%"
                ));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getMinBalance() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("balance"), filter.getMinBalance()));
            }
            if (filter.getMaxBalance() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("balance"), filter.getMaxBalance()));
            }

            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Pageable createPageable(CardFilterRequest filter) {
        Sort.Direction direction = filter.getSortDirection().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, filter.getSortBy());
        return PageRequest.of(filter.getPage(), filter.getSize(), sort);
    }

    private CardResponse mapToCardResponse(Card card) {
        try {
            CardResponse response = new CardResponse();
            response.setId(card.getId());
            response.setMaskedCardNumber(card.getMaskedNumber());
            response.setCardHolderName(card.getCardHolderName());
            response.setExpirationMonth(card.getExpirationMonth());
            response.setExpirationYear(card.getExpirationYear());
            response.setBalance(card.getBalance());
            response.setStatus(card.getStatus());

            return response;
        } catch (Exception e) {
            log.error("Error mapping card to response: {}", e.getMessage(), e);
            throw new CardOperationException("Error mapping card: " + e.getMessage());
        }
    }
}
