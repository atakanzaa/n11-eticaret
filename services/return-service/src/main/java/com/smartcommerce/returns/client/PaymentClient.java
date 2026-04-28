package com.smartcommerce.returns.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "return-payment-service", url = "${services.payment.url:http://localhost:8090}")
public interface PaymentClient {

    @PostMapping("/api/payments/{paymentId}/refund")
    @CircuitBreaker(name = "payment-service")
    RefundResponse refund(@PathVariable UUID paymentId, @RequestBody RefundRequest request);

    record RefundRequest(BigDecimal amount, String reason) {}

    record RefundResponse(UUID id, BigDecimal amount, String status) {}
}
