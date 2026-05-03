package com.smartcommerce.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;

    @Column(name = "refunded_amount", nullable = false)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "TRY";

    @Column(nullable = false)
    @Builder.Default
    private Integer installment = 1;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String provider = "IYZICO";

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(name = "provider_payment_transaction_id")
    private String providerPaymentTransactionId;

    @Column(name = "provider_conversation_id", nullable = false, unique = true)
    private String providerConversationId;

    @Column(name = "three_ds_html_content", columnDefinition = "text")
    private String threeDsHtmlContent;

    @Column(name = "three_ds_callback_url", length = 500)
    private String threeDsCallbackUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.INITIATED;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "card_holder_name")
    private String cardHolderName;

    @Column(name = "initiated_at", nullable = false)
    @Builder.Default
    private Instant initiatedAt = Instant.now();

    @Column(name = "threeds_completed_at")
    private Instant threedsCompletedAt;

    @Column(name = "succeeded_at")
    private Instant succeededAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "failure_message", columnDefinition = "text")
    private String failureMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    /**
     * Enforces the PaymentStatus state machine. Throws IllegalStateException
     * for transitions not allowed by PaymentStatus.canTransitionTo. This
     * prevents bugs like SUCCEEDED → CANCELLED or terminal-state regressions.
     */
    public void transitionTo(PaymentStatus target) {
        if (target == null) throw new IllegalArgumentException("target status is null");
        if (this.status != null && !this.status.canTransitionTo(target)) {
            throw new IllegalStateException(
                "Illegal payment status transition: " + this.status + " -> " + target + " (paymentId=" + id + ")");
        }
        this.status = target;
    }
}
