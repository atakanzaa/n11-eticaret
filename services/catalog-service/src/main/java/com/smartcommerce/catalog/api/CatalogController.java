package com.smartcommerce.catalog.api;

import com.smartcommerce.catalog.api.dto.SearchQuery;
import com.smartcommerce.catalog.api.dto.SearchResultResponse;
import com.smartcommerce.catalog.domain.ProductDocument;
import com.smartcommerce.catalog.projection.CatalogIndexService;
import com.smartcommerce.catalog.projection.CatalogProjectionService;
import com.smartcommerce.catalog.search.SearchService;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {

    private final SearchService searchService;
    private final CatalogIndexService indexService;
    private final CatalogProjectionService projectionService;

    @GetMapping("/search")
    public SearchResultResponse search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "false") Boolean inStockOnly,
            @RequestParam(required = false, defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new SearchQuery(q,
            categoryId != null ? categoryId.toString() : null,
            brandId != null ? brandId.toString() : null,
            minPrice, maxPrice, inStockOnly, sort, page, size);
        return searchService.search(query);
    }

    @GetMapping("/catalog/products/{id}")
    public ProductDocument getProduct(@PathVariable String id) {
        return indexService.getProduct(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found in catalog"));
    }

    @PostMapping("/catalog/internal/products/{id}/reindex")
    public void reindex(@PathVariable String id) {
        projectionService.rebuildProductDocument(id);
    }
}
