package com.smartcommerce.catalog.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.catalog.client.ProductClient;
import com.smartcommerce.catalog.projection.CatalogIndexService;
import com.smartcommerce.catalog.projection.CatalogProjectionService;
import com.smartcommerce.common.events.BaseEvent;
import com.smartcommerce.common.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogProjectionConsumer {

    private final CatalogProjectionService projectionService;
    private final CatalogIndexService indexService;
    private final ProductClient productClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {Topics.PRODUCT_CREATED, Topics.PRODUCT_UPDATED}, groupId = "catalog-service")
    public void onProductChanged(BaseEvent<?> event) {
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var productId = textValue(payload, "id");
        if (productId == null) productId = textValue(payload, "productId");
        if (productId == null) {
            log.warn("PRODUCT event without id, skipping: {}", event.getEventId());
            return;
        }
        projectionService.rebuildProductDocument(productId);
    }

    @KafkaListener(topics = Topics.PRODUCT_DELETED, groupId = "catalog-service")
    public void onProductDeleted(BaseEvent<?> event) {
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var productId = textValue(payload, "id");
        if (productId == null) productId = textValue(payload, "productId");
        if (productId != null) indexService.deleteProduct(productId);
    }

    @KafkaListener(topics = {
        Topics.OFFER_CREATED, Topics.OFFER_PRICE_CHANGED, Topics.OFFER_STATUS_CHANGED
    }, groupId = "catalog-service")
    public void onOfferChanged(BaseEvent<?> event) {
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var productId = textValue(payload, "productId");
        if (productId != null) projectionService.rebuildProductDocument(productId);
    }

    @KafkaListener(topics = Topics.OFFER_STOCK_CHANGED, groupId = "catalog-service")
    public void onStockChanged(BaseEvent<?> event) {
        var payload = objectMapper.convertValue(event.getPayload(), JsonNode.class);
        var productId = textValue(payload, "productId");
        if (productId != null) {
            projectionService.rebuildProductDocument(productId);
            return;
        }
        var offerIdText = textValue(payload, "offerId");
        if (offerIdText == null) return;
        try {
            var offer = productClient.getOffer(UUID.fromString(offerIdText));
            projectionService.rebuildProductDocument(offer.productId().toString());
        } catch (Exception e) {
            log.error("Failed to resolve offer {} for stock event", offerIdText, e);
        }
    }

    private String textValue(JsonNode payload, String field) {
        if (payload == null) return null;
        var node = payload.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }
}
