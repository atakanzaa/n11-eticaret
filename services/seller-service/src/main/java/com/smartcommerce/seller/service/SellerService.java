package com.smartcommerce.seller.service;

import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.common.events.payload.SellerRegisteredPayload;
import com.smartcommerce.seller.api.dto.SellerDto;
import com.smartcommerce.seller.api.dto.UpdateSellerRequest;
import com.smartcommerce.seller.api.mapper.SellerMapper;
import com.smartcommerce.seller.domain.Seller;
import com.smartcommerce.seller.domain.SellerStatus;
import com.smartcommerce.seller.event.outbox.OutboxService;
import com.smartcommerce.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerService {

    private final SellerRepository sellerRepository;
    private final SellerMapper sellerMapper;
    private final OutboxService outboxService;

    public SellerDto getByUserId(UUID userId) {
        return sellerRepository.findByUserId(userId)
            .map(sellerMapper::toDto)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,
                "Seller not found for user: " + userId));
    }

    @Transactional
    public SellerDto getOrCreateByUserId(UUID userId) {
        var existing = sellerRepository.findByUserId(userId);
        if (existing.isPresent()) {
            return sellerMapper.toDto(existing.get());
        }

        var seller = Seller.builder()
            .userId(userId)
            .storeName("Mağazam-" + userId.toString().substring(0, 8))
            .status(SellerStatus.PENDING)
            .commissionRate(new BigDecimal("0.0500"))
            .rating(BigDecimal.ZERO)
            .ratingCount(0)
            .build();
        seller = sellerRepository.save(seller);
        log.info("Lazy-created seller record for user {}: sellerId={}", userId, seller.getId());

        outboxService.publish(
            Topics.SELLER_REGISTERED, EventType.SELLER_REGISTERED,
            seller.getId().toString(), "SELLER",
            SellerRegisteredPayload.builder()
                .sellerId(seller.getId())
                .userId(userId)
                .storeName(seller.getStoreName())
                .build()
        );

        return sellerMapper.toDto(seller);
    }

    public SellerDto getById(UUID id) {
        return sellerRepository.findById(id)
            .map(sellerMapper::toDto)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,
                "Seller not found: " + id));
    }

    @Transactional
    public SellerDto update(UUID userId, UpdateSellerRequest request) {
        var seller = sellerRepository.findByUserId(userId)
            .orElseGet(() -> createSeller(userId, "MaÄŸazam-" + userId.toString().substring(0, 8)));
        if (request.storeName() != null) {
            seller.setStoreName(request.storeName());
        }
        if (request.description() != null) {
            seller.setDescription(request.description());
        }
        return sellerMapper.toDto(sellerRepository.save(seller));
    }

    private Seller createSeller(UUID userId, String storeName) {
        var seller = Seller.builder()
            .userId(userId)
            .storeName(storeName)
            .status(SellerStatus.PENDING)
            .commissionRate(new BigDecimal("0.0500"))
            .rating(BigDecimal.ZERO)
            .ratingCount(0)
            .build();
        seller = sellerRepository.save(seller);
        log.info("Created seller record for user {}: sellerId={}", userId, seller.getId());

        outboxService.publish(
            Topics.SELLER_REGISTERED, EventType.SELLER_REGISTERED,
            seller.getId().toString(), "SELLER",
            SellerRegisteredPayload.builder()
                .sellerId(seller.getId())
                .userId(userId)
                .storeName(seller.getStoreName())
                .build()
        );
        return seller;
    }
}
