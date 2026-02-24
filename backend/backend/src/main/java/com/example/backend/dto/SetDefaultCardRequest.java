package com.example.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class SetDefaultCardRequest {
    @NotNull(message = "Card ID is required")
    private Long cardId;
}
