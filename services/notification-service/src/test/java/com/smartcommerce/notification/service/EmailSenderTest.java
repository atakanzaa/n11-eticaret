package com.smartcommerce.notification.service;

import com.smartcommerce.notification.api.dto.EmailJob;
import com.smartcommerce.notification.domain.*;
import com.smartcommerce.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {
    @Mock NotificationLogRepository notificationLogRepository;
    @Captor ArgumentCaptor<NotificationLog> notificationLogCaptor;

    @Test
    @DisplayName("send renders and stores a successful email notification")
    void send_storesNotificationLog() {
        var sender = new EmailSender(notificationLogRepository, new EmailTemplateService());
        var userId = UUID.randomUUID();
        var job = new EmailJob(userId, "ORDER_CONFIRMED", "user@example.com", "Siparisiniz onaylandi",
            Map.of("orderNumber", "SC-1", "grandTotal", new BigDecimal("99.90"), "currency", "TRY"), "corr-1");

        sender.send(job);

        verify(notificationLogRepository).save(notificationLogCaptor.capture());
        var log = notificationLogCaptor.getValue();
        assertThat(log.getUserId()).isEqualTo(userId);
        assertThat(log.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(log.getBody()).contains("SC-1");
        assertThat(log.getCorrelationId()).isEqualTo("corr-1");
    }

    @Test
    @DisplayName("dead letter handling stores a failed notification log")
    void deadLetter_storesFailedNotificationLog() {
        var sender = new EmailSender(notificationLogRepository, new EmailTemplateService());
        var job = new EmailJob(UUID.randomUUID(), "WELCOME", "user@example.com", "Welcome", Map.of(), "corr-2");

        sender.onDeadLetter(job);

        verify(notificationLogRepository).save(notificationLogCaptor.capture());
        assertThat(notificationLogCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notificationLogCaptor.getValue().getErrorMessage()).contains("DLQ");
    }
}
