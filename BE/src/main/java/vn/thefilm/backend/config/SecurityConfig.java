package vn.thefilm.backend.config;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import vn.thefilm.backend.dto.ApiResponse;
import vn.thefilm.backend.security.JwtService;
import vn.thefilm.backend.security.KeycloakJwtAuthenticationConverter;
import vn.thefilm.backend.security.TheFilmUserPrincipal;
import vn.thefilm.backend.security.UserAccountStatusFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    FilterRegistrationBean<UserAccountStatusFilter> userAccountStatusFilterRegistration(
            UserAccountStatusFilter userAccountStatusFilter) {
        FilterRegistrationBean<UserAccountStatusFilter> registration =
                new FilterRegistrationBean<>(userAccountStatusFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(Environment environment) {
        String configuredOrigins = environment.getProperty(
                "app.cors.allowed-origins",
                "http://localhost:4200,http://localhost:4300");
        List<String> origins = List.of(configuredOrigins.split(",")).stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Profile("local")
    SecurityFilterChain localSecurityFilterChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            JwtService jwtService,
            UserAccountStatusFilter userAccountStatusFilter) throws Exception {
        MockCustomerAuthenticationFilter mockAuthenticationFilter = new MockCustomerAuthenticationFilter(jwtService);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(apiAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(apiAccessDeniedHandler(objectMapper)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/health", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/public/**", "/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")
                        .anyRequest().permitAll())
                .addFilterBefore(mockAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(userAccountStatusFilter, MockCustomerAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Profile("oracle")
    SecurityFilterChain oracleSecurityFilterChain(
            HttpSecurity http,
            KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter,
            UserAccountStatusFilter userAccountStatusFilter,
            ObjectMapper objectMapper) throws Exception {
        AuthenticationEntryPoint authenticationEntryPoint = apiAuthenticationEntryPoint(objectMapper);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler(objectMapper)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/health", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/public/**", "/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)))
                .addFilterAfter(userAccountStatusFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    private AuthenticationEntryPoint apiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeApiFailure(
                objectMapper, response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Yêu cầu xác thực.");
    }

    private AccessDeniedHandler apiAccessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeApiFailure(
                objectMapper, response, HttpServletResponse.SC_FORBIDDEN, "ACCESS_DENIED", "Không có quyền truy cập.");
    }

    private static void writeApiFailure(
            ObjectMapper objectMapper,
            HttpServletResponse response,
            int status,
            String code,
            String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(code, message));
    }

    private static final class MockCustomerAuthenticationFilter extends OncePerRequestFilter {
        private static final String MOCK_TOKEN = "mock-jwt-token";
        private static final String MOCK_ISSUER = "http://localhost:8080/realms/thefilm";
        private static final String MOCK_SUBJECT = "local-mock-customer";
        private static final String MOCK_EMAIL = "an.nguyen@example.com";
        private static final String MOCK_NAME = "Nguyen Van A";

        private final JwtService jwtService;

        MockCustomerAuthenticationFilter(JwtService jwtService) {
            this.jwtService = jwtService;
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();
            if (path == null || path.isBlank()) {
                path = request.getServletPath();
            }
            return path == null || !path.startsWith("/api/v1/");
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                String token = authorization.substring(7).trim();
                TheFilmUserPrincipal validatedPrincipal = jwtService != null ? jwtService.parseAndValidate(token) : null;
                if (validatedPrincipal != null) {
                    Jwt jwt = Jwt.withTokenValue(token)
                            .header("alg", "HS256")
                            .issuer(validatedPrincipal.oidcIssuer() != null ? validatedPrincipal.oidcIssuer() : "http://localhost:8080/auth")
                            .subject(validatedPrincipal.oidcSubject())
                            .issuedAt(Instant.now())
                            .expiresAt(Instant.now().plusSeconds(jwtService.getExpirationSeconds()))
                            .claim("app_user_id", validatedPrincipal.appUserId())
                            .claim("email", validatedPrincipal.email())
                            .claim("name", validatedPrincipal.displayName())
                            .build();
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities, validatedPrincipal.oidcSubject()) {
                        @Override
                        public Object getPrincipal() {
                            return validatedPrincipal;
                        }
                    };
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                } else {
                    MockIdentity identity = mockIdentity(authorization);
                    if (identity != null) {
                        Jwt jwt = Jwt.withTokenValue(token)
                                .header("alg", "none")
                                .issuer(identity.issuer())
                                .subject(identity.subject())
                                .issuedAt(Instant.now())
                                .expiresAt(Instant.now().plusSeconds(3600))
                                .claim("email", identity.email())
                                .claim("preferred_username", identity.preferredUsername())
                                .claim("name", identity.displayName())
                                .build();
                        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
                        JwtAuthenticationToken authentication;
                        if (identity.jwtPrincipal()) {
                            authentication = new JwtAuthenticationToken(jwt, authorities, identity.subject());
                        } else {
                            TheFilmUserPrincipal principal = new TheFilmUserPrincipal(identity.subject(), identity.issuer(),
                                    identity.email(), identity.preferredUsername(), identity.displayName(), null);
                            authentication = new JwtAuthenticationToken(jwt, authorities, identity.subject()) {
                                @Override
                                public Object getPrincipal() {
                                    return principal;
                                }
                            };
                        }
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);
                    }
                }
            }
            filterChain.doFilter(request, response);
        }

        private MockIdentity mockIdentity(String authorization) {
            if (authorization == null || authorization.length() <= 7
                    || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return null;
            }
            String token = authorization.substring(7).trim();
            if (MOCK_TOKEN.equals(token)) {
                return new MockIdentity(MOCK_SUBJECT, MOCK_ISSUER, MOCK_EMAIL, MOCK_SUBJECT, MOCK_NAME, false);
            }

            boolean jwtPrincipal;
            String prefix;
            if (token.startsWith(MOCK_TOKEN + ".jwt.")) {
                jwtPrincipal = true;
                prefix = MOCK_TOKEN + ".jwt.";
            } else if (token.startsWith(MOCK_TOKEN + ".principal.")) {
                jwtPrincipal = false;
                prefix = MOCK_TOKEN + ".principal.";
            } else {
                return null;
            }

            try {
                String[] claims = token.substring(prefix.length()).split("\\.", -1);
                if (claims.length != 5) return null;
                String subject = decodeClaim(claims[0]);
                String issuer = decodeClaim(claims[1]);
                if (subject == null || subject.isBlank() || issuer == null || !URI.create(issuer).isAbsolute()) {
                    return null;
                }
                return new MockIdentity(subject, issuer, decodeClaim(claims[2]), decodeClaim(claims[3]),
                        decodeClaim(claims[4]), jwtPrincipal);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }

        private String decodeClaim(String encoded) {
            if (encoded.isEmpty()) return null;
            return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        }

        private record MockIdentity(
                String subject,
                String issuer,
                String email,
                String preferredUsername,
                String displayName,
                boolean jwtPrincipal) { }
    }
}
