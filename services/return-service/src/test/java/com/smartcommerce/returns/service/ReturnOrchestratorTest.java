package com.smartcommerce.returns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.returns.api.dto.CreateReturnRequest;
import com.smartcommerce.returns.api.mapper.ReturnMapper;
import com.smartcommerce.returns.client.InventoryClient;
import com.smartcommerce.returns.client.OrderClient;
import com.smartcommerce.returns.client.PaymentClient;
import com.smartcommerce.returns.domain.ReturnEntity;
import com.smartcommerce.returns.domain.ReturnItem;
import com.smartcommerce.returns.domain.ReturnReason;
import com.smartcommerce.returns.domain.ReturnSagaLog;
import com.smartcommerce.returns.domain.ReturnStatus;
import com.smartcommerce.returns.event.outbox.OutboxService;
import com.smartcommerce.returns.repository.ReturnItemRepository;
import com.smartcommerce.returns.repository.ReturnRepository;
import com.smartcommerce.returns.repository.ReturnSagaLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnOrchestratorTest {

    @Mock ReturnRepository returnRepository;
    @Mock ReturnItemRepository returnItemRepository;
    @Mock ReturnSagaLogRepository sagaLogRepository;
    @Mock OrderClient orderClient;
    @Mock PaymentClient paymentClient;
    @Mock InventoryClient inventoryClient;
    @Mock OutboxService outboxService;

    ReturnOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new ReturnOrchestrator(
            returnRepository, returnItemRepository, sagaLogRepository,
            orderClient, paymentClient, inventoryClient,
            outboxService, new ReturnMapper(), new ObjectMapper());
    }

    @Test
    @DisplayName("requestReturn creates return entity, saves items, publishes RETURN_REQUESTED")
    void requestReturn_happyPath() {
        var userId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var offerId = UUID.randomUUID();

        when(orderClient.getOrder(orderId)).thenReturn(orderDetail(orderId, userId, paymentId, offerId));
        when(returnRepository.save(any(ReturnEntity.class))).thenAnswer(inv -> {
            ReturnEntity r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(returnRepository.findById(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return Optional.of(ReturnEntity.builder().id(id).orderId(orderId).userId(userId)
                .paymentId(paymentId).reasonCode("DEFECTIVE").status(ReturnStatus.REQUESTED)
                .refundAmount(new BigDecimal("100.00")).build());
        });
        when(returnItemRepository.findByReturnId(any())).thenReturn(List.of());

        var request = new CreateReturnRequest(orderId, ReturnReason.DEFECTIVE, "broken",
            List.of(new CreateReturnRequest.ReturnItemRequest(offerId, 1)));

        var response = orchestrator.requestReturn(userId, request);

        assertThat(response.status()).isEqualTo(ReturnStatus.REQUESTED);
        verify(returnItemRepository).save(any(ReturnItem.class));
        verify(outboxService).publish(eq("return.requested.v1"), eq("RETURN_REQUESTED"),
            anyString(), eq("RETURN"), anyMap());
    }

    @Test
    @DisplayName("requestReturn rejects another user's order with FORBIDDEN")
    void requestReturn_otherUsersOrderForbidden() {
        var orderId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var attackerId = UUID.randomUUID();
        when(orderClient.getOrder(orderId)).thenReturn(orderDetail(orderId, ownerId, UUID.randomUUID(), UUID.randomUUID()));

        var request = new CreateReturnRequest(orderId, ReturnReason.OTHER, "x",
            List.of(new CreateReturnRequest.ReturnItemRequest(UUID.randomUUID(), 1)));

        assertThatThrownBy(() -> orchestrator.requestReturn(attackerId, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("another user");
    }

    @Test
    @DisplayName("requestReturn rejects quantity over ordered")
    void requestReturn_quantityOverOrdered() {
        var userId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var offerId = UUID.randomUUID();
        when(orderClient.getOrder(orderId)).thenReturn(orderDetail(orderId, userId, UUID.randomUUID(), offerId));

        var request = new CreateReturnRequest(orderId, ReturnReason.OTHER, "x",
            List.of(new CreateReturnRequest.ReturnItemRequest(offerId, 99)));

        assertThatThrownBy(() -> orchestrator.requestReturn(userId, request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("more than ordered");
    }

    @Test
    @DisplayName("approve runs refund saga: refund + restock + COMPLETED + 3 events")
    void approve_runsRefundSaga() {
        var returnId = UUID.randomUUID();
        var paymentId = UUID.randomUUID();
        var entity = ReturnEntity.builder()
            .id(returnId).orderId(UUID.randomUUID()).userId(UUID.randomUUID())
            .paymentId(paymentId).reasonCode("DEFECTIVE").status(ReturnStatus.REQUESTED)
            .refundAmount(new BigDecimal("100.00")).returnNumber("RT-001").build();
        when(returnRepository.findById(returnId)).thenReturn(Optional.of(entity));
        when(returnRepository.save(any(ReturnEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentClient.refund(eq(paymentId), any())).thenReturn(
            new PaymentClient.RefundResponse(UUID.randomUUID(), new BigDecimal("100.00"), "COMPLETED"));
        when(returnItemRepository.findByReturnId(returnId)).thenReturn(List.of(
            ReturnItem.builder().id(UUID.randomUUID()).returnId(returnId)
                .offerId(UUID.randomUUID()).productId(UUID.randomUUID()).sellerId(UUID.randomUUID())
                .quantity(1).unitPrice(new BigDecimal("100.00")).refundAmount(new BigDecimal("100.00"))
                .itemStatus("PENDING").build()));

        orchestrator.approve(returnId);

        assertThat(entity.getStatus()).isEqualTo(ReturnStatus.COMPLETED);
        verify(inventoryClient).restock(any(), eq(1), anyString());
        // 3 events: RETURN_APPROVED, RETURN_REFUNDED, RETURN_COMPLETED
        verify(outboxService, times(3)).publish(anyString(), anyString(), anyString(), eq("RETURN"), anyMap());
    }

    private OrderClient.OrderDetail orderDetail(UUID orderId, UUID userId, UUID paymentId, UUID offerId) {
        return new OrderClient.OrderDetail(
            orderId, userId, "CONFIRMED", new BigDecimal("100.00"), "TRY", paymentId,
            "Atakan", "+90555", "Istanbul", "Kadikoy", "addr", "34000",
            List.of(new OrderClient.OrderItem(offerId, UUID.randomUUID(), UUID.randomUUID(),
                2, new BigDecimal("50.00"), "Test")));
    }
}
