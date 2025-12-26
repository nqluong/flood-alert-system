package org.project.floodalert.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.project.floodalert.auth.dto.response.RoleResponse;
import org.project.floodalert.auth.model.Role;
import org.springframework.stereotype.Component;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RoleMapper {

    RoleResponse toResponse(Role role);

    Role toEntity(RoleResponse response);
}