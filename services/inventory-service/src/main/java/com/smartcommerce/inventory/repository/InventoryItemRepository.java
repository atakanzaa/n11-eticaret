package com.smartcommerce.inventory.repository;

import com.smartcommerce.inventory.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    Optional<InventoryItem> findByOfferId(UUID offerId);
    boolean existsByOfferId(UUID offerId);
}
