package com.smartcommerce.order.event.consumer;

import com.fasterxml.jackson.databind.*;
import com.smartcommerce.common.events.*;
import com.smartcommerce.order.domain.ProcessedEvent;
import com.smartcommerce.order.repository.ProcessedEventRepository;
import com.smartcommerce.order.service.CheckoutOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {
    private final CheckoutOrchestrator checkoutOrchestrator;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {Topics.PAYMENT_SUCCEEDED, Topics.PAYMENT_FAILED}, groupId = "order-service")
    @Transactional
    public void onPaymentEvent(BaseEvent<?> event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) return;
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var orderId = UUID.fromString(payload.path("orderId").asText());
        if (Topics.PAYMENT_SUCCEEDED.equals(topicFromEvent(event))) {
            var paymentIdText = payload.path("paymentId").asText(null);
            checkoutOrchestrator.confirmPayment(orderId, paymentIdText == null ? UUID.randomUUID() : UUID.fromString(paymentIdText));
        } else {
            checkoutOrchestrator.failPayment(orderId, payload.path("reason").asText("PAYMENT_FAILED"));
        }
        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), Instant.now()));
        }
    }

    private String topicFromEvent(BaseEvent<?> event) {
        if (EventType.PAYMENT_SUCCEEDED.equals(event.getEventType())) return Topics.PAYMENT_SUCCEEDED;
        return Topics.PAYMENT_FAILED;
    }
}
