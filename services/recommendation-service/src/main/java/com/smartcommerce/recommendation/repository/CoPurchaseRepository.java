package com.smartcommerce.recommendation.repository;

import com.smartcommerce.recommendation.domain.CoPurchasePair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoPurchaseRepository extends JpaRepository<CoPurchasePair, CoPurchasePair.PairId> {
    Optional<CoPurchasePair> findByProductAIdAndProductBId(UUID productAId, UUID productBId);
    List<CoPurchasePair> findTop10ByProductAIdOrderByCoPurchaseCountDesc(UUID productAId);
}
