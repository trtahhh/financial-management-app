package com.example.finance.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationInMs;

    private SecretKey getSigningKey() {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JWT_SECRET format. Must be valid Base64", e);
        }
    }

    public String generateToken(String username, Long userId) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(expiryDate)
                .claim("userId", userId)
                .signWith(getSigningKey())
                .compact();
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                       .verifyWith(getSigningKey())
                       .build()
                       .parseSignedClaims(token)
                       .getPayload();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("JWT token has expired", e);
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException("Unsupported JWT token", e);
        } catch (MalformedJwtException e) {
            throw new RuntimeException("Malformed JWT token", e);
        } catch (JwtException e) {
            throw new RuntimeException("Invalid JWT signature", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("JWT token is empty or null", e);
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parse(token);
            return !isTokenExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    public String getUsername(String token) {
        Claims claims = parse(token);
        return claims.getSubject();
    }

    public Long getUserId(String token) {
        Claims claims = parse(token);
        Object id = claims.get("userId");
        if (id instanceof Integer) return ((Integer) id).longValue();
        if (id instanceof Long) return (Long) id;
        if (id instanceof String) return Long.parseLong((String) id);
        return null;
    }

    public String[] getAuthorities(String token) {
        Claims claims = parse(token);
        Object authorities = claims.get("authorities");
        if (authorities instanceof String) {
            return new String[]{authorities.toString()};
        }
        return new String[]{"USER"};
    }

    public Collection<SimpleGrantedAuthority> getAuthoritiesForSecurity(String token) {
        String[] authorities = getAuthorities(token);
        return Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
