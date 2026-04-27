package com.smartcommerce.seller.service;

import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.seller.api.dto.SellerDto;
import com.smartcommerce.seller.api.dto.UpdateSellerRequest;
import com.smartcommerce.seller.api.mapper.SellerMapper;
import com.smartcommerce.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final SellerMapper sellerMapper;

    public SellerDto getByUserId(UUID userId) {
        return sellerRepository.findByUserId(userId)
            .map(sellerMapper::toDto)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,
                "Seller not found for user: " + userId));
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
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,
                "Seller not found for user: " + userId));
        seller.setStoreName(request.storeName());
        seller.setDescription(request.description());
        return sellerMapper.toDto(sellerRepository.save(seller));
    }
}
