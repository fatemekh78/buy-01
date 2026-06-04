package com.backend.JwtUtil;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import com.backend.api_gateway.util.JwtUtil;
/**
 * Unit tests for the API Gateway JwtUtil.
 * Verifies token parsing, validation, and proper handling of security exceptions.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String validSecret;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Use a securely sized 512-bit key for testing
        validSecret = "aVeryLongSecretKeyThatIsAtLeast64BytesLongForHmacSha512AlgorithmToWorkProperly12345!";
        
        // Use ReflectionTestUtils to inject the @Value properties manually for the unit test
        ReflectionTestUtils.setField(jwtUtil, "secret", validSecret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L); // 1 hour
        
        // Trigger the @PostConstruct method
        jwtUtil.init();
    }

    @Test
    void validateToken_WithValidToken_ReturnsTrue() {
        Key key = Keys.hmacShaKeyFor(validSecret.getBytes());
        String token = Jwts.builder()
                .setSubject("test@example.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        boolean isValid = jwtUtil.validateToken(token);
        
        assertTrue(isValid, "Token should be validated successfully.");
    }

    @Test
    void validateToken_WithForgedSignature_ReturnsFalse() {
        // Create a token signed with a COMPLETELY DIFFERENT secret (simulating a hacker)
        Key hackerKey = Keys.hmacShaKeyFor("aCompletelyDifferentSecretKeyThatIsAlsoVeryLongAndSecureForTesting123!".getBytes());
        String forgedToken = Jwts.builder()
                .setSubject("admin@example.com")
                .signWith(hackerKey, SignatureAlgorithm.HS512)
                .compact();

        boolean isValid = jwtUtil.validateToken(forgedToken);
        
        assertFalse(isValid, "Token with a forged signature must fail validation.");
    }

    @Test
    void validateToken_WithMalformedString_ReturnsFalse() {
        String malformedToken = "this.is.not.a.real.jwt";

        boolean isValid = jwtUtil.validateToken(malformedToken);
        
        assertFalse(isValid, "Malformed token string must fail validation safely.");
    }

    @Test
    void getUsernameFromToken_WithValidToken_ReturnsSubject() {
        Key key = Keys.hmacShaKeyFor(validSecret.getBytes());
        String expectedEmail = "user@test.com";
        String token = Jwts.builder()
                .setSubject(expectedEmail)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        String extractedUsername = jwtUtil.getUsernameFromToken(token);
        
        assertEquals(expectedEmail, extractedUsername, "The extracted subject should match the provided email.");
    }
}