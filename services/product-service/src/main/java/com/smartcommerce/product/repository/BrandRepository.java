package com.smartcommerce.product.repository;

import com.smartcommerce.product.domain.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
    Optional<Brand> findBySlug(String slug);

    @Query("SELECT b FROM Brand b WHERE b.active = true ORDER BY b.name ASC")
    List<Brand> findAllActiveOrdered();
}
