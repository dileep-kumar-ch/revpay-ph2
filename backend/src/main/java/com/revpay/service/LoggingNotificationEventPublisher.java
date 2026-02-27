package com.revpay.service;

import com.revpay.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.UnexpectedRollbackException;

@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingNotificationEventPublisher implements NotificationEventPublisher {
    private final NotificationService notificationService;

    @Override
    public void publish(NotificationEvent event) {
        try {
            notificationService.publish(event);
            log.info("notification_event recipient={} category={} type={} title={} metadata={}",
                    event.getRecipientUserId(),
                    event.getCategory(),
                    event.getType(),
                    event.getTitle(),
                    event.getMetadata());
        } catch (UnexpectedRollbackException ex) {
            log.error("notification_event_rollback recipient={} category={} type={} reason={}",
                    event != null ? event.getRecipientUserId() : null,
                    event != null ? event.getCategory() : null,
                    event != null ? event.getType() : null,
                    ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("notification_event_failed recipient={} category={} type={} reason={}",
                    event != null ? event.getRecipientUserId() : null,
                    event != null ? event.getCategory() : null,
                    event != null ? event.getType() : null,
                    ex.getMessage());
        }
    }
}
