package com.revpay.service;

import com.revpay.dto.NotificationEvent;

public interface NotificationEventPublisher {
    void publish(NotificationEvent event);
}
