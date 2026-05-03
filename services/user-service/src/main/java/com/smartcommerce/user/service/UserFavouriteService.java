package com.smartcommerce.user.service;

import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.user.api.dto.FavouriteResponse;
import com.smartcommerce.user.domain.UserFavourite;
import com.smartcommerce.user.repository.UserFavouriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserFavouriteService {

    private final UserFavouriteRepository repository;

    @Transactional(readOnly = true)
    public Page<FavouriteResponse> list(UUID userId, Pageable pageable) {
        return repository.findByUserIdOrderByAddedAtDesc(userId, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public boolean contains(UUID userId, UUID productId) {
        return repository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public FavouriteResponse add(UUID userId, UUID productId) {
        if (repository.existsByUserIdAndProductId(userId, productId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, HttpStatus.CONFLICT,
                "Bu ürün zaten favorilerinizde");
        }
        var favourite = UserFavourite.builder()
            .userId(userId)
            .productId(productId)
            .build();
        favourite = repository.save(favourite);
        return toResponse(favourite);
    }

    @Transactional
    public void remove(UUID userId, UUID productId) {
        repository.deleteByUserIdAndProductId(userId, productId);
    }

    private FavouriteResponse toResponse(UserFavourite f) {
        return new FavouriteResponse(f.getId(), f.getProductId(), f.getAddedAt());
    }
}
