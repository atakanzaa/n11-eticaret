package com.smartcommerce.inventory.job;

import com.smartcommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpirationJob {
    private final InventoryService inventoryService;

    @Scheduled(fixedDelay = 30000)
    public void releaseExpired() {
        inventoryService.releaseExpiredReservations();
    }
}
