package com.smartcommerce.user.api.mapper;

import com.smartcommerce.user.api.dto.UserProfileDto;
import com.smartcommerce.user.domain.UserProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    UserProfileDto toDto(UserProfile profile);
}
