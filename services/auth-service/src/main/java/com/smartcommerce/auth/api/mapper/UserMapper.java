package com.smartcommerce.auth.api.mapper;

import com.smartcommerce.auth.api.dto.UserDto;
import com.smartcommerce.auth.domain.Role;
import com.smartcommerce.auth.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStrings")
    UserDto toDto(User user);

    @Named("rolesToStrings")
    static Set<String> rolesToStrings(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }
}
