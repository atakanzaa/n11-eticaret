package com.smartcommerce.shipment.service;

import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.shipment.client.OrderClient;
import com.smartcommerce.shipment.domain.Shipment;
import com.smartcommerce.shipment.domain.ShipmentEventLog;
import com.smartcommerce.shipment.domain.ShipmentStatus;
import com.smartcommerce.shipment.event.outbox.OutboxService;
import com.smartcommerce.shipment.repository.ShipmentEventRepository;
import com.smartcommerce.shipment.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService {

    private static final String DEFAULT_PROVIDER = "YURTICI";

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository eventRepository;
    private final OrderClient orderClient;
    private final Map<String, CargoProvider> providers;
    private final OutboxService outboxService;

    @Transactional
    public List<Shipment> createForOrder(UUID orderId) {
        if (!shipmentRepository.findByOrderId(orderId).isEmpty()) {
            log.info("Shipments already exist for order {}, skipping", orderId);
            return shipmentRepository.findByOrderId(orderId);
        }
        var order = orderClient.getOrder(orderId);
        var bySeller = order.items().stream()
            .collect(Collectors.groupingBy(OrderClient.OrderItem::sellerId));
        var shipments = new ArrayList<Shipment>();
        for (var sellerId : bySeller.keySet()) {
            shipments.add(createSingle(order, sellerId, DEFAULT_PROVIDER));
        }
        return shipments;
    }

    private Shipment createSingle(OrderClient.OrderDetail order, UUID sellerId, String providerName) {
        var provider = providers.get(providerName);
        if (provider == null) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "Cargo provider not available: " + providerName);
        }
        var shipment = Shipment.builder()
            .orderId(order.id()).sellerId(sellerId).cargoProvider(providerName)
            .recipientFullName(order.shippingFullName()).recipientPhone(order.shippingPhone())
            .fullAddress(order.shippingFullAddress()).city(order.shippingCity())
            .district(order.shippingDistrict()).postalCode(order.shippingPostalCode())
            .status(ShipmentStatus.CREATED)
            .build();
        shipment = shipmentRepository.save(shipment);

        var result = provider.create(new CargoProvider.CargoCreateRequest(
            shipment.getId(), shipment.getRecipientFullName(), shipment.getRecipientPhone(),
            shipment.getFullAddress(), shipment.getCity(), shipment.getDistrict(),
            shipment.getPostalCode(), BigDecimal.ONE, order.grandTotal()));

        if (!result.success()) {
            shipment.setStatus(ShipmentStatus.FAILED_DELIVERY);
            shipment.setFailureReason(result.errorMessage());
            shipment.setFailedAt(Instant.now());
            shipmentRepository.save(shipment);
            recordEvent(shipment.getId(), "CREATE_FAILED", result.errorMessage());
            return shipment;
        }

        shipment.setTrackingNumber(result.trackingNumber());
        shipment.setTrackingUrl(result.trackingUrl());
        shipment.setEstimatedDeliveryDate(result.estimatedDeliveryDate());
        shipmentRepository.save(shipment);

        recordEvent(shipment.getId(), "CREATED", "Shipment created with " + providerName);
        outboxService.publish(Topics.SHIPMENT_CREATED, EventType.SHIPMENT_CREATED,
            shipment.getId().toString(), "SHIPMENT", buildPayload(shipment));
        return shipment;
    }

    @Transactional
    public Shipment simulateDispatch(UUID shipmentId) {
        var s = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Shipment not found"));
        if (s.getStatus() != ShipmentStatus.CREATED && s.getStatus() != ShipmentStatus.READY_FOR_PICKUP) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "Cannot dispatch shipment in state: " + s.getStatus());
        }
        s.setStatus(ShipmentStatus.DISPATCHED);
        s.setDispatchedAt(Instant.now());
        shipmentRepository.save(s);
        recordEvent(shipmentId, "DISPATCHED", "Picked up by carrier");
        outboxService.publish(Topics.SHIPMENT_DISPATCHED, EventType.SHIPMENT_DISPATCHED,
            shipmentId.toString(), "SHIPMENT", buildPayload(s));
        return s;
    }

    @Transactional
    public Shipment simulateDelivery(UUID shipmentId) {
        var s = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Shipment not found"));
        if (s.getStatus() == ShipmentStatus.DELIVERED) return s;
        if (s.getStatus() != ShipmentStatus.DISPATCHED && s.getStatus() != ShipmentStatus.IN_TRANSIT
            && s.getStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "Cannot deliver shipment in state: " + s.getStatus());
        }
        s.setStatus(ShipmentStatus.DELIVERED);
        s.setDeliveredAt(Instant.now());
        shipmentRepository.save(s);
        recordEvent(shipmentId, "DELIVERED", "Package delivered to recipient");
        outboxService.publish(Topics.SHIPMENT_DELIVERED, EventType.SHIPMENT_DELIVERED,
            shipmentId.toString(), "SHIPMENT", buildPayload(s));
        return s;
    }

    public Shipment getById(UUID id) {
        return shipmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Shipment not found"));
    }

    public Shipment trackByNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Tracking number not found"));
    }

    public List<Shipment> getByOrderId(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId);
    }

    public List<ShipmentEventLog> getEvents(UUID shipmentId) {
        return eventRepository.findByShipmentIdOrderByOccurredAtAsc(shipmentId);
    }

    private void recordEvent(UUID shipmentId, String type, String desc) {
        eventRepository.save(ShipmentEventLog.builder()
            .shipmentId(shipmentId).eventType(type).description(desc)
            .occurredAt(Instant.now()).build());
    }

    private Map<String, Object> buildPayload(Shipment s) {
        return Map.of(
            "shipmentId", s.getId(),
            "orderId", s.getOrderId(),
            "sellerId", s.getSellerId(),
            "trackingNumber", s.getTrackingNumber() != null ? s.getTrackingNumber() : "",
            "status", s.getStatus().name()
        );
    }
}
