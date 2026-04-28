package com.smartcommerce.payment.repository;

import com.smartcommerce.payment.domain.Payment;
import com.smartcommerce.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByProviderConversationId(String conversationId);
    List<Payment> findByStatusInAndUpdatedAtAfter(List<PaymentStatus> statuses, Instant since);
}
