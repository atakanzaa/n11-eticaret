package com.smartcommerce.payment.api.mapper;

import com.smartcommerce.payment.api.dto.PaymentResponse;
import com.smartcommerce.payment.domain.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {
    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getUserId(),
            payment.getAmount(),
            payment.getPaidAmount(),
            payment.getRefundedAmount(),
            payment.getCurrency(),
            payment.getInstallment(),
            payment.getProvider(),
            payment.getStatus(),
            payment.getCardLastFour(),
            payment.getCardBrand(),
            payment.getInitiatedAt(),
            payment.getSucceededAt(),
            payment.getFailedAt(),
            payment.getFailureCode(),
            payment.getFailureMessage()
        );
    }
}
