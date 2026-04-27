package com.smartcommerce.order.event.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.web.CorrelationIdFilter;
import com.smartcommerce.order.domain.*;
import com.smartcommerce.order.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topic, String eventType, String aggregateId, String aggregateType, Object payload) {
        try {
            outboxRepository.save(OutboxEvent.builder()
                .topic(topic).eventType(eventType).aggregateId(aggregateId).aggregateType(aggregateType)
                .payload(objectMapper.writeValueAsString(payload))
                .correlationId(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY))
                .status(OutboxStatus.PENDING).build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
