package com.smartcommerce.user.api.dto;

import com.smartcommerce.user.domain.AddressType;

import java.util.UUID;

public record AddressDto(
    UUID id,
    String label,
    String fullName,
    String phone,
    String country,
    String city,
    String district,
    String neighborhood,
    String street,
    String buildingNo,
    String apartmentNo,
    String postalCode,
    String fullAddress,
    boolean defaultShipping,
    boolean defaultBilling,
    AddressType addressType
) {
}
