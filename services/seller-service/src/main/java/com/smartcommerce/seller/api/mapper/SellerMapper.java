package com.smartcommerce.seller.api.mapper;

import com.smartcommerce.seller.api.dto.SellerDto;
import com.smartcommerce.seller.domain.Seller;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SellerMapper {
    SellerDto toDto(Seller seller);
}
