package com.revpay.controller;

import com.revpay.dto.AuthRequest;
import com.revpay.dto.AuthResponse;
import com.revpay.dto.RegisterRequest;
import com.revpay.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @org.springframework.web.bind.annotation.GetMapping("/recovery/questions")
    public ResponseEntity<java.util.List<String>> getRecoveryQuestions(
            @org.springframework.web.bind.annotation.RequestParam String email) {
        return ResponseEntity.ok(authService.getRecoveryQuestions(email));
    }

    @PostMapping("/recovery/reset")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid java.util.Map<String, Object> body) {
        String email = (String) body.get("email");
        String newPassword = (String) body.get("newPassword");
        java.util.List<java.util.Map<String, String>> answersMap = (java.util.List<java.util.Map<String, String>>) body
                .get("answers");

        java.util.List<com.revpay.dto.SecurityQuestionDto> answers = answersMap.stream()
                .map(m -> new com.revpay.dto.SecurityQuestionDto(m.get("question"), m.get("answer")))
                .collect(java.util.stream.Collectors.toList());

        authService.resetPassword(email, answers, newPassword);
        return ResponseEntity.ok(java.util.Map.of("message", "Password reset successfully"));
    }
}
