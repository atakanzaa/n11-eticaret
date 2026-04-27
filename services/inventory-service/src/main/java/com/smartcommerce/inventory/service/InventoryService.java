package com.smartcommerce.inventory.service;

import com.smartcommerce.common.errors.*;
import com.smartcommerce.common.events.*;
import com.smartcommerce.inventory.api.dto.*;
import com.smartcommerce.inventory.api.mapper.InventoryMapper;
import com.smartcommerce.inventory.domain.*;
import com.smartcommerce.inventory.event.outbox.OutboxService;
import com.smartcommerce.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(15);
    private static final int OPTIMISTIC_RETRY_ATTEMPTS = 5;

    private final InventoryItemRepository inventoryItemRepository;
    private final ReservationRepository reservationRepository;
    private final OutboxService outboxService;
    private final InventoryMapper mapper;

    @Transactional
    public ReservationResponse reserve(ReserveRequest request) {
        for (var attempt = 1; attempt <= OPTIMISTIC_RETRY_ATTEMPTS; attempt++) {
            try {
                return reserveOnce(request);
            } catch (OptimisticLockingFailureException e) {
                if (attempt == OPTIMISTIC_RETRY_ATTEMPTS) throw e;
                log.debug("Retrying reservation {} after optimistic lock conflict, attempt={}", request.reservationKey(), attempt);
            }
        }
        throw new IllegalStateException("Reservation retry loop exhausted");
    }

    protected ReservationResponse reserveOnce(ReserveRequest request) {
        var existing = reservationRepository.findByReservationKey(request.reservationKey());
        if (existing.isPresent()) return mapper.toResponse(existing.get());

        var item = inventoryItemRepository.findByOfferId(request.offerId())
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,
                "Inventory item not found for offer: " + request.offerId()));
        if (!item.canReserve(request.quantity())) {
            outboxService.publish(Topics.INVENTORY_RESERVATION_FAILED, EventType.INVENTORY_RESERVATION_FAILED,
                request.orderId().toString(), "ORDER",
                Map.of("orderId", request.orderId(), "offerId", request.offerId(),
                    "requestedQuantity", request.quantity(), "availableQuantity", item.getAvailableQuantity(),
                    "reason", "INSUFFICIENT_STOCK"));
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                "Insufficient stock: available=" + item.getAvailableQuantity() + ", requested=" + request.quantity());
        }

        item.reserve(request.quantity());
        inventoryItemRepository.save(item);
        var reservation = Reservation.builder()
            .reservationKey(request.reservationKey())
            .orderId(request.orderId())
            .offerId(request.offerId())
            .inventoryItemId(item.getId())
            .quantity(request.quantity())
            .status(ReservationStatus.ACTIVE)
            .expiresAt(Instant.now().plus(RESERVATION_TTL))
            .build();
        reservationRepository.save(reservation);
        outboxService.publish(Topics.INVENTORY_RESERVED, EventType.INVENTORY_RESERVED,
            request.orderId().toString(), "ORDER",
            Map.of("reservationId", reservation.getId(), "orderId", reservation.getOrderId(),
                "offerId", reservation.getOfferId(), "quantity", reservation.getQuantity(),
                "expiresAt", reservation.getExpiresAt()));
        publishLowStockIfNeeded(item);
        return mapper.toResponse(reservation);
    }

    @Transactional
    public void confirm(UUID orderId) {
        var reservations = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
        for (var reservation : reservations) {
            var item = inventoryItemRepository.findById(reservation.getInventoryItemId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Inventory item not found"));
            item.confirm(reservation.getQuantity());
            inventoryItemRepository.save(item);
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setConfirmedAt(Instant.now());
            reservationRepository.save(reservation);
            outboxService.publish(Topics.INVENTORY_CONFIRMED, EventType.INVENTORY_CONFIRMED,
                orderId.toString(), "ORDER",
                Map.of("reservationId", reservation.getId(), "orderId", orderId,
                    "offerId", reservation.getOfferId(), "quantity", reservation.getQuantity()));
        }
    }

    @Transactional
    public void release(UUID orderId) {
        var reservations = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
        for (var reservation : reservations) releaseReservation(reservation, "RELEASED");
    }

    public InventoryItemResponse getByOfferId(UUID offerId) {
        return inventoryItemRepository.findByOfferId(offerId).map(mapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Inventory item not found"));
    }

    @Transactional
    public InventoryItemResponse adjustStock(UUID sellerId, UUID offerId, AdjustStockRequest request) {
        var item = inventoryItemRepository.findByOfferId(offerId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Inventory item not found"));
        if (!item.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Cannot adjust another seller's inventory");
        }
        var oldQty = item.getAvailableQuantity();
        item.setAvailableQuantity(request.newAvailableQuantity());
        if (request.lowStockThreshold() != null) item.setLowStockThreshold(request.lowStockThreshold());
        item = inventoryItemRepository.save(item);
        outboxService.publish(Topics.OFFER_STOCK_CHANGED, EventType.OFFER_STOCK_CHANGED,
            offerId.toString(), "OFFER",
            Map.of("offerId", offerId, "oldQuantity", oldQty, "newQuantity", item.getAvailableQuantity()));
        return mapper.toResponse(item);
    }

    @Transactional
    public InventoryItemResponse createOrGet(UUID offerId, UUID productId, UUID sellerId) {
        return inventoryItemRepository.findByOfferId(offerId)
            .map(mapper::toResponse)
            .orElseGet(() -> mapper.toResponse(inventoryItemRepository.save(InventoryItem.builder()
                .offerId(offerId).productId(productId).sellerId(sellerId)
                .availableQuantity(0).reservedQuantity(0).soldQuantity(0).incomingQuantity(0)
                .lowStockThreshold(5).allowBackorder(false).build())));
    }

    @Transactional
    public void releaseExpiredReservations() {
        reservationRepository.findByStatusAndExpiresAtLessThanEqual(ReservationStatus.ACTIVE, Instant.now())
            .forEach(reservation -> releaseReservation(reservation, "EXPIRED"));
    }

    private void releaseReservation(Reservation reservation, String reason) {
        var item = inventoryItemRepository.findById(reservation.getInventoryItemId()).orElseThrow();
        item.release(reservation.getQuantity());
        inventoryItemRepository.save(item);
        reservation.setStatus("EXPIRED".equals(reason) ? ReservationStatus.EXPIRED : ReservationStatus.RELEASED);
        reservation.setReleasedAt(Instant.now());
        reservationRepository.save(reservation);
        outboxService.publish(Topics.INVENTORY_RELEASED, EventType.INVENTORY_RELEASED,
            reservation.getOrderId().toString(), "ORDER",
            Map.of("reservationId", reservation.getId(), "orderId", reservation.getOrderId(),
                "offerId", reservation.getOfferId(), "quantity", reservation.getQuantity(), "reason", reason));
    }

    private void publishLowStockIfNeeded(InventoryItem item) {
        if (!item.isLowStock()) return;
        outboxService.publish(Topics.INVENTORY_LOW_STOCK, EventType.INVENTORY_LOW_STOCK,
            item.getId().toString(), "INVENTORY_ITEM",
            Map.of("offerId", item.getOfferId(), "sellerId", item.getSellerId(),
                "availableQuantity", item.getAvailableQuantity(), "threshold", item.getLowStockThreshold()));
    }
}
