package com.revpay.dto;

import com.revpay.model.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerId;
    private BigDecimal amount;
    private String currency;
    private LocalDate dueDate;
    private String description;
    private String paymentTerms;
    private InvoiceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private List<InvoiceItemResponse> items;
}
