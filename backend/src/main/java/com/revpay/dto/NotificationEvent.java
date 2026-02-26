package com.revpay.dto;

import com.revpay.model.NotificationCategory;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class NotificationEvent {
    private Long recipientUserId;
    private NotificationCategory category;
    private String type;
    private String title;
    private String message;
    private Map<String, Object> metadata;
}
