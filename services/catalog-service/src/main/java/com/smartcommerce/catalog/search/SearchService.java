package com.smartcommerce.catalog.search;

import com.smartcommerce.catalog.api.dto.SearchQuery;
import com.smartcommerce.catalog.api.dto.SearchResultResponse;
import com.smartcommerce.catalog.domain.ProductDocument;
import com.smartcommerce.catalog.projection.CatalogIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.SearchRequest;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final OpenSearchClient client;

    public SearchResultResponse search(SearchQuery query) {
        var request = SearchRequest.of(b -> {
            b.index(CatalogIndexService.INDEX);
            b.query(q -> q.bool(bq -> {
                if (query.queryText() != null && !query.queryText().isBlank()) {
                    bq.must(m -> m.multiMatch(mm -> mm
                        .query(query.queryText())
                        .fields("title^3", "description", "categoryName", "brandName")
                        .fuzziness("AUTO")));
                }
                if (query.categoryId() != null) {
                    bq.filter(f -> f.term(t -> t.field("categoryId").value(FieldValue.of(query.categoryId()))));
                }
                if (query.brandId() != null) {
                    bq.filter(f -> f.term(t -> t.field("brandId").value(FieldValue.of(query.brandId()))));
                }
                if (query.minPrice() != null || query.maxPrice() != null) {
                    bq.filter(f -> f.range(r -> {
                        r.field("minPrice");
                        if (query.minPrice() != null) r.gte(JsonData.of(query.minPrice()));
                        if (query.maxPrice() != null) r.lte(JsonData.of(query.maxPrice()));
                        return r;
                    }));
                }
                if (Boolean.TRUE.equals(query.inStockOnly())) {
                    bq.filter(f -> f.term(t -> t.field("hasStock").value(FieldValue.of(true))));
                }
                return bq;
            }));

            b.from(query.page() * query.size()).size(query.size());

            switch (query.sort() == null ? "" : query.sort()) {
                case "price_asc" -> b.sort(s -> s.field(f -> f.field("minPrice").order(SortOrder.Asc)));
                case "price_desc" -> b.sort(s -> s.field(f -> f.field("minPrice").order(SortOrder.Desc)));
                case "rating" -> b.sort(s -> s.field(f -> f.field("averageRating").order(SortOrder.Desc)));
                case "newest" -> b.sort(s -> s.field(f -> f.field("createdAt").order(SortOrder.Desc)));
                default -> { /* relevance — no sort */ }
            }

            b.aggregations("by_category", a -> a.terms(t -> t.field("categoryName.keyword").size(20)));
            b.aggregations("by_brand", a -> a.terms(t -> t.field("brandName").size(20)));

            return b;
        });

        try {
            var response = client.search(request, ProductDocument.class);
            var hits = response.hits().hits().stream().map(h -> h.source()).toList();
            var total = response.hits().total() != null ? response.hits().total().value() : 0L;
            var facets = extractFacets(response.aggregations());
            return new SearchResultResponse(hits, total, query.page(), query.size(), facets);
        } catch (IOException e) {
            log.error("Search failed", e);
            return new SearchResultResponse(List.of(), 0L, query.page(), query.size(), Map.of());
        }
    }

    private Map<String, Map<String, Long>> extractFacets(Map<String, org.opensearch.client.opensearch._types.aggregations.Aggregate> aggregations) {
        var facets = new LinkedHashMap<String, Map<String, Long>>();
        if (aggregations == null) return facets;
        for (var entry : aggregations.entrySet()) {
            var agg = entry.getValue();
            if (agg.isSterms()) {
                var buckets = agg.sterms().buckets().array();
                facets.put(entry.getKey(), buckets.stream()
                    .collect(Collectors.toMap(StringTermsBucket::key, StringTermsBucket::docCount,
                        (a, b) -> a, LinkedHashMap::new)));
            }
        }
        return facets;
    }
}
