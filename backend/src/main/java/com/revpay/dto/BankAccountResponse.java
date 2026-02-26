package com.revpay.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankAccountResponse {
    private Long id;
    private String bankName;
    private String accountHolderName;
    private String accountLastFour;
    private String accountType;
    private boolean defaultAccount;
}
