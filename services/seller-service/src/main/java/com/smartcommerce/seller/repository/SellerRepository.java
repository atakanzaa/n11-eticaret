package com.smartcommerce.seller.repository;

import com.smartcommerce.seller.domain.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SellerRepository extends JpaRepository<Seller, UUID> {
    Optional<Seller> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
