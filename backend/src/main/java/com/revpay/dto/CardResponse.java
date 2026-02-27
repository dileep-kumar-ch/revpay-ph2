package com.revpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {
    private Long id;
    private String cardHolderName;
    private String lastFourDigits;
    private String expiryDate;
    private String cardType;
    private String paymentMethodType;
    private boolean defaultCard;
}
