package com.revpay.dto;

import com.revpay.model.RepaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanRepaymentResponse {
    private Long id;
    private Integer installmentNumber;
    private BigDecimal amount;
    private LocalDate dueDate;
    private RepaymentStatus status;
    private LocalDateTime paidAt;
}
