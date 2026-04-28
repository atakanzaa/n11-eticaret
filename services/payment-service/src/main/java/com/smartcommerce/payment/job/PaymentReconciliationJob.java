package com.smartcommerce.payment.job;

import com.smartcommerce.payment.domain.PaymentStatus;
import com.smartcommerce.payment.repository.PaymentRepository;
import com.smartcommerce.payment.service.PaymentProvider;
import com.smartcommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;
    private final PaymentService paymentService;

    @Scheduled(cron = "${payment.reconciliation.cron:0 30 * * * *}")
    @Transactional
    public void reconcile() {
        var since = Instant.now().minus(Duration.ofHours(2));
        var candidates = paymentRepository.findByStatusInAndUpdatedAtAfter(
            List.of(PaymentStatus.CAPTURING, PaymentStatus.THREEDS_AUTHENTICATED, PaymentStatus.THREEDS_PENDING),
            since
        );
        log.info("Reconciliation: checking {} payments", candidates.size());

        for (var payment : candidates) {
            if (payment.getProviderPaymentId() == null) continue;
            try {
                var details = paymentProvider.getDetails(payment.getProviderPaymentId());
                var providerStatus = details.status();
                var success = "success".equalsIgnoreCase(providerStatus) || "SUCCESS".equalsIgnoreCase(providerStatus);
                if (success && payment.getStatus() != PaymentStatus.SUCCEEDED) {
                    log.warn("Reconciliation: payment {} status drift — local={}, provider=SUCCESS. Fixing.",
                        payment.getId(), payment.getStatus());
                    paymentService.markSucceeded(payment, details.paidAmount(),
                        payment.getCardLastFour(), payment.getCardBrand(),
                        payment.getProviderPaymentTransactionId(), true);
                }
            } catch (Exception e) {
                log.error("Reconciliation failed for payment {}", payment.getId(), e);
            }
        }
    }
}
