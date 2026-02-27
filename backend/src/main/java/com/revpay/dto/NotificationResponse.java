package com.revpay.dto;

import com.revpay.model.NotificationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private NotificationCategory category;
    private String type;
    private String title;
    private String message;
    private BigDecimal amount;
    private String counterparty;
    private String status;
    private String navigationTarget;
    private LocalDateTime eventTime;
    private boolean read;
    private LocalDateTime createdAt;
}

