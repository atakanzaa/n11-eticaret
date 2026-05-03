package com.smartcommerce.payment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStatusTransitionTest {

    @Test
    @DisplayName("FAILED is terminal — cannot revive")
    void failedIsTerminal() {
        var p = new Payment();
        p.setStatus(PaymentStatus.FAILED);
        assertThatThrownBy(() -> p.transitionTo(PaymentStatus.SUCCEEDED))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("SUCCEEDED can only forward to refund states")
    void succeededOnlyToRefund() {
        var p = new Payment();
        p.setStatus(PaymentStatus.SUCCEEDED);
        p.transitionTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
    }

    @Test
    @DisplayName("Cannot transition SUCCEEDED -> CANCELLED")
    void successCannotBeCancelled() {
        var p = new Payment();
        p.setStatus(PaymentStatus.SUCCEEDED);
        assertThatThrownBy(() -> p.transitionTo(PaymentStatus.CANCELLED))
            .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest(name = "{0} -> {1} allowed")
    @CsvSource({
        "INITIATED, THREEDS_PENDING",
        "THREEDS_PENDING, THREEDS_AUTHENTICATED",
        "THREEDS_AUTHENTICATED, CAPTURING",
        "CAPTURING, SUCCEEDED",
        "SUCCEEDED, REFUNDED",
        "PARTIALLY_REFUNDED, REFUNDED"
    })
    void allowed(PaymentStatus from, PaymentStatus to) {
        var p = new Payment();
        p.setStatus(from);
        p.transitionTo(to);
        assertThat(p.getStatus()).isEqualTo(to);
    }
}
