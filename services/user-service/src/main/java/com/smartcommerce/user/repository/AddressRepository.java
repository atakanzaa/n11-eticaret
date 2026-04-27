package com.smartcommerce.user.repository;

import com.smartcommerce.user.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);
    Optional<Address> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
    List<Address> findByUserIdAndDefaultShippingTrueAndDeletedAtIsNull(UUID userId);
    List<Address> findByUserIdAndDefaultBillingTrueAndDeletedAtIsNull(UUID userId);
}
