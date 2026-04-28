package com.smartcommerce.promotion.service;

import com.smartcommerce.promotion.api.dto.ApplyCouponRequest;
import com.smartcommerce.promotion.api.dto.ValidateCouponRequest;
import com.smartcommerce.promotion.api.mapper.CouponMapper;
import com.smartcommerce.promotion.domain.Coupon;
import com.smartcommerce.promotion.domain.CouponUsage;
import com.smartcommerce.promotion.domain.DiscountType;
import com.smartcommerce.promotion.repository.CouponRepository;
import com.smartcommerce.promotion.repository.CouponUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock CouponRepository couponRepository;
    @Mock CouponUsageRepository usageRepository;

    CouponService service;

    @BeforeEach
    void setUp() {
        service = new CouponService(couponRepository, usageRepository, new CouponMapper());
    }

    @Test
    @DisplayName("WELCOME10 percentage discount on cart=200 returns 20.00 discount")
    void validate_percentageOnEligibleCart() {
        var coupon = baseCoupon("WELCOME10", DiscountType.PERCENTAGE, new BigDecimal("10.00"));
        coupon.setMinimumOrderAmount(new BigDecimal("100.00"));
        when(couponRepository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponIdAndUserId(any(), any())).thenReturn(0L);

        var result = service.validate(new ValidateCouponRequest(
            "WELCOME10", UUID.randomUUID(), new BigDecimal("200.00"), BigDecimal.ZERO,
            List.of(), List.of(), List.of(), false));

        assertThat(result.valid()).isTrue();
        assertThat(result.discountAmount()).isEqualByComparingTo("20.00");
        assertThat(result.discountType()).isEqualTo("PERCENTAGE");
    }

    @Test
    @DisplayName("WELCOME10 below minimum cart amount → MIN_AMOUNT_NOT_MET")
    void validate_belowMinimum() {
        var coupon = baseCoupon("WELCOME10", DiscountType.PERCENTAGE, new BigDecimal("10.00"));
        coupon.setMinimumOrderAmount(new BigDecimal("100.00"));
        when(couponRepository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));

        var result = service.validate(new ValidateCouponRequest(
            "WELCOME10", UUID.randomUUID(), new BigDecimal("50.00"), BigDecimal.ZERO,
            List.of(), List.of(), List.of(), false));

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo("MIN_AMOUNT_NOT_MET");
    }

    @Test
    @DisplayName("Same user using coupon twice hits PER_USER_LIMIT_REACHED")
    void validate_perUserLimitReached() {
        var coupon = baseCoupon("WELCOME10", DiscountType.PERCENTAGE, new BigDecimal("10.00"));
        coupon.setPerUserLimit(1);
        when(couponRepository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));
        when(usageRepository.countByCouponIdAndUserId(any(), any())).thenReturn(1L);

        var result = service.validate(new ValidateCouponRequest(
            "WELCOME10", UUID.randomUUID(), new BigDecimal("200.00"), BigDecimal.ZERO,
            List.of(), List.of(), List.of(), false));

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo("PER_USER_LIMIT_REACHED");
    }

    @Test
    @DisplayName("Expired coupon → COUPON_EXPIRED")
    void validate_expiredCoupon() {
        var coupon = baseCoupon("OLDCODE", DiscountType.PERCENTAGE, new BigDecimal("10.00"));
        coupon.setValidFrom(Instant.now().minus(60, ChronoUnit.DAYS));
        coupon.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
        when(couponRepository.findByCode("OLDCODE")).thenReturn(Optional.of(coupon));

        var result = service.validate(new ValidateCouponRequest(
            "OLDCODE", UUID.randomUUID(), new BigDecimal("200.00"), BigDecimal.ZERO,
            List.of(), List.of(), List.of(), false));

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo("COUPON_EXPIRED");
    }

    @Test
    @DisplayName("Apply coupon increments timesUsed and creates usage record")
    void apply_recordsUsage() {
        var coupon = baseCoupon("WELCOME10", DiscountType.PERCENTAGE, new BigDecimal("10.00"));
        coupon.setId(UUID.randomUUID());
        coupon.setTimesUsed(3);
        when(couponRepository.findByCode("WELCOME10")).thenReturn(Optional.of(coupon));
        when(usageRepository.findByCouponIdAndOrderId(any(), any())).thenReturn(Optional.empty());
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        service.apply(new ApplyCouponRequest("WELCOME10", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("20.00")));

        assertThat(coupon.getTimesUsed()).isEqualTo(4);
        verify(usageRepository).save(any(CouponUsage.class));
    }

    private Coupon baseCoupon(String code, DiscountType type, BigDecimal value) {
        return Coupon.builder()
            .id(UUID.randomUUID())
            .code(code)
            .name(code)
            .discountType(type)
            .discountValue(value)
            .perUserLimit(1)
            .timesUsed(0)
            .validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
            .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
            .active(true)
            .firstOrderOnly(false)
            .stackable(false)
            .build();
    }
}
