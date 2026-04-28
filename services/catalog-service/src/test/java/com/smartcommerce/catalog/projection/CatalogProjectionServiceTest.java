package com.smartcommerce.catalog.projection;

import com.smartcommerce.catalog.client.ProductClient;
import com.smartcommerce.catalog.domain.ProductDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogProjectionServiceTest {

    @Mock ProductClient productClient;
    @Mock CatalogIndexService indexService;

    @Test
    @DisplayName("rebuildProductDocument computes min/max price from offers")
    void rebuild_computesPriceRange() {
        var productId = UUID.randomUUID();
        var product = new ProductClient.ProductSummary(
            productId, "iPhone 15", "iphone-15", "Telefon", "Apple iPhone 15",
            UUID.randomUUID(), UUID.randomUUID(), Map.of(),
            "ACTIVE", "https://img/1.jpg", List.of(), 0L);
        var offers = List.of(
            offer(productId, new BigDecimal("39999.00")),
            offer(productId, new BigDecimal("41500.00")),
            offer(productId, new BigDecimal("38500.00"))
        );
        when(productClient.getProduct(productId)).thenReturn(product);
        when(productClient.getOffersForProduct(productId)).thenReturn(offers);

        new CatalogProjectionService(productClient, indexService).rebuildProductDocument(productId.toString());

        var captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(indexService).upsertProduct(captor.capture());
        var doc = captor.getValue();
        assertThat(doc.minPrice()).isEqualByComparingTo("38500.00");
        assertThat(doc.maxPrice()).isEqualByComparingTo("41500.00");
        assertThat(doc.offerCount()).isEqualTo(3);
        assertThat(doc.title()).isEqualTo("iPhone 15");
    }

    @Test
    @DisplayName("rebuildProductDocument deletes index entry when no offers remain")
    void rebuild_deletesWhenNoOffers() {
        var productId = UUID.randomUUID();
        when(productClient.getProduct(productId)).thenReturn(new ProductClient.ProductSummary(
            productId, "Test", "test", "desc", "short", null, UUID.randomUUID(),
            Map.of(), "ACTIVE", null, List.of(), 0L));
        when(productClient.getOffersForProduct(productId)).thenReturn(List.of());

        new CatalogProjectionService(productClient, indexService).rebuildProductDocument(productId.toString());

        verify(indexService, times(1)).deleteProduct(productId.toString());
    }

    private ProductClient.OfferSummary offer(UUID productId, BigDecimal price) {
        return new ProductClient.OfferSummary(
            UUID.randomUUID(), productId, UUID.randomUUID(), "SKU",
            price, "TRY", price, "YURTICI", BigDecimal.ZERO, 2,
            BigDecimal.ZERO, "ACTIVE", 0L);
    }
}
