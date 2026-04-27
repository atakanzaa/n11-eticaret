package com.smartcommerce.cart.event.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.cart.domain.OutboxEvent;
import com.smartcommerce.cart.domain.OutboxStatus;
import com.smartcommerce.cart.repository.OutboxRepository;
import com.smartcommerce.common.web.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topic, String eventType, String aggregateId, String aggregateType, Object payload) {
        try {
            var outbox = OutboxEvent.builder()
                .topic(topic)
                .eventType(eventType)
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .payload(objectMapper.writeValueAsString(payload))
                .correlationId(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
                .status(OutboxStatus.PENDING)
                .build();
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
