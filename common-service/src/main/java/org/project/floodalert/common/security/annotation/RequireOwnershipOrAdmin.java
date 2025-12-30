package org.project.floodalert.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireOwnershipOrAdmin {
    /**
     * Tên của parameter chứa userId cần kiểm tra
     * Mặc định là "userId"
     */
    String userIdParam() default "userId";

    /**
     * Có cho phép admin bypass không
     * Mặc định là true
     */
    boolean allowAdmin() default true;
}
