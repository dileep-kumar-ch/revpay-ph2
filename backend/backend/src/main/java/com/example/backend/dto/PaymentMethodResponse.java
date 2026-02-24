package com.example.backend.dto;
import com.example.backend.model.CardType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentMethodResponse {
    private Long id;
    private String cardHolderName;
    private CardType cardType;
    private String lastFourDigits;
    private Integer expiryMonth;
    private Integer expiryYear;
    private Boolean isDefault;
    private Boolean isActive;
    private LocalDateTime createdAt;
}