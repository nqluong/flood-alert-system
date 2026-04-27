package org.project.floodalert.auth.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserListResponse {
    String userId;
    String email;
    String phone;
    String fullName;
    String avatarUrl;
    String status;
    Boolean emailVerified;
    String authProvider;
    int reputationScore;
    int totalReportsSubmitted;
    List<String> roles;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime lastLoginAt;
}
