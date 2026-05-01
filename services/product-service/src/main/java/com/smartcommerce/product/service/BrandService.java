package com.smartcommerce.product.service;

import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.product.api.dto.BrandResponse;
import com.smartcommerce.product.api.mapper.BrandMapper;
import com.smartcommerce.product.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService {
    private final BrandRepository brandRepository;
    private final BrandMapper mapper;

    public List<BrandResponse> listAll() {
        return brandRepository.findAllActiveOrdered().stream().map(mapper::toResponse).toList();
    }

    public BrandResponse getBySlug(String slug) {
        return brandRepository.findBySlug(slug)
            .map(mapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Brand not found: " + slug));
    }
}
