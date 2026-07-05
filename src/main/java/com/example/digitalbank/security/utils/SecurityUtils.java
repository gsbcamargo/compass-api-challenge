package com.example.digitalbank.security.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    public static boolean canAccess(UUID accountId, Authentication authentication) {
        return !isAdmin(authentication) && !accountId.equals(authentication.getPrincipal());
    }
}