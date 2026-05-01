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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    public List<CouponResponse> listMine(@AuthenticationPrincipal String userId) {
        return couponService.listMine(UUID.fromString(userId));
    }

    @GetMapping("/active")
    public List<CouponResponse> listActive() {
        return couponService.listActive();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CouponResponse create(@AuthenticationPrincipal String userId,
                                 @Valid @RequestBody CreateCouponRequest request) {
        if (isSeller()) {
            return couponService.createAsSeller(UUID.fromString(userId), request);
        }
        return couponService.create(request);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public CouponResponse deactivate(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        if (isSeller()) {
            return couponService.deactivateAsSeller(UUID.fromString(userId), id);
        }
        return couponService.deactivate(id);
    }

    private boolean isSeller() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        var authorities = auth.getAuthorities();
        boolean isAdmin = authorities.stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean isSeller = authorities.stream().anyMatch(a -> "ROLE_SELLER".equals(a.getAuthority()));
        return isSeller && !isAdmin;
    }
}
