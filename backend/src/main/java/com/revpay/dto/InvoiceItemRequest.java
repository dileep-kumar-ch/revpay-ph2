package com.revpay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceItemRequest {
    @NotBlank
    private String itemName;
    @NotNull
    @Min(1)
    private Integer quantity;
    @NotNull
    private BigDecimal unitPrice;
}
