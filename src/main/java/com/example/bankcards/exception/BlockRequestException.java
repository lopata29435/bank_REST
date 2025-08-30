package com.example.bankcards.exception;

public class BlockRequestException extends RuntimeException {
    public BlockRequestException(String message) {
        super(message);
    }

    public BlockRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public static BlockRequestException notFound(Long requestId) {
        return new BlockRequestException("Block request not found: " + requestId);
    }

    public static BlockRequestException alreadyProcessed(Long requestId) {
        return new BlockRequestException("Block request already processed: " + requestId);
    }

    public static BlockRequestException pendingExists(Long cardId) {
        return new BlockRequestException("Card already has pending block request: " + cardId);
    }
}
