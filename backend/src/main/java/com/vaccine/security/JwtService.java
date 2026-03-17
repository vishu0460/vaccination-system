package com.vaccine.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final long accessMinutes;
    private final long refreshDays;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.issuer}") String issuer,
                      @Value("${security.jwt.access-token-minutes}") long accessMinutes,
                      @Value("${security.jwt.refresh-token-days}") long refreshDays) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessMinutes = accessMinutes;
        this.refreshDays = refreshDays;
    }

    // Generate Access Token
    public String createAccessToken(String subject, Map<String, Object> claims) {

        Instant now = Instant.now();

        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    // Generate Refresh Token
    public String createRefreshToken(String subject) {

        Instant now = Instant.now();

        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshDays, ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    // Parse JWT Token
    public Claims parse(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Validate token
    public boolean isTokenValid(String token, UserDetails userDetails) {

        Claims claims = parse(token);

        String username = claims.getSubject();
        Date expiration = claims.getExpiration();

        return username.equals(userDetails.getUsername())
                && expiration.after(new Date());
    }

    // Access Token expiry time
    public long accessExpirySeconds() {
        return accessMinutes * 60;
    }
}