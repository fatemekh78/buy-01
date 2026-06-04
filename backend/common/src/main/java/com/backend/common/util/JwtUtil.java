package com.backend.common.util;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Enterprise utility for generating, parsing, and validating JSON Web Tokens
 * (JWT).
 * Used across the microservices layer to enforce stateless authentication.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private Key key;

    /**
     * Initializes the cryptographic key after Spring injects the properties.
     * Uses HMAC-SHA to ensure high-security token signing.
     */
    @PostConstruct
    public void init() {
        // Ensure the secret is loaded as bytes for the HMAC-SHA key
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        log.info("JWT Utility initialized successfully with configured secret and expiration.");
    }

    /**
     * Extracts the subject (username/email) from the provided token.
     *
     * @param token The JWT string.
     * @return The username extracted from the token payload.
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Generates a new JWT for the given username with empty default claims.
     *
     * @param username The principal user for whom the token is generated.
     * @return A fully signed JWT string.
     */
    public String generateToken(String username) {
        return doGenerateToken(new HashMap<>(), username);
    }

    /**
     * Generates a new JWT incorporating specific custom claims (e.g., roles, user
     * ID).
     *
     * @param claims   A map of custom payload data.
     * @param username The principal user for whom the token is generated.
     * @return A fully signed JWT string.
     */
    public String generateToken(Map<String, Object> claims, String username) {
        return doGenerateToken(claims, username);
    }

    /**
     * Internal builder to construct and cryptographically sign the JWT.
     */
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validates the integrity, signature, and expiration of the provided token.
     * Logs exact security failures for audit trailing.
     *
     * @param token The JWT string to validate.
     * @return true if the token is valid, false if it fails any security checks.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            log.warn("Invalid JWT signature detected. Possible tampering attempt: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT token received: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.info("Expired JWT token. User must re-authenticate: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token format: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty or null: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extracts a specific claim from the token payload using a resolver function.
     *
     * @param token          The JWT string.
     * @param claimsResolver Function to specify which claim to extract.
     * @param <T>            The type of the claim being extracted.
     * @return The extracted claim value.
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses the JWT to extract all claims. This will implicitly validate the
     * signature
     * and throw an exception if the token is tampered with or expired.
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}