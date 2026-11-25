package vn.thefilm.backend.security;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import vn.thefilm.backend.service.DepartmentPermissionService;

@Component("adminModuleAccess")
public class AdminModuleAccess {
    private final DepartmentPermissionService permissions;

    public AdminModuleAccess(DepartmentPermissionService permissions) {
        this.permissions = permissions;
    }

    public boolean canAccess(Authentication authentication, String moduleCode) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof TheFilmUserPrincipal principal)
                || !"STAFF".equals(principal.accountType())) return false;
        List<String> roles = principal.roles();
        if (roles == null || roles.isEmpty()) {
            roles = authentication.getAuthorities().stream().map(Object::toString).toList();
        }
        return permissions.canAccess(principal.staffAccountId(), roles, moduleCode);
    }
}
