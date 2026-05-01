package com.smartcommerce.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.payment.api.dto.IyzicoCallbackRequest;
import com.smartcommerce.payment.domain.WebhookEvent;
import com.smartcommerce.payment.repository.WebhookEventRepository;
import com.smartcommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Iyzico's 3DS sandbox POSTs back to this endpoint after the bank challenge.
 * We persist the raw event for audit, advance the payment state machine, and
 * then 303-See-Other the customer back to the Angular result page so the
 * browser navigates out of the bank flow into our SPA.
 */
@RestController
@RequestMapping("/api/payments/iyzico")
@RequiredArgsConstructor
@Slf4j
public class IyzicoWebhookController {

    private final PaymentService paymentService;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend.callback-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @PostMapping(value = "/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentId,
            @RequestParam(required = false) String conversationData,
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) String mdStatus) {

        log.info("Iyzico callback received: status={}, paymentId={}, conversationId={}, mdStatus={}",
            status, paymentId, conversationId, mdStatus);

        var payload = new HashMap<String, Object>();
        payload.put("status", status);
        payload.put("paymentId", paymentId);
        payload.put("conversationId", conversationId);
        payload.put("mdStatus", mdStatus);

        var webhookEvent = WebhookEvent.builder()
            .provider("IYZICO")
            .eventType("3DS_CALLBACK")
            .providerEventId(paymentId + ":" + conversationId)
            .payload(objectMapper.valueToTree(payload))
            .signatureValid(true)
            .processed(false)
            .build();
        webhookEvent = webhookEventRepository.save(webhookEvent);

        var resolvedStatus = "failure";
        java.util.UUID internalPaymentId = null;
        try {
            internalPaymentId = paymentService.handle3dsCallback(new IyzicoCallbackRequest(
                status, paymentId, conversationData, conversationId, mdStatus));
            webhookEvent.setProcessed(true);
            webhookEvent.setProcessedAt(Instant.now());
            webhookEventRepository.save(webhookEvent);
            resolvedStatus = "SUCCESS".equalsIgnoreCase(status) ? "success" : "failure";
        } catch (Exception e) {
            log.error("Failed to process callback", e);
            webhookEvent.setProcessingError(e.getMessage());
            webhookEventRepository.save(webhookEvent);
        }

        // Redirect to frontend with INTERNAL UUID (not iyzico's numeric provider id),
        // because the SPA polls GET /api/payments/{uuid} which expects UUID.
        var redirectId = internalPaymentId != null ? internalPaymentId.toString() : paymentId;
        var redirectUri = URI.create(frontendBaseUrl + "/odeme/sonuc/" + redirectId
            + "?status=" + resolvedStatus);
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
            .header(HttpHeaders.LOCATION, redirectUri.toString())
            .build();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
