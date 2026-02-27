package com.revpay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BusinessVerificationDecisionRequest {
    @NotBlank
    private String decision;
    private String reason;
}
