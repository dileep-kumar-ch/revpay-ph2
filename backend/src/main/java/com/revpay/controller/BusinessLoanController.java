package com.revpay.controller;

import com.revpay.dto.LoanApplicationRequest;
import com.revpay.dto.LoanDecisionRequest;
import com.revpay.dto.LoanResponse;
import com.revpay.service.BusinessLoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/business/loans")
@RequiredArgsConstructor
public class BusinessLoanController {
    private final BusinessLoanService businessLoanService;

    @PostMapping
    public ResponseEntity<LoanResponse> apply(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.ok(businessLoanService.applyForLoan(userDetails.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<List<LoanResponse>> myLoans(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(businessLoanService.getMyLoans(userDetails.getUsername()));
    }

    @PostMapping("/{loanId}/repayments/{repaymentId}/pay")
    public ResponseEntity<LoanResponse> repayInstallment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long loanId,
            @PathVariable Long repaymentId) {
        return ResponseEntity.ok(businessLoanService.repayInstallment(userDetails.getUsername(), loanId, repaymentId));
    }

    @PostMapping("/admin/{loanId}/review")
    public ResponseEntity<LoanResponse> reviewLoan(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long loanId,
            @Valid @RequestBody LoanDecisionRequest request) {
        return ResponseEntity.ok(
                businessLoanService.reviewLoan(userDetails.getUsername(), loanId, request.getDecision(), request.getNote()));
    }
}
