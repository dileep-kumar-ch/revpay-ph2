package com.revpay.controller;

import com.revpay.dto.AddCardRequest;
import com.revpay.dto.CardResponse;
import com.revpay.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CardResponse> addCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AddCardRequest request) {
        return ResponseEntity.ok(cardService.addCard(userDetails.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<List<CardResponse>> getMyCards(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cardService.getMyCards(userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        cardService.deleteCard(userDetails.getUsername(), id);
        return ResponseEntity.ok(Map.of("message", "Card deleted successfully"));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<CardResponse> setDefaultCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(cardService.setDefaultCard(userDetails.getUsername(), id));
    }

}
