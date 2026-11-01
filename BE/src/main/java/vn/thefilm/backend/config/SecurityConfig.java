package vn.thefilm.backend.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

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
    SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Profile("oracle")
    SecurityFilterChain oracleSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter keycloakJwtAuthenticationConverter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/health", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter)))
                .build();
    }

    @Bean
    @Profile("oracle")
    JwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Collection<GrantedAuthority> scopes = scopeConverter.convert(jwt);
            if (scopes != null) {
                authorities.addAll(scopes);
            }

            addRoleAuthorities(readRoles(jwt.getClaim("realm_access")), authorities);

            Object resourceAccessClaim = jwt.getClaim("resource_access");
            if (resourceAccessClaim instanceof Map<?, ?> resourceAccess) {
                addRoleAuthorities(readRoles(resourceAccess.get("thefilm-api")), authorities);
            }
            return authorities;
        };

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    private static Object readRoles(Object accessClaim) {
        if (accessClaim instanceof Map<?, ?> access) {
            return access.get("roles");
        }
        return null;
    }

    private static void addRoleAuthorities(Object rolesClaim, Collection<GrantedAuthority> authorities) {
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return;
        }
        for (Object role : roles) {
            if (role instanceof String roleName && !roleName.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()));
            }
        }
    }
}