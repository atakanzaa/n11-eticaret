package com.smartcommerce.order.job;

import com.smartcommerce.order.service.CheckoutOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderExpirationJob {
    private final CheckoutOrchestrator checkoutOrchestrator;

    @Scheduled(fixedDelay = 60000)
    public void expirePaymentPendingOrders() {
        checkoutOrchestrator.expirePaymentPendingOrders();
    }
}
