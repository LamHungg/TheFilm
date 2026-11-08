package vn.thefilm.backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final long DEFAULT_EXPIRATION_SECONDS = 86400L; // 24 hours

    private final byte[] secretKeyBytes;
    private final String issuer;

    public JwtService(
            @Value("${app.security.jwt.secret:thefilm-super-secret-jwt-key-256-bit-minimum-size-here-2026!}") String secret,
            @Value("${app.security.jwt.issuer:http://localhost:8080/auth}") String issuer) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.secretKeyBytes = keyBytes;
        this.issuer = issuer;
    }

    public String generateToken(Long appUserId, String email, String displayName, List<String> roles) {
        try {
            JWSSigner signer = new MACSigner(secretKeyBytes);
            Date now = new Date();
            Date expiry = new Date(now.getTime() + DEFAULT_EXPIRATION_SECONDS * 1000);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(email)
                    .issueTime(now)
                    .expirationTime(expiry)
                    .claim("app_user_id", appUserId)
                    .claim("email", email)
                    .claim("name", displayName)
                    .claim("preferred_username", email)
                    .claim("roles", roles)
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception ex) {
            log.error("Failed to generate JWT token", ex);
            throw new IllegalStateException("Không thể tạo token xác thực.", ex);
        }
    }

    public TheFilmUserPrincipal parseAndValidate(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secretKeyBytes);
            if (!signedJWT.verify(verifier)) {
                return null;
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (expiration != null && expiration.before(new Date())) {
                return null;
            }

            Long appUserId = claims.getLongClaim("app_user_id");
            String email = claims.getStringClaim("email");
            String name = claims.getStringClaim("name");
            String preferredUsername = claims.getStringClaim("preferred_username");
            String subject = claims.getSubject();
            String tokenIssuer = claims.getIssuer();

            return new TheFilmUserPrincipal(subject, tokenIssuer, email, preferredUsername, name, appUserId);
        } catch (Exception ex) {
            return null;
        }
    }

    public long getExpirationSeconds() {
        return DEFAULT_EXPIRATION_SECONDS;
    }
}

