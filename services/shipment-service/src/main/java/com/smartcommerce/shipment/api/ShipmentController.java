package com.smartcommerce.shipment.api;

import com.smartcommerce.shipment.api.dto.ShipmentEventResponse;
import com.smartcommerce.shipment.api.dto.ShipmentResponse;
import com.smartcommerce.shipment.api.mapper.ShipmentMapper;
import com.smartcommerce.shipment.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final ShipmentMapper mapper;

    @GetMapping("/{id}")
    public ShipmentResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(shipmentService.getById(id));
    }

    @GetMapping("/by-order/{orderId}")
    public List<ShipmentResponse> getByOrder(@PathVariable UUID orderId) {
        return shipmentService.getByOrderId(orderId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/track/{trackingNumber}")
    public ShipmentResponse track(@PathVariable String trackingNumber) {
        return mapper.toResponse(shipmentService.trackByNumber(trackingNumber));
    }

    @GetMapping("/{id}/events")
    public List<ShipmentEventResponse> getEvents(@PathVariable UUID id) {
        return shipmentService.getEvents(id).stream().map(mapper::toEventResponse).toList();
    }

    @PostMapping("/{id}/simulate-dispatch")
    @PreAuthorize("hasRole('ADMIN')")
    public ShipmentResponse simulateDispatch(@PathVariable UUID id) {
        return mapper.toResponse(shipmentService.simulateDispatch(id));
    }

    @PostMapping("/{id}/simulate-delivery")
    @PreAuthorize("hasRole('ADMIN')")
    public ShipmentResponse simulateDelivery(@PathVariable UUID id) {
        return mapper.toResponse(shipmentService.simulateDelivery(id));
    }

    @PostMapping("/internal/orders/{orderId}/create")
    public List<ShipmentResponse> createForOrder(@PathVariable UUID orderId) {
        return shipmentService.createForOrder(orderId).stream().map(mapper::toResponse).toList();
    }
}
