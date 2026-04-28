package com.smartcommerce.returns.repository;

import com.smartcommerce.returns.domain.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReturnItemRepository extends JpaRepository<ReturnItem, UUID> {
    List<ReturnItem> findByReturnId(UUID returnId);
}
