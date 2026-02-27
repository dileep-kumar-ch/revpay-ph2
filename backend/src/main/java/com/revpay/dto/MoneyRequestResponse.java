package com.revpay.dto;

import com.revpay.model.RequestStatus;
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
public class MoneyRequestResponse {
    private Long id;
    private UserSummary requester;
    private UserSummary payer;
    private BigDecimal amount;
    private String note;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private String direction;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private String username;
        private String fullName;
        private String email;
    }
}
