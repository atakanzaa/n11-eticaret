package com.smartcommerce.inventory.repository;

import com.smartcommerce.inventory.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.*;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    Optional<InventoryItem> findByOfferId(UUID offerId);
    boolean existsByOfferId(UUID offerId);
    List<InventoryItem> findByOfferIdIn(Collection<UUID> offerIds);

    long countBySellerId(UUID sellerId);

    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.sellerId = :sellerId AND i.availableQuantity <= i.lowStockThreshold")
    long countLowStockBySeller(UUID sellerId);
}
