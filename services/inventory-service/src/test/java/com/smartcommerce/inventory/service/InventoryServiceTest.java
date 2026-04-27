package com.smartcommerce.inventory.service;

import com.smartcommerce.common.errors.*;
import com.smartcommerce.inventory.api.dto.ReserveRequest;
import com.smartcommerce.inventory.api.mapper.InventoryMapper;
import com.smartcommerce.inventory.domain.*;
import com.smartcommerce.inventory.event.outbox.OutboxService;
import com.smartcommerce.inventory.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    @Mock InventoryItemRepository inventoryItemRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock OutboxService outboxService;

    InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryItemRepository, reservationRepository, outboxService, new InventoryMapper());
    }

    @Test
    @DisplayName("reserve decreases available stock and creates an idempotent reservation")
    void reserve_createsReservation() {
        var orderId = UUID.randomUUID();
        var offerId = UUID.randomUUID();
        var item = InventoryItem.builder()
            .id(UUID.randomUUID())
            .offerId(offerId)
            .productId(UUID.randomUUID())
            .sellerId(UUID.randomUUID())
            .availableQuantity(5)
            .reservedQuantity(0)
            .soldQuantity(0)
            .incomingQuantity(0)
            .lowStockThreshold(2)
            .allowBackorder(false)
            .build();

        when(reservationRepository.findByReservationKey("r-1")).thenReturn(Optional.empty());
        when(inventoryItemRepository.findByOfferId(offerId)).thenReturn(Optional.of(item));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(UUID.randomUUID());
            return reservation;
        });

        var response = inventoryService.reserve(new ReserveRequest(orderId, offerId, 3, "r-1"));

        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(item.getAvailableQuantity()).isEqualTo(2);
        assertThat(item.getReservedQuantity()).isEqualTo(3);
        verify(outboxService, atLeastOnce()).publish(anyString(), anyString(), eq(orderId.toString()), eq("ORDER"), anyMap());
    }

    @Test
    @DisplayName("reserve publishes a failure event when stock is insufficient")
    void reserve_insufficientStock() {
        var orderId = UUID.randomUUID();
        var offerId = UUID.randomUUID();
        var item = InventoryItem.builder()
            .id(UUID.randomUUID())
            .offerId(offerId)
            .productId(UUID.randomUUID())
            .sellerId(UUID.randomUUID())
            .availableQuantity(1)
            .reservedQuantity(0)
            .soldQuantity(0)
            .incomingQuantity(0)
            .lowStockThreshold(2)
            .allowBackorder(false)
            .build();

        when(reservationRepository.findByReservationKey("r-1")).thenReturn(Optional.empty());
        when(inventoryItemRepository.findByOfferId(offerId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> inventoryService.reserve(new ReserveRequest(orderId, offerId, 2, "r-1")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Insufficient stock");

        assertThat(item.getAvailableQuantity()).isEqualTo(1);
        verify(outboxService).publish(anyString(), anyString(), eq(orderId.toString()), eq("ORDER"), anyMap());
    }
}
