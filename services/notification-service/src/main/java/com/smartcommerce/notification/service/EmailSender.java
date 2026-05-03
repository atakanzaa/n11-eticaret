package com.smartcommerce.notification.service;

import com.smartcommerce.notification.api.dto.EmailJob;
import com.smartcommerce.notification.config.RabbitMQConfig;
import com.smartcommerce.notification.domain.*;
import com.smartcommerce.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSender {
    private final NotificationLogRepository notificationLogRepository;
    private final EmailTemplateService emailTemplateService;
    private final JavaMailSender mailSender;

    @Value("${notification.mail.from:noreply@smartcommerce.local}")
    private String fromAddress;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    @Transactional
    public void send(EmailJob job) {
        var body = emailTemplateService.renderBody(job);
        try {
            mailSender.send(toMessage(job, body));
            notificationLogRepository.save(logEntry(job, body, NotificationStatus.SENT, null));
            log.info("Email notification sent to {} template={} correlationId={}",
                job.recipient(), job.templateCode(), job.correlationId());
        } catch (MailException e) {
            notificationLogRepository.save(logEntry(job, body, NotificationStatus.FAILED, e.getMessage()));
            log.error("Email notification failed for {} template={} correlationId={}",
                job.recipient(), job.templateCode(), job.correlationId(), e);
            throw e;
        }
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_DLQ)
    @Transactional
    public void onDeadLetter(EmailJob job) {
        notificationLogRepository.save(NotificationLog.builder()
            .userId(job.userId())
            .channel(NotificationChannel.EMAIL)
            .templateCode(job.templateCode())
            .recipient(job.recipient())
            .subject(job.subject())
            .status(NotificationStatus.FAILED)
            .correlationId(job.correlationId())
            .errorMessage("Message moved to email DLQ")
            .build());
        log.error("Email job moved to DLQ for recipient={} template={}", job.recipient(), job.templateCode());
    }

    private SimpleMailMessage toMessage(EmailJob job, String body) {
        var message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(job.recipient());
        message.setSubject(job.subject());
        message.setText(body);
        return message;
    }

    private NotificationLog logEntry(EmailJob job, String body, NotificationStatus status, String errorMessage) {
        return NotificationLog.builder()
            .userId(job.userId())
            .channel(NotificationChannel.EMAIL)
            .templateCode(job.templateCode())
            .recipient(job.recipient())
            .subject(job.subject())
            .body(body)
            .status(status)
            .correlationId(job.correlationId())
            .errorMessage(errorMessage)
            .sentAt(status == NotificationStatus.SENT ? Instant.now() : null)
            .build();
    }
}
