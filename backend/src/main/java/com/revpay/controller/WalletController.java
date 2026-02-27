package com.revpay.controller;

import com.revpay.dto.WalletResponse;
import com.revpay.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<WalletResponse> getBalance(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(walletService.getBalance(userDetails.getUsername()));
    }

    @PostMapping("/add-funds")
    public ResponseEntity<WalletResponse> addFunds(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody com.revpay.dto.WalletOperationRequest request) {
        return ResponseEntity
                .ok(walletService.addFunds(userDetails.getUsername(), request.getAmount(), request.getCardId()));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdrawFunds(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody com.revpay.dto.WalletOperationRequest request) {
        return ResponseEntity.ok(walletService.withdrawFunds(userDetails.getUsername(), request.getAmount()));
    }
}
