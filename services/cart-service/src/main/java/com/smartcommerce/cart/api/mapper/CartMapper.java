package com.smartcommerce.cart.api.mapper;

import com.smartcommerce.cart.api.dto.*;
import com.smartcommerce.cart.domain.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CartMapper {
    @Mapping(target = "total", expression = "java(cart.total())")
    @Mapping(target = "itemCount", expression = "java(cart.getItems().stream().mapToInt(com.smartcommerce.cart.domain.CartItem::getQuantity).sum())")
    CartResponse toResponse(Cart cart);

    @Mapping(target = "subtotal", expression = "java(item.subtotal())")
    CartItemDto toDto(CartItem item);
}
