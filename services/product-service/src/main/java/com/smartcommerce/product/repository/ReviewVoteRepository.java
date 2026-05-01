package com.smartcommerce.product.repository;

import com.smartcommerce.product.domain.ReviewVote;
import com.smartcommerce.product.domain.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewVoteRepository extends JpaRepository<ReviewVote, UUID> {
    Optional<ReviewVote> findByReviewIdAndUserId(UUID reviewId, UUID userId);
    boolean existsByReviewIdAndUserId(UUID reviewId, UUID userId);
    long countByReviewIdAndVoteType(UUID reviewId, VoteType voteType);
}
