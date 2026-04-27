package com.smartcommerce.cart.repository;

import com.smartcommerce.cart.domain.Cart;
import com.smartcommerce.cart.domain.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.*;

public interface CartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUserIdAndStatus(UUID userId, CartStatus status);

    List<Cart> findByStatusAndUpdatedAtBefore(CartStatus status, Instant threshold);
}
