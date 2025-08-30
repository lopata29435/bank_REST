package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UserFilterParamsRequest {

    @Parameter(description = "Page number", example = "0")
    @Min(0)
    private int page = 0;

    @Parameter(description = "Page size", example = "20")
    @Min(1)
    @Max(100)
    private int size = 20;

    @Parameter(description = "Sort by field", example = "username")
    private String sortBy = "id";

    @Parameter(description = "Sort direction", example = "asc")
    private String sortDirection = "asc";
}