package com.revpay.dto;

import com.revpay.model.LoanStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LoanResponse {
    private Long id;
    private BigDecimal loanAmount;
    private String purpose;
    private String financialDetails;
    private String supportingDocumentsPath;
    private LoanStatus status;
    private Integer termMonths;
    private BigDecimal remainingBalance;
    private LocalDateTime appliedAt;
    private List<LoanRepaymentResponse> repayments;
}
