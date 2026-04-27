package com.smartcommerce.notification.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${services.user.url:http://localhost:8082}")
public interface UserClient {
    @GetMapping("/api/users/internal/{userId}")
    @CircuitBreaker(name = "user-service")
    UserProfile getProfile(@PathVariable UUID userId);

    record UserProfile(UUID id, UUID userId, String email, String firstName, String lastName,
                       String preferredLanguage, String preferredCurrency) {
        public String displayName() {
            var name = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
            return name.isBlank() ? email : name;
        }
    }
}
