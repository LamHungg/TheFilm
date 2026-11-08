package vn.thefilm.backend.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import vn.thefilm.backend.entity.AppUserEntity;
import vn.thefilm.backend.repository.AppUserRepository;

@Component
@Profile("oracle")
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String CLIENT_ID = "thefilm-api";

    private final AppUserRepository appUserRepository;
    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

    public KeycloakJwtAuthenticationConverter(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        Collection<GrantedAuthority> scopes = scopeConverter.convert(jwt);
        if (scopes != null) {
            authorities.addAll(scopes);
        }
        addRoles(jwt.getClaim("realm_access"), authorities);
        Object resourceAccessClaim = jwt.getClaim("resource_access");
        if (resourceAccessClaim instanceof Map<?, ?> resourceAccess) {
            addRoles(resourceAccess.get(CLIENT_ID), authorities);
        }

        String subject = jwt.getSubject();
        String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");
        String displayName = firstNonBlank(jwt.getClaimAsString("name"), username, email, subject);
        AppUserEntity appUser = findAppUser(issuer, subject, email);
        TheFilmUserPrincipal principal = new TheFilmUserPrincipal(
                subject,
                issuer,
                email,
                username,
                displayName,
                appUser == null ? null : appUser.getAppUserId());
        return new JwtAuthenticationToken(jwt, authorities, subject) {
            @Override
            public Object getPrincipal() {
                return principal;
            }
        };
    }

    private AppUserEntity findAppUser(String issuer, String subject, String email) {
        if (issuer != null && subject != null) {
            for (String candidateIssuer : issuerVariants(issuer)) {
                AppUserEntity appUser = appUserRepository
                        .findByOidcIssuerAndOidcSubject(candidateIssuer, subject)
                        .orElse(null);
                if (appUser != null) {
                    return appUser;
                }
            }
        }
        if (email != null && !email.isBlank()) {
            var emailMatches = appUserRepository.findByEmail(email);
            if (subject != null) {
                AppUserEntity subjectMatch = emailMatches.stream()
                        .filter(user -> subject.equals(user.getOidcSubject()))
                        .findFirst()
                        .orElse(null);
                if (subjectMatch != null) {
                    return subjectMatch;
                }
            }
            return emailMatches.size() == 1 ? emailMatches.get(0) : null;
        }
        return null;
    }

    static Set<String> issuerVariants(String issuer) {
        String normalized = issuer.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        Set<String> variants = new LinkedHashSet<>();
        variants.add(normalized);
        variants.add(normalized + "/");
        variants.add(issuer.trim());
        return variants;
    }

    private static void addRoles(Object accessClaim, Collection<GrantedAuthority> authorities) {
        if (!(accessClaim instanceof Map<?, ?> access)
                || !(access.get("roles") instanceof Collection<?> roles)) {
            return;
        }
        for (Object role : roles) {
            if (role instanceof String roleName && !roleName.isBlank()) {
                String normalizedRole = roleName.trim().toUpperCase(Locale.ROOT);
                if (!normalizedRole.startsWith("ROLE_")) {
                    normalizedRole = "ROLE_" + normalizedRole;
                }
                authorities.add(new SimpleGrantedAuthority(normalizedRole));
            }
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
