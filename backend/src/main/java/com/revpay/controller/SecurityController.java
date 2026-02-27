package com.revpay.controller;

import com.revpay.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
public class SecurityController {

    private final AuthService authService;

    @PostMapping("/pin/setup")
    public ResponseEntity<?> setupPin(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        if (pin == null || !pin.matches("\\d{4}")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid PIN format. PIN must be exactly 4 digits."));
        }
        authService.setTransactionPin(userDetails.getUsername(), pin);
        return ResponseEntity.ok(Map.of("message", "Transaction PIN set successfully"));
    }

    @PostMapping("/pin/verify")
    public ResponseEntity<?> verifyPin(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        if (pin == null || !pin.matches("\\d{4}")) {
            return ResponseEntity.badRequest().body(Map.of("valid", false, "message", "PIN must be exactly 4 digits."));
        }
        boolean isValid = authService.verifyTransactionPin(userDetails.getUsername(), pin);
        if (isValid) {
            return ResponseEntity.ok(Map.of("valid", true));
        } else {
            return ResponseEntity.status(401).body(Map.of("valid", false, "message", "Incorrect PIN"));
        }
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<?> verify2fa(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {
        // Simulated 2FA for now
        String code = body.get("code");
        if ("123456".equals(code)) {
            return ResponseEntity.ok(Map.of("verified", true));
        }
        return ResponseEntity.badRequest().body(Map.of("verified", false, "message", "Invalid 2FA code"));
    }

    @PostMapping("/pin/reset")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> resetPin(@RequestBody Map<String, Object> body) {
        String email = (String) body.get("email");
        String newPin = (String) body.get("newPin");
        List<Map<String, String>> answers = (List<Map<String, String>>) body.get("answers");
        if (newPin == null || !newPin.matches("\\d{4}")) {
            return ResponseEntity.badRequest().body(Map.of("message", "PIN must be exactly 4 digits."));
        }

        boolean success = authService.resetTransactionPinWithQuestions(email, answers, newPin);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "PIN reset successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to reset PIN. Incorrect answers."));
        }
    }
}
