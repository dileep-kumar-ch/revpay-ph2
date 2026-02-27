package com.revpay.repository;

import com.revpay.model.Notification;
import com.revpay.model.NotificationCategory;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientUserOrderByCreatedAtDesc(User user);

    List<Notification> findByRecipientUserAndCategoryOrderByCreatedAtDesc(User user, NotificationCategory category);

    List<Notification> findByRecipientUserAndReadFalseOrderByCreatedAtDesc(User user);

    List<Notification> findByRecipientUserAndCategoryAndReadFalseOrderByCreatedAtDesc(User user,
            NotificationCategory category);

    long countByRecipientUserAndReadFalse(User user);

    Optional<Notification> findByIdAndRecipientUser(Long id, User user);

    Optional<Notification> findTopByRecipientUserAndTypeAndReadFalseOrderByCreatedAtDesc(User user, String type);
}

