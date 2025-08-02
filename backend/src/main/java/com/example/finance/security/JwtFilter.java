package com.example.finance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        log.info("🔍 Processing request: {} {}", request.getMethod(), path);
        
        if (path.startsWith("/actuator")) {
            chain.doFilter(request, response); 
            return;
        }

        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.info("🔑 Authorization header: {}", header != null ? "Bearer token present" : "No auth header");

        if (header != null && header.startsWith("Bearer ")) {
            final String token = header.substring(7);
            try {
                String username = jwtUtil.getUsername(token);
                log.info("✅ JWT username extracted: {}", username);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            username, null, jwtUtil.getAuthorities(token));
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.info("✅ Authentication set for user: {}", username);
                }
            } catch (Exception e) {
                log.warn("❌ JWT processing failed: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}
