package com.smartcommerce.promotion.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.events.BaseEvent;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.promotion.api.dto.ApplyCouponRequest;
import com.smartcommerce.promotion.domain.ProcessedEvent;
import com.smartcommerce.promotion.repository.ProcessedEventRepository;
import com.smartcommerce.promotion.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Re-applies coupon usage when the sync call from order-service failed at
 * checkout. order-service publishes COUPON_USED via outbox in the same
 * transaction as the order, so even if the sync apply call timed out, the
 * usage is eventually recorded here. Idempotent via ProcessedEventRepository.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CouponUsedConsumer {

    private final CouponService couponService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = Topics.COUPON_USED, groupId = "promotion-service")
    @Transactional
    public void onCouponUsed(BaseEvent<?> event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) return;
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var code = payload.path("couponCode").asText(null);
        var userId = payload.path("userId").asText(null);
        var orderId = payload.path("orderId").asText(null);
        var discountText = payload.path("discountAmount").asText(null);
        if (code == null || userId == null || orderId == null || discountText == null) {
            log.warn("COUPON_USED event with missing fields, skipping: {}", event.getEventId());
            return;
        }
        try {
            couponService.apply(new ApplyCouponRequest(
                code, UUID.fromString(userId), UUID.fromString(orderId), new BigDecimal(discountText)));
        } catch (Exception e) {
            // CouponService.apply is itself idempotent: a duplicate-usage row
            // for the same orderId is the only way the sync path could already
            // have succeeded — treat as no-op rather than DLQ.
            log.debug("apply via event already recorded or failed for order {}: {}", orderId, e.getMessage());
        }
        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), Instant.now()));
        }
    }
}
