package com.smartcommerce.recommendation.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.events.BaseEvent;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.recommendation.domain.ProcessedEvent;
import com.smartcommerce.recommendation.repository.ProcessedEventRepository;
import com.smartcommerce.recommendation.service.BehaviorTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BehaviorEventConsumer {

    private final BehaviorTrackingService trackingService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.CART_ITEM_ADDED, groupId = "recommendation-service")
    @Transactional
    public void onCartItemAdded(BaseEvent<?> event) {
        if (alreadyProcessed(event)) return;
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var userIdText = payload.path("userId").asText(null);
        var productIdText = payload.path("productId").asText(null);
        if (userIdText == null || productIdText == null) return;
        try {
            trackingService.trackCartAdd(UUID.fromString(userIdText), UUID.fromString(productIdText));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID in cart event: userId={}, productId={}", userIdText, productIdText);
        }
        markProcessed(event);
    }

    @KafkaListener(topics = Topics.ORDER_CONFIRMED, groupId = "recommendation-service")
    @Transactional
    public void onOrderConfirmed(BaseEvent<?> event) {
        if (alreadyProcessed(event)) return;
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var userIdText = payload.path("userId").asText(null);
        if (userIdText == null) return;
        var items = payload.path("items");
        var productIds = new ArrayList<UUID>();
        if (items.isArray()) {
            for (var item : items) {
                var pidText = item.path("productId").asText(null);
                if (pidText != null) {
                    try {
                        productIds.add(UUID.fromString(pidText));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        if (!productIds.isEmpty()) {
            try {
                trackingService.trackPurchase(UUID.fromString(userIdText), productIds);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid userId in order event: {}", userIdText);
            }
        }
        markProcessed(event);
    }

    private boolean alreadyProcessed(BaseEvent<?> event) {
        return event.getEventId() != null && processedEventRepository.existsById(event.getEventId());
    }

    private void markProcessed(BaseEvent<?> event) {
        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), Instant.now()));
        }
    }
}
