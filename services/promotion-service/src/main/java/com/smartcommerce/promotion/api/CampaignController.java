package com.smartcommerce.promotion.api;

import com.smartcommerce.promotion.api.dto.CampaignResponse;
import com.smartcommerce.promotion.api.dto.CreateCampaignRequest;
import com.smartcommerce.promotion.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignResponse create(@AuthenticationPrincipal String userId,
                                   @Valid @RequestBody CreateCampaignRequest request) {
        return campaignService.create(UUID.fromString(userId), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    public List<CampaignResponse> listMine(@AuthenticationPrincipal String userId) {
        return campaignService.listMine(UUID.fromString(userId));
    }

    @GetMapping("/active-by-offer/{offerId}")
    public ResponseEntity<CampaignResponse> getActiveByOffer(@PathVariable UUID offerId) {
        return campaignService.getActiveByOfferId(offerId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.noContent().build());
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SELLER')")
    public CampaignResponse deactivate(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        return campaignService.deactivate(UUID.fromString(userId), id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal String userId, @PathVariable UUID id) {
        campaignService.delete(UUID.fromString(userId), id);
    }
}
