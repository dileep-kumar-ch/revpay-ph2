package com.revpay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMoneyRequestDto {
    private String payerUsername; // Email or Username
    private BigDecimal amount;
    private String note;

    public String getPayerUsername() {
        return payerUsername;
    }

    public void setPayerUsername(String payerUsername) {
        this.payerUsername = payerUsername;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
