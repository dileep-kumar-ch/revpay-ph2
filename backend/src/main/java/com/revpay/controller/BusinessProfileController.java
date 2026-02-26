package com.revpay.controller;

import com.revpay.dto.BusinessProfileResponse;
import com.revpay.dto.BusinessVerificationDecisionRequest;
import com.revpay.dto.BusinessVerificationUpdateRequest;
import com.revpay.service.BusinessProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/business/profile")
@RequiredArgsConstructor
public class BusinessProfileController {
    private final BusinessProfileService businessProfileService;

    @GetMapping
    public ResponseEntity<BusinessProfileResponse> myProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(businessProfileService.getMyBusinessProfile(userDetails.getUsername()));
    }

    @PostMapping("/verification")
    public ResponseEntity<BusinessProfileResponse> submitVerification(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BusinessVerificationUpdateRequest request) {
        return ResponseEntity.ok(businessProfileService.submitOrUpdateVerification(userDetails.getUsername(), request));
    }

    @PostMapping("/admin/{businessUserId}/verification")
    public ResponseEntity<BusinessProfileResponse> reviewVerification(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long businessUserId,
            @Valid @RequestBody BusinessVerificationDecisionRequest request) {
        if ("APPROVE".equalsIgnoreCase(request.getDecision())) {
            return ResponseEntity.ok(businessProfileService.approveBusiness(userDetails.getUsername(), businessUserId));
        }
        if ("REJECT".equalsIgnoreCase(request.getDecision())) {
            return ResponseEntity.ok(
                    businessProfileService.rejectBusiness(userDetails.getUsername(), businessUserId, request.getReason()));
        }
        throw new RuntimeException("Decision must be APPROVE or REJECT");
    }
}
