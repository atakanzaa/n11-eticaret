package com.smartcommerce.order.service;

import com.smartcommerce.common.errors.*;
import com.smartcommerce.order.api.dto.AdminOverviewResponse;
import com.smartcommerce.order.api.dto.OrderInternalResponse;
import com.smartcommerce.order.api.dto.OrderResponse;
import com.smartcommerce.order.api.dto.RevenuePoint;
import com.smartcommerce.order.api.dto.SellerOrderKpiResponse;
import com.smartcommerce.order.api.mapper.OrderMapper;
import com.smartcommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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

    public OrderInternalResponse getInternal(UUID orderId) {
        return orderRepository.findById(orderId).map(orderMapper::toInternalResponse)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
    }

    public SellerOrderKpiResponse getSellerKpi(UUID sellerId) {
        var todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        var weekStart = Instant.now().minus(7, ChronoUnit.DAYS);
        return new SellerOrderKpiResponse(
            orderRepository.sumSellerRevenueSince(sellerId, todayStart),
            orderRepository.sumSellerRevenueSince(sellerId, weekStart),
            orderRepository.countPendingForSeller(sellerId)
        );
    }

    public List<RevenuePoint> getSellerRevenueSeries(UUID sellerId, int days) {
        var since = LocalDate.now(ZoneOffset.UTC).minusDays(days - 1L).atStartOfDay().toInstant(ZoneOffset.UTC);
        var byDay = new TreeMap<LocalDate, RevenuePoint>();
        for (Object[] row : orderRepository.sellerRevenueByDay(sellerId, since)) {
            var day = ((Timestamp) row[0]).toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
            byDay.put(day, new RevenuePoint(day, toBigDecimal(row[1]), ((Number) row[2]).longValue()));
        }
        var today = LocalDate.now(ZoneOffset.UTC);
        for (int i = days - 1; i >= 0; i--) {
            var d = today.minusDays(i);
            byDay.putIfAbsent(d, new RevenuePoint(d, BigDecimal.ZERO, 0));
        }
        return byDay.values().stream().toList();
    }

    public AdminOverviewResponse getAdminOverview() {
        var since = Instant.now().minus(30, ChronoUnit.DAYS);
        return new AdminOverviewResponse(
            orderRepository.sumGmvSince(since),
            orderRepository.countOrdersSince(since),
            orderRepository.averageBasketSince(since),
            orderRepository.countActiveSellersSince(since)
        );
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
