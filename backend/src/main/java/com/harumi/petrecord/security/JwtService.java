package com.harumi.petrecord.security;

import com.harumi.petrecord.config.JwtProperties;
import com.harumi.petrecord.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes());
    }

    public String issueToken(CurrentUser user, int tokenVersion) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(properties.getExpirationMinutes()));
        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("username", user.username())
                .claim("role", user.role().name())
                .claim("ver", tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public VerifiedToken parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long id = Long.parseLong(claims.getSubject());
            String username = claims.get("username", String.class);
            UserRole role = UserRole.valueOf(claims.get("role", String.class));
            Integer version = claims.get("ver", Integer.class);
            return new VerifiedToken(new CurrentUser(id, username, role), version == null ? 0 : version);
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidJwtException("Invalid JWT");
        }
    }

    /** A validly-signed token's principal together with the token version it was issued at. */
    public record VerifiedToken(CurrentUser user, int tokenVersion) {
    }

    public long getExpirationSeconds() {
        return Duration.ofMinutes(properties.getExpirationMinutes()).toSeconds();
    }
}
