package com.backend.user_service.repository;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.backend.user_service.dto.RegisterUserDTO;
import com.backend.user_service.dto.UpdateUserDTO;
import com.backend.common.dto.InfoUserDTO;
import com.backend.user_service.model.User;

/**
 * MapStruct interface for mapping User Entities to and from Data Transfer Objects.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterUserDTO dto);

    /**
     * Partially updates an existing User entity.
     * Explicitly ignores core identity and generated fields to satisfy MapStruct strictness.
     * Note: Password is ignored here because it requires BCrypt hashing in the Service layer.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateUserFromDto(UpdateUserDTO dto, @MappingTarget User entity);

    InfoUserDTO toInfoUserDTO(User user);
}