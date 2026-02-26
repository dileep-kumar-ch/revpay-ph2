package com.revpay.controller;

import com.revpay.dto.BusinessAnalyticsResponse;
import com.revpay.service.BusinessAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/business/analytics")
@RequiredArgsConstructor
public class BusinessAnalyticsController {
    private final BusinessAnalyticsService businessAnalyticsService;

    @GetMapping
    public ResponseEntity<BusinessAnalyticsResponse> dashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(businessAnalyticsService.getDashboard(userDetails.getUsername(), from, to));
    }
}
