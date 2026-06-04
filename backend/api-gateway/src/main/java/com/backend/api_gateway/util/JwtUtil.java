package com.backend.api_gateway.util;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Security utility dedicated to the API Gateway.
 * Responsible for verifying the cryptographic signatures of incoming JWTs 
 * before allowing traffic to route to downstream microservices.
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
     * Initializes the cryptographic key for HMAC-SHA operations.
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        log.info("API Gateway JWT Utility initialized securely.");
    }

    /**
     * Extracts the subject (username/email) from the provided token.
     *
     * @param token The JWT string.
     * @return The subject extracted from the payload.
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * Validates the integrity, signature, and expiration of the provided token.
     * Crucially, logs exact security failures to the Gateway console for intrusion detection.
     *
     * @param token The JWT string to validate.
     * @return true if the token is completely valid, false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (SignatureException ex) {
            log.warn("Gateway Security Alert: Invalid JWT signature. Possible forgery attempt: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Gateway Security Alert: Malformed JWT token received: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.info("Gateway Auth: Expired JWT token presented: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Gateway Security Alert: Unsupported JWT token format: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Gateway Security Alert: JWT claims string is empty: {}", ex.getMessage());
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
     * Parses the JWT to extract all claims.
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    // (Note: generateToken methods are usually not needed in the Gateway since the user-service 
    // issues the tokens, but they can remain if the Gateway handles refresh tokens).
}