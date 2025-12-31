package org.project.floodalert.auth.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.checkerframework.checker.units.qual.N;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FirebaseUserInfo {
    String firebaseUid;
    String email;
    String fullName;
    String avatarUrl;
    boolean emailVerified;
    String provider;
}
