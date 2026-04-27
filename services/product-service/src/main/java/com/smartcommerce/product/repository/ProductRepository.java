package com.smartcommerce.product.repository;

import com.smartcommerce.product.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsBySlug(String slug);

    @Query("""
        select p from Product p
        where p.deletedAt is null
          and (:query is null or lower(p.title) like lower(concat('%', :query, '%')))
          and (:categoryId is null or p.categoryId = :categoryId)
          and (:brandId is null or p.brandId = :brandId)
        """)
    Page<Product> search(@Param("query") String query,
                         @Param("categoryId") UUID categoryId,
                         @Param("brandId") UUID brandId,
                         Pageable pageable);
}
