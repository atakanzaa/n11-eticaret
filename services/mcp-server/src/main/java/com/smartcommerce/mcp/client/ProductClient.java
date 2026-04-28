package com.smartcommerce.mcp.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "mcp-product-service", url = "${services.product.url:http://localhost:8084}")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    @CircuitBreaker(name = "product-service")
    Object getProduct(@PathVariable UUID id);

    @GetMapping("/api/products/{productId}/offers")
    @CircuitBreaker(name = "product-service")
    Object getOffers(@PathVariable UUID productId);
}
