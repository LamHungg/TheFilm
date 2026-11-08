package vn.thefilm.backend.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import vn.thefilm.backend.service.CinemaScopeService;

@Component
public class CinemaScopeValidator {

    private final CinemaScopeService cinemaScopeService;

    public CinemaScopeValidator(CinemaScopeService cinemaScopeService) {
        this.cinemaScopeService = cinemaScopeService;
    }

    public void requireAccess(Authentication authentication, Long cinemaId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required for cinema-scoped operations.");
        }
        if (cinemaId == null || cinemaId <= 0) {
            throw new AccessDeniedException("A valid target cinema is required for this operation.");
        }
        if (authentication.getAuthorities() != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CHAIN_ADMIN"::equals)) {
            return;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof TheFilmUserPrincipal userPrincipal)
                || !cinemaScopeService.hasEffectiveCinemaScope(userPrincipal.appUserId(), cinemaId)) {
            throw new AccessDeniedException("The account does not have an active assignment to this cinema.");
        }
    }
}
