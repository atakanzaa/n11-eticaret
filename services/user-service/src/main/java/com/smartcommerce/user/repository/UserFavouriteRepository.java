package com.smartcommerce.user.repository;

import com.smartcommerce.user.domain.UserFavourite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserFavouriteRepository extends JpaRepository<UserFavourite, UUID> {

    Page<UserFavourite> findByUserIdOrderByAddedAtDesc(UUID userId, Pageable pageable);

    Optional<UserFavourite> findByUserIdAndProductId(UUID userId, UUID productId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    long deleteByUserIdAndProductId(UUID userId, UUID productId);
}
