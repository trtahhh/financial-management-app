package com.example.finance.util;
import com.example.finance.entity.User;
import com.example.finance.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends GenericFilter {
    @Autowired JwtUtil jwtUtil;
    @Autowired UserRepository userRepo;
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        String h = r.getHeader("Authorization");
        
        System.out.println("JWT Filter - Authorization header: " + (h != null ? h.substring(0, Math.min(20, h.length())) + "..." : "null"));
        
        if (h != null && h.startsWith("Bearer ")) {
            String token = h.substring(7);
            try {
                String email = jwtUtil.getEmail(token);
                System.out.println("JWT Filter - Extracted email: " + email);
                
                User user = userRepo.findByEmail(email).orElse(null);
                if (user != null) {
                    System.out.println("JWT Filter - User found: " + user.getEmail());
                    Authentication auth = new UsernamePasswordAuthenticationToken(
                        user.getEmail(), null, List.of(new SimpleGrantedAuthority(user.getRole())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    System.out.println("JWT Filter - User not found for email: " + email);
                }
            } catch (JwtException e) {
                System.out.println("JWT Filter - JWT Exception: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("JWT Filter - General Exception: " + e.getMessage());
            }
        } else {
            System.out.println("JWT Filter - No valid Authorization header found");
        }
        chain.doFilter(req, res);
    }
}