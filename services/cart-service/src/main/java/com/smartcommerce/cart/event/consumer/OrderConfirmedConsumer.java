package com.smartcommerce.cart.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.cart.domain.ProcessedEvent;
import com.smartcommerce.cart.repository.ProcessedEventRepository;
import com.smartcommerce.cart.service.CartService;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.common.events.BaseEvent;
import com.smartcommerce.common.events.Topics;
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

    private final CartService cartService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.ORDER_CONFIRMED, groupId = "cart-service")
    @Transactional
    public void onOrderConfirmed(BaseEvent<?> event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) {
            return;
        }
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var userIdText = payload.path("userId").asText(null);
        if (userIdText == null || userIdText.isBlank()) {
            log.warn("ORDER_CONFIRMED event without userId, skipping: {}", event.getEventId());
            return;
        }
        var userId = UUID.fromString(userIdText);
        try {
            cartService.clearCart(userId);
        } catch (ResourceNotFoundException e) {
            log.debug("Cart not found for user {}, treating as already cleared", userId);
        }
        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), Instant.now()));
        }
    }
}
