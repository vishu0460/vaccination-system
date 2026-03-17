package com.vaccine.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    
    private JwtService jwtService;
    
    private static final String SECRET = "TestSecretKeyForJWTAuthenticationThatIsAtLeast32BytesLong123!";
    private static final String ISSUER = "test-issuer";
    private static final long ACCESS_MINUTES = 15;
    private static final long REFRESH_DAYS = 7;
    
    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, ISSUER, ACCESS_MINUTES, REFRESH_DAYS);
    }
    
    @Test
    void createAccessToken_ShouldCreateValidToken() {
        String token = jwtService.createAccessToken("test@example.com", Map.of("role", "USER"));
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
    
    @Test
    void parseAccessToken_ShouldReturnCorrectClaims() {
        String email = "test@example.com";
        String role = "USER";
        String token = jwtService.createAccessToken(email, Map.of("role", role));
        
        Claims claims = jwtService.parse(token);
        
        assertEquals(email, claims.getSubject());
        assertEquals(role, claims.get("role"));
        assertEquals(ISSUER, claims.getIssuer());
    }
    
    @Test
    void createRefreshToken_ShouldCreateValidToken() {
        String token = jwtService.createRefreshToken("test@example.com");
        
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
    
    @Test
    void parseRefreshToken_ShouldHaveCorrectTypeClaim() {
        String email = "test@example.com";
        String token = jwtService.createRefreshToken(email);
        
        Claims claims = jwtService.parse(token);
        
        assertEquals(email, claims.getSubject());
        assertEquals("refresh", claims.get("type"));
    }
    
    @Test
    void accessExpirySeconds_ShouldReturnCorrectValue() {
        long expected = ACCESS_MINUTES * 60;
        assertEquals(expected, jwtService.accessExpirySeconds());
    }
    
    @Test
    void parseInvalidToken_ShouldThrowException() {
        assertThrows(Exception.class, () -> jwtService.parse("invalid.token.here"));
    }
    
    @Test
    void createTokenWithDifferentEmails_ShouldBeUnique() {
        String token1 = jwtService.createAccessToken("user1@example.com", Map.of("role", "USER"));
        String token2 = jwtService.createAccessToken("user2@example.com", Map.of("role", "USER"));
        
        assertNotEquals(token1, token2);
    }
}
