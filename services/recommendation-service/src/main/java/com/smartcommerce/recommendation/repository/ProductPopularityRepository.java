package com.smartcommerce.recommendation.repository;

import com.smartcommerce.recommendation.domain.ProductPopularity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductPopularityRepository extends JpaRepository<ProductPopularity, UUID> {
    List<ProductPopularity> findTop50ByOrderByScoreDesc();
}
