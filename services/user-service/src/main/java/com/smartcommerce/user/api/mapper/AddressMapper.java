package com.smartcommerce.user.api.mapper;

import com.smartcommerce.user.api.dto.AddressDto;
import com.smartcommerce.user.domain.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    AddressDto toDto(Address address);
}
