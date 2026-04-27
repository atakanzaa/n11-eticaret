package com.smartcommerce.order.api.mapper;

import com.smartcommerce.order.api.dto.*;
import com.smartcommerce.order.domain.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderMapper {
    public OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getUserId(), order.getStatus(),
            order.getGrandTotal(), order.getCurrency(), order.getSagaState(), order.getExpiresAt(),
            order.getItems().stream().map(this::toResponse).toList());
    }

    public OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(item.getId(), item.getOfferId(), item.getProductId(), item.getSellerId(),
            item.getQuantity(), item.getUnitPrice(), item.getLineTotal(), item.getProductTitle(),
            item.getProductImageUrl(), item.getSellerName());
    }

    public Map<String, Object> toEvent(Order order) {
        return Map.of(
            "orderId", order.getId(),
            "orderNumber", order.getOrderNumber(),
            "userId", order.getUserId(),
            "status", order.getStatus().name(),
            "grandTotal", order.getGrandTotal(),
            "currency", order.getCurrency()
        );
    }
}
