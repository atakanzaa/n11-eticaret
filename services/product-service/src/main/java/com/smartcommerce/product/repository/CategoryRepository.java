package com.smartcommerce.product.repository;

import com.smartcommerce.product.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);

    @Query("SELECT c FROM Category c WHERE c.active = true ORDER BY c.displayOrder ASC, c.name ASC")
    List<Category> findAllActiveOrdered();

    @Query("SELECT COUNT(p.id) FROM Product p WHERE p.categoryId = :categoryId AND p.deletedAt IS NULL")
    long countActiveProducts(UUID categoryId);
}
