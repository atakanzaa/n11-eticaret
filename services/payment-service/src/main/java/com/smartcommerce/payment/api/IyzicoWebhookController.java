package com.smartcommerce.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.payment.api.dto.IyzicoCallbackRequest;
import com.smartcommerce.payment.domain.WebhookEvent;
import com.smartcommerce.payment.repository.WebhookEventRepository;
import com.smartcommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/iyzico")
@RequiredArgsConstructor
@Slf4j
public class IyzicoWebhookController {

    private final PaymentService paymentService;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;

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

        try {
            paymentService.handle3dsCallback(new IyzicoCallbackRequest(status, paymentId,
                conversationData, conversationId, mdStatus));
            webhookEvent.setProcessed(true);
            webhookEvent.setProcessedAt(Instant.now());
            webhookEventRepository.save(webhookEvent);
        } catch (Exception e) {
            log.error("Failed to process callback", e);
            webhookEvent.setProcessingError(e.getMessage());
            webhookEventRepository.save(webhookEvent);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
