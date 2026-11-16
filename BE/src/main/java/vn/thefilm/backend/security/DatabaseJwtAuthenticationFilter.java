package vn.thefilm.backend.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
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

/** Validates the application's signed database-auth tokens without an external identity provider. */
public final class DatabaseJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public DatabaseJwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null
                && authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authorization.substring(7).trim();
            TheFilmUserPrincipal principal = jwtService.parseAndValidate(token);
            if (principal != null) {
                Instant now = Instant.now();
                Jwt jwt = Jwt.withTokenValue(token)
                        .header("alg", "HS256")
                        .issuer(principal.oidcIssuer())
                        .subject(principal.oidcSubject())
                        .issuedAt(now)
                        .expiresAt(Date.from(now.plusSeconds(jwtService.getExpirationSeconds())).toInstant())
                        .claim("app_user_id", principal.appUserId())
                        .claim("email", principal.email())
                        .claim("name", principal.displayName())
                        .build();
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                        jwt,
                        List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                        principal.oidcSubject()) {
                    @Override
                    public Object getPrincipal() {
                        return principal;
                    }
                };
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }
        }
        filterChain.doFilter(request, response);
    }
}
