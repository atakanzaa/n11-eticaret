package com.smartcommerce.mcp.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "catalog-service", url = "${services.catalog.url:http://localhost:8086}")
public interface CatalogClient {

    @GetMapping("/api/search")
    @CircuitBreaker(name = "catalog-service")
    Object search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String categoryId,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    );
}
