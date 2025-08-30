package com.example.bankcards.entity;

import com.example.bankcards.enums.CardStatus;
import com.example.bankcards.util.CardEncryptor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 512)
    private String encryptedNumber;

    @Column(nullable = false)
    private String cardHolderName;

    @Column(nullable = false)
    private Integer expirationMonth;

    @Column(nullable = false)
    private Integer expirationYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

    @Column(precision = 18, scale = 2, nullable = false)
    private BigDecimal balance;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Transient
    private static CardEncryptor encryptor;

    public static void setEncryptor(CardEncryptor enc) {
        encryptor = enc;
    }

    public void setCardNumber(String cardNumber) throws Exception {
        this.encryptedNumber = encryptor.encrypt(cardNumber);
    }

    public String getCardNumber() throws Exception {
        return encryptor.decrypt(encryptedNumber);
    }

    public String getMaskedNumber() throws Exception {
        String number = getCardNumber();
        if (number.length() >= 4) {
            return "**** **** **** " + number.substring(number.length() - 4);
        }
        return number;
    }

    public static class CardBuilder {
        private String encryptedNumber;

        public CardBuilder cardNumber(String number) throws Exception {
            if (encryptor == null) throw new IllegalStateException("Encryptor not initialized");
            this.encryptedNumber = encryptor.encrypt(number);
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card card)) return false;
        return id != null && id.equals(card.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
}