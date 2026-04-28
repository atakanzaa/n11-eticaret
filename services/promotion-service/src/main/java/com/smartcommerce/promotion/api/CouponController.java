package com.smartcommerce.promotion.api;

import com.smartcommerce.promotion.api.dto.ApplyCouponRequest;
import com.smartcommerce.promotion.api.dto.CouponResponse;
import com.smartcommerce.promotion.api.dto.CouponValidationResponse;
import com.smartcommerce.promotion.api.dto.CreateCouponRequest;
import com.smartcommerce.promotion.api.dto.ValidateCouponRequest;
import com.smartcommerce.promotion.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/validate")
    public CouponValidationResponse validate(@Valid @RequestBody ValidateCouponRequest request) {
        return couponService.validate(request);
    }

    @PostMapping("/internal/apply")
    public CouponResponse apply(@Valid @RequestBody ApplyCouponRequest request) {
        return couponService.apply(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CouponResponse> listAll() {
        return couponService.listAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@Valid @RequestBody CreateCouponRequest request) {
        return couponService.create(request);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public CouponResponse deactivate(@PathVariable UUID id) {
        return couponService.deactivate(id);
    }
}
