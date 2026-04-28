package com.smartcommerce.shipment.service;

import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.shipment.client.OrderClient;
import com.smartcommerce.shipment.domain.Shipment;
import com.smartcommerce.shipment.domain.ShipmentEventLog;
import com.smartcommerce.shipment.domain.ShipmentStatus;
import com.smartcommerce.shipment.event.outbox.OutboxService;
import com.smartcommerce.shipment.repository.ShipmentEventRepository;
import com.smartcommerce.shipment.repository.ShipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock ShipmentRepository shipmentRepository;
    @Mock ShipmentEventRepository eventRepository;
    @Mock OrderClient orderClient;
    @Mock OutboxService outboxService;

    ShipmentService service;

    @BeforeEach
    void setUp() {
        var providers = Map.<String, CargoProvider>of("YURTICI", new MockYurticiAdapter());
        service = new ShipmentService(shipmentRepository, eventRepository, orderClient, providers, outboxService);
    }

    @Test
    @DisplayName("multi-seller order creates one shipment per seller")
    void createForOrder_multiSellerSplitsShipments() {
        var orderId = UUID.randomUUID();
        var sellerA = UUID.randomUUID();
        var sellerB = UUID.randomUUID();
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderClient.getOrder(orderId)).thenReturn(orderDetail(orderId, sellerA, sellerB));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> {
            Shipment s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });

        var shipments = service.createForOrder(orderId);

        assertThat(shipments).hasSize(2);
        assertThat(shipments).extracting(Shipment::getSellerId).containsExactlyInAnyOrder(sellerA, sellerB);
        verify(outboxService, times(2)).publish(eq("shipment.created.v1"), eq("SHIPMENT_CREATED"),
            anyString(), eq("SHIPMENT"), anyMap());
    }

    @Test
    @DisplayName("idempotent: re-creating shipments for an order returns existing without re-creating")
    void createForOrder_idempotent() {
        var orderId = UUID.randomUUID();
        var existing = Shipment.builder().id(UUID.randomUUID()).orderId(orderId)
            .sellerId(UUID.randomUUID()).cargoProvider("YURTICI")
            .recipientFullName("X").recipientPhone("+90").fullAddress("addr")
            .city("Istanbul").district("Kadikoy")
            .status(ShipmentStatus.CREATED).build();
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(List.of(existing));

        var result = service.createForOrder(orderId);

        assertThat(result).hasSize(1);
        verify(shipmentRepository, times(0)).save(any(Shipment.class));
    }

    @Test
    @DisplayName("simulate dispatch transitions CREATED → DISPATCHED and publishes event")
    void simulateDispatch_publishesEvent() {
        var shipmentId = UUID.randomUUID();
        var s = Shipment.builder().id(shipmentId).orderId(UUID.randomUUID()).sellerId(UUID.randomUUID())
            .cargoProvider("YURTICI").recipientFullName("X").recipientPhone("+90").fullAddress("addr")
            .city("Istanbul").district("Kadikoy").status(ShipmentStatus.CREATED).build();
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(s));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.simulateDispatch(shipmentId);

        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.DISPATCHED);
        assertThat(s.getDispatchedAt()).isNotNull();
        verify(outboxService).publish(eq("shipment.dispatched.v1"), eq("SHIPMENT_DISPATCHED"),
            anyString(), eq("SHIPMENT"), anyMap());
    }

    @Test
    @DisplayName("simulate delivery on DISPATCHED → DELIVERED + event")
    void simulateDelivery_publishesEvent() {
        var shipmentId = UUID.randomUUID();
        var s = Shipment.builder().id(shipmentId).orderId(UUID.randomUUID()).sellerId(UUID.randomUUID())
            .cargoProvider("YURTICI").recipientFullName("X").recipientPhone("+90").fullAddress("addr")
            .city("Istanbul").district("Kadikoy").status(ShipmentStatus.DISPATCHED).build();
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(s));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        service.simulateDelivery(shipmentId);

        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(s.getDeliveredAt()).isNotNull();
        verify(outboxService).publish(eq("shipment.delivered.v1"), eq("SHIPMENT_DELIVERED"),
            anyString(), eq("SHIPMENT"), anyMap());
    }

    @Test
    @DisplayName("dispatch from non-eligible state throws BusinessException")
    void simulateDispatch_invalidState() {
        var shipmentId = UUID.randomUUID();
        var s = Shipment.builder().id(shipmentId).orderId(UUID.randomUUID()).sellerId(UUID.randomUUID())
            .cargoProvider("YURTICI").recipientFullName("X").recipientPhone("+90").fullAddress("addr")
            .city("Istanbul").district("Kadikoy").status(ShipmentStatus.DELIVERED).build();
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.simulateDispatch(shipmentId))
            .isInstanceOf(BusinessException.class);
    }

    private OrderClient.OrderDetail orderDetail(UUID orderId, UUID sellerA, UUID sellerB) {
        return new OrderClient.OrderDetail(
            orderId, UUID.randomUUID(), "CONFIRMED", new BigDecimal("250.00"), "TRY",
            "Atakan Yel", "+905555555555", "Istanbul", "Kadikoy",
            "123 Test Street", "34000",
            List.of(
                new OrderClient.OrderItem(UUID.randomUUID(), UUID.randomUUID(), sellerA, 1, new BigDecimal("100.00"), "P1"),
                new OrderClient.OrderItem(UUID.randomUUID(), UUID.randomUUID(), sellerB, 2, new BigDecimal("75.00"), "P2")
            )
        );
    }
}
