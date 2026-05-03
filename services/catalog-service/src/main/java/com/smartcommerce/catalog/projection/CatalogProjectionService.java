package com.smartcommerce.catalog.projection;

import com.smartcommerce.catalog.client.InventoryClient;
import com.smartcommerce.catalog.client.ProductClient;
import com.smartcommerce.catalog.domain.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogProjectionService {

    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final CatalogIndexService indexService;

    public void rebuildProductDocument(String productId) {
        try {
            var product = productClient.getProduct(UUID.fromString(productId));
            var offers = productClient.getOffersForProduct(UUID.fromString(productId));

            if (offers == null || offers.isEmpty()) {
                indexService.deleteProduct(productId);
                return;
            }

            var minPrice = offers.stream().map(ProductClient.OfferSummary::price)
                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            var maxPrice = offers.stream().map(ProductClient.OfferSummary::price)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            var sellerIds = offers.stream()
                .map(ProductClient.OfferSummary::sellerId)
                .filter(java.util.Objects::nonNull)
                .map(UUID::toString)
                .distinct()
                .toList();

            // Aggregate available stock across all offers. Inventory lookup is
            // best-effort — if it fails, we fall back to "stock unknown but
            // probably-available" so search results aren't accidentally hidden.
            int totalStock = 0;
            try {
                var offerIds = offers.stream()
                    .map(ProductClient.OfferSummary::id)
                    .filter(java.util.Objects::nonNull)
                    .toList();
                if (!offerIds.isEmpty()) {
                    var items = inventoryClient.byOfferIds(offerIds);
                    if (items != null) {
                        totalStock = items.stream()
                            .mapToInt(InventoryClient.InventorySummary::availableQuantity)
                            .sum();
                    }
                }
            } catch (Exception e) {
                log.warn("Inventory aggregate failed for product {}, indexing with totalStock=0: {}",
                    productId, e.getMessage());
            }

            float averageRating = product.averageRating() != null
                ? product.averageRating().floatValue() : 0f;
            int reviewCount = product.reviewCount() != null
                ? product.reviewCount().intValue() : 0;

            var doc = new ProductDocument(
                productId,
                product.title(),
                product.description(),
                product.categoryId().toString(),
                null,
                product.brandId() != null ? product.brandId().toString() : null,
                null,
                sellerIds,
                product.attributes(),
                minPrice,
                maxPrice,
                offers.size(),
                totalStock,
                totalStock > 0,
                averageRating,
                reviewCount,
                product.primaryImageUrl(),
                product.status(),
                Instant.now(),
                Instant.now()
            );
            indexService.upsertProduct(doc);
        } catch (Exception e) {
            log.error("Failed to rebuild product document for {}", productId, e);
        }
    }
}
