package com.smartcommerce.order.security;

import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.order.client.SellerClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Verifies that the authenticated user owns the seller record they are acting on.
 *
 * Why: SELLER endpoints accept {sellerId} in the path, and a SELLER role check alone
 * would let any seller view another seller's KPI/orders. This guard resolves the
 * caller's seller record (by user-id) and asserts it matches the path id.
 *
 * Admins bypass the check.
 */
@Component
@RequiredArgsConstructor
public class SellerOwnershipGuard {

    private final SellerClient sellerClient;

    public void requireOwnsSeller(String userId, UUID sellerId, Authentication auth) {
        if (isAdmin(auth)) return;
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Authentication required");
        }
        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Invalid user id");
        }
        SellerClient.SellerDto seller = sellerClient.getByUserId(userUuid);
        if (seller == null || !sellerId.equals(seller.id())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                "Cannot access another seller's data");
        }
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(ga.getAuthority())) return true;
        }
        return false;
    }
}
