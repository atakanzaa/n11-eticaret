package com.smartcommerce.notification.service;

import com.smartcommerce.notification.api.dto.EmailJob;
import com.smartcommerce.notification.config.RabbitMQConfig;
import com.smartcommerce.notification.domain.*;
import com.smartcommerce.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSender {
    private final NotificationLogRepository notificationLogRepository;
    private final EmailTemplateService emailTemplateService;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    @Transactional
    public void send(EmailJob job) {
        var body = emailTemplateService.renderBody(job);
        var logEntry = NotificationLog.builder()
            .userId(job.userId())
            .channel(NotificationChannel.EMAIL)
            .templateCode(job.templateCode())
            .recipient(job.recipient())
            .subject(job.subject())
            .body(body)
            .status(NotificationStatus.SENT)
            .correlationId(job.correlationId())
            .sentAt(Instant.now())
            .build();
        notificationLogRepository.save(logEntry);
        log.info("Email notification sent to {} template={} correlationId={}",
            job.recipient(), job.templateCode(), job.correlationId());
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
}
