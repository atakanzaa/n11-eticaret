package com.smartcommerce.order.api.mapper;

import com.smartcommerce.order.api.dto.*;
import com.smartcommerce.order.domain.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OrderMapper {
    public OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getUserId(), order.getStatus(),
            order.getGrandTotal(), order.getCurrency(), order.getSagaState(), order.getExpiresAt(),
            order.getCreatedAt(), order.getConfirmedAt(), order.getDeliveredAt(),
            order.getItems().stream().map(this::toResponse).toList());
    }

    public OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(item.getId(), item.getOfferId(), item.getProductId(), item.getSellerId(),
            item.getQuantity(), item.getUnitPrice(), item.getLineTotal(), item.getProductTitle(),
            item.getProductImageUrl(), item.getSellerName());
    }

    public Map<String, Object> toEvent(Order order) {
        var items = order.getItems().stream()
            .map(i -> Map.<String, Object>of(
                "offerId", i.getOfferId(),
                "productId", i.getProductId(),
                "sellerId", i.getSellerId(),
                "quantity", i.getQuantity(),
                "unitPrice", i.getUnitPrice(),
                "productTitle", i.getProductTitle()))
            .toList();
        var payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", order.getId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("userId", order.getUserId());
        payload.put("status", order.getStatus().name());
        payload.put("grandTotal", order.getGrandTotal());
        payload.put("currency", order.getCurrency());
        payload.put("items", items);
        return payload;
    }

    public OrderInternalResponse toInternalResponse(Order order) {
        var items = order.getItems().stream()
            .map(i -> new OrderInternalResponse.OrderInternalItem(
                i.getOfferId(), i.getProductId(), i.getSellerId(), i.getQuantity(),
                i.getUnitPrice(), i.getLineTotal(), i.getProductTitle()))
            .toList();
        return new OrderInternalResponse(
            order.getId(), order.getUserId(), order.getStatus().name(),
            order.getGrandTotal(), order.getCurrency(), order.getPaymentId(),
            order.getShippingFullName(), order.getShippingPhone(),
            order.getShippingCity(), order.getShippingDistrict(),
            order.getShippingFullAddress(), order.getShippingPostalCode(),
            items
        );
    }
}
