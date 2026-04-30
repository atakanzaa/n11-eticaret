package com.smartcommerce.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.payment.api.dto.InitiatePaymentRequest;
import com.smartcommerce.payment.api.dto.InitiatePaymentResponse;
import com.smartcommerce.payment.api.dto.IyzicoCallbackRequest;
import com.smartcommerce.payment.api.dto.PaymentResponse;
import com.smartcommerce.payment.api.dto.RefundRequest;
import com.smartcommerce.payment.api.dto.RefundResponse;
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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    private static final String ORDER_PAYMENT_PENDING = "PAYMENT_PENDING";

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final RefundRepository refundRepository;
    private final PaymentProvider paymentProvider;
    private final OrderClient orderClient;
    private final UserClient userClient;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final PaymentMapper paymentMapper;

    private final Counter paymentSuccess;
    private final Counter paymentFailure;
    private final Counter paymentRefund;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentAttemptRepository paymentAttemptRepository,
                          RefundRepository refundRepository,
                          PaymentProvider paymentProvider,
                          OrderClient orderClient, UserClient userClient,
                          OutboxService outboxService, ObjectMapper objectMapper,
                          PaymentMapper paymentMapper, MeterRegistry registry) {
        this.paymentRepository = paymentRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.refundRepository = refundRepository;
        this.paymentProvider = paymentProvider;
        this.orderClient = orderClient;
        this.userClient = userClient;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
        this.paymentMapper = paymentMapper;
        this.paymentSuccess = Counter.builder("smartcommerce.payment.success")
            .description("Total successful payments").register(registry);
        this.paymentFailure = Counter.builder("smartcommerce.payment.failure")
            .description("Total failed payments").register(registry);
        this.paymentRefund = Counter.builder("smartcommerce.payment.refund")
            .description("Total refunded payments").register(registry);
    }

    @Transactional
    public InitiatePaymentResponse initiatePayment(InitiatePaymentRequest request) {
        var existing = paymentRepository.findByOrderId(request.orderId());
        if (existing.isPresent()) {
            var existingPayment = existing.get();
            if (existingPayment.getStatus() == PaymentStatus.SUCCEEDED) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Payment already completed");
            }
            if (existingPayment.getStatus() == PaymentStatus.THREEDS_PENDING) {
                return new InitiatePaymentResponse(existingPayment.getId(),
                    existingPayment.getThreeDsHtmlContent(), existingPayment.getStatus().name());
            }
        }

        var order = orderClient.getOrder(request.orderId());
        if (!ORDER_PAYMENT_PENDING.equals(order.status())) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATE,
                "Order is not in PAYMENT_PENDING state: " + order.status());
        }
        var user = userClient.getProfile(order.userId());

        var conversationId = UUID.randomUUID().toString();
        var payment = Payment.builder()
            .orderId(request.orderId())
            .userId(order.userId())
            .amount(order.grandTotal())
            .currency(order.currency())
            .installment(request.installment() != null ? request.installment() : 1)
            .provider("IYZICO")
            .providerConversationId(conversationId)
            .status(PaymentStatus.INITIATED)
            .cardHolderName(request.card().holderName())
            .build();
        payment = paymentRepository.save(payment);

        var providerRequest = new PaymentProvider.InitiateRequest(
            order.id(),
            order.userId(),
            user.email(),
            user.phone(),
            null,
            order.grandTotal(),
            order.currency(),
            payment.getInstallment(),
            new PaymentProvider.CardInfo(
                request.card().holderName(),
                request.card().number(),
                request.card().expireMonth(),
                request.card().expireYear(),
                request.card().cvc()
            ),
            new PaymentProvider.AddressInfo(
                order.shippingFullName(),
                order.shippingCity(),
                "Turkey",
                order.shippingFullAddress(),
                order.shippingPostalCode() != null ? order.shippingPostalCode() : "34000"
            ),
            new PaymentProvider.AddressInfo(
                order.shippingFullName(),
                order.shippingCity(),
                "Turkey",
                order.shippingFullAddress(),
                order.shippingPostalCode() != null ? order.shippingPostalCode() : "34000"
            ),
            order.items().stream().map(item -> new PaymentProvider.BasketItem(
                item.offerId().toString(),
                item.productTitle(),
                "GENERAL",
                item.unitPrice()
            )).toList(),
            conversationId,
            request.userIp()
        );

        var attempt = PaymentAttempt.builder()
            .paymentId(payment.getId())
            .attemptNumber(1)
            .status("INITIATING")
            .requestPayload(redactedRequest(providerRequest))
            .build();
        attempt = paymentAttemptRepository.save(attempt);

        var result = paymentProvider.initiate(providerRequest);

        attempt.setResponsePayload(objectMapper.valueToTree(result));
        attempt.setCompletedAt(Instant.now());

        if (!result.success()) {
            attempt.setStatus("FAILED");
            attempt.setErrorCode(result.errorCode());
            attempt.setErrorMessage(result.errorMessage());
            paymentAttemptRepository.save(attempt);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureCode(result.errorCode());
            payment.setFailureMessage(result.errorMessage());
            payment.setFailedAt(Instant.now());
            paymentRepository.save(payment);

            publishPaymentFailed(payment, result.errorCode(), result.errorMessage());

            throw new BusinessException(ErrorCode.PAYMENT_FAILED,
                "Payment initiation failed: " + result.errorMessage());
        }

        attempt.setStatus("3DS_INITIATED");
        paymentAttemptRepository.save(attempt);

        payment.setProviderPaymentId(result.providerPaymentId());
        payment.setThreeDsHtmlContent(result.threeDsHtmlContent());
        payment.setStatus(PaymentStatus.THREEDS_PENDING);
        paymentRepository.save(payment);

        outboxService.publish(Topics.PAYMENT_INITIATED, EventType.PAYMENT_INITIATED,
            payment.getId().toString(), "PAYMENT",
            Map.of(
                "paymentId", payment.getId(),
                "orderId", payment.getOrderId(),
                "amount", payment.getAmount()
            ));

        return new InitiatePaymentResponse(payment.getId(), result.threeDsHtmlContent(),
            payment.getStatus().name());
    }

    @Transactional
    public void handle3dsCallback(IyzicoCallbackRequest callback) {
        log.info("Received 3DS callback: paymentId={}, status={}, conversationId={}",
            callback.paymentId(), callback.status(), callback.conversationId());

        var payment = paymentRepository.findByProviderConversationId(callback.conversationId())
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,
                "Payment not found for conversationId: " + callback.conversationId()));

        if (payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.FAILED) {
            log.info("Payment {} already in terminal state {}, ignoring callback", payment.getId(), payment.getStatus());
            return;
        }

        if (!"success".equals(callback.status())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureCode("3DS_AUTHENTICATION_FAILED");
            payment.setFailureMessage(callback.mdStatus());
            payment.setFailedAt(Instant.now());
            paymentRepository.save(payment);
            publishPaymentFailed(payment, "3DS_AUTHENTICATION_FAILED", callback.mdStatus());
            return;
        }

        if (callback.paymentId() != null && !callback.paymentId().isBlank()) {
            payment.setProviderPaymentId(callback.paymentId());
        }
        payment.setStatus(PaymentStatus.THREEDS_AUTHENTICATED);
        payment.setThreedsCompletedAt(Instant.now());
        paymentRepository.save(payment);

        capturePayment(payment, callback);
    }

    private void capturePayment(Payment payment, IyzicoCallbackRequest callback) {
        payment.setStatus(PaymentStatus.CAPTURING);
        paymentRepository.save(payment);

        var attemptNum = paymentAttemptRepository.countByPaymentId(payment.getId()) + 1;
        var attempt = PaymentAttempt.builder()
            .paymentId(payment.getId())
            .attemptNumber((int) attemptNum)
            .status("CAPTURING")
            .build();
        attempt = paymentAttemptRepository.save(attempt);

        var result = paymentProvider.capture(new PaymentProvider.CaptureRequest(
            payment.getProviderPaymentId(),
            payment.getProviderConversationId(),
            null,
            callback.conversationData()
        ));

        attempt.setResponsePayload(objectMapper.valueToTree(result));
        attempt.setCompletedAt(Instant.now());

        if (!result.success()) {
            attempt.setStatus("FAILED");
            attempt.setErrorCode(result.errorCode());
            attempt.setErrorMessage(result.errorMessage());
            paymentAttemptRepository.save(attempt);

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureCode(result.errorCode());
            payment.setFailureMessage(result.errorMessage());
            payment.setFailedAt(Instant.now());
            paymentRepository.save(payment);

            publishPaymentFailed(payment, result.errorCode(), result.errorMessage());
            return;
        }

        attempt.setStatus("CAPTURED");
        paymentAttemptRepository.save(attempt);

        markSucceeded(payment, result.paidAmount(), result.cardLastFour(), result.cardBrand(),
            result.providerPaymentTransactionId(), false);
    }

    @Transactional
    public RefundResponse refund(UUID paymentId, RefundRequest request) {
        var payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Payment not found"));

        if (payment.getStatus() != PaymentStatus.SUCCEEDED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "Cannot refund payment in state: " + payment.getStatus());
        }

        var refundAmount = request.amount() != null ? request.amount() : payment.getPaidAmount();
        var remaining = payment.getPaidAmount().subtract(payment.getRefundedAmount());
        if (refundAmount.compareTo(remaining) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                "Refund amount exceeds remaining refundable amount");
        }

        var refund = Refund.builder()
            .paymentId(paymentId)
            .orderId(payment.getOrderId())
            .amount(refundAmount)
            .reason(request.reason())
            .status("PENDING")
            .build();
        refund = refundRepository.save(refund);

        var result = paymentProvider.refund(new PaymentProvider.RefundRequest(
            payment.getProviderPaymentTransactionId() != null
                ? payment.getProviderPaymentTransactionId()
                : payment.getProviderPaymentId(),
            refundAmount,
            payment.getCurrency(),
            request.reason()
        ));

        if (!result.success()) {
            refund.setStatus("FAILED");
            refund.setFailureMessage(result.errorMessage());
            refundRepository.save(refund);
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_ERROR,
                "Refund failed: " + result.errorMessage());
        }

        refund.setStatus("COMPLETED");
        refund.setProviderRefundId(result.providerRefundId());
        refund.setCompletedAt(Instant.now());
        refundRepository.save(refund);
        paymentRefund.increment();

        payment.setRefundedAmount(payment.getRefundedAmount().add(refundAmount));
        payment.setStatus(payment.getRefundedAmount().compareTo(payment.getPaidAmount()) >= 0
            ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);

        outboxService.publish(Topics.PAYMENT_REFUNDED, EventType.PAYMENT_REFUNDED,
            payment.getId().toString(), "PAYMENT",
            Map.of(
                "paymentId", payment.getId(),
                "orderId", payment.getOrderId(),
                "refundId", refund.getId(),
                "amount", refund.getAmount(),
                "totalRefunded", payment.getRefundedAmount(),
                "fullyRefunded", payment.getStatus() == PaymentStatus.REFUNDED
            ));

        return new RefundResponse(refund.getId(), refund.getAmount(), refund.getStatus());
    }

    public PaymentResponse getById(UUID id) {
        return paymentMapper.toResponse(paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Payment not found")));
    }

    public PaymentResponse getByOrderId(UUID orderId) {
        return paymentMapper.toResponse(paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Payment not found for order")));
    }

    @Transactional
    public void markSucceeded(Payment payment, BigDecimal paidAmount, String cardLastFour,
                              String cardBrand, String providerPaymentTransactionId, boolean reconciled) {
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaidAmount(paidAmount);
        payment.setCardLastFour(cardLastFour);
        payment.setCardBrand(cardBrand);
        if (providerPaymentTransactionId != null) {
            payment.setProviderPaymentTransactionId(providerPaymentTransactionId);
        }
        payment.setSucceededAt(Instant.now());
        paymentRepository.save(payment);
        paymentSuccess.increment();

        var event = new HashMap<String, Object>();
        event.put("paymentId", payment.getId());
        event.put("orderId", payment.getOrderId());
        event.put("userId", payment.getUserId());
        event.put("paidAmount", payment.getPaidAmount());
        event.put("currency", payment.getCurrency());
        event.put("cardLastFour", payment.getCardLastFour());
        event.put("cardBrand", payment.getCardBrand());
        if (reconciled) event.put("reconciled", true);

        outboxService.publish(Topics.PAYMENT_SUCCEEDED, EventType.PAYMENT_SUCCEEDED,
            payment.getId().toString(), "PAYMENT", event);
    }

    private void publishPaymentFailed(Payment payment, String code, String message) {
        paymentFailure.increment();
        outboxService.publish(Topics.PAYMENT_FAILED, EventType.PAYMENT_FAILED,
            payment.getId().toString(), "PAYMENT",
            Map.of(
                "paymentId", payment.getId(),
                "orderId", payment.getOrderId(),
                "reason", message != null ? message : "PAYMENT_FAILED",
                "errorCode", code != null ? code : "UNKNOWN"
            ));
    }

    private com.fasterxml.jackson.databind.JsonNode redactedRequest(PaymentProvider.InitiateRequest request) {
        var redactedCard = new PaymentProvider.CardInfo(
            request.card().holderName(),
            mask(request.card().number()),
            request.card().expireMonth(),
            request.card().expireYear(),
            "***"
        );
        var copy = new PaymentProvider.InitiateRequest(
            request.orderId(), request.userId(), request.userEmail(), request.userPhone(),
            request.userIdentityNumber(), request.amount(), request.currency(), request.installment(),
            redactedCard, request.billingAddress(), request.shippingAddress(),
            request.basketItems(), request.conversationId(), request.userIp()
        );
        return objectMapper.valueToTree(copy);
    }

    private String mask(String number) {
        if (number == null || number.length() < 4) return "****";
        return "**** **** **** " + number.substring(number.length() - 4);
    }
}
