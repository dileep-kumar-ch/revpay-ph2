package com.revpay.controller;

import com.revpay.dto.*;
import com.revpay.service.BusinessPaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/business/payment-methods")
@RequiredArgsConstructor
public class BusinessPaymentMethodController {
    private final BusinessPaymentMethodService businessPaymentMethodService;

    @PostMapping("/cards")
    public ResponseEntity<CardResponse> addCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddCardRequest request) {
        return ResponseEntity.ok(businessPaymentMethodService.addBusinessCard(userDetails.getUsername(), request));
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Map<String, String>> removeCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cardId) {
        businessPaymentMethodService.removeBusinessCard(userDetails.getUsername(), cardId);
        return ResponseEntity.ok(Map.of("message", "Business card removed"));
    }

    @PostMapping("/bank-accounts")
    public ResponseEntity<BankAccountResponse> addBankAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BankAccountRequest request) {
        return ResponseEntity.ok(businessPaymentMethodService.addBankAccount(userDetails.getUsername(), request));
    }

    @GetMapping("/bank-accounts")
    public ResponseEntity<List<BankAccountResponse>> listBankAccounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(businessPaymentMethodService.listBankAccounts(userDetails.getUsername()));
    }

    @DeleteMapping("/bank-accounts/{bankAccountId}")
    public ResponseEntity<Map<String, String>> removeBankAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long bankAccountId) {
        businessPaymentMethodService.removeBankAccount(userDetails.getUsername(), bankAccountId);
        return ResponseEntity.ok(Map.of("message", "Business bank account removed"));
    }
}
