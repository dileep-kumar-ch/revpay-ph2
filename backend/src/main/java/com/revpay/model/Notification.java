package com.revpay.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User recipientUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationCategory category;

    @Column(nullable = false, length = 80)
    private String type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String counterparty;

    @Column(name = "event_status", length = 40)
    private String eventStatus;

    @Column(name = "navigation_target", length = 255)
    private String navigationTarget;

    private LocalDateTime eventTime;

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
