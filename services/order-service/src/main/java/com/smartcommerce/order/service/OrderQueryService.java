package com.smartcommerce.order.service;

import com.smartcommerce.common.errors.*;
import com.smartcommerce.order.api.dto.OrderResponse;
import com.smartcommerce.order.api.mapper.OrderMapper;
import com.smartcommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public Page<OrderResponse> getMyOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(orderMapper::toResponse);
    }

    public OrderResponse getMyOrder(UUID userId, UUID orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId).map(orderMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
    }
}
