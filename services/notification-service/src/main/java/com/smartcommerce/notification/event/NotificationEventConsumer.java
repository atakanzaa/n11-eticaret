package com.smartcommerce.notification.event;

import com.fasterxml.jackson.databind.*;
import com.smartcommerce.common.events.*;
import com.smartcommerce.common.events.payload.UserRegisteredPayload;
import com.smartcommerce.notification.api.dto.EmailJob;
import com.smartcommerce.notification.client.UserClient;
import com.smartcommerce.notification.config.RabbitMQConfig;
import com.smartcommerce.notification.domain.ProcessedEvent;
import com.smartcommerce.notification.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {
    private final RabbitTemplate rabbitTemplate;
    private final UserClient userClient;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${notifications.admin.email:}")
    private String adminEmailRecipients;

    @KafkaListener(topics = Topics.USER_REGISTERED, groupId = "notification-service")
    @Transactional
    public void onUserRegistered(BaseEvent<?> event) {
        if (alreadyProcessed(event)) return;
        UserRegisteredPayload payload;
        try {
            payload = objectMapper.convertValue(event.getPayload(), UserRegisteredPayload.class);
            if (payload == null || payload.getUserId() == null || payload.getEmail() == null || payload.getEmail().isBlank()) {
                throw new IllegalArgumentException("Missing userId or email");
            }
        } catch (IllegalArgumentException e) {
            log.warn("Skipping malformed user registration event eventId={}: {}", event.getEventId(), e.getMessage());
            markProcessed(event);
            return;
        }
        enqueue(new EmailJob(payload.getUserId(), "WELCOME", payload.getEmail(), "SmartCommerce'a hos geldin",
            Map.of("name", displayName(payload.getFirstName(), payload.getLastName(), payload.getEmail())),
            correlationId(event)));
        markProcessed(event);
    }

    @KafkaListener(topics = {Topics.ORDER_CONFIRMED, Topics.ORDER_CANCELLED, Topics.ORDER_RECEIVED}, groupId = "notification-service")
    @Transactional
    public void onOrderEvent(BaseEvent<?> event) {
        if (alreadyProcessed(event)) return;
        JsonNode payload;
        UUID userId;
        try {
            payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
            var rawUserId = payload.path("userId").asText(null);
            if (rawUserId == null || rawUserId.isBlank()) {
                throw new IllegalArgumentException("Missing userId");
            }
            userId = UUID.fromString(rawUserId);
        } catch (IllegalArgumentException e) {
            log.warn("Skipping malformed order notification event eventId={}: {}", event.getEventId(), e.getMessage());
            markProcessed(event);
            return;
        }
        var user = userClient.getProfile(userId);
        var type = event.getEventType();
        if (EventType.ORDER_CONFIRMED.equals(type)) {
            enqueue(new EmailJob(userId, "ORDER_CONFIRMED", user.email(), "Siparisiniz onaylandi",
                Map.of("name", user.displayName(),
                    "orderNumber", payload.path("orderNumber").asText("-"),
                    "grandTotal", payload.path("grandTotal").asText("-"),
                    "currency", payload.path("currency").asText("TRY")),
                correlationId(event)));
        } else if (EventType.ORDER_CANCELLED.equals(type)) {
            enqueue(new EmailJob(userId, "ORDER_CANCELLED", user.email(), "Siparisiniz iptal edildi",
                Map.of("name", user.displayName(),
                    "orderNumber", payload.path("orderNumber").asText("-"),
                    "reason", payload.path("reason").asText("ORDER_CANCELLED")),
                correlationId(event)));
        } else if (EventType.ORDER_RECEIVED.equals(type)) {
            enqueue(new EmailJob(userId, "ORDER_RECEIVED", user.email(), "Siparişiniz teslim alındı",
                Map.of("name", user.displayName(),
                    "orderNumber", payload.path("orderNumber").asText("-")),
                correlationId(event)));
        } else {
            log.warn("Unsupported order eventType: {}", type);
        }
        markProcessed(event);
    }

    /**
     * Buyer-facing review events. REVIEW_APPROVED → "yorumun onaylandı" mail.
     * Other states (CREATED with status PENDING, REJECTED) are intentionally
     * dropped to avoid noise; admin moderation tools surface those.
     */
    @KafkaListener(topics = Topics.REVIEW_APPROVED, groupId = "notification-service")
    @Transactional
    public void onReviewApproved(BaseEvent<?> event) {
        if (alreadyProcessed(event)) return;
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var rawUserId = payload.path("userId").asText(null);
        if (rawUserId == null || rawUserId.isBlank()) {
            log.warn("REVIEW_APPROVED missing userId, skipping: {}", event.getEventId());
            markProcessed(event);
            return;
        }
        var userId = UUID.fromString(rawUserId);
        var user = userClient.getProfile(userId);
        enqueue(new EmailJob(userId, "REVIEW_APPROVED", user.email(), "Yorumunuz yayında",
            Map.of("name", user.displayName(),
                "rating", payload.path("rating").asText("-"),
                "title", payload.path("title").asText("")),
            correlationId(event)));
        markProcessed(event);
    }

    /**
     * Operations alerts: low-stock and fraud-flagged orders. We don't have a
     * standing admin email registry, so the recipient is read from
     * `notifications.admin.email` (env-configurable, can list multiple comma-
     * separated). When unset, the event is logged and dropped — no DLQ noise.
     */
    @KafkaListener(topics = {Topics.INVENTORY_LOW_STOCK, Topics.ORDER_FRAUD_FLAGGED}, groupId = "notification-service")
    @Transactional
    public void onAdminAlert(BaseEvent<?> event) {
        if (alreadyProcessed(event)) return;
        var adminEmail = adminEmailRecipients;
        if (adminEmail == null || adminEmail.isBlank()) {
            log.info("Admin alert dropped (no notifications.admin.email configured): type={}, eventId={}",
                event.getEventType(), event.getEventId());
            markProcessed(event);
            return;
        }
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        if (EventType.INVENTORY_LOW_STOCK.equals(event.getEventType())) {
            enqueue(new EmailJob(null, "ADMIN_ALERT", adminEmail, "[ALERT] Düşük stok",
                Map.of(
                    "title", "Düşük stok uyarısı",
                    "offerId", payload.path("offerId").asText("-"),
                    "available", payload.path("availableQuantity").asText("-"),
                    "threshold", payload.path("threshold").asText("-")
                ),
                correlationId(event)));
        } else { // ORDER_FRAUD_FLAGGED
            enqueue(new EmailJob(null, "ADMIN_ALERT", adminEmail, "[ALERT] Şüpheli sipariş",
                Map.of(
                    "title", "Fraud detection flagged an order",
                    "orderId", payload.path("orderId").asText("-"),
                    "reason", payload.path("reason").asText("-")
                ),
                correlationId(event)));
        }
        markProcessed(event);
    }

    private void enqueue(EmailJob job) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EMAIL_EXCHANGE, RabbitMQConfig.EMAIL_ROUTING_KEY, job);
        log.info("Queued email notification template={} recipient={}", job.templateCode(), job.recipient());
    }

    private boolean alreadyProcessed(BaseEvent<?> event) {
        return event.getEventId() != null && processedEventRepository.existsById(event.getEventId());
    }

    private void markProcessed(BaseEvent<?> event) {
        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), Instant.now()));
        }
    }

    private String correlationId(BaseEvent<?> event) {
        return event.getCorrelationId() != null ? event.getCorrelationId() : String.valueOf(event.getEventId());
    }

    private String displayName(String firstName, String lastName, String email) {
        var name = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        return name.isBlank() ? email : name;
    }
}
