package com.smartcommerce.seller.api;

import com.smartcommerce.seller.api.dto.SellerDto;
import com.smartcommerce.seller.api.dto.UpdateSellerRequest;
import com.smartcommerce.seller.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
@Tag(name = "Sellers", description = "Seller profile management")
public class SellerController {

    private final SellerService sellerService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get current seller's profile")
    public SellerDto getMyProfile(@AuthenticationPrincipal String userId) {
        return sellerService.getByUserId(UUID.fromString(userId));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Update current seller's profile")
    public SellerDto updateMyProfile(@AuthenticationPrincipal String userId,
                                     @Valid @RequestBody UpdateSellerRequest request) {
        return sellerService.update(UUID.fromString(userId), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get seller by ID (public)")
    public SellerDto getSeller(@PathVariable UUID id) {
        return sellerService.getById(id);
    }
}
