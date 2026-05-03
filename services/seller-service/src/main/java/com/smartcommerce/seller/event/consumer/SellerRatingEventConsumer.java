package com.smartcommerce.seller.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.events.BaseEvent;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.common.events.payload.ReviewEventPayload;
import com.smartcommerce.seller.client.OrderClient;
import com.smartcommerce.seller.domain.ProcessedEvent;
import com.smartcommerce.seller.domain.Seller;
import com.smartcommerce.seller.repository.ProcessedEventRepository;
import com.smartcommerce.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Maintains Seller.rating / Seller.ratingCount as customers review their
 * purchases. Listens to REVIEW_APPROVED (rating actually published),
 * REVIEW_UPDATED (rating may have changed), REVIEW_DELETED (rollback) and
 * REVIEW_REJECTED (rollback if it was previously approved).
 *
 * Resolves the affected seller via the order-service internal lookup —
 * REVIEW events carry orderId+productId but not sellerId, and the order-item
 * snapshot is the only place that ties a productId to a sellerId for a
 * specific purchase.
 *
 * Idempotency: ProcessedEventRepository keyed by eventId. On replay, the
 * running average is not re-applied. CREATE is a no-op (review starts
 * PENDING; rating only counts at APPROVED).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SellerRatingEventConsumer {

    private final SellerRepository sellerRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OrderClient orderClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = {Topics.REVIEW_APPROVED, Topics.REVIEW_UPDATED, Topics.REVIEW_DELETED, Topics.REVIEW_REJECTED},
        groupId = "seller-service-rating"
    )
    @Transactional
    public void onReviewEvent(BaseEvent<?> event) {
        if (event.getEventId() != null && processedEventRepository.existsById(event.getEventId())) {
            return;
        }

        ReviewEventPayload payload;
        try {
            payload = objectMapper.convertValue(event.getPayload(), ReviewEventPayload.class);
        } catch (IllegalArgumentException e) {
            log.warn("Skipping malformed review event eventId={}: {}", event.getEventId(), e.getMessage());
            markProcessed(event);
            return;
        }
        if (payload == null || payload.getProductId() == null || payload.getOrderId() == null) {
            log.warn("Review event missing productId/orderId, skipping: {}", event.getEventId());
            markProcessed(event);
            return;
        }

        UUID sellerId = resolveSellerId(payload);
        if (sellerId == null) {
            log.debug("Could not resolve sellerId for review event {}, skipping", event.getEventId());
            markProcessed(event);
            return;
        }

        var seller = sellerRepository.findById(sellerId).orElse(null);
        if (seller == null) {
            log.warn("Seller {} not found while applying review {}", sellerId, payload.getReviewId());
            markProcessed(event);
            return;
        }

        var type = event.getEventType();
        if (EventType.REVIEW_APPROVED.equals(type)) {
            applyRatingDelta(seller, null, payload.getRating());
        } else if (EventType.REVIEW_UPDATED.equals(type)) {
            // Only rating changes shift the aggregate; previousRating must be
            // populated by the producer for safe diff.
            if (payload.getPreviousRating() != null
                && payload.getPreviousRating() != payload.getRating()) {
                applyRatingDelta(seller, payload.getPreviousRating(), payload.getRating());
            }
        } else if (EventType.REVIEW_DELETED.equals(type) || EventType.REVIEW_REJECTED.equals(type)) {
            // Roll the rating back. previousRating represents what was counted.
            if (payload.getPreviousRating() != null) {
                applyRatingDelta(seller, payload.getPreviousRating(), null);
            }
        }
        sellerRepository.save(seller);
        markProcessed(event);
    }

    /**
     * Differential aggregation for an in-place running average.
     * - oldRating null + newRating x  → add new (count += 1)
     * - oldRating x + newRating null  → remove (count -= 1)
     * - oldRating x + newRating y     → replace (count unchanged)
     */
    private void applyRatingDelta(Seller seller, Integer oldRating, Integer newRating) {
        var count = seller.getRatingCount() == null ? 0 : seller.getRatingCount();
        var avg = seller.getRating() == null ? BigDecimal.ZERO : seller.getRating();
        var sum = avg.multiply(BigDecimal.valueOf(count));

        if (oldRating != null) {
            sum = sum.subtract(BigDecimal.valueOf(oldRating));
            count -= 1;
        }
        if (newRating != null) {
            sum = sum.add(BigDecimal.valueOf(newRating));
            count += 1;
        }

        if (count <= 0) {
            seller.setRating(BigDecimal.ZERO);
            seller.setRatingCount(0);
            return;
        }
        seller.setRating(sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
        seller.setRatingCount(count);
    }

    private UUID resolveSellerId(ReviewEventPayload payload) {
        try {
            var order = orderClient.getById(payload.getOrderId());
            if (order == null || order.items() == null) return null;
            return order.items().stream()
                .filter(it -> payload.getProductId().equals(it.productId()))
                .findFirst()
                .map(OrderClient.OrderItem::sellerId)
                .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to resolve sellerId for orderId={} productId={}: {}",
                payload.getOrderId(), payload.getProductId(), e.getMessage());
            return null;
        }
    }

    private void markProcessed(BaseEvent<?> event) {
        if (event.getEventId() != null) {
            processedEventRepository.save(new ProcessedEvent(event.getEventId(), Instant.now()));
        }
    }
}
