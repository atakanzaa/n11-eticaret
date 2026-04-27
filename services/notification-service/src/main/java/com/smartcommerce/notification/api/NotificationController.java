package com.smartcommerce.notification.api;

import com.smartcommerce.notification.api.dto.NotificationLogResponse;
import com.smartcommerce.notification.domain.NotificationLog;
import com.smartcommerce.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationLogRepository notificationLogRepository;

    @GetMapping("/me")
    public Page<NotificationLogResponse> myNotifications(@AuthenticationPrincipal String userId,
                                                         @PageableDefault(size = 20, sort = "createdAt",
                                                             direction = Sort.Direction.DESC) Pageable pageable) {
        return notificationLogRepository.findByUserId(UUID.fromString(userId), pageable).map(this::toResponse);
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<NotificationLogResponse> logs(@PageableDefault(size = 50, sort = "createdAt",
                                                  direction = Sort.Direction.DESC) Pageable pageable) {
        return notificationLogRepository.findAll(pageable).map(this::toResponse);
    }

    private NotificationLogResponse toResponse(NotificationLog log) {
        return new NotificationLogResponse(log.getId(), log.getUserId(), log.getChannel(), log.getTemplateCode(),
            log.getRecipient(), log.getSubject(), log.getStatus(), log.getCorrelationId(), log.getErrorMessage(),
            log.getSentAt(), log.getCreatedAt());
    }
}
