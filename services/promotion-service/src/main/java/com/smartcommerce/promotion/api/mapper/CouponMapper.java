package com.smartcommerce.promotion.api.mapper;

import com.smartcommerce.promotion.api.dto.CouponResponse;
import com.smartcommerce.promotion.domain.Coupon;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper {
    public CouponResponse toResponse(Coupon c) {
        return new CouponResponse(
            c.getId(), c.getCode(), c.getName(), c.getDescription(),
            c.getDiscountType(), c.getDiscountValue(), c.getMaxDiscountAmount(),
            c.getMinimumOrderAmount(), c.getTotalUsageLimit(), c.getPerUserLimit(),
            c.getTimesUsed(), c.getValidFrom(), c.getValidUntil(),
            c.isActive(), c.isFirstOrderOnly(), c.isStackable());
    }
}
