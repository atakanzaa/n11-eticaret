package com.smartcommerce.payment.api;

import com.smartcommerce.payment.api.dto.InitiatePaymentRequest;
import com.smartcommerce.payment.api.dto.InitiatePaymentResponse;
import com.smartcommerce.payment.api.dto.PaymentResponse;
import com.smartcommerce.payment.api.dto.RefundRequest;
import com.smartcommerce.payment.api.dto.RefundResponse;
import com.smartcommerce.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public InitiatePaymentResponse initiate(@AuthenticationPrincipal String userId,
                                            @Valid @RequestBody InitiatePaymentRequest request,
                                            HttpServletRequest httpRequest) {
        return paymentService.initiatePayment(request.withUserIp(extractIp(httpRequest)));
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable UUID id) {
        return paymentService.getById(id);
    }

    @GetMapping("/by-order/{orderId}")
    public PaymentResponse getByOrder(@PathVariable UUID orderId) {
        return paymentService.getByOrderId(orderId);
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT')")
    public RefundResponse refund(@PathVariable UUID id, @Valid @RequestBody RefundRequest request) {
        return paymentService.refund(id, request);
    }

    /**
     * Internal endpoint used by other services (e.g. order-service when a user
     * cancels a CONFIRMED order). Permitted via SecurityConfig matcher
     * `/api/payments/internal/**`. Network isolation between services is the
     * trust boundary for this path.
     */
    @PostMapping("/internal/{id}/refund")
    public RefundResponse internalRefund(@PathVariable UUID id, @Valid @RequestBody RefundRequest request) {
        return paymentService.refund(id, request);
    }

    private String extractIp(HttpServletRequest request) {
        var xff = request.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
