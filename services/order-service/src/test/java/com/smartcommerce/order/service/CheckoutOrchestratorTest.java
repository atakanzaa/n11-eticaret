package com.smartcommerce.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.errors.*;
import com.smartcommerce.order.api.dto.*;
import com.smartcommerce.order.api.mapper.OrderMapper;
import com.smartcommerce.order.client.*;
import com.smartcommerce.order.domain.IdempotencyKey;
import com.smartcommerce.order.event.outbox.OutboxService;
import com.smartcommerce.order.repository.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutOrchestratorTest {
    @Mock CartClient cartClient;
    @Mock UserClient userClient;
    @Mock InventoryClient inventoryClient;
    @Mock FraudDetectionClient fraudDetectionClient;
    @Mock PromotionClient promotionClient;
    @Mock PaymentClient paymentClient;
    @Mock OrderRepository orderRepository;
    @Mock SagaLogRepository sagaLogRepository;
    @Mock IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock OutboxService outboxService;

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    CheckoutOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new CheckoutOrchestrator(cartClient, userClient, inventoryClient, fraudDetectionClient,
            promotionClient, paymentClient, orderRepository, sagaLogRepository, idempotencyKeyRepository, outboxService,
            new OrderMapper(), objectMapper, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("idempotency key conflict stops checkout before external calls")
    void checkout_idempotencyConflict() {
        var userId = UUID.randomUUID();
        var request = new CheckoutRequest(UUID.randomUUID(), "MOCK_CARD", null);
        when(idempotencyKeyRepository.findById("idem-1")).thenReturn(Optional.of(IdempotencyKey.builder()
            .key("idem-1")
            .requestHash("different")
            .responseStatus(201)
            .responseBody(objectMapper.createObjectNode())
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build()));

        assertThatThrownBy(() -> orchestrator.checkout(userId, request, "idem-1"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Idempotency key");

        verifyNoInteractions(cartClient, userClient, inventoryClient, promotionClient);
    }

    @Test
    @DisplayName("reused idempotency key returns stored checkout response")
    void checkout_idempotencyReplay() {
        var userId = UUID.randomUUID();
        var request = new CheckoutRequest(UUID.randomUUID(), "MOCK_CARD", null);
        var response = new CheckoutResponse(UUID.randomUUID(), "SC123", "PAYMENT_PENDING",
            new BigDecimal("120.00"), Instant.now());
        var hash = DigestUtils.md5DigestAsHex((userId + ":" + request.addressId() + ":" + request.paymentMethod())
            .getBytes(StandardCharsets.UTF_8));
        when(idempotencyKeyRepository.findById("idem-1")).thenReturn(Optional.of(IdempotencyKey.builder()
            .key("idem-1")
            .requestHash(hash)
            .responseStatus(201)
            .responseBody(objectMapper.valueToTree(response))
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build()));

        var replayed = orchestrator.checkout(userId, request, "idem-1");

        assertThat(replayed.orderNumber()).isEqualTo("SC123");
        assertThat(replayed.grandTotal()).isEqualByComparingTo("120.00");
        verifyNoInteractions(cartClient, userClient, inventoryClient, promotionClient);
    }
}
