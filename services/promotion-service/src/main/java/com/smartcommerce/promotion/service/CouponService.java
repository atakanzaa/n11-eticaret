package com.smartcommerce.promotion.service;

import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.DuplicateResourceException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.promotion.api.dto.ApplyCouponRequest;
import com.smartcommerce.promotion.api.dto.CouponResponse;
import com.smartcommerce.promotion.api.dto.CouponValidationResponse;
import com.smartcommerce.promotion.api.dto.CreateCouponRequest;
import com.smartcommerce.promotion.api.dto.ValidateCouponRequest;
import com.smartcommerce.promotion.api.mapper.CouponMapper;
import com.smartcommerce.promotion.domain.Coupon;
import com.smartcommerce.promotion.domain.CouponUsage;
import com.smartcommerce.promotion.domain.DiscountType;
import com.smartcommerce.promotion.repository.CouponRepository;
import com.smartcommerce.promotion.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository usageRepository;
    private final CouponMapper mapper;

    public CouponValidationResponse validate(ValidateCouponRequest request) {
        var coupon = couponRepository.findByCode(request.code()).orElse(null);
        if (coupon == null) {
            return CouponValidationResponse.invalid(request.code(), "COUPON_NOT_FOUND", "Kupon bulunamadı");
        }

        var now = Instant.now();
        if (!coupon.isActive() || now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            return CouponValidationResponse.invalid(request.code(), "COUPON_EXPIRED",
                "Kupon geçersiz veya süresi dolmuş");
        }
        if (coupon.isFirstOrderOnly() && !request.firstOrder()) {
            return CouponValidationResponse.invalid(request.code(), "FIRST_ORDER_ONLY",
                "Bu kupon sadece ilk siparişte geçerli");
        }
        if (coupon.getMinimumOrderAmount() != null
                && request.cartTotal().compareTo(coupon.getMinimumOrderAmount()) < 0) {
            return CouponValidationResponse.invalid(request.code(), "MIN_AMOUNT_NOT_MET",
                "Minimum sepet tutarı: " + coupon.getMinimumOrderAmount());
        }
        if (coupon.getTotalUsageLimit() != null && coupon.getTimesUsed() >= coupon.getTotalUsageLimit()) {
            return CouponValidationResponse.invalid(request.code(), "COUPON_LIMIT_REACHED",
                "Kupon kullanım limiti dolmuş");
        }
        var userUsage = usageRepository.countByCouponIdAndUserId(coupon.getId(), request.userId());
        if (userUsage >= coupon.getPerUserLimit()) {
            return CouponValidationResponse.invalid(request.code(), "PER_USER_LIMIT_REACHED",
                "Bu kuponu zaten kullandınız");
        }
        var discount = calculateDiscount(coupon, request);
        return CouponValidationResponse.valid(request.code(), coupon.getDiscountType().name(), discount);
    }

    private BigDecimal calculateDiscount(Coupon coupon, ValidateCouponRequest req) {
        return switch (coupon.getDiscountType()) {
            case PERCENTAGE -> {
                var raw = req.cartTotal().multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                yield coupon.getMaxDiscountAmount() != null
                        && raw.compareTo(coupon.getMaxDiscountAmount()) > 0
                    ? coupon.getMaxDiscountAmount() : raw;
            }
            case FIXED_AMOUNT -> coupon.getDiscountValue().min(req.cartTotal());
            case FREE_SHIPPING -> req.shippingCost() != null ? req.shippingCost() : BigDecimal.ZERO;
        };
    }

    @Transactional
    public CouponResponse apply(ApplyCouponRequest request) {
        var coupon = couponRepository.findByCode(request.code())
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Coupon not found"));
        var existing = usageRepository.findByCouponIdAndOrderId(coupon.getId(), request.orderId());
        if (existing.isPresent()) {
            log.info("Coupon {} already applied to order {}, skipping", request.code(), request.orderId());
            return mapper.toResponse(coupon);
        }
        coupon.setTimesUsed(coupon.getTimesUsed() + 1);
        couponRepository.save(coupon);
        usageRepository.save(CouponUsage.builder()
            .couponId(coupon.getId()).userId(request.userId()).orderId(request.orderId())
            .discountAmount(request.discountAmount()).build());
        log.info("Applied coupon {} to order {} (discount {})",
            request.code(), request.orderId(), request.discountAmount());
        return mapper.toResponse(coupon);
    }

    @Transactional
    public CouponResponse create(CreateCouponRequest request) {
        return createInternal(request, null);
    }

    @Transactional
    public CouponResponse createAsSeller(UUID sellerId, CreateCouponRequest request) {
        return createInternal(request, sellerId);
    }

    private CouponResponse createInternal(CreateCouponRequest request, UUID sellerScope) {
        if (couponRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException(ErrorCode.DUPLICATE_RESOURCE,
                "Coupon code already exists: " + request.code());
        }
        if (request.validUntil().isBefore(request.validFrom())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "validUntil must be after validFrom");
        }
        var builder = Coupon.builder()
            .code(request.code())
            .name(request.name())
            .description(request.description())
            .discountType(request.discountType())
            .discountValue(request.discountValue())
            .maxDiscountAmount(request.maxDiscountAmount())
            .minimumOrderAmount(request.minimumOrderAmount())
            .totalUsageLimit(request.totalUsageLimit())
            .perUserLimit(request.perUserLimit() != null ? request.perUserLimit() : 1)
            .validFrom(request.validFrom())
            .validUntil(request.validUntil())
            .firstOrderOnly(Boolean.TRUE.equals(request.firstOrderOnly()))
            .stackable(Boolean.TRUE.equals(request.stackable()))
            .active(true);
        if (sellerScope != null) {
            builder.applicableSellerIds(new ArrayList<>(List.of(sellerScope)));
        }
        return mapper.toResponse(couponRepository.save(builder.build()));
    }

    @Transactional
    public CouponResponse deactivate(UUID id) {
        var coupon = couponRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Coupon not found"));
        coupon.setActive(false);
        return mapper.toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse deactivateAsSeller(UUID sellerId, UUID id) {
        var coupon = couponRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Coupon not found"));
        if (coupon.getApplicableSellerIds() == null || !coupon.getApplicableSellerIds().contains(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, org.springframework.http.HttpStatus.FORBIDDEN,
                "Cannot modify another seller's coupon");
        }
        coupon.setActive(false);
        return mapper.toResponse(couponRepository.save(coupon));
    }

    public List<CouponResponse> listAll() {
        return couponRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    public List<CouponResponse> listActive() {
        return couponRepository.findCurrentlyActive(Instant.now()).stream()
            .map(mapper::toResponse).toList();
    }

    public List<CouponResponse> listMine(UUID sellerId) {
        var sellerIdJson = "[\"" + sellerId.toString() + "\"]";
        return couponRepository.findByApplicableSellerId(sellerIdJson).stream()
            .map(mapper::toResponse).toList();
    }
}
