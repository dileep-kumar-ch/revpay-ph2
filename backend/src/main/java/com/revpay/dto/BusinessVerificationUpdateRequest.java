package com.revpay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BusinessVerificationUpdateRequest {
    @NotBlank
    private String businessName;
    @NotBlank
    private String businessType;
    @NotBlank
    private String taxId;
    @NotBlank
    private String businessAddress;
    @NotBlank
    private String verificationDocsPath;
}
