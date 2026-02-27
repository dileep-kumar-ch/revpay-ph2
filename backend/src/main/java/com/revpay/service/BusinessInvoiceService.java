package com.revpay.service;

import com.revpay.dto.*;
import com.revpay.model.*;
import com.revpay.repository.InvoiceRepository;
import com.revpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessInvoiceService {
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransactionService transactionService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public InvoiceResponse createInvoice(String principal, CreateInvoiceRequest request) {
        User businessUser = getBusinessUser(principal);
        if (request.getDueDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Due date cannot be in the past");
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .businessUser(businessUser)
                .customerName(request.getCustomerName())
                .customerEmail(blankToNull(request.getCustomerEmail()))
                .customerPhone(blankToNull(request.getCustomerPhone()))
                .customerExternalId(blankToNull(request.getCustomerId()))
                .currency(request.getCurrency().toUpperCase())
                .dueDate(request.getDueDate())
                .description(request.getDescription())
                .paymentTerms(request.getPaymentTerms())
                .status(InvoiceStatus.SENT)
                .build();

        List<InvoiceItem> items = request.getItems().stream().map(item -> InvoiceItem.builder()
                .invoice(invoice)
                .itemName(item.getItemName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .build()).collect(Collectors.toList());
        invoice.setItems(items);
        invoice.setAmount(calculateInvoiceTotal(items));

        Invoice saved = invoiceRepository.save(invoice);
        sendInvoiceCreatedNotification(saved);
        return mapToInvoiceResponse(saved);
    }

    public List<InvoiceResponse> getMyInvoices(String principal, String status) {
        User businessUser = getBusinessUser(principal);
        markOverdueInvoices();
        List<Invoice> invoices;
        if (status == null || status.isBlank()) {
            invoices = invoiceRepository.findByBusinessUserOrderByCreatedAtDesc(businessUser);
        } else {
            InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status.trim().toUpperCase());
            invoices = invoiceRepository.findByBusinessUserAndStatusOrderByCreatedAtDesc(businessUser, invoiceStatus);
        }
        return invoices.stream().map(this::mapToInvoiceResponse).collect(Collectors.toList());
    }

    public List<InvoiceResponse> lookupInvoices(String lookupType, String lookupValue) {
        markOverdueInvoices();
        String type = normalizeLookupType(lookupType);
        if ("INVOICE_NUMBER".equals(type)) {
            return invoiceRepository.findByInvoiceNumber(lookupValue)
                    .map(this::mapToInvoiceResponse)
                    .map(List::of)
                    .orElse(List.of());
        }
        List<Invoice> invoices = switch (type) {
            case "PHONE" -> invoiceRepository.findByCustomerPhoneOrCustomerEmailOrCustomerExternalIdOrderByCreatedAtDesc(
                    lookupValue, "__NO_MATCH__", "__NO_MATCH__");
            case "EMAIL" -> invoiceRepository.findByCustomerPhoneOrCustomerEmailOrCustomerExternalIdOrderByCreatedAtDesc(
                    "__NO_MATCH__", lookupValue, "__NO_MATCH__");
            case "CUSTOMER_ID" -> invoiceRepository.findByCustomerPhoneOrCustomerEmailOrCustomerExternalIdOrderByCreatedAtDesc(
                    "__NO_MATCH__", "__NO_MATCH__", lookupValue);
            default -> throw new RuntimeException("Unsupported lookup type");
        };
        return invoices.stream().map(this::mapToInvoiceResponse).collect(Collectors.toList());
    }

    @Transactional
    public InvoiceResponse payInvoice(String payerPrincipal, InvoicePaymentRequest request) {
        markOverdueInvoices();
        User payer = getUser(payerPrincipal);
        String type = normalizeLookupType(request.getLookupType());
        Invoice invoice = findPayableInvoice(type, request.getLookupValue());

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new RuntimeException("Invoice is already paid");
        }

        SendMoneyRequest sendMoneyRequest = SendMoneyRequest.builder()
                .receiverUsername(invoice.getBusinessUser().getUsername())
                .amount(invoice.getAmount())
                .note("Invoice payment: " + invoice.getInvoiceNumber())
                .build();

        transactionService.sendMoney(payer.getUsername(), sendMoneyRequest);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        Invoice saved = invoiceRepository.save(invoice);

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(invoice.getBusinessUser().getId())
                .category(NotificationCategory.TRANSACTIONS)
                .type("INVOICE_PAID")
                .title("Invoice paid")
                .message("Invoice " + invoice.getInvoiceNumber() + " was paid.")
                .metadata(Map.of("invoiceNumber", invoice.getInvoiceNumber(), "amount", invoice.getAmount()))
                .build());

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(payer.getId())
                .category(NotificationCategory.TRANSACTIONS)
                .type("INVOICE_PAYMENT_CONFIRMED")
                .title("Payment successful")
                .message("Your payment for invoice " + invoice.getInvoiceNumber() + " is successful.")
                .metadata(Map.of("invoiceNumber", invoice.getInvoiceNumber(), "amount", invoice.getAmount()))
                .build());
        return mapToInvoiceResponse(saved);
    }

    @Transactional
    public void markOverdueInvoices() {
        List<Invoice> toMarkOverdue = invoiceRepository.findByStatusAndDueDateBefore(InvoiceStatus.SENT, LocalDate.now());
        for (Invoice invoice : toMarkOverdue) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
            notificationEventPublisher.publish(NotificationEvent.builder()
                    .recipientUserId(invoice.getBusinessUser().getId())
                    .category(NotificationCategory.ALERTS)
                    .type("INVOICE_OVERDUE")
                    .title("Invoice overdue")
                    .message("Invoice " + invoice.getInvoiceNumber() + " is overdue.")
                    .metadata(Map.of("invoiceNumber", invoice.getInvoiceNumber()))
                    .build());
        }
    }

    private void sendInvoiceCreatedNotification(Invoice invoice) {
        Optional<User> customerUser = findCustomerUser(invoice);
        if (customerUser.isPresent()) {
            User user = customerUser.get();
            notificationEventPublisher.publish(NotificationEvent.builder()
                    .recipientUserId(user.getId())
                    .category(NotificationCategory.REQUESTS)
                    .type("INVOICE_CREATED")
                    .title("New invoice request")
                    .message("You received invoice " + invoice.getInvoiceNumber() + " for " + invoice.getAmount() + " "
                            + invoice.getCurrency())
                    .metadata(Map.of(
                            "invoiceNumber", invoice.getInvoiceNumber(),
                            "amount", invoice.getAmount(),
                            "status", InvoiceStatus.SENT.name(),
                            "dueDate", invoice.getDueDate().toString(),
                            "navigation", "/notifications",
                            "eventTime", LocalDateTime.now().toString()))
                    .build());
        } else {
            log.warn("Invoice created but no customer user matched. invoiceNumber={} email={} phone={} customerId={}",
                    invoice.getInvoiceNumber(), invoice.getCustomerEmail(), invoice.getCustomerPhone(),
                    invoice.getCustomerExternalId());
        }

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(invoice.getBusinessUser().getId())
                .category(NotificationCategory.REQUESTS)
                .type("INVOICE_SENT")
                .title("Invoice sent")
                .message("Invoice " + invoice.getInvoiceNumber() + " was created and sent.")
                .metadata(Map.of(
                        "invoiceNumber", invoice.getInvoiceNumber(),
                        "amount", invoice.getAmount(),
                        "status", InvoiceStatus.SENT.name(),
                        "navigation", "/business",
                        "eventTime", LocalDateTime.now().toString()))
                .build());

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(invoice.getBusinessUser().getId())
                .category(NotificationCategory.ALERTS)
                .type("INVOICE_CREATED_ALERT")
                .title("Invoice created")
                .message("Invoice " + invoice.getInvoiceNumber() + " was created successfully.")
                .metadata(Map.of(
                        "invoiceNumber", invoice.getInvoiceNumber(),
                        "amount", invoice.getAmount(),
                        "status", InvoiceStatus.SENT.name(),
                        "navigation", "/business",
                        "eventTime", LocalDateTime.now().toString()))
                .build());
    }

    private Optional<User> findCustomerUser(Invoice invoice) {
        String customerEmail = normalize(invoice.getCustomerEmail());
        if (customerEmail != null) {
            Optional<User> byEmail = userRepository.findByEmailIgnoreCase(customerEmail);
            if (byEmail.isPresent()) {
                return byEmail;
            }
        }
        String customerExternalId = normalize(invoice.getCustomerExternalId());
        if (customerExternalId != null) {
            Optional<User> byExternalId = userRepository.findByUsernameIgnoreCase(customerExternalId)
                    .or(() -> userRepository.findByEmailIgnoreCase(customerExternalId));
            if (byExternalId.isPresent()) {
                return byExternalId;
            }
        }

        String customerPhone = digitsOnly(invoice.getCustomerPhone());
        if (customerPhone != null) {
            return userRepository.findAll().stream()
                    .filter(u -> customerPhone.equals(digitsOnly(u.getPhoneNumber())))
                    .findFirst();
        }
        return Optional.empty();
    }

    private Invoice findPayableInvoice(String lookupType, String lookupValue) {
        if ("INVOICE_NUMBER".equals(lookupType)) {
            return invoiceRepository.findByInvoiceNumber(lookupValue)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
        }
        List<Invoice> matches = lookupInvoices(lookupType, lookupValue).stream()
                .map(this::toEntity)
                .filter(i -> i.getStatus() != InvoiceStatus.PAID)
                .sorted(Comparator.comparing(Invoice::getCreatedAt).reversed())
                .toList();
        if (matches.isEmpty()) {
            throw new RuntimeException("No payable invoice found for lookup");
        }
        return matches.get(0);
    }

    private Invoice toEntity(InvoiceResponse response) {
        return invoiceRepository.findById(response.getId())
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    private String normalizeLookupType(String lookupType) {
        if (lookupType == null || lookupType.isBlank()) {
            throw new RuntimeException("lookupType is required");
        }
        return lookupType.trim().toUpperCase();
    }

    private BigDecimal calculateInvoiceTotal(List<InvoiceItem> items) {
        return items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private InvoiceResponse mapToInvoiceResponse(Invoice invoice) {
        List<InvoiceItemResponse> itemResponses = invoice.getItems() == null ? List.of() :
                invoice.getItems().stream().map(item -> InvoiceItemResponse.builder()
                        .id(item.getId())
                        .itemName(item.getItemName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build()).collect(Collectors.toList());
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .customerName(invoice.getCustomerName())
                .customerEmail(invoice.getCustomerEmail())
                .customerPhone(invoice.getCustomerPhone())
                .customerId(invoice.getCustomerExternalId())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .dueDate(invoice.getDueDate())
                .description(invoice.getDescription())
                .paymentTerms(invoice.getPaymentTerms())
                .status(invoice.getStatus())
                .createdAt(invoice.getCreatedAt())
                .paidAt(invoice.getPaidAt())
                .items(itemResponses)
                .build();
    }

    private String generateInvoiceNumber() {
        return "INV-" + java.time.LocalDate.now().toString().replace("-", "") + "-"
                + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private User getBusinessUser(String principal) {
        User user = getUser(principal);
        if (user.getRole() != Role.BUSINESS) {
            throw new RuntimeException("Business role required");
        }
        return user;
    }

    private User getUser(String principal) {
        return userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String digitsOnly(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }
}
