package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    List<Card> findByUser(User user);

    List<Card> findByUserAndStatus(User user, CardStatus status);

    Optional<Card> findByIdAndUserUsername(Long id, String username);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.user = :user AND c.status = :status")
    int countByUserAndStatus(@Param("user") User user, @Param("status") CardStatus status);

    @Query("SELECT c FROM Card c WHERE c.user.username = :username AND c.status = 'ACTIVE'")
    List<Card> findActiveCardsByUsername(@Param("username") String username);

    boolean existsByEncryptedNumber(String encryptedNumber);
}
