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
public class NotificationPreferenceResponse {
    private boolean transactionsEnabled;
    private boolean requestsEnabled;
    private boolean alertsEnabled;
    private BigDecimal lowBalanceThreshold;
}

