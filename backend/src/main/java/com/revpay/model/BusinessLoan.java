package com.revpay.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "business_loans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessLoan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_user_id", nullable = false)
    private User businessUser;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal loanAmount;

    @Column(length = 2000, nullable = false)
    private String purpose;

    @Column(length = 4000, nullable = false)
    private String financialDetails;

    @Column(length = 2000)
    private String supportingDocumentsPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(nullable = false)
    private Integer termMonths;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingBalance;

    private LocalDateTime appliedAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoanRepayment> repayments;

    @PrePersist
    public void onCreate() {
        if (appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }
    }
}
