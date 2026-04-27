package com.smartcommerce.inventory.event.consumer;

import com.fasterxml.jackson.databind.*;
import com.smartcommerce.common.events.*;
import com.smartcommerce.inventory.domain.ProcessedEvent;
import com.smartcommerce.inventory.repository.ProcessedEventRepository;
import com.smartcommerce.inventory.service.InventoryService;
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
public class OfferCreatedConsumer {
    private final InventoryService inventoryService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.OFFER_CREATED, groupId = "inventory-service")
    @Transactional
    public void onOfferCreated(BaseEvent<?> event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) return;
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var offerId = uuid(payload.hasNonNull("offerId") ? payload.get("offerId") : payload.get("id"));
        var productId = uuid(payload.get("productId"));
        var sellerId = uuid(payload.get("sellerId"));
        inventoryService.createOrGet(offerId, productId, sellerId);
        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), Instant.now()));
        }
        log.info("Inventory item initialized for offer {}", offerId);
    }

    private UUID uuid(JsonNode node) {
        return UUID.fromString(node.asText());
    }
}
