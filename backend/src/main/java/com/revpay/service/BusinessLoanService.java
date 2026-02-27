package com.revpay.service;

import com.revpay.dto.*;
import com.revpay.model.*;
import com.revpay.repository.BusinessLoanRepository;
import com.revpay.repository.LoanRepaymentRepository;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import com.revpay.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessLoanService {
    private final UserRepository userRepository;
    private final BusinessLoanRepository businessLoanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Transactional
    public LoanResponse applyForLoan(String principal, LoanApplicationRequest request) {
        User businessUser = getBusinessUser(principal);
        if (request.getLoanAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Loan amount should be positive");
        }

        BusinessLoan loan = BusinessLoan.builder()
                .businessUser(businessUser)
                .loanAmount(request.getLoanAmount())
                .purpose(request.getPurpose())
                .financialDetails(request.getFinancialDetails())
                .supportingDocumentsPath(request.getSupportingDocumentsPath())
                .status(LoanStatus.SUBMITTED)
                .termMonths(request.getTermMonths())
                .remainingBalance(request.getLoanAmount())
                .build();
        BusinessLoan saved = businessLoanRepository.save(loan);

        List<LoanRepayment> schedule = buildRepaymentSchedule(saved);
        loanRepaymentRepository.saveAll(schedule);
        saved.setRepayments(schedule);

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(saved.getBusinessUser().getId())
                .category(NotificationCategory.ALERTS)
                .type("LOAN_SUBMITTED")
                .title("Loan application submitted")
                .message("Your loan application has been submitted.")
                .metadata(Map.of("loanId", saved.getId(), "status", saved.getStatus().name()))
                .build());
        return mapToResponse(saved);
    }

    public List<LoanResponse> getMyLoans(String principal) {
        User businessUser = getBusinessUser(principal);
        updateOverdueRepayments();
        return businessLoanRepository.findByBusinessUserOrderByAppliedAtDesc(businessUser).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public LoanResponse reviewLoan(String principal, Long loanId, String decision, String note) {
        User admin = getUser(principal);
        if (admin.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admin can review loans");
        }
        BusinessLoan loan = businessLoanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        String normalized = decision == null ? "" : decision.trim().toUpperCase();
        LoanStatus status = switch (normalized) {
            case "APPROVE", "APPROVED" -> LoanStatus.APPROVED;
            case "REJECT", "REJECTED" -> LoanStatus.REJECTED;
            case "ADDITIONAL_DOCUMENTS_REQUIRED", "DOCS_REQUIRED" -> LoanStatus.ADDITIONAL_DOCUMENTS_REQUIRED;
            default -> throw new RuntimeException("Decision must be APPROVE, REJECT, or ADDITIONAL_DOCUMENTS_REQUIRED");
        };
        loan.setStatus(status);
        BusinessLoan saved = businessLoanRepository.save(loan);

        String message = switch (status) {
            case APPROVED -> "Your business loan has been approved.";
            case REJECTED -> "Your business loan has been rejected.";
            case ADDITIONAL_DOCUMENTS_REQUIRED -> "Additional documents are required for your loan.";
            default -> "Loan status updated.";
        };
        if (note != null && !note.isBlank()) {
            message = message + " " + note;
        }
        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(saved.getBusinessUser().getId())
                .category(NotificationCategory.ALERTS)
                .type("LOAN_STATUS_UPDATED")
                .title("Loan application update")
                .message(message)
                .metadata(Map.of("loanId", saved.getId(), "status", saved.getStatus().name()))
                .build());
        return mapToResponse(saved);
    }

    @Transactional
    public LoanResponse repayInstallment(String principal, Long loanId, Long repaymentId) {
        User businessUser = getBusinessUser(principal);
        BusinessLoan loan = businessLoanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        if (!loan.getBusinessUser().getId().equals(businessUser.getId())) {
            throw new RuntimeException("Not authorized for this loan");
        }
        LoanRepayment repayment = loanRepaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new RuntimeException("Repayment not found"));
        if (!repayment.getLoan().getId().equals(loanId)) {
            throw new RuntimeException("Repayment does not belong to this loan");
        }
        if (repayment.getStatus() == RepaymentStatus.PAID) {
            throw new RuntimeException("Repayment already completed");
        }

        Wallet wallet = walletRepository.findByUser(businessUser)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        if (wallet.getBalance().compareTo(repayment.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance for repayment");
        }

        wallet.setBalance(wallet.getBalance().subtract(repayment.getAmount()));
        walletRepository.save(wallet);

        transactionRepository.save(Transaction.builder()
                .senderWallet(wallet)
                .amount(repayment.getAmount())
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .description("Loan repayment installment #" + repayment.getInstallmentNumber())
                .timestamp(LocalDateTime.now())
                .build());

        repayment.setStatus(RepaymentStatus.PAID);
        repayment.setPaidAt(LocalDateTime.now());
        loanRepaymentRepository.save(repayment);

        loan.setRemainingBalance(loan.getRemainingBalance().subtract(repayment.getAmount()).max(BigDecimal.ZERO));
        businessLoanRepository.save(loan);

        notificationEventPublisher.publish(NotificationEvent.builder()
                .recipientUserId(loan.getBusinessUser().getId())
                .category(NotificationCategory.TRANSACTIONS)
                .type("LOAN_REPAYMENT_SUCCESS")
                .title("Repayment successful")
                .message("Installment #" + repayment.getInstallmentNumber() + " has been paid.")
                .metadata(Map.of("loanId", loan.getId(), "repaymentId", repayment.getId()))
                .build());
        return mapToResponse(loan);
    }

    @Transactional
    public void updateOverdueRepayments() {
        List<LoanRepayment> dueRepayments = loanRepaymentRepository.findByStatusAndDueDateLessThanEqual(
                RepaymentStatus.PENDING, LocalDate.now());
        for (LoanRepayment repayment : dueRepayments) {
            if (repayment.getDueDate().isBefore(LocalDate.now())) {
                repayment.setStatus(RepaymentStatus.OVERDUE);
                loanRepaymentRepository.save(repayment);
            }
            notificationEventPublisher.publish(NotificationEvent.builder()
                    .recipientUserId(repayment.getLoan().getBusinessUser().getId())
                    .category(NotificationCategory.ALERTS)
                    .type("LOAN_REPAYMENT_DUE")
                    .title("Loan repayment due")
                    .message("Installment #" + repayment.getInstallmentNumber() + " is due on " + repayment.getDueDate())
                    .metadata(Map.of("loanId", repayment.getLoan().getId(), "repaymentId", repayment.getId()))
                    .build());
        }
    }

    private List<LoanRepayment> buildRepaymentSchedule(BusinessLoan loan) {
        int term = loan.getTermMonths();
        BigDecimal installment = loan.getLoanAmount().divide(BigDecimal.valueOf(term), 2, RoundingMode.DOWN);
        BigDecimal assigned = BigDecimal.ZERO;
        List<LoanRepayment> schedule = new ArrayList<>();
        for (int i = 1; i <= term; i++) {
            BigDecimal amount = i == term ? loan.getLoanAmount().subtract(assigned) : installment;
            assigned = assigned.add(amount);
            schedule.add(LoanRepayment.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .amount(amount)
                    .dueDate(LocalDate.now().plusMonths(i))
                    .status(RepaymentStatus.PENDING)
                    .build());
        }
        return schedule;
    }

    private LoanResponse mapToResponse(BusinessLoan loan) {
        List<LoanRepaymentResponse> repayments = loanRepaymentRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId())
                .stream().map(r -> LoanRepaymentResponse.builder()
                        .id(r.getId())
                        .installmentNumber(r.getInstallmentNumber())
                        .amount(r.getAmount())
                        .dueDate(r.getDueDate())
                        .status(r.getStatus())
                        .paidAt(r.getPaidAt())
                        .build()).collect(Collectors.toList());
        return LoanResponse.builder()
                .id(loan.getId())
                .loanAmount(loan.getLoanAmount())
                .purpose(loan.getPurpose())
                .financialDetails(loan.getFinancialDetails())
                .supportingDocumentsPath(loan.getSupportingDocumentsPath())
                .status(loan.getStatus())
                .termMonths(loan.getTermMonths())
                .remainingBalance(loan.getRemainingBalance())
                .appliedAt(loan.getAppliedAt())
                .repayments(repayments)
                .build();
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
}
