package com.smartcommerce.product.api.mapper;

import com.smartcommerce.product.api.dto.OfferResponse;
import com.smartcommerce.product.domain.Offer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OfferMapper {
    OfferResponse toResponse(Offer offer);
}
