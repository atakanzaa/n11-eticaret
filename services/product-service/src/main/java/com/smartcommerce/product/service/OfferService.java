package com.smartcommerce.product.service;

import com.smartcommerce.common.errors.*;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.product.api.dto.*;
import com.smartcommerce.product.api.mapper.OfferMapper;
import com.smartcommerce.product.domain.*;
import com.smartcommerce.product.event.outbox.OutboxService;
import com.smartcommerce.product.repository.OfferRepository;
import com.smartcommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OfferService {
    private static final String DEFAULT_CURRENCY = "TRY";
    private static final int DEFAULT_DELIVERY_DAYS = 3;

    private final OfferRepository offerRepository;
    private final ProductRepository productRepository;
    private final OutboxService outboxService;
    private final OfferMapper offerMapper;

    @Transactional
    public OfferResponse create(UUID sellerId, CreateOfferRequest request) {
        var product = productRepository.findByIdAndDeletedAtIsNull(request.productId())
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found"));
        if (offerRepository.existsByProductIdAndSellerId(product.getId(), sellerId)) {
            throw new DuplicateResourceException(ErrorCode.DUPLICATE_RESOURCE, "You already have an offer for this product");
        }

        int initialStock = request.initialStock() == null ? 0 : request.initialStock();
        var offer = Offer.builder()
            .productId(product.getId())
            .sellerId(sellerId)
            .sku(request.sku() == null ? generateSku(product.getId(), sellerId) : request.sku())
            .price(request.price())
            .currency(DEFAULT_CURRENCY)
            .listPrice(request.listPrice())
            .cargoProvider(request.cargoProvider() == null ? CargoProvider.DEFAULT : request.cargoProvider())
            .cargoPrice(request.cargoPrice() == null ? BigDecimal.ZERO : request.cargoPrice())
            .estimatedDeliveryDays(request.estimatedDeliveryDays() == null ? DEFAULT_DELIVERY_DAYS : request.estimatedDeliveryDays())
            .freeShippingThreshold(request.freeShippingThreshold())
            .status(initialStock > 0 ? OfferStatus.ACTIVE : OfferStatus.PAUSED)
            .build();
        offer.normalizeMoney();
        offer = offerRepository.save(offer);
        var eventPayload = new HashMap<String, Object>();
        eventPayload.put("id", offer.getId());
        eventPayload.put("offerId", offer.getId());
        eventPayload.put("productId", offer.getProductId());
        eventPayload.put("sellerId", offer.getSellerId());
        eventPayload.put("sku", offer.getSku());
        eventPayload.put("price", offer.getPrice());
        eventPayload.put("currency", offer.getCurrency());
        eventPayload.put("listPrice", offer.getListPrice());
        eventPayload.put("status", offer.getStatus());
        eventPayload.put("initialStock", initialStock);
        outboxService.publish(Topics.OFFER_CREATED, EventType.OFFER_CREATED,
            offer.getId().toString(), "OFFER", eventPayload);
        return offerMapper.toResponse(offer);
    }

    @Transactional
    public OfferResponse update(UUID sellerId, UUID offerId, UpdateOfferRequest request) {
        var offer = offerRepository.findByIdAndDeletedAtIsNull(offerId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Offer not found"));
        ensureOwner(offer, sellerId);

        var oldPrice = offer.getPrice();
        var oldStatus = offer.getStatus();
        if (request.price() != null) offer.setPrice(request.price());
        if (request.listPrice() != null) offer.setListPrice(request.listPrice());
        if (request.cargoProvider() != null) offer.setCargoProvider(request.cargoProvider());
        if (request.cargoPrice() != null) offer.setCargoPrice(request.cargoPrice());
        if (request.estimatedDeliveryDays() != null) offer.setEstimatedDeliveryDays(request.estimatedDeliveryDays());
        if (request.freeShippingThreshold() != null) offer.setFreeShippingThreshold(request.freeShippingThreshold());
        if (request.status() != null) offer.setStatus(request.status());
        offer.normalizeMoney();
        offer = offerRepository.save(offer);

        if (request.price() != null && oldPrice.compareTo(offer.getPrice()) != 0) {
            outboxService.publish(Topics.OFFER_PRICE_CHANGED, EventType.OFFER_PRICE_CHANGED,
                offer.getId().toString(), "OFFER",
                Map.of("offerId", offer.getId(), "productId", offer.getProductId(), "sellerId", offer.getSellerId(),
                    "oldPrice", oldPrice, "newPrice", offer.getPrice(), "currency", offer.getCurrency()));
        }
        if (request.status() != null && oldStatus != offer.getStatus()) {
            outboxService.publish(Topics.OFFER_STATUS_CHANGED, EventType.OFFER_STATUS_CHANGED,
                offer.getId().toString(), "OFFER",
                Map.of("offerId", offer.getId(), "oldStatus", oldStatus, "newStatus", offer.getStatus()));
        }
        return offerMapper.toResponse(offer);
    }

    public OfferResponse getById(UUID id) {
        var offer = offerRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Offer not found"));
        return offerMapper.toResponse(offer);
    }

    public List<OfferResponse> getOffersForProduct(UUID productId) {
        return offerRepository.findByProductIdAndStatusAndDeletedAtIsNull(productId, OfferStatus.ACTIVE)
            .stream()
            .map(offerMapper::toResponse)
            .toList();
    }

    public List<OfferResponse> getMyOffers(UUID sellerId) {
        return offerRepository.findBySellerIdAndDeletedAtIsNull(sellerId)
            .stream()
            .map(offerMapper::toResponse)
            .toList();
    }

    @Transactional
    public void delete(UUID sellerId, UUID offerId) {
        var offer = offerRepository.findByIdAndDeletedAtIsNull(offerId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Offer not found"));
        ensureOwner(offer, sellerId);
        offer.setDeletedAt(Instant.now());
        offer.setStatus(OfferStatus.PAUSED);
        offerRepository.save(offer);
    }

    private void ensureOwner(Offer offer, UUID sellerId) {
        if (!offer.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Cannot modify another seller's offer");
        }
    }

    private String generateSku(UUID productId, UUID sellerId) {
        return "SKU-" + productId.toString().substring(0, 8) + "-" + sellerId.toString().substring(0, 8);
    }
}
