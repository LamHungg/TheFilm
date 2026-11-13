package vn.thefilm.backend.security;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import vn.thefilm.backend.service.CinemaScopeService;
@Component
public class AdminCinemaAccess {
    private final CinemaScopeValidator validator;
    private final CinemaScopeService scopes;
    public AdminCinemaAccess(CinemaScopeValidator validator,CinemaScopeService scopes) { this.validator=validator; this.scopes=scopes; }
    /** null means chain-wide access. An empty set means no accessible cinema. */
    public Set<Long> allowed(Authentication auth,Long cinemaId) {
        if(auth==null || !auth.isAuthenticated()) throw new AccessDeniedException("Yêu cầu xác thực.");
        if(cinemaId!=null) { validator.requireAccess(auth,cinemaId); return Set.of(cinemaId); }
        if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_CHAIN_ADMIN"))) return null;
        if(!(auth.getPrincipal() instanceof TheFilmUserPrincipal p)) throw new AccessDeniedException("Yêu cầu tài khoản nội bộ.");
        return scopes.getEffectiveCinemaIds(p.appUserId());
    }
}
