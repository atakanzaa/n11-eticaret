package com.smartcommerce.returns.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "return-inventory-service", url = "${services.inventory.url:http://localhost:8085}")
public interface InventoryClient {

    @PostMapping("/api/inventory/internal/offers/{offerId}/restock")
    @CircuitBreaker(name = "inventory-service")
    Object restock(@PathVariable UUID offerId,
                   @RequestParam int quantity,
                   @RequestParam(defaultValue = "RETURN_RESTOCK") String reason);
}
