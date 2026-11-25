package vn.thefilm.backend.security;

import java.util.List;

/** Authenticated identity resolved from a verified application token or external JWT. */
public record TheFilmUserPrincipal(
        String oidcSubject,
        String oidcIssuer,
        String email,
        String preferredUsername,
        String displayName,
        Long appUserId,
        List<String> roles,
        String accountType,
        Long staffAccountId,
        boolean emailVerified) {

    public TheFilmUserPrincipal(
            String oidcSubject,
            String oidcIssuer,
            String email,
            String preferredUsername,
            String displayName,
            Long appUserId) {
        this(oidcSubject, oidcIssuer, email, preferredUsername, displayName, appUserId,
                List.of("ROLE_CUSTOMER"), "CUSTOMER", null, false);
    }

    public TheFilmUserPrincipal(String oidcSubject, String oidcIssuer, String email,
            String preferredUsername, String displayName, Long appUserId, List<String> roles) {
        this(oidcSubject, oidcIssuer, email, preferredUsername, displayName, appUserId,
                roles, "CUSTOMER", null, false);
    }

    public TheFilmUserPrincipal(String oidcSubject, String oidcIssuer, String email,
            String preferredUsername, String displayName, Long appUserId, List<String> roles,
            String accountType, Long staffAccountId) {
        this(oidcSubject, oidcIssuer, email, preferredUsername, displayName, appUserId,
                roles, accountType, staffAccountId, false);
    }

    public TheFilmUserPrincipal {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
