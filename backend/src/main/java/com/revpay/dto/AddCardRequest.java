package com.revpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCardRequest {
    private String cardHolderName;
    private String cardNumber;
    private String expiryDate;
    private String cvv;
    private String paymentMethodType;

    @JsonProperty("setAsDefault")
    private Boolean setAsDefault;
}
