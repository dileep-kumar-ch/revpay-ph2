package com.revpay.service;

import com.revpay.dto.BusinessAnalyticsResponse;
import com.revpay.model.*;
import com.revpay.repository.InvoiceRepository;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import com.revpay.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessAnalyticsService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;

    public BusinessAnalyticsResponse getDashboard(String principal, LocalDate from, LocalDate to) {
        User businessUser = getBusinessUser(principal);
        LocalDate fromDate = from == null ? LocalDate.now().minusMonths(1) : from;
        LocalDate toDate = to == null ? LocalDate.now() : to;
        if (toDate.isBefore(fromDate)) {
            throw new RuntimeException("Invalid date range");
        }

        List<Invoice> invoices = invoiceRepository.findByBusinessUserOrderByCreatedAtDesc(businessUser).stream()
                .filter(i -> isWithinRange(i.getCreatedAt() == null ? null : i.getCreatedAt().toLocalDate(), fromDate, toDate))
                .toList();

        BigDecimal totalRevenue = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long outstandingCount = invoices.stream().filter(i -> i.getStatus() == InvoiceStatus.SENT || i.getStatus() == InvoiceStatus.OVERDUE).count();
        BigDecimal outstandingAmount = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.SENT || i.getStatus() == InvoiceStatus.OVERDUE)
                .map(Invoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Wallet businessWallet = walletRepository.findByUser(businessUser)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        List<Transaction> transactions = transactionRepository.findAllByWallet(businessWallet).stream()
                .filter(t -> isWithinRange(t.getTimestamp() == null ? null : t.getTimestamp().toLocalDate(), fromDate, toDate))
                .toList();

        BigDecimal totalReceived = transactions.stream()
                .filter(t -> t.getReceiverWallet() != null && t.getReceiverWallet().getId().equals(businessWallet.getId()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSent = transactions.stream()
                .filter(t -> t.getSenderWallet() != null && t.getSenderWallet().getId().equals(businessWallet.getId()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BusinessAnalyticsResponse.PaymentTrendPoint> paymentTrends = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID && i.getPaidAt() != null)
                .collect(Collectors.groupingBy(i -> i.getPaidAt().toLocalDate().toString(),
                        Collectors.mapping(Invoice::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> BusinessAnalyticsResponse.PaymentTrendPoint.builder()
                        .date(e.getKey())
                        .amount(e.getValue())
                        .build())
                .toList();

        List<BusinessAnalyticsResponse.TopCustomerPoint> topCustomers = invoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .collect(Collectors.groupingBy(this::customerKey,
                        Collectors.mapping(Invoice::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<String, BigDecimal> e) -> e.getValue()).reversed())
                .limit(5)
                .map(e -> BusinessAnalyticsResponse.TopCustomerPoint.builder()
                        .customer(e.getKey())
                        .totalPaid(e.getValue())
                        .build())
                .toList();

        return BusinessAnalyticsResponse.builder()
                .totalRevenue(totalRevenue)
                .transactionSummary(BusinessAnalyticsResponse.TransactionSummary.builder()
                        .totalTransactions(transactions.size())
                        .totalReceived(totalReceived)
                        .totalSent(totalSent)
                        .build())
                .outstandingInvoices(BusinessAnalyticsResponse.OutstandingInvoiceSummary.builder()
                        .count(outstandingCount)
                        .totalAmount(outstandingAmount)
                        .build())
                .paymentTrends(paymentTrends)
                .topCustomers(topCustomers)
                .build();
    }

    private String customerKey(Invoice invoice) {
        if (invoice.getCustomerName() != null && !invoice.getCustomerName().isBlank()) {
            return invoice.getCustomerName();
        }
        if (invoice.getCustomerEmail() != null && !invoice.getCustomerEmail().isBlank()) {
            return invoice.getCustomerEmail();
        }
        if (invoice.getCustomerPhone() != null && !invoice.getCustomerPhone().isBlank()) {
            return invoice.getCustomerPhone();
        }
        if (invoice.getCustomerExternalId() != null && !invoice.getCustomerExternalId().isBlank()) {
            return invoice.getCustomerExternalId();
        }
        return "Unknown Customer";
    }

    private boolean isWithinRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) {
            return false;
        }
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private User getBusinessUser(String principal) {
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (user.getRole() != Role.BUSINESS) {
            throw new RuntimeException("Business role required");
        }
        return user;
    }
}
