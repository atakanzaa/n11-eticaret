package com.smartcommerce.product.api;

import com.smartcommerce.product.api.dto.*;
import com.smartcommerce.product.client.SellerClient;
import com.smartcommerce.product.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OfferController {
    private final OfferService offerService;
    private final SellerClient sellerClient;

    @PostMapping("/offers")
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OfferResponse create(@AuthenticationPrincipal String userId,
                                @Valid @RequestBody CreateOfferRequest request) {
        var sellerId = sellerClient.getSellerIdByUserId(UUID.fromString(userId));
        return offerService.create(sellerId, request);
    }

    @PutMapping("/offers/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public OfferResponse update(@AuthenticationPrincipal String userId,
                                @PathVariable UUID id,
                                @Valid @RequestBody UpdateOfferRequest request) {
        var sellerId = sellerClient.getSellerIdByUserId(UUID.fromString(userId));
        return offerService.update(sellerId, id, request);
    }

    @GetMapping("/offers/my")
    @PreAuthorize("hasRole('SELLER')")
    public List<OfferResponse> getMyOffers(@AuthenticationPrincipal String userId) {
        var sellerId = sellerClient.getSellerIdByUserId(UUID.fromString(userId));
        return offerService.getMyOffers(sellerId);
    }

    @GetMapping("/offers/{id}")
    public OfferResponse getById(@PathVariable UUID id) {
        return offerService.getById(id);
    }

    @GetMapping("/products/{productId}/offers")
    public List<OfferResponse> getOffersForProduct(@PathVariable UUID productId) {
        return offerService.getOffersForProduct(productId);
    }

    @DeleteMapping("/offers/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        var sellerId = sellerClient.getSellerIdByUserId(UUID.fromString(userId));
        offerService.delete(sellerId, id);
    }
}
