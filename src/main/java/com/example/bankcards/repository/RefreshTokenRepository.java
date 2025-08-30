package com.example.bankcards.repository;

import com.example.bankcards.entity.RefreshToken;
import com.example.bankcards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.user WHERE rt.tokenHash = :tokenHash AND rt.revoked = false")
    Optional<RefreshToken> findByTokenHashAndRevokedFalseWithUser(@Param("tokenHash") String tokenHash);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.tokenHash = :tokenHash")
    void revokeByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user AND r.revoked = false")
    void revokeAllByUser(@Param("user") User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.revoked = true AND r.expiresAt < :expiryTime")
    void deleteExpiredAndRevoked(@Param("expiryTime") Instant expiryTime);

    @Query("SELECT COUNT(r) FROM RefreshToken r WHERE r.revoked = true AND r.expiresAt < :expiryTime")
    int countExpiredAndRevoked(@Param("expiryTime") Instant expiryTime);

    @Query("SELECT COUNT(r) FROM RefreshToken r WHERE r.user = :user AND r.revoked = false AND r.expiresAt > :now")
    int countActiveByUser(@Param("user") User user, @Param("now") Instant now);

    @Query("SELECT r FROM RefreshToken r WHERE r.user = :user AND r.revoked = false AND r.expiresAt > :now ORDER BY r.createdAt DESC")
    List<RefreshToken> findActiveByUser(@Param("user") User user, @Param("now") Instant now);
}
