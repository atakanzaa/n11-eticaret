package com.smartcommerce.ai.service;

import com.smartcommerce.ai.repository.AiUsageLogRepository;
import com.smartcommerce.common.errors.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiBudgetGuardTest {

    @Mock AiUsageLogRepository repository;

    AiBudgetGuard guard;

    @BeforeEach
    void setUp() {
        guard = new AiBudgetGuard(repository);
        ReflectionTestUtils.setField(guard, "dailyBudget", new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("check passes when today's spend below budget")
    void check_underBudgetPasses() {
        when(repository.sumCostUsdSince(any(Instant.class))).thenReturn(new BigDecimal("3.50"));
        guard.check();
    }

    @Test
    @DisplayName("check throws when today's spend equals or exceeds budget")
    void check_atBudgetThrows() {
        when(repository.sumCostUsdSince(any(Instant.class))).thenReturn(new BigDecimal("10.00"));
        assertThatThrownBy(() -> guard.check())
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("budget exceeded");
    }

    @Test
    @DisplayName("remainingBudget returns positive value when under budget")
    void remainingBudget_underBudget() {
        when(repository.sumCostUsdSince(any(Instant.class))).thenReturn(new BigDecimal("4.25"));
        assertThat(guard.remainingBudget()).isEqualByComparingTo("5.75");
    }

    @Test
    @DisplayName("remainingBudget clamps to zero when over budget")
    void remainingBudget_overBudgetClampsToZero() {
        when(repository.sumCostUsdSince(any(Instant.class))).thenReturn(new BigDecimal("12.00"));
        assertThat(guard.remainingBudget()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("todaySpend returns zero when no usage")
    void todaySpend_noUsageReturnsZero() {
        when(repository.sumCostUsdSince(any(Instant.class))).thenReturn(null);
        assertThat(guard.todaySpend()).isEqualByComparingTo("0");
    }
}
