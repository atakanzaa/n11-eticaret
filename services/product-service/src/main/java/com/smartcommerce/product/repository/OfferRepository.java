package com.smartcommerce.product.repository;

import com.smartcommerce.product.domain.Offer;
import com.smartcommerce.product.domain.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {
    boolean existsByProductIdAndSellerId(UUID productId, UUID sellerId);
    Optional<Offer> findByIdAndDeletedAtIsNull(UUID id);
    List<Offer> findByProductIdAndStatusAndDeletedAtIsNull(UUID productId, OfferStatus status);
    List<Offer> findBySellerIdAndDeletedAtIsNull(UUID sellerId);
}
