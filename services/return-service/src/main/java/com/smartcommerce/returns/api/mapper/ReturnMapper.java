package com.smartcommerce.returns.api.mapper;

import com.smartcommerce.returns.api.dto.ReturnResponse;
import com.smartcommerce.returns.domain.ReturnEntity;
import com.smartcommerce.returns.domain.ReturnItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ReturnMapper {

    public ReturnResponse toResponse(ReturnEntity r, List<ReturnItem> items) {
        var itemDtos = items.stream().map(this::toItemResponse).toList();
        return new ReturnResponse(
            r.getId(), r.getReturnNumber(), r.getOrderId(), r.getUserId(), r.getPaymentId(),
            r.getReasonCode(), r.getReasonDescription(), r.getStatus(), r.getSagaState(),
            r.getRefundAmount(), r.getRefundId(), r.getRejectionReason(),
            r.getRequestedAt(), r.getApprovedAt(), r.getRejectedAt(),
            r.getRefundedAt(), r.getCompletedAt(),
            itemDtos);
    }

    public ReturnResponse.ReturnItemResponse toItemResponse(ReturnItem item) {
        return new ReturnResponse.ReturnItemResponse(
            item.getId(), item.getOfferId(), item.getProductId(), item.getSellerId(),
            item.getQuantity(), item.getUnitPrice(), item.getRefundAmount(), item.getItemStatus());
    }

    public Map<String, Object> toEvent(ReturnEntity r) {
        return Map.of(
            "returnId", r.getId(),
            "returnNumber", r.getReturnNumber(),
            "orderId", r.getOrderId(),
            "userId", r.getUserId(),
            "paymentId", r.getPaymentId(),
            "status", r.getStatus().name(),
            "refundAmount", r.getRefundAmount() != null ? r.getRefundAmount() : 0,
            "reasonCode", r.getReasonCode() != null ? r.getReasonCode() : "");
    }

    public UUID toId(UUID id) {
        return id;
    }
}
