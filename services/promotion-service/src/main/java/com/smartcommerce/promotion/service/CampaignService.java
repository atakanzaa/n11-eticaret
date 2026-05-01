package com.smartcommerce.promotion.service;

import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.promotion.api.dto.CampaignResponse;
import com.smartcommerce.promotion.api.dto.CreateCampaignRequest;
import com.smartcommerce.promotion.domain.Campaign;
import com.smartcommerce.promotion.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;

    @Transactional
    public CampaignResponse create(UUID sellerId, CreateCampaignRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "endsAt must be after startsAt");
        }
        var campaign = Campaign.builder()
            .offerId(request.offerId())
            .sellerId(sellerId)
            .productId(request.productId())
            .discountedPrice(request.discountedPrice())
            .startsAt(request.startsAt())
            .endsAt(request.endsAt())
            .active(true)
            .build();
        campaign = campaignRepository.save(campaign);
        log.info("Campaign created: id={} offerId={} seller={}", campaign.getId(), campaign.getOfferId(), sellerId);
        return toResponse(campaign);
    }

    public List<CampaignResponse> listMine(UUID sellerId) {
        return campaignRepository.findBySellerIdOrderByCreatedAtDesc(sellerId).stream()
            .map(this::toResponse).toList();
    }

    public Optional<CampaignResponse> getActiveByOfferId(UUID offerId) {
        return campaignRepository.findActiveByOfferId(offerId, Instant.now()).map(this::toResponse);
    }

    @Transactional
    public CampaignResponse deactivate(UUID sellerId, UUID id) {
        var campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Campaign not found"));
        if (!campaign.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                "Cannot modify another seller's campaign");
        }
        campaign.setActive(false);
        return toResponse(campaignRepository.save(campaign));
    }

    @Transactional
    public void delete(UUID sellerId, UUID id) {
        var campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Campaign not found"));
        if (!campaign.getSellerId().equals(sellerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                "Cannot delete another seller's campaign");
        }
        campaignRepository.delete(campaign);
    }

    private CampaignResponse toResponse(Campaign c) {
        return new CampaignResponse(c.getId(), c.getOfferId(), c.getSellerId(), c.getProductId(),
            c.getDiscountedPrice(), c.getStartsAt(), c.getEndsAt(), c.isActive());
    }
}
