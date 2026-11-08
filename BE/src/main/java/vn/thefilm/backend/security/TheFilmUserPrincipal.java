package vn.thefilm.backend.security;

/** Authenticated identity exposed to application code after Keycloak JWT validation. */
public record TheFilmUserPrincipal(
        String oidcSubject,
        String oidcIssuer,
        String email,
        String preferredUsername,
        String displayName,
        Long appUserId) {
}
