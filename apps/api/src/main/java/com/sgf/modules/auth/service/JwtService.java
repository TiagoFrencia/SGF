package com.sgf.modules.auth.service;

import com.sgf.api.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    public String generateToken(SgfUserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(properties.expirationMinutes() * 60);
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(principal.getUsername())
                .claims(Map.of("uid", principal.getId().toString()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).get("uid", String.class));
    }

    public boolean isValid(String token, SgfUserPrincipal principal) {
        Claims claims = parse(token);
        return principal.getUsername().equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        String secret = properties.secret();
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(raw.length >= 32 ? raw : String.format("%-32s", secret).getBytes(StandardCharsets.UTF_8));
    }
}
