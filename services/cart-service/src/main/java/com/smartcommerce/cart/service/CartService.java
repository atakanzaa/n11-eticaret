package com.smartcommerce.cart.service;

import com.smartcommerce.cart.api.dto.*;
import com.smartcommerce.cart.api.mapper.CartMapper;
import com.smartcommerce.cart.client.*;
import com.smartcommerce.cart.domain.*;
import com.smartcommerce.cart.event.outbox.OutboxService;
import com.smartcommerce.cart.repository.*;
import com.smartcommerce.common.errors.*;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private static final String CART_KEY_PREFIX = "cart:user:";
    private static final Duration CART_TTL = Duration.ofDays(7);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductClient productClient;
    private final SellerClient sellerClient;
    private final PromotionClient promotionClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OutboxService outboxService;
    private final CartMapper cartMapper;

    public CartResponse getCart(UUID userId) {
        var cached = redisTemplate.opsForValue().get(cartKey(userId));
        if (cached instanceof CartResponse response) {
            return response;
        }
        var cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
            .orElseGet(() -> createCart(userId));
        var response = cartMapper.toResponse(cart);
        writeCache(userId, response);
        return response;
    }

    @Transactional
    public CartResponse addItem(UUID userId, AddItemRequest request) {
        var cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
            .orElseGet(() -> createCart(userId));
        var offer = productClient.getOffer(request.offerId());
        if (offer.status() != ProductClient.OfferStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.OFFER_UNAVAILABLE, "Offer is not active");
        }
        var product = productClient.getProduct(offer.productId());
        var seller = sellerClient.getSeller(offer.sellerId());

        // Campaign override: aktif "Kısa Süreli Teklif" varsa indirimli fiyat uygula
        var effectivePrice = offer.price();
        try {
            var campaign = promotionClient.activeCampaign(offer.id());
            if (campaign != null && campaign.active() && campaign.discountedPrice() != null) {
                effectivePrice = campaign.discountedPrice();
                log.info("Active campaign applied for offer {}: {} -> {}",
                    offer.id(), offer.price(), effectivePrice);
            }
        } catch (Exception e) {
            log.debug("No active campaign for offer {} (or service unavailable): {}",
                offer.id(), e.getMessage());
        }

        var existingItem = cartItemRepository.findByCartIdAndOfferId(cart.getId(), request.offerId());

        CartItem item;
        if (existingItem.isPresent()) {
            item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.quantity());
            item.setUnitPriceSnapshot(effectivePrice);
        } else {
            item = CartItem.builder()
                .cart(cart)
                .offerId(offer.id())
                .productId(offer.productId())
                .sellerId(offer.sellerId())
                .quantity(request.quantity())
                .unitPriceSnapshot(effectivePrice)
                .currencySnapshot(offer.currency())
                .productTitleSnapshot(product.title())
                .productImageSnapshot(product.primaryImageUrl())
                .sellerNameSnapshot(seller.storeName())
                .cargoPriceSnapshot(offer.cargoPrice())
                .build();
            cart.getItems().add(item);
        }
        cartItemRepository.save(item);
        outboxService.publish(Topics.CART_ITEM_ADDED, EventType.CART_ITEM_ADDED,
            cart.getId().toString(), "CART",
            Map.of("userId", userId, "cartId", cart.getId(), "offerId", offer.id(),
                "productId", offer.productId(), "quantity", request.quantity()));
        invalidateCache(userId);
        return cartMapper.toResponse(cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE).orElseThrow());
    }

    @Transactional
    public CartResponse updateQuantity(UUID userId, UUID offerId, int quantity) {
        if (quantity <= 0) {
            return removeItem(userId, offerId);
        }
        var cart = getActiveCart(userId);
        var item = cartItemRepository.findByCartIdAndOfferId(cart.getId(), offerId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Item not in cart"));
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        invalidateCache(userId);
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID offerId) {
        var cart = getActiveCart(userId);
        cartItemRepository.deleteByCartIdAndOfferId(cart.getId(), offerId);
        cart.getItems().removeIf(item -> item.getOfferId().equals(offerId));
        invalidateCache(userId);
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        var cart = getActiveCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        invalidateCache(userId);
    }

    @Transactional(readOnly = true)
    public CartValidationResponse validateCart(UUID userId) {
        var cart = getActiveCart(userId);
        var issues = new ArrayList<CartValidationIssue>();
        for (var item : cartItemRepository.findByCartId(cart.getId())) {
            try {
                var offer = productClient.getOffer(item.getOfferId());
                if (offer.status() != ProductClient.OfferStatus.ACTIVE) {
                    issues.add(new CartValidationIssue(item.getOfferId(), "OFFER_UNAVAILABLE", "This offer is no longer available"));
                    continue;
                }
                if (offer.price().compareTo(item.getUnitPriceSnapshot()) != 0) {
                    issues.add(new CartValidationIssue(item.getOfferId(), "PRICE_CHANGED",
                        "Price changed from " + item.getUnitPriceSnapshot() + " to " + offer.price()));
                }
            } catch (Exception e) {
                issues.add(new CartValidationIssue(item.getOfferId(), "OFFER_FETCH_FAILED", e.getMessage()));
            }
        }
        return new CartValidationResponse(cartMapper.toResponse(cart), issues);
    }

    private Cart createCart(UUID userId) {
        var cart = Cart.builder()
            .userId(userId)
            .status(CartStatus.ACTIVE)
            .build();
        return cartRepository.save(cart);
    }

    private Cart getActiveCart(UUID userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CART_NOT_FOUND, "Cart not found"));
    }

    private void writeCache(UUID userId, CartResponse response) {
        try {
            redisTemplate.opsForValue().set(cartKey(userId), response, CART_TTL);
        } catch (Exception e) {
            log.warn("Cart cache write failed for user {}: {}", userId, e.getMessage());
        }
    }

    private void invalidateCache(UUID userId) {
        try {
            redisTemplate.delete(cartKey(userId));
        } catch (Exception e) {
            log.warn("Cart cache invalidation failed for user {}: {}", userId, e.getMessage());
        }
    }

    private String cartKey(UUID userId) {
        return CART_KEY_PREFIX + userId;
    }
}
