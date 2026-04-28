package com.smartcommerce.recommendation.repository;

import com.smartcommerce.recommendation.domain.UserBehavior;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserBehaviorRepository extends JpaRepository<UserBehavior, UUID> {
    List<UserBehavior> findTop50ByUserIdOrderByOccurredAtDesc(UUID userId);
}
