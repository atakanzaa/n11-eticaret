package com.smartcommerce.order.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusTransitionTest {

    @Test
    @DisplayName("self-transition is a no-op and accepted")
    void selfTransitionAccepted() {
        var o = new Order();
        o.setStatus(OrderStatus.CONFIRMED);
        o.transitionTo(OrderStatus.CONFIRMED);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("CANCELLED is terminal — cannot transition back to CONFIRMED")
    void cancelledIsTerminal() {
        var o = new Order();
        o.setStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> o.transitionTo(OrderStatus.CONFIRMED))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Illegal order status transition");
    }

    @Test
    @DisplayName("REFUNDED is terminal")
    void refundedIsTerminal() {
        var o = new Order();
        o.setStatus(OrderStatus.REFUNDED);
        assertThatThrownBy(() -> o.transitionTo(OrderStatus.CONFIRMED))
            .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest(name = "{0} -> {1} allowed")
    @CsvSource({
        "CREATED, PAYMENT_PENDING",
        "PAYMENT_PENDING, CONFIRMED",
        "PAYMENT_PENDING, PAYMENT_FAILED",
        "PAYMENT_PENDING, EXPIRED",
        "CONFIRMED, PROCESSING",
        "CONFIRMED, SHIPPED",
        "CONFIRMED, CANCELLED",
        "PROCESSING, SHIPPED",
        "SHIPPED, DELIVERED",
        "DELIVERED, COMPLETED",
        "DELIVERED, RETURN_REQUESTED",
        "RETURN_REQUESTED, REFUNDED"
    })
    void allowedHappyPaths(OrderStatus from, OrderStatus to) {
        var o = new Order();
        o.setStatus(from);
        o.transitionTo(to);
        assertThat(o.getStatus()).isEqualTo(to);
    }

    @ParameterizedTest(name = "{0} -> {1} rejected")
    @CsvSource({
        "PAYMENT_PENDING, DELIVERED",
        "CREATED, SHIPPED",
        "DELIVERED, CREATED",
        "CANCELLED, PROCESSING",
        "REFUNDED, CONFIRMED",
        "EXPIRED, CONFIRMED"
    })
    void rejectedTransitions(OrderStatus from, OrderStatus to) {
        var o = new Order();
        o.setStatus(from);
        assertThatThrownBy(() -> o.transitionTo(to)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("null target is rejected")
    void nullTargetRejected() {
        var o = new Order();
        o.setStatus(OrderStatus.CREATED);
        assertThatThrownBy(() -> o.transitionTo(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
