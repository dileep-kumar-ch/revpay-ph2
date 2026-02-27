package com.revpay.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "business_bank_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessBankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_user_id", nullable = false)
    private User businessUser;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false)
    private String encryptedAccountNumber;

    @Column(nullable = false)
    private String accountLastFour;

    @Column(nullable = false)
    private String encryptedRoutingNumber;

    @Column(nullable = false)
    private String accountType;

    private boolean isDefault;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
