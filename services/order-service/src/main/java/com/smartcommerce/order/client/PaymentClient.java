package com.smartcommerce.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "payment-service", url = "${services.payment.url:http://localhost:8086}")
public interface PaymentClient {

    @GetMapping("/api/payments/by-order/{orderId}")
    @CircuitBreaker(name = "payment-service")
    PaymentSummary getByOrderId(@PathVariable UUID orderId);

    @PostMapping("/api/payments/internal/{paymentId}/refund")
    @CircuitBreaker(name = "payment-service")
    RefundResponse refund(@PathVariable UUID paymentId, @RequestBody RefundRequest request);

    record PaymentSummary(UUID id, UUID orderId, String status, BigDecimal amount, String currency) {}
    record RefundRequest(BigDecimal amount, String reason) {}
    record RefundResponse(UUID paymentId, BigDecimal amount, String status) {}
}
