package com.revpay.controller;

import com.revpay.dto.SendMoneyRequest;
import com.revpay.dto.TransactionResponse;
import com.revpay.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/send")
    public ResponseEntity<TransactionResponse> sendMoney(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SendMoneyRequest request) {
        return ResponseEntity.ok(transactionService.sendMoney(userDetails.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getHistory(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(userDetails.getUsername()));
    }
}
