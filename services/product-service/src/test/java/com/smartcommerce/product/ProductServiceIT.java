package com.smartcommerce.product;

import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.test.AbstractIntegrationTest;
import com.smartcommerce.product.api.dto.*;
import com.smartcommerce.product.domain.*;
import com.smartcommerce.product.repository.OutboxRepository;
import com.smartcommerce.product.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductServiceIT extends AbstractIntegrationTest {
    private static final UUID ELECTRONICS_CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired ProductService productService;
    @Autowired OfferService offerService;
    @Autowired OutboxRepository outboxRepository;

    @Test
    @DisplayName("create product, create offer, publish events and list active offers")
    void productOfferFullFlow() {
        var sellerId = UUID.randomUUID();
        var product = productService.create(new CreateProductRequest(
            "Akilli Telefon Çığır", "A flagship phone", "Short", null,
            ELECTRONICS_CATEGORY_ID, Map.of("color", "black"), null));

        var offer = offerService.create(sellerId, new CreateOfferRequest(
            product.id(), "SKU-TEST-1", new BigDecimal("100.00"), new BigDecimal("120.00"),
            CargoProvider.ARAS, BigDecimal.ZERO, 2, new BigDecimal("500.00"), 0));

        var active = offerService.update(sellerId, offer.id(), new UpdateOfferRequest(
            null, null, null, null, null, null, OfferStatus.ACTIVE));
        var updated = offerService.update(sellerId, offer.id(), new UpdateOfferRequest(
            new BigDecimal("150.00"), null, null, null, null, null, null));

        assertThat(product.slug()).isEqualTo("akilli-telefon-cigir");
        assertThat(active.status()).isEqualTo(OfferStatus.ACTIVE);
        assertThat(updated.price()).isEqualByComparingTo("150.00");
        assertThat(offerService.getOffersForProduct(product.id())).hasSize(1);
        assertThat(outboxRepository.findAll()).extracting("eventType")
            .contains(EventType.PRODUCT_CREATED, EventType.OFFER_CREATED,
                EventType.OFFER_STATUS_CHANGED, EventType.OFFER_PRICE_CHANGED);
    }

    @Test
    @DisplayName("seller cannot modify another seller's offer")
    void crossSellerAccess_forbidden() {
        var sellerId = UUID.randomUUID();
        var otherSellerId = UUID.randomUUID();
        var product = productService.create(new CreateProductRequest(
            "Laptop Test", null, null, null, ELECTRONICS_CATEGORY_ID, Map.of(), null));
        var offer = offerService.create(sellerId, new CreateOfferRequest(
            product.id(), null, new BigDecimal("200.00"), null, CargoProvider.DEFAULT,
            BigDecimal.ZERO, 3, null, 0));

        assertThatThrownBy(() -> offerService.update(otherSellerId, offer.id(), new UpdateOfferRequest(
            new BigDecimal("210.00"), null, null, null, null, null, null)))
            .hasMessageContaining("Cannot modify another seller's offer");
    }

    @Test
    @DisplayName("duplicate offer for same product and seller fails")
    void duplicateOffer_conflict() {
        var sellerId = UUID.randomUUID();
        var product = productService.create(new CreateProductRequest(
            "Tablet Test", null, null, null, ELECTRONICS_CATEGORY_ID, Map.of(), null));
        var request = new CreateOfferRequest(product.id(), null, new BigDecimal("300.00"),
            null, CargoProvider.DEFAULT, BigDecimal.ZERO, 3, null, 0);

        offerService.create(sellerId, request);

        assertThatThrownBy(() -> offerService.create(sellerId, request))
            .hasMessageContaining("You already have an offer for this product");
    }
}
