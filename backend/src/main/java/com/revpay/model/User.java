package com.revpay.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String username;

    @Email
    @NotBlank
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    private String fullName;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled;

    // Security Enhancements
    private String transactionPin;
    private int failedLoginAttempts;
    private java.time.LocalDateTime lockoutUntil;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<SecurityQuestion> securityQuestions;

    private String twoFactorCode;
    private java.time.LocalDateTime twoFactorExpiry;

    // Business specific fields
    private String businessName;
    private String businessType;
    private String taxId;
    private String businessAddress;
    private String verificationDocsPath;
    @Enumerated(EnumType.STRING)
    private BusinessVerificationStatus businessVerificationStatus;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
