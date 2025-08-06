package com.example.finance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil() {
        String secret = System.getenv().getOrDefault("JWT_SECRET", "Zm9yLXR1YW5hbmgtYW5oLWRhLWphdmEtc3ByaW5nLXNlY3VyaXR5LXNob3J0a2V5LTM2YiE=");
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    private Claims parse(String token) {
        return Jwts.parser()
                   .verifyWith(key)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    public Long getUserId(String token) {
        Object id = parse(token).get("userId");
        if (id instanceof Integer) return ((Integer) id).longValue();
        if (id instanceof Long) return (Long) id;
        if (id instanceof String) return Long.parseLong((String) id);
        return null;
    }

    @SuppressWarnings("unchecked")
    public Collection<SimpleGrantedAuthority> getAuthorities(String token) {
        List<String> roles = (List<String>) parse(token).get("roles", List.class);
        return roles == null ? List.of()
                             : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }
}
