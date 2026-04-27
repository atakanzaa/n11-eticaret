package com.smartcommerce.inventory.api;

import com.smartcommerce.inventory.api.dto.*;
import com.smartcommerce.inventory.client.SellerClient;
import com.smartcommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;
    private final SellerClient sellerClient;

    @PostMapping("/reserve")
    public ReservationResponse reserve(@Valid @RequestBody ReserveRequest request) {
        return inventoryService.reserve(request);
    }

    @PostMapping("/orders/{orderId}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@PathVariable UUID orderId) {
        inventoryService.confirm(orderId);
    }

    @PostMapping("/orders/{orderId}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable UUID orderId) {
        inventoryService.release(orderId);
    }

    @GetMapping("/offers/{offerId}")
    public InventoryItemResponse getByOfferId(@PathVariable UUID offerId) {
        return inventoryService.getByOfferId(offerId);
    }

    @PutMapping("/offers/{offerId}/stock")
    @PreAuthorize("hasRole('SELLER')")
    public InventoryItemResponse adjustStock(@AuthenticationPrincipal String userId,
                                             @PathVariable UUID offerId,
                                             @Valid @RequestBody AdjustStockRequest request) {
        var sellerId = sellerClient.getSellerIdByUserId(UUID.fromString(userId));
        return inventoryService.adjustStock(sellerId, offerId, request);
    }
}
