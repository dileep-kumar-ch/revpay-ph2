package com.revpay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InvoicePaymentRequest {
    @NotBlank
    private String lookupType;
    @NotBlank
    private String lookupValue;
}
