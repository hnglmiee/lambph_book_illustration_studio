package com.hoanglam.bis.security;

import com.hoanglam.bis.enums.ErrorCode;
import com.hoanglam.bis.exceptions.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class SecurityUtils {

    private SecurityUtils() {}

    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Not authenticated", 401);
        }

        Object principal = auth.getPrincipal();

        // TODO: chỉnh đúng theo cách JwtAuthenticationFilter của bạn set principal.
        // Case 1: nếu filter set principal = UUID trực tiếp
        if (principal instanceof UUID uuid) {
            return uuid;
        }
        // Case 2: nếu filter set principal = String (userId dạng string)
        if (principal instanceof String str) {
            return UUID.fromString(str);
        }
        // Case 3: nếu filter set principal = custom UserPrincipal có getId()
        // return ((UserPrincipal) principal).getId();

        throw new ApiException(ErrorCode.INTERNAL_ERROR, "Cannot resolve current user", 500);
    }
}