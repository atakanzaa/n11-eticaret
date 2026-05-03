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

    @KafkaListener(topics = {Topics.PAYMENT_SUCCEEDED, Topics.PAYMENT_FAILED, Topics.PAYMENT_REFUNDED}, groupId = "order-service")
    @Transactional
    public void onPaymentEvent(BaseEvent<?> event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) return;
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var orderId = UUID.fromString(payload.path("orderId").asText());
        var type = event.getEventType();
        if (EventType.PAYMENT_SUCCEEDED.equals(type)) {
            var paymentIdText = payload.path("paymentId").asText(null);
            checkoutOrchestrator.confirmPayment(orderId,
                paymentIdText == null ? UUID.randomUUID() : UUID.fromString(paymentIdText));
        } else if (EventType.PAYMENT_FAILED.equals(type)) {
            checkoutOrchestrator.failPayment(orderId, payload.path("reason").asText("PAYMENT_FAILED"));
        } else if (EventType.PAYMENT_REFUNDED.equals(type)) {
            // Only act on full refunds. Partial refunds keep order status as-is and
            // are handled by the return-service flow.
            if (payload.path("fullyRefunded").asBoolean(false)) {
                checkoutOrchestrator.markOrderRefunded(orderId);
            }
        }
        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), Instant.now()));
        }
    }
}
