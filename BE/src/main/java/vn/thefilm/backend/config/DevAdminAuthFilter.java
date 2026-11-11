package vn.thefilm.backend.config;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import vn.thefilm.backend.security.TheFilmUserPrincipal;

/**
 * Chi dung khi chay profile devauth: token "mock-admin-token" duoc coi la CHAIN_ADMIN.
 * Khong dung cho moi truong that, se bo khi da cau hinh Keycloak.
 */
class DevAdminAuthFilter extends OncePerRequestFilter {

    static final String TOKEN = "mock-admin-token";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)
                && TOKEN.equals(header.substring(7).trim())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Jwt jwt = Jwt.withTokenValue(TOKEN)
                    .header("alg", "none")
                    .issuer("http://localhost:8080/dev")
                    .subject("dev-admin")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            TheFilmUserPrincipal principal = new TheFilmUserPrincipal("dev-admin", "http://localhost:8080/dev",
                    "admin@thefilm.local", "dev-admin", "Dev Admin", null);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_CHAIN_ADMIN")), "dev-admin") {
                @Override
                public Object getPrincipal() {
                    return principal;
                }
            };
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request, response);
    }
}
