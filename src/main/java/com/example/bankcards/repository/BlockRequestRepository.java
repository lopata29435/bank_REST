package com.example.bankcards.repository;

import com.example.bankcards.entity.BlockRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.enums.BlockRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockRequestRepository extends JpaRepository<BlockRequest, Long> {

    Page<BlockRequest> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<BlockRequest> findByStatusOrderByCreatedAtDesc(BlockRequestStatus status, Pageable pageable);

    Optional<BlockRequest> findByCardAndStatus(Card card, BlockRequestStatus status);

    boolean existsByCardAndStatus(Card card, BlockRequestStatus status);

    List<BlockRequest> findByUserAndStatus(User user, BlockRequestStatus status);

    @Query("SELECT COUNT(br) FROM BlockRequest br WHERE br.status = :status")
    long countByStatus(@Param("status") BlockRequestStatus status);
}
