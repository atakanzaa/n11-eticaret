package com.smartcommerce.fraud.repository;

import com.smartcommerce.fraud.domain.FraudBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FraudBlacklistRepository extends JpaRepository<FraudBlacklist, UUID> {
    boolean existsByBlacklistTypeAndValue(String blacklistType, String value);
    List<FraudBlacklist> findByBlacklistType(String blacklistType);
}
