package com.smartcommerce.fraud.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Instant;
import java.util.UUID;

@FeignClient(name = "fraud-user-service", url = "${services.user.url:http://localhost:8082}")
public interface UserClient {

    @GetMapping("/api/users/internal/{userId}")
    @CircuitBreaker(name = "user-service")
    UserProfile getProfile(@PathVariable UUID userId);

    record UserProfile(
        UUID id,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String phone,
        Instant createdAt
    ) {}
}
