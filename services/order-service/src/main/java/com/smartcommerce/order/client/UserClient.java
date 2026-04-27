package com.smartcommerce.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${services.user.url:http://localhost:8082}")
public interface UserClient {
    @GetMapping("/api/users/internal/addresses/{id}")
    @CircuitBreaker(name = "user-service")
    AddressDetail getAddress(@PathVariable UUID id);

    record AddressDetail(UUID id, String fullName, String phone, String country, String city,
                         String district, String postalCode, String fullAddress) {}
}
