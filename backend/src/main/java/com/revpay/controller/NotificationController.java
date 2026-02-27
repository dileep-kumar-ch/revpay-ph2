package com.revpay.controller;

import com.revpay.dto.NotificationPreferenceResponse;
import com.revpay.dto.NotificationResponse;
import com.revpay.dto.UpdateNotificationPreferenceRequest;
import com.revpay.model.NotificationCategory;
import com.revpay.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) NotificationCategory category,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return ResponseEntity.ok(notificationService.getNotifications(userDetails.getUsername(), category, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        long unreadCount = notificationService.getUnreadCount(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long notificationId) {
        notificationService.markAsRead(userDetails.getUsername(), notificationId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @GetMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> getPreference(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getPreference(userDetails.getUsername()));
    }

    @PutMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> updatePreference(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateNotificationPreferenceRequest request) {
        return ResponseEntity.ok(notificationService.updatePreference(userDetails.getUsername(), request));
    }
}

