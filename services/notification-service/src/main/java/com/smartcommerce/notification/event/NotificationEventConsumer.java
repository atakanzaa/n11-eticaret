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

    @KafkaListener(topics = Topics.USER_REGISTERED, groupId = "notification-service")
    @Transactional
    public void onUserRegistered(BaseEvent<?> event) {
        if (alreadyProcessed(event)) return;
        var payload = objectMapper.convertValue(event.getPayload(), UserRegisteredPayload.class);
        enqueue(new EmailJob(payload.getUserId(), "WELCOME", payload.getEmail(), "SmartCommerce'a hos geldin",
            Map.of("name", displayName(payload.getFirstName(), payload.getLastName(), payload.getEmail())),
            correlationId(event)));
        markProcessed(event);
    }

    @KafkaListener(topics = {Topics.ORDER_CONFIRMED, Topics.ORDER_CANCELLED}, groupId = "notification-service")
    @Transactional
    public void onOrderEvent(BaseEvent<?> event) {
        if (alreadyProcessed(event)) return;
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var userId = UUID.fromString(payload.path("userId").asText());
        var user = userClient.getProfile(userId);
        if (EventType.ORDER_CONFIRMED.equals(event.getEventType())) {
            enqueue(new EmailJob(userId, "ORDER_CONFIRMED", user.email(), "Siparisiniz onaylandi",
                Map.of("name", user.displayName(),
                    "orderNumber", payload.path("orderNumber").asText("-"),
                    "grandTotal", payload.path("grandTotal").asText("-"),
                    "currency", payload.path("currency").asText("TRY")),
                correlationId(event)));
        } else {
            enqueue(new EmailJob(userId, "ORDER_CANCELLED", user.email(), "Siparisiniz iptal edildi",
                Map.of("name", user.displayName(),
                    "orderNumber", payload.path("orderNumber").asText("-"),
                    "reason", payload.path("reason").asText("ORDER_CANCELLED")),
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
