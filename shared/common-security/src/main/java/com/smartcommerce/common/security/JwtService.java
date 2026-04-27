package com.smartcommerce.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
        @Value("${jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(UUID userId, String email, Set<String> roles) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("roles", roles)
            .claim("type", "ACCESS")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
            .id(UUID.randomUUID().toString())
            .signWith(secretKey)
            .compact();
    }

    public String generateRefreshToken(UUID userId, UUID tokenId, UUID familyId) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "REFRESH")
            .claim("familyId", familyId.toString())
            .id(tokenId.toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
            .signWith(secretKey)
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(parseToken(token));
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }
}
