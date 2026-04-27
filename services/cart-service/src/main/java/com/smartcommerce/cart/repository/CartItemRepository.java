package com.smartcommerce.cart.repository;

import com.smartcommerce.cart.domain.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByCartIdAndOfferId(UUID cartId, UUID offerId);
    List<CartItem> findByCartId(UUID cartId);
    void deleteByCartIdAndOfferId(UUID cartId, UUID offerId);
    void deleteByCartId(UUID cartId);
}
