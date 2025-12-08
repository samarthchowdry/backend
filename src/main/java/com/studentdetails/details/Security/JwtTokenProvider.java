package com.studentdetails.details.Security;

import com.studentdetails.details.Domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for JWT token generation and validation.
 * Supports both client and organization deployments.
 */
@Component
@SuppressWarnings("unused") // Suppress unused warning - class is used by Spring Framework
public class JwtTokenProvider {

    @Value("${app.jwt.secret:defaultSecretKeyForDevelopmentOnlyChangeInProduction12345678901234567890}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}") // 24 hours default
    private long jwtExpirationMs;

    @Value("${app.jwt.issuer:student-management-system}")
    private String jwtIssuer;

    /**
     * Generates a JWT token for a user.
     *
     * @param email the user email
     * @param role the user role
     * @param userId the user ID
     * @return the JWT token
     */
    public String generateToken(String email, UserRole role, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role.name())
                .claim("userId", userId)
                .issuer(jwtIssuer)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts email from JWT token.
     *
     * @param token the JWT token
     * @return the email
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Extracts user role from JWT token.
     *
     * @param token the JWT token
     * @return the user role
     */
    public UserRole getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        String roleStr = claims.get("role", String.class);
        return UserRole.valueOf(roleStr);
    }

    /**
     * Extracts user ID from JWT token.
     *
     * @param token the JWT token
     * @return the user ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Validates a JWT token.
     *
     * @param token the JWT token
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            return expiration.after(new Date());
        } catch (Exception _) {
            return false;
        }
    }

    /**
     * Extracts claims from JWT token.
     *
     * @param token the JWT token
     * @return the claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(jwtIssuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Gets the signing key for JWT.
     *
     * @return the signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Converts UserRole to Spring Security authorities.
     *
     * @param role the user role
     * @return collection of granted authorities
     */
    public Collection<? extends GrantedAuthority> getAuthorities(UserRole role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}






