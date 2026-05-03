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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
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

    /**
     * Optional shared secret to verify the redirect was issued by Iyzico (or an
     * upstream proxy that re-signs callbacks). Empty in sandbox; required in
     * prod. When set, the request must carry header X-Iyzico-Signature with the
     * HMAC-SHA256 hex digest of "{paymentId}:{conversationId}:{status}".
     */
    @Value("${iyzico.webhook.secret:}")
    private String webhookSecret;

    @PostMapping(value = "/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentId,
            @RequestParam(required = false) String conversationData,
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) String mdStatus,
            @RequestHeader(value = "X-Iyzico-Signature", required = false) String signature) {

        log.info("Iyzico callback received: status={}, paymentId={}, conversationId={}, mdStatus={}",
            status, paymentId, conversationId, mdStatus);

        if (webhookSecret != null && !webhookSecret.isBlank()) {
            if (!verifySignature(paymentId, conversationId, status, signature)) {
                log.warn("Iyzico callback signature verification FAILED — paymentId={}, conversationId={}",
                    paymentId, conversationId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } else {
            log.warn("iyzico.webhook.secret not configured — skipping signature check (DEV ONLY)");
        }

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

    private boolean verifySignature(String paymentId, String conversationId, String status, String signature) {
        if (signature == null || signature.isBlank()) return false;
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var payload = (paymentId == null ? "" : paymentId) + ":"
                + (conversationId == null ? "" : conversationId) + ":"
                + (status == null ? "" : status);
            var expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return constantTimeEquals(expected, signature);
        } catch (Exception e) {
            log.error("Signature verification crashed", e);
            return false;
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }
}
