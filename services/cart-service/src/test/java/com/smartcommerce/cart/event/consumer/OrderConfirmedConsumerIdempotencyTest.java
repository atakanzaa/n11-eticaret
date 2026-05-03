package com.smartcommerce.cart.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.cart.domain.ProcessedEvent;
import com.smartcommerce.cart.repository.ProcessedEventRepository;
import com.smartcommerce.cart.service.CartService;
import com.smartcommerce.common.events.BaseEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderConfirmedConsumerIdempotencyTest {

    @Mock CartService cartService;
    @Mock ProcessedEventRepository processedEventRepository;
    @Spy ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks OrderConfirmedConsumer consumer;

    @Test
    void firstDeliveryClearsCart_secondDeliverySkipped() {
        var eventId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        BaseEvent<Map<String, Object>> event = new BaseEvent<>();
        event.setEventId(eventId);
        event.setEventType("ORDER_CONFIRMED");
        event.setOccurredAt(Instant.now());
        event.setPayload(Map.of("userId", userId.toString(), "orderId", UUID.randomUUID().toString()));

        // First delivery: not seen yet
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        consumer.onOrderConfirmed(event);
        verify(cartService).clearCart(userId);
        verify(processedEventRepository).save(any(ProcessedEvent.class));

        // Second delivery: now seen
        when(processedEventRepository.existsById(eventId)).thenReturn(true);
        consumer.onOrderConfirmed(event);
        // clearCart still called only once across both deliveries
        verify(cartService, times(1)).clearCart(userId);
    }

    @Test
    void missingUserIdEventIsSkippedAndNotRecorded() {
        BaseEvent<Map<String, Object>> event = new BaseEvent<>();
        event.setEventId(UUID.randomUUID());
        event.setPayload(Map.of("orderId", UUID.randomUUID().toString()));
        when(processedEventRepository.existsById(event.getEventId())).thenReturn(false);

        consumer.onOrderConfirmed(event);

        verify(cartService, never()).clearCart(any());
        // intentional: malformed events shouldn't be retried — and we don't
        // record them either so a fixed re-publish can succeed.
        verify(processedEventRepository, never()).save(any());
    }
}
