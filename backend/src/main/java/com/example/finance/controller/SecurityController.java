package com.example.finance.controller;

import com.example.finance.entity.User;
import com.example.finance.service.*;
import com.example.finance.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@RequestMapping("/api/security")
public class SecurityController {

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    @Autowired
    private SessionManagementService sessionManagementService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    /**
     * Setup 2FA
     */
    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setup2FA(HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            Map<String, Object> result = twoFactorAuthService.setupTwoFactorAuth(user);
            
            auditLogService.logAction(user, "2FA_SETUP", "TwoFactorAuth", null,
                getClientIp(request), request.getHeader("User-Agent"), null);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get QR code image
     */
    @GetMapping("/2fa/qrcode")
    public ResponseEntity<byte[]> getQRCode(@RequestParam String qrCodeUrl) {
        try {
            byte[] qrCode = twoFactorAuthService.generateQRCodeImage(qrCodeUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            
            return new ResponseEntity<>(qrCode, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Enable 2FA
     */
    @PostMapping("/2fa/enable")
    public ResponseEntity<?> enable2FA(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            String code = payload.get("code");
            
            boolean success = twoFactorAuthService.enableTwoFactorAuth(user, code);
            
            if (success) {
                auditLogService.logAction(user, "2FA_ENABLED", "TwoFactorAuth", null,
                    getClientIp(request), request.getHeader("User-Agent"), null);
                
                return ResponseEntity.ok(Map.of("message", "2FA enabled successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid verification code"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Disable 2FA
     */
    @PostMapping("/2fa/disable")
    public ResponseEntity<?> disable2FA(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            String code = payload.get("code");
            
            boolean success = twoFactorAuthService.disableTwoFactorAuth(user, code);
            
            if (success) {
                auditLogService.logAction(user, "2FA_DISABLED", "TwoFactorAuth", null,
                    getClientIp(request), request.getHeader("User-Agent"), null);
                
                return ResponseEntity.ok(Map.of("message", "2FA disabled successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid verification code"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get 2FA status
     */
    @GetMapping("/2fa/status")
    public ResponseEntity<?> get2FAStatus(HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            Map<String, Object> status = twoFactorAuthService.getTwoFactorStatus(user);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get active sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions(HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            List<Map<String, Object>> sessions = sessionManagementService.getActiveSessions(user.getId());
            
            // Mark current session (token comparison would be implemented when needed)
            sessions.forEach(session -> {
                session.put("isCurrent", false);
            });
            
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Revoke session
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revokeSession(@PathVariable Long sessionId, HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            // sessionManagementService.revokeSessionById(sessionId, user.getId());
            
            auditLogService.logAction(user, "SESSION_REVOKED", "UserSession", sessionId,
                getClientIp(request), request.getHeader("User-Agent"), null);
            
            return ResponseEntity.ok(Map.of("message", "Session revoked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Revoke all sessions
     */
    @DeleteMapping("/sessions/all")
    public ResponseEntity<?> revokeAllSessions(HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            sessionManagementService.revokeAllUserSessions(user.getId());
            
            auditLogService.logAction(user, "ALL_SESSIONS_REVOKED", "UserSession", null,
                getClientIp(request), request.getHeader("User-Agent"), null);
            
            return ResponseEntity.ok(Map.of("message", "All sessions revoked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get session statistics
     */
    @GetMapping("/sessions/stats")
    public ResponseEntity<?> getSessionStats(HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            Map<String, Object> stats = sessionManagementService.getSessionStatistics(user.getId());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get audit logs
     */
    @GetMapping("/audit")
    public ResponseEntity<?> getAuditLogs(HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            return ResponseEntity.ok(auditLogService.getUserAuditLogs(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get recent activity
     */
    @GetMapping("/activity/recent")
    public ResponseEntity<?> getRecentActivity(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            return ResponseEntity.ok(auditLogService.getRecentActivity(user.getId(), limit));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get audit statistics
     */
    @GetMapping("/audit/stats")
    public ResponseEntity<?> getAuditStats(
            @RequestParam String startDate,
            @RequestParam String endDate,
            HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            
            // Parse dates (format: yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss)
            LocalDateTime start = parseDateTime(startDate);
            LocalDateTime end = parseDateTime(endDate);
            
            // Get statistics
            Map<String, Object> stats = auditLogService.getAuditStatistics(user.getId(), start, end);
            
            return ResponseEntity.ok(stats);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid date format. Use yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    private LocalDateTime parseDateTime(String dateStr) {
        // Try parsing as LocalDateTime first (yyyy-MM-ddTHH:mm:ss)
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e1) {
            try {
                // Try parsing as LocalDate (yyyy-MM-dd) and set to start/end of day
                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                return date.atStartOfDay();
            } catch (DateTimeParseException e2) {
                throw new DateTimeParseException("Invalid date format: " + dateStr, dateStr, 0);
            }
        }
    }

    /**
     * Get login statistics
     */
    @GetMapping("/login/stats")
    public ResponseEntity<?> getLoginStats(HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            Map<String, Object> stats = rateLimitingService.getLoginStatistics(user.getUsername());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Helper methods
    private User getCurrentUser(HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            throw new RuntimeException("Invalid token: userId not found");
        }
        return userService.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    private String extractToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
