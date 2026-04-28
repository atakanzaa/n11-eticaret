package com.smartcommerce.returns.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.returns.api.dto.CreateReturnRequest;
import com.smartcommerce.returns.api.dto.ReturnResponse;
import com.smartcommerce.returns.api.mapper.ReturnMapper;
import com.smartcommerce.returns.client.InventoryClient;
import com.smartcommerce.returns.client.OrderClient;
import com.smartcommerce.returns.client.PaymentClient;
import com.smartcommerce.returns.domain.ReturnEntity;
import com.smartcommerce.returns.domain.ReturnItem;
import com.smartcommerce.returns.domain.ReturnSagaLog;
import com.smartcommerce.returns.domain.ReturnStatus;
import com.smartcommerce.returns.event.outbox.OutboxService;
import com.smartcommerce.returns.repository.ReturnItemRepository;
import com.smartcommerce.returns.repository.ReturnRepository;
import com.smartcommerce.returns.repository.ReturnSagaLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnOrchestrator {

    private final ReturnRepository returnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReturnSagaLogRepository sagaLogRepository;
    private final OrderClient orderClient;
    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;
    private final OutboxService outboxService;
    private final ReturnMapper mapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReturnResponse requestReturn(UUID userId, CreateReturnRequest request) {
        var order = orderClient.getOrder(request.orderId());
        if (!order.userId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                "Cannot return another user's order");
        }
        if (!"CONFIRMED".equals(order.status()) && !"COMPLETED".equals(order.status())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "Cannot return order in state: " + order.status());
        }

        var orderItemsByOffer = order.items().stream()
            .collect(Collectors.toMap(OrderClient.OrderItem::offerId, item -> item, (a, b) -> a));

        var refundAmount = BigDecimal.ZERO;
        for (var ri : request.items()) {
            var oi = orderItemsByOffer.get(ri.offerId());
            if (oi == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Offer not found in order: " + ri.offerId());
            }
            if (ri.quantity() > oi.quantity()) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Cannot return more than ordered quantity for offer " + ri.offerId());
            }
            refundAmount = refundAmount.add(oi.unitPrice().multiply(BigDecimal.valueOf(ri.quantity())));
        }

        var paymentId = order.paymentId();
        if (paymentId == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "Order has no associated payment to refund");
        }

        var returnEntity = ReturnEntity.builder()
            .returnNumber(generateReturnNumber())
            .orderId(request.orderId())
            .userId(userId)
            .paymentId(paymentId)
            .reasonCode(request.reasonCode().name())
            .reasonDescription(request.reasonDescription())
            .status(ReturnStatus.REQUESTED)
            .sagaState("STARTED")
            .refundAmount(refundAmount)
            .build();
        returnEntity = returnRepository.save(returnEntity);

        for (var ri : request.items()) {
            var oi = orderItemsByOffer.get(ri.offerId());
            returnItemRepository.save(ReturnItem.builder()
                .returnId(returnEntity.getId())
                .orderItemId(oi.offerId())
                .offerId(oi.offerId())
                .productId(oi.productId())
                .sellerId(oi.sellerId())
                .quantity(ri.quantity())
                .unitPrice(oi.unitPrice())
                .refundAmount(oi.unitPrice().multiply(BigDecimal.valueOf(ri.quantity())))
                .itemStatus("PENDING")
                .build());
        }

        outboxService.publish(Topics.RETURN_REQUESTED, EventType.RETURN_REQUESTED,
            returnEntity.getId().toString(), "RETURN", mapper.toEvent(returnEntity));
        logSaga(returnEntity.getId(), "REQUESTED", "SUCCESS", null, null);
        return loadResponse(returnEntity.getId());
    }

    @Transactional
    public ReturnResponse approve(UUID returnId) {
        var returnEntity = returnRepository.findById(returnId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Return not found"));
        if (returnEntity.getStatus() != ReturnStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "Can only approve REQUESTED returns; current=" + returnEntity.getStatus());
        }
        returnEntity.setStatus(ReturnStatus.APPROVED);
        returnEntity.setApprovedAt(Instant.now());
        returnEntity.setSagaState("APPROVED");
        returnRepository.save(returnEntity);

        outboxService.publish(Topics.RETURN_APPROVED, EventType.RETURN_APPROVED,
            returnId.toString(), "RETURN", mapper.toEvent(returnEntity));
        logSaga(returnId, "APPROVED", "SUCCESS", null, null);

        processRefund(returnEntity);
        return loadResponse(returnId);
    }

    @Transactional
    public ReturnResponse reject(UUID returnId, String reason) {
        var returnEntity = returnRepository.findById(returnId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Return not found"));
        if (returnEntity.getStatus() != ReturnStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "Can only reject REQUESTED returns");
        }
        returnEntity.setStatus(ReturnStatus.REJECTED);
        returnEntity.setRejectedAt(Instant.now());
        returnEntity.setRejectionReason(reason);
        returnEntity.setSagaState("REJECTED");
        returnRepository.save(returnEntity);
        outboxService.publish(Topics.RETURN_REJECTED, EventType.RETURN_REJECTED,
            returnId.toString(), "RETURN",
            Map.of("returnId", returnId, "reason", reason));
        logSaga(returnId, "REJECTED", "SUCCESS", Map.of("reason", reason), null);
        return loadResponse(returnId);
    }

    @Transactional
    public ReturnResponse cancel(UUID returnId, UUID userId) {
        var returnEntity = returnRepository.findById(returnId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Return not found"));
        if (!returnEntity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Cannot cancel another user's return");
        }
        if (returnEntity.getStatus() != ReturnStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Can only cancel REQUESTED returns");
        }
        returnEntity.setStatus(ReturnStatus.CANCELLED);
        returnEntity.setSagaState("CANCELLED");
        returnRepository.save(returnEntity);
        logSaga(returnId, "CANCELLED", "SUCCESS", null, null);
        return loadResponse(returnId);
    }

    public ReturnResponse get(UUID returnId) {
        return loadResponse(returnId);
    }

    public List<ReturnResponse> listForUser(UUID userId) {
        return returnRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(r -> mapper.toResponse(r, returnItemRepository.findByReturnId(r.getId())))
            .toList();
    }

    private ReturnResponse loadResponse(UUID returnId) {
        var entity = returnRepository.findById(returnId).orElseThrow();
        return mapper.toResponse(entity, returnItemRepository.findByReturnId(returnId));
    }

    private void processRefund(ReturnEntity returnEntity) {
        returnEntity.setStatus(ReturnStatus.REFUND_PROCESSING);
        returnEntity.setSagaState("REFUND_PROCESSING");
        returnRepository.save(returnEntity);

        try {
            var refundResp = paymentClient.refund(returnEntity.getPaymentId(),
                new PaymentClient.RefundRequest(returnEntity.getRefundAmount(),
                    "RETURN_" + returnEntity.getReturnNumber()));
            returnEntity.setRefundId(refundResp.id());
            returnEntity.setStatus(ReturnStatus.REFUNDED);
            returnEntity.setRefundedAt(Instant.now());
            returnEntity.setSagaState("REFUNDED");
            returnRepository.save(returnEntity);
            logSaga(returnEntity.getId(), "REFUND", "SUCCESS",
                Map.of("refundId", refundResp.id().toString()), null);

            outboxService.publish(Topics.RETURN_REFUNDED, EventType.RETURN_REFUNDED,
                returnEntity.getId().toString(), "RETURN", mapper.toEvent(returnEntity));

            restockInventory(returnEntity);

            returnEntity.setStatus(ReturnStatus.COMPLETED);
            returnEntity.setCompletedAt(Instant.now());
            returnEntity.setSagaState("COMPLETED");
            returnRepository.save(returnEntity);

            outboxService.publish(Topics.RETURN_COMPLETED, EventType.RETURN_COMPLETED,
                returnEntity.getId().toString(), "RETURN", mapper.toEvent(returnEntity));
            logSaga(returnEntity.getId(), "COMPLETED", "SUCCESS", null, null);

        } catch (Exception e) {
            log.error("Return saga failed for {}", returnEntity.getId(), e);
            returnEntity.setStatus(ReturnStatus.REFUND_FAILED);
            returnEntity.setSagaState("FAILED");
            returnRepository.save(returnEntity);
            logSaga(returnEntity.getId(), "REFUND", "FAILED",
                Map.of("error", e.getMessage() != null ? e.getMessage() : "unknown"),
                e.getMessage());
        }
    }

    private void restockInventory(ReturnEntity returnEntity) {
        var items = returnItemRepository.findByReturnId(returnEntity.getId());
        var anyFailure = false;
        for (var item : items) {
            try {
                inventoryClient.restock(item.getOfferId(), item.getQuantity(), "RETURN_RESTOCK");
                logSaga(returnEntity.getId(), "RESTOCK_ITEM", "SUCCESS",
                    Map.of("offerId", item.getOfferId().toString(), "quantity", item.getQuantity()), null);
            } catch (Exception e) {
                anyFailure = true;
                log.error("Failed to restock offer {}", item.getOfferId(), e);
                logSaga(returnEntity.getId(), "RESTOCK_ITEM", "FAILED",
                    Map.of("offerId", item.getOfferId().toString()),
                    e.getMessage());
            }
        }
        if (!anyFailure) {
            returnEntity.setStatus(ReturnStatus.INVENTORY_RESTOCKED);
            returnRepository.save(returnEntity);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void logSaga(UUID returnId, String step, String status, Map<String, Object> payload, String errorMessage) {
        sagaLogRepository.save(ReturnSagaLog.builder()
            .returnId(returnId)
            .step(step)
            .status(status)
            .payload(payload != null ? objectMapper.valueToTree(payload) : null)
            .errorMessage(errorMessage)
            .startedAt(Instant.now())
            .completedAt(Instant.now())
            .build());
    }

    private String generateReturnNumber() {
        return "RT-" + LocalDate.now().getYear() + "-"
            + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
