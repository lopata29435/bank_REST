package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlockRequestProcessRequest {
    @NotBlank(message = "Decision is required")
    private String decision;

    @Size(max = 500, message = "Admin comment must not exceed 500 characters")
    private String adminComment;
}
