package com.smartcommerce.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.payment.api.dto.CardDto;
import com.smartcommerce.payment.api.dto.InitiatePaymentRequest;
import com.smartcommerce.payment.api.dto.IyzicoCallbackRequest;
import com.smartcommerce.payment.api.dto.RefundRequest;
import com.smartcommerce.payment.api.mapper.PaymentMapper;
import com.smartcommerce.payment.client.OrderClient;
import com.smartcommerce.payment.client.UserClient;
import com.smartcommerce.payment.domain.Payment;
import com.smartcommerce.payment.domain.PaymentAttempt;
import com.smartcommerce.payment.domain.PaymentStatus;
import com.smartcommerce.payment.domain.Refund;
import com.smartcommerce.payment.event.outbox.OutboxService;
import com.smartcommerce.payment.repository.PaymentAttemptRepository;
import com.smartcommerce.payment.repository.PaymentRepository;
import com.smartcommerce.payment.repository.RefundRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentAttemptRepository paymentAttemptRepository;
    @Mock RefundRepository refundRepository;
    @Mock PaymentProvider paymentProvider;
    @Mock OrderClient orderClient;
    @Mock UserClient userClient;
    @Mock OutboxService outboxService;

    PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentAttemptRepository, refundRepository,
            paymentProvider, orderClient, userClient, outboxService, new ObjectMapper(), new PaymentMapper(),
            new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("initiate persists payment and publishes PAYMENT_INITIATED on Iyzico success")
    void initiate_publishesInitiatedEvent() {
        var orderId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var card = new CardDto("Atakan Yel", "5528790000000008", "12", "2030", "123");
        when(orderClient.getOrder(orderId)).thenReturn(orderDetail(orderId, userId, "PAYMENT_PENDING"));
        when(userClient.getProfile(userId)).thenReturn(new UserClient.UserProfile(
            UUID.randomUUID(), userId, "atakan@example.com", "Atakan", "Yel", "+905555555555"));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        when(paymentAttemptRepository.save(any(PaymentAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentProvider.initiate(any())).thenReturn(new PaymentProvider.InitiateResult(
            true, null, "BASE64_HTML", null, null));

        var response = paymentService.initiatePayment(
            new InitiatePaymentRequest(orderId, card, 1, "127.0.0.1"));

        assertThat(response.status()).isEqualTo(PaymentStatus.THREEDS_PENDING.name());
        assertThat(response.threeDsHtmlContent()).isEqualTo("BASE64_HTML");
        verify(outboxService).publish(eq("payment.initiated.v1"), eq("PAYMENT_INITIATED"),
            anyString(), eq("PAYMENT"), anyMap());
    }

    @Test
    @DisplayName("initiate publishes PAYMENT_FAILED and throws when Iyzico rejects")
    void initiate_failurePublishesFailedEvent() {
        var orderId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        when(orderClient.getOrder(orderId)).thenReturn(orderDetail(orderId, userId, "PAYMENT_PENDING"));
        when(userClient.getProfile(userId)).thenReturn(new UserClient.UserProfile(
            UUID.randomUUID(), userId, "x@x.com", "A", "B", "+9050"));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        when(paymentAttemptRepository.save(any(PaymentAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentProvider.initiate(any())).thenReturn(new PaymentProvider.InitiateResult(
            false, null, null, "BAD_CARD", "Card declined"));

        assertThatThrownBy(() -> paymentService.initiatePayment(
            new InitiatePaymentRequest(orderId, new CardDto("a", "5528790000000008", "12", "2030", "123"), 1, null)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Card declined");

        verify(outboxService).publish(eq("payment.failed.v1"), eq("PAYMENT_FAILED"),
            anyString(), eq("PAYMENT"), anyMap());
    }

    @Test
    @DisplayName("3DS callback success captures payment and publishes PAYMENT_SUCCEEDED")
    void callback_success_publishesSucceeded() {
        var conversationId = UUID.randomUUID().toString();
        var payment = Payment.builder()
            .id(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .amount(new BigDecimal("250.00"))
            .currency("TRY")
            .installment(1)
            .provider("IYZICO")
            .providerConversationId(conversationId)
            .status(PaymentStatus.THREEDS_PENDING)
            .refundedAmount(BigDecimal.ZERO)
            .build();
        when(paymentRepository.findByProviderConversationId(conversationId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentAttemptRepository.countByPaymentId(payment.getId())).thenReturn(1L);
        when(paymentAttemptRepository.save(any(PaymentAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentProvider.capture(any())).thenReturn(new PaymentProvider.CaptureResult(
            true, "iyz-pay-1", "iyz-tx-1", new BigDecimal("250.00"),
            "0008", "MASTER_CARD", null, null));

        paymentService.handle3dsCallback(new IyzicoCallbackRequest(
            "success", "iyz-pay-1", "convdata", conversationId, "1"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getCardLastFour()).isEqualTo("0008");
        verify(outboxService).publish(eq("payment.succeeded.v1"), eq("PAYMENT_SUCCEEDED"),
            anyString(), eq("PAYMENT"), anyMap());
    }

    @Test
    @DisplayName("duplicate callback for already-SUCCEEDED payment is ignored (idempotent)")
    void callback_idempotent_whenAlreadyTerminal() {
        var conversationId = UUID.randomUUID().toString();
        var payment = Payment.builder()
            .id(UUID.randomUUID())
            .orderId(UUID.randomUUID())
            .userId(UUID.randomUUID())
            .amount(new BigDecimal("250.00"))
            .currency("TRY")
            .installment(1)
            .provider("IYZICO")
            .providerConversationId(conversationId)
            .status(PaymentStatus.SUCCEEDED)
            .refundedAmount(BigDecimal.ZERO)
            .build();
        when(paymentRepository.findByProviderConversationId(conversationId)).thenReturn(Optional.of(payment));

        paymentService.handle3dsCallback(new IyzicoCallbackRequest(
            "success", "iyz-pay-1", "convdata", conversationId, "1"));

        verify(paymentProvider, times(0)).capture(any());
        verify(outboxService, times(0)).publish(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("refund full amount marks payment REFUNDED and publishes PAYMENT_REFUNDED")
    void refund_full() {
        var paymentId = UUID.randomUUID();
        var payment = Payment.builder()
            .id(paymentId)
            .orderId(UUID.randomUUID())
            .amount(new BigDecimal("100.00"))
            .paidAmount(new BigDecimal("100.00"))
            .refundedAmount(BigDecimal.ZERO)
            .currency("TRY")
            .provider("IYZICO")
            .providerPaymentId("iyz-pay-1")
            .providerPaymentTransactionId("iyz-tx-1")
            .status(PaymentStatus.SUCCEEDED)
            .build();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> {
            Refund r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
        when(paymentProvider.refund(any())).thenReturn(new PaymentProvider.RefundResult(
            true, "iyz-refund-1", null, null));

        var response = paymentService.refund(paymentId,
            new RefundRequest(new BigDecimal("100.00"), "Customer asked"));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(outboxService).publish(eq("payment.refunded.v1"), eq("PAYMENT_REFUNDED"),
            anyString(), eq("PAYMENT"), anyMap());
    }

    @Test
    @DisplayName("refund rejected when amount exceeds remaining refundable")
    void refund_rejectsExcessiveAmount() {
        var paymentId = UUID.randomUUID();
        var payment = Payment.builder()
            .id(paymentId)
            .orderId(UUID.randomUUID())
            .paidAmount(new BigDecimal("100.00"))
            .refundedAmount(new BigDecimal("80.00"))
            .currency("TRY")
            .status(PaymentStatus.PARTIALLY_REFUNDED)
            .build();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refund(paymentId,
            new RefundRequest(new BigDecimal("50.00"), "test")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("exceeds remaining");
    }

    private OrderClient.OrderDetail orderDetail(UUID orderId, UUID userId, String status) {
        return new OrderClient.OrderDetail(
            orderId, userId, status, new BigDecimal("250.00"), "TRY",
            "Atakan Yel", "+905555555555", "Istanbul", "Kadikoy",
            "123 Test Street", "34000",
            List.of(new OrderClient.OrderItem(
                UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("250.00"), "Test Product"))
        );
    }
}
