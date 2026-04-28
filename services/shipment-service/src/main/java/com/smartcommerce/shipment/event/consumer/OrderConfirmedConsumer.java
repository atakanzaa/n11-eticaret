package com.smartcommerce.shipment.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.events.BaseEvent;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.shipment.domain.ProcessedEvent;
import com.smartcommerce.shipment.repository.ProcessedEventRepository;
import com.smartcommerce.shipment.service.ShipmentService;
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
public class OrderConfirmedConsumer {

    private final ShipmentService shipmentService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.ORDER_CONFIRMED, groupId = "shipment-service")
    @Transactional
    public void onOrderConfirmed(BaseEvent<?> event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) return;
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var orderIdText = payload.path("orderId").asText(null);
        if (orderIdText == null || orderIdText.isBlank()) {
            log.warn("ORDER_CONFIRMED event without orderId, skipping: {}", event.getEventId());
            return;
        }
        var orderId = UUID.fromString(orderIdText);
        try {
            shipmentService.createForOrder(orderId);
        } catch (Exception e) {
            log.error("Failed to auto-create shipments for order {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), Instant.now()));
        }
    }
}
