package com.revpay.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateInvoiceRequest {
    @NotBlank
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerId;
    @NotBlank
    private String currency;
    @NotNull
    private LocalDate dueDate;
    private String description;
    private String paymentTerms;
    @NotEmpty
    @Valid
    private List<InvoiceItemRequest> items;
}
