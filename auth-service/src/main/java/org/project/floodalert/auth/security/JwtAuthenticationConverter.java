package org.project.floodalert.auth.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Extract roles từ JWT claims
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);

        // Tạo custom principal chứa thông tin user
        UserPrincipal principal = UserPrincipal.builder()
                .userId(jwt.getSubject())
                .email(jwt.getClaimAsString("email"))
                .fullName(jwt.getClaimAsString("fullName"))
                .roles(extractRoles(jwt))
                .build();

            return new CustomAuthenticationToken(jwt, principal, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> roles = extractRoles(jwt);

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Object roles = jwt.getClaim("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        return List.of();
    }
}
