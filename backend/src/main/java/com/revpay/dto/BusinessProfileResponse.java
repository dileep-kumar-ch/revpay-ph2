package com.revpay.dto;

import com.revpay.model.BusinessVerificationStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BusinessProfileResponse {
    private Long userId;
    private String businessName;
    private String businessType;
    private String taxId;
    private String businessAddress;
    private String verificationDocsPath;
    private BusinessVerificationStatus verificationStatus;
}
