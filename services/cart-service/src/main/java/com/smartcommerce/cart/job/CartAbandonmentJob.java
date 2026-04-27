package com.smartcommerce.cart.job;

import com.smartcommerce.cart.domain.CartStatus;
import com.smartcommerce.cart.event.outbox.OutboxService;
import com.smartcommerce.cart.repository.CartRepository;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CartAbandonmentJob {
    private final CartRepository cartRepository;
    private final OutboxService outboxService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void detectAbandoned() {
        var threshold = Instant.now().minus(Duration.ofHours(24));
        var abandoned = cartRepository.findByStatusAndUpdatedAtBefore(CartStatus.ACTIVE, threshold);
        for (var cart : abandoned) {
            cart.setStatus(CartStatus.ABANDONED);
            cart.setAbandonedAt(Instant.now());
            cartRepository.save(cart);
            outboxService.publish(Topics.CART_ABANDONED, EventType.CART_ABANDONED,
                cart.getId().toString(), "CART", Map.of("cartId", cart.getId(), "userId", cart.getUserId()));
        }
    }
}
