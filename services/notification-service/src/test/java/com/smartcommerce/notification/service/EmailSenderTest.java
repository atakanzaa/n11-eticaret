package com.smartcommerce.notification.service;

import com.smartcommerce.notification.api.dto.EmailJob;
import com.smartcommerce.notification.domain.*;
import com.smartcommerce.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {
    @Mock NotificationLogRepository notificationLogRepository;
    @Mock JavaMailSender mailSender;
    @Captor ArgumentCaptor<NotificationLog> notificationLogCaptor;
    @Captor ArgumentCaptor<SimpleMailMessage> mailMessageCaptor;

    @Test
    @DisplayName("send dispatches email and stores a successful notification")
    void send_storesNotificationLog() {
        var sender = new EmailSender(notificationLogRepository, new EmailTemplateService(), mailSender);
        var userId = UUID.randomUUID();
        var job = new EmailJob(userId, "ORDER_CONFIRMED", "user@example.com", "Siparisiniz onaylandi",
            Map.of("orderNumber", "SC-1", "grandTotal", new BigDecimal("99.90"), "currency", "TRY"), "corr-1");

        sender.send(job);

        verify(mailSender).send(mailMessageCaptor.capture());
        assertThat(mailMessageCaptor.getValue().getTo()).containsExactly("user@example.com");
        assertThat(mailMessageCaptor.getValue().getSubject()).isEqualTo("Siparisiniz onaylandi");
        assertThat(mailMessageCaptor.getValue().getText()).contains("SC-1");
        verify(notificationLogRepository).save(notificationLogCaptor.capture());
        var log = notificationLogCaptor.getValue();
        assertThat(log.getUserId()).isEqualTo(userId);
        assertThat(log.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(log.getBody()).contains("SC-1");
        assertThat(log.getCorrelationId()).isEqualTo("corr-1");
    }

    @Test
    @DisplayName("send stores failed notification and rethrows when JavaMailSender fails")
    void send_storesFailureAndRethrows() {
        var sender = new EmailSender(notificationLogRepository, new EmailTemplateService(), mailSender);
        var job = new EmailJob(UUID.randomUUID(), "WELCOME", "user@example.com", "Welcome",
            Map.of("name", "Test User"), "corr-fail");
        doThrow(new MailSendException("smtp unavailable")).when(mailSender).send(Mockito.any(SimpleMailMessage.class));

        assertThatThrownBy(() -> sender.send(job))
            .isInstanceOf(MailSendException.class)
            .hasMessageContaining("smtp unavailable");

        verify(notificationLogRepository).save(notificationLogCaptor.capture());
        var log = notificationLogCaptor.getValue();
        assertThat(log.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(log.getErrorMessage()).contains("smtp unavailable");
        assertThat(log.getSentAt()).isNull();
    }

    @Test
    @DisplayName("dead letter handling stores a failed notification log")
    void deadLetter_storesFailedNotificationLog() {
        var sender = new EmailSender(notificationLogRepository, new EmailTemplateService(), mailSender);
        var job = new EmailJob(UUID.randomUUID(), "WELCOME", "user@example.com", "Welcome", Map.of(), "corr-2");

        sender.onDeadLetter(job);

        verify(notificationLogRepository).save(notificationLogCaptor.capture());
        assertThat(notificationLogCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notificationLogCaptor.getValue().getErrorMessage()).contains("DLQ");
    }
}
