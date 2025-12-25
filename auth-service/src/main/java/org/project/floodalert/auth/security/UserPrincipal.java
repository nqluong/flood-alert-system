package org.project.floodalert.auth.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal {
    private String userId;
    private String email;
    private String fullName;
    private List<String> roles;

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
