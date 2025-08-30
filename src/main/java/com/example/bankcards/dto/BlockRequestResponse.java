package com.example.bankcards.dto;

import com.example.bankcards.enums.BlockRequestStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class BlockRequestResponse {
    private Long id;
    private Long cardId;
    private String cardMaskedNumber;
    private String reason;
    private BlockRequestStatus status;
    private Instant createdAt;
    private Instant processedAt;
    private String processedByAdmin;
    private String adminComment;
}
