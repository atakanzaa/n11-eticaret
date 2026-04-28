package com.smartcommerce.catalog.projection;

import com.smartcommerce.catalog.domain.ProductDocument;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.analysis.Analyzer;
import org.opensearch.client.opensearch._types.analysis.CustomAnalyzer;
import org.opensearch.client.opensearch._types.analysis.StemmerTokenFilter;
import org.opensearch.client.opensearch._types.analysis.StopTokenFilter;
import org.opensearch.client.opensearch._types.analysis.TokenFilter;
import org.opensearch.client.opensearch._types.analysis.TokenFilterDefinition;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogIndexService {

    public static final String INDEX = "products";

    private final OpenSearchClient client;

    @PostConstruct
    public void initializeIndex() {
        try {
            var exists = client.indices().exists(b -> b.index(INDEX)).value();
            if (!exists) {
                client.indices().create(b -> b
                    .index(INDEX)
                    .settings(s -> s
                        .numberOfShards("1")
                        .numberOfReplicas("0")
                        .analysis(buildAnalysis()))
                    .mappings(buildMappings()));
                log.info("Created OpenSearch index: {}", INDEX);
            }
        } catch (Exception e) {
            log.warn("OpenSearch index initialization skipped (cluster may be down): {}", e.getMessage());
        }
    }

    private IndexSettingsAnalysis buildAnalysis() {
        var analyzer = Analyzer.of(a -> a.custom(CustomAnalyzer.of(c -> c
            .tokenizer("standard")
            .filter("lowercase", "turkish_stop", "turkish_stemmer", "asciifolding"))));
        var stop = TokenFilter.of(t -> t.definition(TokenFilterDefinition.of(d -> d
            .stop(StopTokenFilter.of(s -> s.stopwords("_turkish_"))))));
        var stemmer = TokenFilter.of(t -> t.definition(TokenFilterDefinition.of(d -> d
            .stemmer(StemmerTokenFilter.of(s -> s.language("turkish"))))));
        return IndexSettingsAnalysis.of(b -> b
            .analyzer("turkish_analyzer", analyzer)
            .filter("turkish_stop", stop)
            .filter("turkish_stemmer", stemmer));
    }

    private TypeMapping buildMappings() {
        return TypeMapping.of(m -> m.properties(Map.of(
            "productId", Property.of(p -> p.keyword(k -> k)),
            "title", Property.of(p -> p.text(t -> t.analyzer("turkish_analyzer")
                .fields("keyword", Property.of(f -> f.keyword(k -> k))))),
            "description", Property.of(p -> p.text(t -> t.analyzer("turkish_analyzer"))),
            "categoryId", Property.of(p -> p.keyword(k -> k)),
            "categoryName", Property.of(p -> p.text(t -> t.analyzer("turkish_analyzer")
                .fields("keyword", Property.of(f -> f.keyword(k -> k))))),
            "brandId", Property.of(p -> p.keyword(k -> k)),
            "brandName", Property.of(p -> p.keyword(k -> k)),
            "minPrice", Property.of(p -> p.scaledFloat(s -> s.scalingFactor(100.0))),
            "maxPrice", Property.of(p -> p.scaledFloat(s -> s.scalingFactor(100.0)))
        )));
    }

    public void upsertProduct(ProductDocument doc) {
        try {
            client.index(b -> b.index(INDEX).id(doc.productId()).document(doc));
            log.debug("Indexed product {}", doc.productId());
        } catch (IOException | OpenSearchException e) {
            log.error("Failed to index product {}", doc.productId(), e);
        }
    }

    public void deleteProduct(String productId) {
        try {
            client.delete(b -> b.index(INDEX).id(productId));
        } catch (IOException | OpenSearchException e) {
            log.error("Failed to delete product {}", productId, e);
        }
    }

    public Optional<ProductDocument> getProduct(String productId) {
        try {
            var resp = client.get(b -> b.index(INDEX).id(productId), ProductDocument.class);
            return resp.found() ? Optional.ofNullable(resp.source()) : Optional.empty();
        } catch (IOException | OpenSearchException e) {
            log.error("Failed to fetch product {}", productId, e);
            return Optional.empty();
        }
    }
}
