package com.revpay.service;

import com.revpay.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingNotificationEventPublisher implements NotificationEventPublisher {
    @Override
    public void publish(NotificationEvent event) {
        log.info("notification_event recipient={} category={} type={} title={} metadata={}",
                event.getRecipientUserId(),
                event.getCategory(),
                event.getType(),
                event.getTitle(),
                event.getMetadata());
    }
}
