package com.revpay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {
    @NotNull
    private BigDecimal loanAmount;
    @NotBlank
    private String purpose;
    @NotBlank
    private String financialDetails;
    private String supportingDocumentsPath;
    @NotNull
    @Min(1)
    private Integer termMonths;
}
