package com.revpay.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BusinessAnalyticsResponse {
    private BigDecimal totalRevenue;
    private TransactionSummary transactionSummary;
    private OutstandingInvoiceSummary outstandingInvoices;
    private List<PaymentTrendPoint> paymentTrends;
    private List<TopCustomerPoint> topCustomers;

    @Data
    @Builder
    public static class TransactionSummary {
        private long totalTransactions;
        private BigDecimal totalReceived;
        private BigDecimal totalSent;
    }

    @Data
    @Builder
    public static class OutstandingInvoiceSummary {
        private long count;
        private BigDecimal totalAmount;
    }

    @Data
    @Builder
    public static class PaymentTrendPoint {
        private String date;
        private BigDecimal amount;
    }

    @Data
    @Builder
    public static class TopCustomerPoint {
        private String customer;
        private BigDecimal totalPaid;
    }
}
