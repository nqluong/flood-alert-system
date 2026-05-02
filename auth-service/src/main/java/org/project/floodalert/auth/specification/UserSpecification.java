package org.project.floodalert.auth.specification;

import jakarta.persistence.criteria.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.project.floodalert.auth.dto.request.UserSearchRequest;
import org.project.floodalert.auth.enums.AuthProvider;
import org.project.floodalert.auth.enums.UserStatus;
import org.project.floodalert.auth.model.Role;
import org.project.floodalert.auth.model.User;
import org.project.floodalert.auth.model.UserProfile;
import org.project.floodalert.auth.model.UserRole;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserSpecification {

    UserSearchRequest searchRequest;

    public static Specification<User> withFilters(UserSearchRequest searchRequest) {
        return new UserSpecification(searchRequest).buildSpecification();
    }

    private Specification<User> buildSpecification() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(searchRequest.getKeyword())) {
                String keyword = "%" + searchRequest.getKeyword().toLowerCase() + "%";
                
                Subquery<UUID> profileSubquery = query.subquery(UUID.class);
                Root<UserProfile> profileRoot = profileSubquery.from(UserProfile.class);
                profileSubquery.select(profileRoot.get("userId"))
                        .where(criteriaBuilder.like(
                                criteriaBuilder.lower(profileRoot.get("fullName")), 
                                keyword
                        ));

                Predicate keywordPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), keyword),
                        root.get("id").in(profileSubquery)
                );
                predicates.add(keywordPredicate);
            }

            if (searchRequest.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), searchRequest.getStatus()));
            }

            if (searchRequest.getEmailVerified() != null) {
                predicates.add(criteriaBuilder.equal(root.get("emailVerified"), searchRequest.getEmailVerified()));
            }

            if (StringUtils.hasText(searchRequest.getAuthProvider())) {
                try {
                    AuthProvider provider = AuthProvider.valueOf(searchRequest.getAuthProvider().toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("authProvider"), provider));
                } catch (IllegalArgumentException e) {
                }
            }

            if (searchRequest.getRoles() != null && !searchRequest.getRoles().isEmpty()) {
                Subquery<UUID> roleSubquery = query.subquery(UUID.class);
                Root<UserRole> userRoleRoot = roleSubquery.from(UserRole.class);
                Root<Role> roleRoot = query.from(Role.class);
                
                roleSubquery.select(userRoleRoot.get("userId"))
                        .where(
                                criteriaBuilder.and(
                                        criteriaBuilder.equal(userRoleRoot.get("roleId"), roleRoot.get("id")),
                                        roleRoot.get("name").in(searchRequest.getRoles())
                                )
                        );
                
                predicates.add(root.get("id").in(roleSubquery));
            }

            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
