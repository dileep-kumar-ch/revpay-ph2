package com.revpay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revpay.dto.NotificationEvent;
import com.revpay.dto.NotificationPreferenceResponse;
import com.revpay.dto.NotificationResponse;
import com.revpay.dto.UpdateNotificationPreferenceRequest;
import com.revpay.model.*;
import com.revpay.repository.NotificationPreferenceRepository;
import com.revpay.repository.NotificationRepository;
import com.revpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private static final BigDecimal DEFAULT_LOW_BALANCE_THRESHOLD = new BigDecimal("100.00");
    private static final String LOW_BALANCE_TYPE = "LOW_BALANCE";

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(NotificationEvent event) {
        try {
            if (event == null || event.getRecipientUserId() == null || event.getCategory() == null) {
                return;
            }

            User recipient = userRepository.findById(event.getRecipientUserId()).orElse(null);
            if (recipient == null) {
                log.warn("Skipping notification for missing recipient userId={}", event.getRecipientUserId());
                return;
            }

            NotificationPreference preference = getOrCreatePreference(recipient);
            if (!isEnabledForCategory(preference, event.getCategory())) {
                return;
            }

            Map<String, Object> metadata = event.getMetadata() == null ? Collections.emptyMap() : event.getMetadata();
            Notification notification = Notification.builder()
                    .recipientUser(recipient)
                    .category(event.getCategory())
                    .type(defaultString(event.getType(), "GENERAL"))
                    .title(defaultString(event.getTitle(), "Notification"))
                    .message(defaultString(event.getMessage(), "You have a new notification."))
                    .amount(asBigDecimal(metadata.get("amount")))
                    .counterparty(asString(metadata.get("counterparty")))
                    .eventStatus(asString(metadata.get("status")))
                    .navigationTarget(asString(metadata.get("navigation")))
                    .eventTime(asDateTime(metadata.get("eventTime")))
                    .metadataJson(toJson(metadata))
                    .read(false)
                    .build();

            Notification saved = notificationRepository.save(notification);
            log.debug("Notification saved id={} recipient={} category={} type={}",
                    saved.getId(), recipient.getId(), saved.getCategory(), saved.getType());
        } catch (Exception ex) {
            log.error("Failed to persist notification event type={} recipient={} reason={}",
                    event != null ? event.getType() : null,
                    event != null ? event.getRecipientUserId() : null,
                    ex.getMessage(),
                    ex);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String principal, NotificationCategory category, boolean unreadOnly) {
        User user = resolveUser(principal);
        List<Notification> notifications;
        if (category != null && unreadOnly) {
            notifications = notificationRepository.findByRecipientUserAndCategoryAndReadFalseOrderByCreatedAtDesc(user, category);
        } else if (category != null) {
            notifications = notificationRepository.findByRecipientUserAndCategoryOrderByCreatedAtDesc(user, category);
        } else if (unreadOnly) {
            notifications = notificationRepository.findByRecipientUserAndReadFalseOrderByCreatedAtDesc(user);
        } else {
            notifications = notificationRepository.findByRecipientUserOrderByCreatedAtDesc(user);
        }
        return notifications.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String principal) {
        User user = resolveUser(principal);
        return notificationRepository.countByRecipientUserAndReadFalse(user);
    }

    @Transactional
    public void markAsRead(String principal, Long notificationId) {
        User user = resolveUser(principal);
        Notification notification = notificationRepository.findByIdAndRecipientUser(notificationId, user)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(String principal) {
        User user = resolveUser(principal);
        List<Notification> unread = notificationRepository.findByRecipientUserAndReadFalseOrderByCreatedAtDesc(user);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreference(String principal) {
        User user = resolveUser(principal);
        return toPreferenceResponse(getOrCreatePreference(user));
    }

    @Transactional
    public NotificationPreferenceResponse updatePreference(String principal, UpdateNotificationPreferenceRequest request) {
        User user = resolveUser(principal);
        NotificationPreference preference = getOrCreatePreference(user);
        if (request.getTransactionsEnabled() != null) {
            preference.setTransactionsEnabled(request.getTransactionsEnabled());
        }
        if (request.getRequestsEnabled() != null) {
            preference.setRequestsEnabled(request.getRequestsEnabled());
        }
        if (request.getAlertsEnabled() != null) {
            preference.setAlertsEnabled(request.getAlertsEnabled());
        }
        if (request.getLowBalanceThreshold() != null) {
            preference.setLowBalanceThreshold(request.getLowBalanceThreshold());
        }
        NotificationPreference saved = notificationPreferenceRepository.save(preference);
        return toPreferenceResponse(saved);
    }

    @Transactional
    public void createLowBalanceAlert(User user, BigDecimal currentBalance) {
        try {
            NotificationPreference preference = getOrCreatePreference(user);
            if (!preference.isAlertsEnabled()) {
                return;
            }
            if (currentBalance.compareTo(preference.getLowBalanceThreshold()) >= 0) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            Notification latestUnread = notificationRepository
                    .findTopByRecipientUserAndTypeAndReadFalseOrderByCreatedAtDesc(user, LOW_BALANCE_TYPE)
                    .orElse(null);
            if (latestUnread != null && Duration.between(latestUnread.getCreatedAt(), now).toMinutes() < 10) {
                return;
            }

            Notification alert = Notification.builder()
                    .recipientUser(user)
                    .category(NotificationCategory.ALERTS)
                    .type(LOW_BALANCE_TYPE)
                    .title("Low balance alert")
                    .message("Your wallet balance is below your configured threshold.")
                    .amount(currentBalance)
                    .eventStatus("LOW")
                    .eventTime(now)
                    .navigationTarget("/wallet")
                    .metadataJson(toJson(Map.of(
                            "currentBalance", currentBalance,
                            "threshold", preference.getLowBalanceThreshold(),
                            "status", "LOW")))
                    .read(false)
                    .build();
            notificationRepository.save(alert);
        } catch (Exception ex) {
            log.error("Failed to create low-balance alert user={} reason={}",
                    user != null ? user.getId() : null,
                    ex.getMessage(),
                    ex);
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .category(notification.getCategory())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .amount(notification.getAmount())
                .counterparty(notification.getCounterparty())
                .status(notification.getEventStatus())
                .navigationTarget(notification.getNavigationTarget())
                .eventTime(notification.getEventTime())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .transactionsEnabled(preference.isTransactionsEnabled())
                .requestsEnabled(preference.isRequestsEnabled())
                .alertsEnabled(preference.isAlertsEnabled())
                .lowBalanceThreshold(preference.getLowBalanceThreshold())
                .build();
    }

    private User resolveUser(String principal) {
        return userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private NotificationPreference getOrCreatePreference(User user) {
        return notificationPreferenceRepository.findByUser(user)
                .orElseGet(() -> notificationPreferenceRepository.save(NotificationPreference.builder()
                        .user(user)
                        .transactionsEnabled(true)
                        .requestsEnabled(true)
                        .alertsEnabled(true)
                        .lowBalanceThreshold(DEFAULT_LOW_BALANCE_THRESHOLD)
                        .build()));
    }

    private boolean isEnabledForCategory(NotificationPreference preference, NotificationCategory category) {
        return switch (category) {
            case TRANSACTIONS -> preference.isTransactionsEnabled();
            case REQUESTS -> preference.isRequestsEnabled();
            case ALERTS -> preference.isAlertsEnabled();
        };
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDateTime asDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }
}
