package com.smartcommerce.user.repository;

import com.smartcommerce.user.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUserIdAndDeletedAtIsNull(UUID userId);
    boolean existsByUserId(UUID userId);
}
