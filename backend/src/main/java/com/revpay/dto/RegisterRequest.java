package com.revpay.dto;

import com.revpay.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank
    private String fullName;

    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;

    private String phoneNumber;

    private Role role;

    // Business specific
    private String businessName;
    private String businessType;
    private String taxId;
    private String businessAddress;
    private String verificationDocsPath;

    private java.util.List<SecurityQuestionDto> securityQuestions;
}
