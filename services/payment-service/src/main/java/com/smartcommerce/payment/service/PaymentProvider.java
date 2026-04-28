package com.smartcommerce.payment.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentProvider {

    InitiateResult initiate(InitiateRequest request);

    CaptureResult capture(CaptureRequest request);

    RefundResult refund(RefundRequest request);

    PaymentDetails getDetails(String providerPaymentId);

    record InitiateRequest(
        UUID orderId,
        UUID userId,
        String userEmail,
        String userPhone,
        String userIdentityNumber,
        BigDecimal amount,
        String currency,
        Integer installment,
        CardInfo card,
        AddressInfo billingAddress,
        AddressInfo shippingAddress,
        List<BasketItem> basketItems,
        String conversationId,
        String userIp
    ) {}

    record InitiateResult(
        boolean success,
        String providerPaymentId,
        String threeDsHtmlContent,
        String errorCode,
        String errorMessage
    ) {}

    record CaptureRequest(String providerPaymentId, String conversationId, String paymentToken, String conversationData) {}

    record CaptureResult(
        boolean success,
        String providerPaymentId,
        String providerPaymentTransactionId,
        BigDecimal paidAmount,
        String cardLastFour,
        String cardBrand,
        String errorCode,
        String errorMessage
    ) {}

    record RefundRequest(
        String providerPaymentTransactionId,
        BigDecimal amount,
        String currency,
        String reason
    ) {}

    record RefundResult(boolean success, String providerRefundId, String errorCode, String errorMessage) {}

    record PaymentDetails(String providerPaymentId, String status, BigDecimal paidAmount) {}

    record CardInfo(String holderName, String number, String expireMonth, String expireYear, String cvc) {}

    record AddressInfo(String contactName, String city, String country, String address, String zipCode) {}

    record BasketItem(String id, String name, String category, BigDecimal price) {}
}
