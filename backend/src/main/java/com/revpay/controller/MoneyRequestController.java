package com.revpay.controller;

import com.revpay.dto.CreateMoneyRequestDto;
import com.revpay.dto.MoneyRequestResponse;
import com.revpay.model.MoneyRequest;
import com.revpay.service.MoneyRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class MoneyRequestController {

    private final MoneyRequestService moneyRequestService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateMoneyRequestDto dto) {
        MoneyRequest request = moneyRequestService.createRequest(userDetails.getUsername(), dto);
        return ResponseEntity.ok(Map.of(
                "message", "Money request sent successfully",
                "requestId", request.getId()));
    }

    @GetMapping
    public ResponseEntity<List<MoneyRequestResponse>> getMyRequests(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(moneyRequestService.getMyRequests(userDetails.getUsername()));
    }

    @PostMapping("/{requestId}/respond")
    public ResponseEntity<?> respondToRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @RequestBody Map<String, Boolean> body) {
        boolean accept = body.getOrDefault("accept", false);
        moneyRequestService.respondToRequest(userDetails.getUsername(), requestId, accept);
        return ResponseEntity.ok(Map.of("message", accept ? "Request accepted" : "Request declined"));
    }
}
