package com.smartcommerce.seller.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.events.BaseEvent;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.common.events.payload.SellerRegisteredPayload;
import com.smartcommerce.common.events.payload.UserRegisteredPayload;
import com.smartcommerce.seller.domain.ProcessedEvent;
import com.smartcommerce.seller.domain.Seller;
import com.smartcommerce.seller.domain.SellerStatus;
import com.smartcommerce.seller.event.outbox.OutboxService;
import com.smartcommerce.seller.repository.ProcessedEventRepository;
import com.smartcommerce.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredConsumer {

    private final SellerRepository sellerRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.USER_REGISTERED, groupId = "seller-service")
    @Transactional
    public void onUserRegistered(BaseEvent<?> event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) {
            log.debug("Event {} already processed, skipping", event.getEventId());
            return;
        }

        // BaseEvent<T> erases T at runtime, so Jackson hands us a LinkedHashMap.
        // Convert explicitly to the typed payload before working with it.
        var payload = event.getPayload() == null ? null
            : objectMapper.convertValue(event.getPayload(), UserRegisteredPayload.class);
        if (payload == null || payload.getRoles() == null || !payload.getRoles().contains("SELLER")) {
            return;
        }

        if (sellerRepository.existsByUserId(payload.getUserId())) {
            return;
        }

        var seller = Seller.builder()
            .userId(payload.getUserId())
            .storeName(payload.getFirstName() + " " + payload.getLastName() + "'s Store")
            .status(SellerStatus.PENDING)
            .commissionRate(new BigDecimal("0.0500"))
            .rating(BigDecimal.ZERO)
            .ratingCount(0)
            .build();
        sellerRepository.save(seller);
        log.info("Created seller record for user {}: sellerId={}", payload.getUserId(), seller.getId());

        outboxService.publish(
            Topics.SELLER_REGISTERED, EventType.SELLER_REGISTERED,
            seller.getId().toString(), "SELLER",
            SellerRegisteredPayload.builder()
                .sellerId(seller.getId())
                .userId(payload.getUserId())
                .storeName(seller.getStoreName())
                .build()
        );

        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), Instant.now()));
        }
    }
}
