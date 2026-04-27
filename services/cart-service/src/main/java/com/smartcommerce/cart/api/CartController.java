package com.smartcommerce.cart.api;

import com.smartcommerce.cart.api.dto.*;
import com.smartcommerce.cart.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
public class CartController {
    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal String userId) {
        return cartService.getCart(UUID.fromString(userId));
    }

    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal String userId,
                                @Valid @RequestBody AddItemRequest request) {
        return cartService.addItem(UUID.fromString(userId), request);
    }

    @PutMapping("/items/{offerId}")
    public CartResponse updateQuantity(@AuthenticationPrincipal String userId,
                                       @PathVariable UUID offerId,
                                       @RequestParam @Min(1) int quantity) {
        return cartService.updateQuantity(UUID.fromString(userId), offerId, quantity);
    }

    @DeleteMapping("/items/{offerId}")
    public CartResponse removeItem(@AuthenticationPrincipal String userId, @PathVariable UUID offerId) {
        return cartService.removeItem(UUID.fromString(userId), offerId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@AuthenticationPrincipal String userId) {
        cartService.clearCart(UUID.fromString(userId));
    }

    @PostMapping("/validate")
    public CartValidationResponse validateCart(@AuthenticationPrincipal String userId) {
        return cartService.validateCart(UUID.fromString(userId));
    }
}
