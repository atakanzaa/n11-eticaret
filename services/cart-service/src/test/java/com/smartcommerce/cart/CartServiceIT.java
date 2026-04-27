package com.smartcommerce.cart;

import com.smartcommerce.cart.api.dto.AddItemRequest;
import com.smartcommerce.cart.client.*;
import com.smartcommerce.cart.domain.CartStatus;
import com.smartcommerce.cart.repository.OutboxRepository;
import com.smartcommerce.cart.service.CartService;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.test.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CartServiceIT extends AbstractIntegrationTest {
    @Autowired CartService cartService;
    @Autowired RedisTemplate<String, Object> redisTemplate;
    @Autowired OutboxRepository outboxRepository;

    @MockBean ProductClient productClient;
    @MockBean SellerClient sellerClient;

    @Test
    @DisplayName("add item captures snapshot and validation detects later price change")
    void priceSnapshotPreserved() {
        var userId = UUID.randomUUID();
        var offerId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var sellerId = UUID.randomUUID();

        mockOffer(offerId, productId, sellerId, new BigDecimal("100.00"), ProductClient.OfferStatus.ACTIVE);
        when(productClient.getProduct(productId)).thenReturn(new ProductClient.ProductDetail(productId, "Phone", "https://cdn.test/phone.jpg"));
        when(sellerClient.getSeller(sellerId)).thenReturn(new SellerClient.SellerInfo(sellerId, UUID.randomUUID(), "Test Store", "ACTIVE"));

        var cart = cartService.addItem(userId, new AddItemRequest(offerId, 2));
        mockOffer(offerId, productId, sellerId, new BigDecimal("150.00"), ProductClient.OfferStatus.ACTIVE);
        var validation = cartService.validateCart(userId);

        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().getFirst().unitPriceSnapshot()).isEqualByComparingTo("100.00");
        assertThat(validation.issues()).anySatisfy(issue -> assertThat(issue.code()).isEqualTo("PRICE_CHANGED"));
    }

    @Test
    @DisplayName("cart cached in Redis after first fetch")
    void cartCaching() {
        var userId = UUID.randomUUID();

        var cart = cartService.getCart(userId);

        assertThat(cart.status()).isEqualTo(CartStatus.ACTIVE);
        assertThat(redisTemplate.hasKey("cart:user:" + userId)).isTrue();
    }

    @Test
    @DisplayName("CART_ITEM_ADDED event is written to outbox")
    void cartItemAdded_publishesEvent() {
        var userId = UUID.randomUUID();
        var offerId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var sellerId = UUID.randomUUID();

        mockOffer(offerId, productId, sellerId, new BigDecimal("90.00"), ProductClient.OfferStatus.ACTIVE);
        when(productClient.getProduct(productId)).thenReturn(new ProductClient.ProductDetail(productId, "Keyboard", null));
        when(sellerClient.getSeller(sellerId)).thenReturn(new SellerClient.SellerInfo(sellerId, UUID.randomUUID(), "Input Store", "ACTIVE"));

        cartService.addItem(userId, new AddItemRequest(offerId, 1));

        assertThat(outboxRepository.findAll()).extracting("eventType").contains(EventType.CART_ITEM_ADDED);
    }

    private void mockOffer(UUID offerId, UUID productId, UUID sellerId, BigDecimal price, ProductClient.OfferStatus status) {
        when(productClient.getOffer(offerId)).thenReturn(new ProductClient.OfferDetail(
            offerId, productId, sellerId, price, "TRY", BigDecimal.ZERO, status));
    }
}
