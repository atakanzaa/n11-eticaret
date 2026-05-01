package com.smartcommerce.promotion.repository;

import com.smartcommerce.promotion.domain.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    List<Campaign> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

    @Query("""
        select c from Campaign c
        where c.offerId = :offerId
          and c.active = true
          and c.startsAt <= :now
          and c.endsAt > :now
        order by c.endsAt asc
        """)
    Optional<Campaign> findActiveByOfferId(@Param("offerId") UUID offerId, @Param("now") Instant now);
}
