package com.smartcommerce.user.api.dto;

import com.smartcommerce.user.domain.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
    @Size(max = 50) String label,
    @NotBlank @Size(max = 255) String fullName,
    @NotBlank @Size(max = 20) String phone,
    @Size(max = 2) String country,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 100) String district,
    @Size(max = 100) String neighborhood,
    @NotBlank @Size(max = 255) String street,
    @Size(max = 20) String buildingNo,
    @Size(max = 20) String apartmentNo,
    @Size(max = 20) String postalCode,
    @NotBlank String fullAddress,
    boolean defaultShipping,
    boolean defaultBilling,
    AddressType addressType
) {
}
