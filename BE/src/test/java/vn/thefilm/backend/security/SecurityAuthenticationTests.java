package vn.thefilm.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import vn.thefilm.backend.entity.AppUserEntity;
import vn.thefilm.backend.entity.EmployeeEntity;
import vn.thefilm.backend.enums.AppUserStatus;
import vn.thefilm.backend.enums.EmployeeStatus;
import vn.thefilm.backend.repository.AppUserRepository;
import vn.thefilm.backend.repository.EmployeeRepository;

class SecurityAuthenticationTests {

    private static final String ISSUER = "https://identity.example/realms/thefilm";
    private static final String SUBJECT = "keycloak-user-123";
    private static final String EMAIL = "member@example.com";
    private static final String USERNAME = "film-fan";
    private static KeyPair signingKey;
    private static KeyPair otherSigningKey;

    @BeforeAll
    static void generateSigningKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        signingKey = generator.generateKeyPair();
        otherSigningKey = generator.generateKeyPair();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void decodesSignedJwtClaimsAndConvertsKeycloakRoles() throws Exception {
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        AppUserEntity linkedUser = mock(AppUserEntity.class);
        when(appUserRepository.findByOidcIssuerAndOidcSubject(ISSUER, SUBJECT))
                .thenReturn(Optional.of(linkedUser));
        when(linkedUser.getAppUserId()).thenReturn(27L);

        Jwt decoded = decoder().decode(signedToken(signingKey, Instant.now().plusSeconds(300)));
        Authentication authentication = new KeycloakJwtAuthenticationConverter(appUserRepository).convert(decoded);

        TheFilmUserPrincipal principal = assertInstanceOf(TheFilmUserPrincipal.class, authentication.getPrincipal());
        assertEquals(SUBJECT, principal.oidcSubject());
        assertEquals(ISSUER, principal.oidcIssuer());
        assertEquals(EMAIL, principal.email());
        assertEquals(USERNAME, principal.preferredUsername());
        assertEquals("Film Fan", principal.displayName());
        assertEquals(27L, principal.appUserId());

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("ROLE_MANAGER"));
        assertTrue(authorities.contains("ROLE_CUSTOMER"));
        assertTrue(authorities.contains("SCOPE_openid"));
        assertTrue(authorities.contains("SCOPE_profile"));
    }

    @Test
    void rejectsJwtSignedByAnUntrustedKey() throws Exception {
        String token = signedToken(otherSigningKey, Instant.now().plusSeconds(300));

        assertThrows(JwtException.class, () -> decoder().decode(token));
    }

    @Test
    void rejectsExpiredJwtEvenWhenItsSignatureIsValid() throws Exception {
        String token = signedToken(signingKey, Instant.now().minusSeconds(300));

        assertThrows(JwtException.class, () -> decoder().decode(token));
    }

    @Test
    void blocksAppUserWithBlockedStatusAndReturns403() throws Exception {
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        AppUserEntity blockedUser = mock(AppUserEntity.class);
        when(appUserRepository.findById(27L)).thenReturn(Optional.of(blockedUser));
        when(blockedUser.getAppUserId()).thenReturn(27L);
        when(blockedUser.getStatus()).thenReturn(AppUserStatus.BLOCKED);

        MockHttpServletResponse response = runFilter(appUserRepository, employeeRepository, principal(27L));

        assertForbidden(response, "ACCOUNT_BLOCKED");
    }

    @Test
    void blocksTerminatedEmployeeAndReturns403() throws Exception {
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        AppUserEntity activeUser = mock(AppUserEntity.class);
        EmployeeEntity terminatedEmployee = mock(EmployeeEntity.class);
        when(appUserRepository.findById(27L)).thenReturn(Optional.of(activeUser));
        when(activeUser.getAppUserId()).thenReturn(27L);
        when(activeUser.getStatus()).thenReturn(AppUserStatus.ACTIVE);
        when(employeeRepository.findByAppUser_AppUserId(27L)).thenReturn(Optional.of(terminatedEmployee));
        when(terminatedEmployee.getStatus()).thenReturn(EmployeeStatus.TERMINATED);

        MockHttpServletResponse response = runFilter(appUserRepository, employeeRepository, principal(27L));

        assertForbidden(response, "EMPLOYEE_TERMINATED");
    }

    private static NimbusJwtDecoder decoder() {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) signingKey.getPublic()).build();
    }

    private static String signedToken(KeyPair keyPair, Instant expiresAt) throws Exception {
        Instant now = Instant.now();
        Instant issuedAt = expiresAt.isBefore(now)
                ? expiresAt.minusSeconds(60)
                : now.minusSeconds(60);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(SUBJECT)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .claim("email", EMAIL)
                .claim("preferred_username", USERNAME)
                .claim("name", "Film Fan")
                .claim("scope", "openid profile")
                .claim("realm_access", Map.of("roles", List.of("admin", "ROLE_manager")))
                .claim("resource_access", Map.of("thefilm-api", Map.of("roles", List.of("customer"))))
                .build();
        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        signedJwt.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
        return signedJwt.serialize();
    }

    private static TheFilmUserPrincipal principal(Long appUserId) {
        return new TheFilmUserPrincipal(SUBJECT, ISSUER, EMAIL, USERNAME, "Film Fan", appUserId);
    }

    private static MockHttpServletResponse runFilter(
            AppUserRepository appUserRepository,
            EmployeeRepository employeeRepository,
            TheFilmUserPrincipal principal) throws Exception {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer(ISSUER)
                .subject(SUBJECT)
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("email", EMAIL)
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of(), SUBJECT) {
            @Override
            public Object getPrincipal() {
                return principal;
            }
        };
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        SimpleModule instantModule = new SimpleModule();
        instantModule.addSerializer(Instant.class, new JsonSerializer<Instant>() {
            @Override
            public void serialize(Instant value, JsonGenerator generator, SerializerProvider serializers)
                    throws IOException {
                generator.writeString(value.toString());
            }
        });
        UserAccountStatusFilter filter = new UserAccountStatusFilter(
                appUserRepository, employeeRepository, new ObjectMapper().registerModule(instantModule));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (request, filterResponse) -> {
            throw new AssertionError("The filter chain must stop when the account is inactive.");
        };
        filter.doFilter(new MockHttpServletRequest(), response, chain);
        return response;
    }

    private static void assertForbidden(MockHttpServletResponse response, String expectedCode) throws Exception {
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        JsonNode body = new ObjectMapper().readTree(response.getContentAsByteArray());
        assertFalse(body.path("success").asBoolean());
        assertEquals(expectedCode, body.path("error").path("code").asText());
    }
}
