package com.revpay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BankAccountRequest {
    @NotBlank
    private String bankName;
    @NotBlank
    private String accountHolderName;
    @NotBlank
    private String accountNumber;
    @NotBlank
    private String routingNumber;
    @NotBlank
    private String accountType;
    private Boolean setAsDefault;
}
