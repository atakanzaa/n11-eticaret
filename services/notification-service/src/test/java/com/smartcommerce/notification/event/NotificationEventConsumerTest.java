package com.smartcommerce.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.events.BaseEvent;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.notification.api.dto.EmailJob;
import com.smartcommerce.notification.client.UserClient;
import com.smartcommerce.notification.config.RabbitMQConfig;
import com.smartcommerce.notification.domain.ProcessedEvent;
import com.smartcommerce.notification.repository.ProcessedEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock RabbitTemplate rabbitTemplate;
    @Mock UserClient userClient;
    @Mock ProcessedEventRepository processedEventRepository;
    @Captor ArgumentCaptor<EmailJob> emailJobCaptor;
    @Captor ArgumentCaptor<ProcessedEvent> processedEventCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("order confirmed event publishes email job to RabbitMQ")
    void orderConfirmed_publishesEmailJob() {
        var userId = UUID.randomUUID();
        when(userClient.getProfile(userId)).thenReturn(new UserClient.UserProfile(
            UUID.randomUUID(), userId, "buyer@example.com", "Ada", "Lovelace", "tr", "TRY"));
        var event = event(EventType.ORDER_CONFIRMED, Map.of(
            "userId", userId.toString(),
            "orderNumber", "SC-100",
            "grandTotal", new BigDecimal("299.90"),
            "currency", "TRY"
        ));

        consumer().onOrderEvent(event);

        verify(rabbitTemplate).convertAndSend(
            org.mockito.ArgumentMatchers.eq(RabbitMQConfig.EMAIL_EXCHANGE),
            org.mockito.ArgumentMatchers.eq(RabbitMQConfig.EMAIL_ROUTING_KEY),
            emailJobCaptor.capture());
        var job = emailJobCaptor.getValue();
        assertThat(job.templateCode()).isEqualTo("ORDER_CONFIRMED");
        assertThat(job.recipient()).isEqualTo("buyer@example.com");
        assertThat(job.model()).containsEntry("orderNumber", "SC-100");
        verify(processedEventRepository).save(processedEventCaptor.capture());
        assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(event.getEventId());
    }

    @Test
    @DisplayName("user registered event publishes welcome email job")
    void userRegistered_publishesWelcomeJob() {
        var userId = UUID.randomUUID();
        var event = event(EventType.USER_REGISTERED, Map.of(
            "userId", userId.toString(),
            "email", "new@example.com",
            "firstName", "Yeni",
            "lastName", "Uye"
        ));

        consumer().onUserRegistered(event);

        verify(rabbitTemplate).convertAndSend(
            org.mockito.ArgumentMatchers.eq(RabbitMQConfig.EMAIL_EXCHANGE),
            org.mockito.ArgumentMatchers.eq(RabbitMQConfig.EMAIL_ROUTING_KEY),
            emailJobCaptor.capture());
        var job = emailJobCaptor.getValue();
        assertThat(job.templateCode()).isEqualTo("WELCOME");
        assertThat(job.recipient()).isEqualTo("new@example.com");
        assertThat(job.model()).containsEntry("name", "Yeni Uye");
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("duplicate event is ignored")
    void duplicateEvent_isIgnored() {
        var event = event(EventType.USER_REGISTERED, Map.of(
            "userId", UUID.randomUUID().toString(),
            "email", "dupe@example.com"
        ));
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(true);

        consumer().onUserRegistered(event);

        verifyNoInteractions(rabbitTemplate);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("malformed order event is marked processed")
    void malformedOrderEvent_isMarkedProcessed() {
        var event = event(EventType.ORDER_CONFIRMED, Map.of("orderNumber", "SC-BAD"));

        consumer().onOrderEvent(event);

        verifyNoInteractions(rabbitTemplate, userClient);
        verify(processedEventRepository).save(processedEventCaptor.capture());
        assertThat(processedEventCaptor.getValue().getEventId()).isEqualTo(event.getEventId());
    }

    private NotificationEventConsumer consumer() {
        return new NotificationEventConsumer(rabbitTemplate, userClient, processedEventRepository, objectMapper);
    }

    private BaseEvent<Object> event(String eventType, Object payload) {
        return BaseEvent.builder()
            .eventId(UUID.randomUUID())
            .eventType(eventType)
            .aggregateId(UUID.randomUUID().toString())
            .aggregateType("TEST")
            .payload(payload)
            .build();
    }
}
