package com.revpay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoanDecisionRequest {
    @NotBlank
    private String decision;
    private String note;
}
