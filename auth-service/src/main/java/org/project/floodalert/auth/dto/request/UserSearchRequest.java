package org.project.floodalert.auth.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.floodalert.auth.enums.UserStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSearchRequest {
    
    /**
     * Tìm kiếm theo từ khóa (tên, email, số điện thoại)
     */
    String keyword;
    
    /**
     * Lọc theo trạng thái người dùng
     */
    UserStatus status;
    
    /**
     * Lọc theo danh sách role
     */
    List<String> roles;
    
    /**
     * Lọc theo email đã xác thực
     */
    Boolean emailVerified;
    
    /**
     * Lọc theo auth provider
     */
    String authProvider;
}
