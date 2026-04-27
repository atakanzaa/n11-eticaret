package com.smartcommerce.inventory.api.mapper;

import com.smartcommerce.inventory.api.dto.*;
import com.smartcommerce.inventory.domain.*;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {
    public InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(item.getId(), item.getOfferId(), item.getProductId(), item.getSellerId(),
            item.getAvailableQuantity(), item.getReservedQuantity(), item.getSoldQuantity(), item.getIncomingQuantity(),
            item.getLowStockThreshold(), item.isAllowBackorder(), item.getVersion());
    }

    public ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getReservationKey(), reservation.getOrderId(),
            reservation.getOfferId(), reservation.getQuantity(), reservation.getStatus(), reservation.getExpiresAt());
    }
}
