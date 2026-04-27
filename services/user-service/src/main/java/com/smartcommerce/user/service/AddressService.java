package com.smartcommerce.user.service;

import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.user.api.dto.AddressDto;
import com.smartcommerce.user.api.dto.CreateAddressRequest;
import com.smartcommerce.user.api.dto.UpdateAddressRequest;
import com.smartcommerce.user.api.mapper.AddressMapper;
import com.smartcommerce.user.domain.Address;
import com.smartcommerce.user.domain.AddressType;
import com.smartcommerce.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    public List<AddressDto> getByUserId(UUID userId) {
        return addressRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
            .stream()
            .map(addressMapper::toDto)
            .toList();
    }

    public AddressDto getById(UUID id) {
        var address = addressRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Address not found"));
        return addressMapper.toDto(address);
    }

    @Transactional
    public AddressDto create(UUID userId, CreateAddressRequest request) {
        unsetDefaults(userId, request.defaultShipping(), request.defaultBilling());
        var address = Address.builder()
            .userId(userId)
            .label(request.label())
            .fullName(request.fullName())
            .phone(request.phone())
            .country(request.country() == null ? "TR" : request.country())
            .city(request.city())
            .district(request.district())
            .neighborhood(request.neighborhood())
            .street(request.street())
            .buildingNo(request.buildingNo())
            .apartmentNo(request.apartmentNo())
            .postalCode(request.postalCode())
            .fullAddress(request.fullAddress())
            .defaultShipping(request.defaultShipping())
            .defaultBilling(request.defaultBilling())
            .addressType(request.addressType() == null ? AddressType.HOME : request.addressType())
            .build();
        return addressMapper.toDto(addressRepository.save(address));
    }

    @Transactional
    public AddressDto update(UUID userId, UUID id, UpdateAddressRequest request) {
        var address = addressRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Address not found"));
        unsetDefaults(userId, request.defaultShipping(), request.defaultBilling());
        address.setLabel(request.label());
        address.setFullName(request.fullName());
        address.setPhone(request.phone());
        address.setCountry(request.country() == null ? "TR" : request.country());
        address.setCity(request.city());
        address.setDistrict(request.district());
        address.setNeighborhood(request.neighborhood());
        address.setStreet(request.street());
        address.setBuildingNo(request.buildingNo());
        address.setApartmentNo(request.apartmentNo());
        address.setPostalCode(request.postalCode());
        address.setFullAddress(request.fullAddress());
        address.setDefaultShipping(request.defaultShipping());
        address.setDefaultBilling(request.defaultBilling());
        address.setAddressType(request.addressType() == null ? AddressType.HOME : request.addressType());
        return addressMapper.toDto(addressRepository.save(address));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        var address = addressRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Address not found"));
        address.setDeletedAt(Instant.now());
        addressRepository.save(address);
    }

    private void unsetDefaults(UUID userId, boolean defaultShipping, boolean defaultBilling) {
        if (defaultShipping) {
            addressRepository.findByUserIdAndDefaultShippingTrueAndDeletedAtIsNull(userId)
                .forEach(address -> address.setDefaultShipping(false));
        }
        if (defaultBilling) {
            addressRepository.findByUserIdAndDefaultBillingTrueAndDeletedAtIsNull(userId)
                .forEach(address -> address.setDefaultBilling(false));
        }
    }
}
