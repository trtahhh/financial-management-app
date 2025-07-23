package com.example.finance.util;
import com.example.finance.entity.User;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${JWT_SECRET:secret}")
    private String secret;
    public String generateToken(User user) {
        return Jwts.builder()
            .setSubject(user.getEmail())
            .claim("role", user.getRole())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis()+86400000))
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }
    public String getEmail(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody().getSubject();
    }
    public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}