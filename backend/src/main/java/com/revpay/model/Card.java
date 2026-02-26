package com.revpay.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String cardHolderName;

    @Column(nullable = false)
    private String encryptedCardNumber;

    @Column(nullable = false)
    private String lastFourDigits;

    @Column(nullable = false)
    private String expiryDate; // MM/YY

    @Column(nullable = false)
    private String encryptedCvv;

    @Enumerated(EnumType.STRING)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethodType paymentMethodType;

    private boolean isDefault;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum CardType {
        VISA, MASTERCARD, AMEX, DISCOVER, OTHER
    }

    public enum PaymentMethodType {
        DEBIT, CREDIT
    }
}
