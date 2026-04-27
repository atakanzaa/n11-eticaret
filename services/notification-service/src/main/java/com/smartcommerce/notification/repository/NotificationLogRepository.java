package com.smartcommerce.notification.repository;

import com.smartcommerce.notification.domain.NotificationLog;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    Page<NotificationLog> findByUserId(UUID userId, Pageable pageable);
}
