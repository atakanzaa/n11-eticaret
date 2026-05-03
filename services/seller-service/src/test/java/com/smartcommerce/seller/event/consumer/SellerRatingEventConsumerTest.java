package com.smartcommerce.seller.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.events.BaseEvent;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.payload.ReviewEventPayload;
import com.smartcommerce.seller.client.OrderClient;
import com.smartcommerce.seller.domain.ProcessedEvent;
import com.smartcommerce.seller.domain.Seller;
import com.smartcommerce.seller.repository.ProcessedEventRepository;
import com.smartcommerce.seller.repository.SellerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerRatingEventConsumerTest {

    @Mock SellerRepository sellerRepository;
    @Mock ProcessedEventRepository processedEventRepository;
    @Mock OrderClient orderClient;
    @Spy ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks SellerRatingEventConsumer consumer;

    @Test
    @DisplayName("REVIEW_APPROVED with first review sets rating to that value")
    void firstApprovedReviewSetsRating() {
        var sellerId = UUID.randomUUID();
        var seller = new Seller();
        seller.setId(sellerId);
        seller.setRating(BigDecimal.ZERO);
        seller.setRatingCount(0);

        var event = approvedEvent(sellerId, /*rating*/5, /*previousRating*/null);
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(orderClient.getById(any())).thenReturn(orderWithSeller(sellerId, event));
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));

        consumer.onReviewEvent(event);

        assertThat(seller.getRating()).isEqualByComparingTo("5.00");
        assertThat(seller.getRatingCount()).isEqualTo(1);
        verify(sellerRepository).save(seller);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Duplicate event delivery is a no-op (idempotent)")
    void duplicateEventIgnored() {
        var event = approvedEvent(UUID.randomUUID(), 4, null);
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(true);

        consumer.onReviewEvent(event);

        verify(orderClient, never()).getById(any());
        verify(sellerRepository, never()).save(any());
    }

    @Test
    @DisplayName("REVIEW_DELETED rolls back the previous rating")
    void deletedReviewRollsBack() {
        var sellerId = UUID.randomUUID();
        var seller = new Seller();
        seller.setId(sellerId);
        seller.setRating(new BigDecimal("4.50"));
        seller.setRatingCount(2);

        var event = new BaseEvent<ReviewEventPayload>();
        event.setEventId(UUID.randomUUID());
        event.setEventType(EventType.REVIEW_DELETED);
        event.setOccurredAt(Instant.now());
        event.setPayload(ReviewEventPayload.builder()
            .reviewId(UUID.randomUUID())
            .productId(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .rating(0)
            .previousRating(5)
            .build());

        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(orderClient.getById(any())).thenReturn(orderWithSeller(sellerId, event));
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));

        consumer.onReviewEvent(event);

        // Was (4+5)/2 = 4.50; remove 5 → 4/1 = 4.00
        assertThat(seller.getRating()).isEqualByComparingTo("4.00");
        assertThat(seller.getRatingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Missing orderId in payload is skipped (not retried)")
    void missingOrderIdSkipped() {
        var event = new BaseEvent<ReviewEventPayload>();
        event.setEventId(UUID.randomUUID());
        event.setEventType(EventType.REVIEW_APPROVED);
        event.setPayload(ReviewEventPayload.builder()
            .reviewId(UUID.randomUUID())
            .productId(UUID.randomUUID())
            .rating(5)
            .build());
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);

        consumer.onReviewEvent(event);

        verify(orderClient, never()).getById(any());
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
    }

    private BaseEvent<ReviewEventPayload> approvedEvent(UUID sellerId, int rating, Integer previousRating) {
        var event = new BaseEvent<ReviewEventPayload>();
        event.setEventId(UUID.randomUUID());
        event.setEventType(EventType.REVIEW_APPROVED);
        event.setOccurredAt(Instant.now());
        event.setPayload(ReviewEventPayload.builder()
            .reviewId(UUID.randomUUID())
            .productId(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .rating(rating)
            .previousRating(previousRating)
            .build());
        return event;
    }

    private OrderClient.OrderDetail orderWithSeller(UUID sellerId, BaseEvent<ReviewEventPayload> event) {
        var productId = event.getPayload().getProductId();
        var item = new OrderClient.OrderItem(
            UUID.randomUUID(), productId, sellerId, 1, BigDecimal.TEN, BigDecimal.TEN, "x");
        return new OrderClient.OrderDetail(
            event.getPayload().getOrderId(), event.getPayload().getUserId(),
            "DELIVERED", BigDecimal.TEN, "TRY", null,
            null, null, null, null, null, null,
            List.of(item));
    }
}
