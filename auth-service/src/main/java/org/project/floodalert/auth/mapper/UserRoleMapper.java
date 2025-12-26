package org.project.floodalert.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.project.floodalert.auth.dto.response.UserRoleResponse;
import org.project.floodalert.auth.model.Role;
import org.project.floodalert.auth.model.User;
import org.project.floodalert.auth.model.UserRole;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserRoleMapper {

    @Mapping(target = "id", source = "userRole.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "userFullName", source = "user.fullName")
    @Mapping(target = "roleName", source = "role.name")
    UserRoleResponse toResponse(UserRole userRole, User user, Role role);

    @Mapping(target = "userEmail", ignore = true)
    @Mapping(target = "userFullName", ignore = true)
    @Mapping(target = "roleName", ignore = true)
    UserRoleResponse toResponse(UserRole userRole);
}
