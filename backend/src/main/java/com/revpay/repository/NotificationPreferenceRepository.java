package com.revpay.repository;

import com.revpay.model.NotificationPreference;
import com.revpay.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUser(User user);
}

