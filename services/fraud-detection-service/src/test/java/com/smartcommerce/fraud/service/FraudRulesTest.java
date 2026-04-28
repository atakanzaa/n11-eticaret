package com.smartcommerce.fraud.service;

import com.smartcommerce.fraud.repository.FraudBlacklistRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudRulesTest {

    @Mock FraudBlacklistRepository blacklistRepository;

    @Test
    @DisplayName("HIGH_AMOUNT triggers at 80 score for >50k TRY")
    void highAmount_above50kTriggersScore80() {
        var rule = new HighAmountRule();
        var ctx = baseContext(new BigDecimal("60000"));
        var result = rule.evaluate(ctx);
        assertThat(result.triggered()).isTrue();
        assertThat(result.riskScore()).isEqualTo(80);
    }

    @Test
    @DisplayName("HIGH_AMOUNT triggers at 30 score for 20k-50k TRY")
    void highAmount_between20kAnd50kTriggersScore30() {
        var rule = new HighAmountRule();
        var result = rule.evaluate(baseContext(new BigDecimal("25000")));
        assertThat(result.triggered()).isTrue();
        assertThat(result.riskScore()).isEqualTo(30);
    }

    @Test
    @DisplayName("HIGH_AMOUNT does not trigger under 20k TRY")
    void highAmount_below20kNotTriggered() {
        var rule = new HighAmountRule();
        var result = rule.evaluate(baseContext(new BigDecimal("19999")));
        assertThat(result.triggered()).isFalse();
    }

    @Test
    @DisplayName("VELOCITY triggers at score 70 for 5+ orders/hour")
    void velocity_5OrdersInHourTriggers() {
        var rule = new VelocityRule();
        var ctx = new FraudRule.FraudCheckContext(
            UUID.randomUUID(), UUID.randomUUID(), null, null,
            BigDecimal.TEN, "TRY", 1, 5, BigDecimal.ZERO, 5, false, null);
        var result = rule.evaluate(ctx);
        assertThat(result.triggered()).isTrue();
        assertThat(result.riskScore()).isEqualTo(70);
    }

    @Test
    @DisplayName("BLACKLIST returns score 100 when user is on blacklist")
    void blacklist_userMatch_returns100() {
        var rule = new BlacklistRule(blacklistRepository);
        var userId = UUID.randomUUID();
        when(blacklistRepository.existsByBlacklistTypeAndValue("USER_ID", userId.toString())).thenReturn(true);
        var ctx = new FraudRule.FraudCheckContext(
            UUID.randomUUID(), userId, "x@x.com", "1.1.1.1",
            BigDecimal.TEN, "TRY", 1, 0, BigDecimal.ZERO, 0, false, null);
        var result = rule.evaluate(ctx);
        assertThat(result.triggered()).isTrue();
        assertThat(result.riskScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("NEW_USER_HIGH_AMOUNT flags new (<24h) users with high orders")
    void newUserHighAmount_recentRegistrationHighOrder() {
        var rule = new NewUserHighAmountRule();
        var ctx = new FraudRule.FraudCheckContext(
            UUID.randomUUID(), UUID.randomUUID(), null, null,
            new BigDecimal("6000"), "TRY", 1, 0, BigDecimal.ZERO, 0, true,
            Instant.now().minus(2, ChronoUnit.HOURS));
        var result = rule.evaluate(ctx);
        assertThat(result.triggered()).isTrue();
        assertThat(result.riskScore()).isEqualTo(40);
    }

    @Test
    @DisplayName("NEW_USER_HIGH_AMOUNT does not flag old account")
    void newUserHighAmount_oldUser_notTriggered() {
        var rule = new NewUserHighAmountRule();
        var ctx = new FraudRule.FraudCheckContext(
            UUID.randomUUID(), UUID.randomUUID(), null, null,
            new BigDecimal("6000"), "TRY", 1, 0, BigDecimal.ZERO, 0, false,
            Instant.now().minus(30, ChronoUnit.DAYS));
        var result = rule.evaluate(ctx);
        assertThat(result.triggered()).isFalse();
    }

    private FraudRule.FraudCheckContext baseContext(BigDecimal amount) {
        return new FraudRule.FraudCheckContext(
            UUID.randomUUID(), UUID.randomUUID(), null, null,
            amount, "TRY", 1, 0, BigDecimal.ZERO, 0, false, null);
    }
}
