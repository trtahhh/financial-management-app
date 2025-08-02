package com.example.finance.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestParam(required = false) Long userId) {
        try {
            // For demo purposes, return mock data
            if (userId == null) {
                userId = 1L;
            }
            
            log.info("Getting profile for userId: {}", userId);
            
            // Mock profile data
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", userId);
            profile.put("username", "testuser");
            profile.put("email", "test@example.com");
            profile.put("fullName", "Test User");
            profile.put("role", "USER");
            profile.put("createdAt", "2025-01-01T00:00:00");
            profile.put("phone", "0123456789");
            profile.put("birthday", "1990-01-01");
            profile.put("gender", "Nam");
            profile.put("address", "123 Test Street");
            profile.put("imageUrl", null);
            
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            log.error("Error getting profile", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi lấy thông tin profile: " + e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, Object> profileData, @RequestParam(required = false) Long userId) {
        try {
            // For demo purposes, return success
            if (userId == null) {
                userId = 1L;
            }
            
            log.info("Updating profile for userId: {} with data: {}", userId, profileData);
            
            // In a real app, you would save to database here
            // For demo, just return success
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật profile thành công!"));
        } catch (Exception e) {
            log.error("Error updating profile", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi cập nhật profile: " + e.getMessage()));
        }
    }
}
