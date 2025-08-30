package com.example.bankcards.config;

import com.example.bankcards.entity.Card;
import com.example.bankcards.util.CardEncryptor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CardEncryptorInitializer {

    private final CardEncryptor cardEncryptor;

    public CardEncryptorInitializer(CardEncryptor cardEncryptor) {
        this.cardEncryptor = cardEncryptor;
    }

    @PostConstruct
    public void initializeCardEncryptor() {
        try {
            Card.setEncryptor(cardEncryptor);
            log.info("Card encryptor initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize card encryptor", e);
            throw new RuntimeException("Failed to initialize card encryptor", e);
        }
    }
}
