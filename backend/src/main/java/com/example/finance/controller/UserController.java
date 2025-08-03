package com.example.finance.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    // Temporary storage for demo - in real app use database
    private static final Map<Long, Map<String, Object>> userProfiles = new ConcurrentHashMap<>();
    
    static {
        // Initialize default profile
        Map<String, Object> defaultProfile = new HashMap<>();
        defaultProfile.put("id", 1L);
        defaultProfile.put("username", "testuser");
        defaultProfile.put("email", "test@example.com");
        defaultProfile.put("fullName", "Test User");
        defaultProfile.put("role", "USER");
        defaultProfile.put("createdAt", "2025-01-01T00:00:00");
        defaultProfile.put("phone", "0123456789");
        defaultProfile.put("birthday", "1990-01-01");
        defaultProfile.put("gender", "Nam");
        defaultProfile.put("address", "123 Test Street");
        defaultProfile.put("imageUrl", null);
        userProfiles.put(1L, defaultProfile);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestParam(required = false) Long userId) {
        try {
            if (userId == null) {
                userId = 1L;
            }
            
            log.info("Getting profile for userId: {}", userId);
            
            // Get profile from temporary storage
            Map<String, Object> profile = userProfiles.get(userId);
            if (profile == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "User not found"));
            }
            
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
            if (userId == null) {
                userId = 1L;
            }
            
            log.info("Updating profile for userId: {} with data: {}", userId, profileData);
            
            // Get existing profile
            Map<String, Object> existingProfile = userProfiles.get(userId);
            if (existingProfile == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "User not found"));
            }
            
            // Update profile data (keep existing fields, update only provided ones)
            Map<String, Object> updatedProfile = new HashMap<>(existingProfile);
            
            // Update only the fields provided in the request
            if (profileData.containsKey("fullName")) {
                updatedProfile.put("fullName", profileData.get("fullName"));
            }
            if (profileData.containsKey("email")) {
                updatedProfile.put("email", profileData.get("email"));
            }
            if (profileData.containsKey("phone")) {
                updatedProfile.put("phone", profileData.get("phone"));
            }
            if (profileData.containsKey("birthday")) {
                updatedProfile.put("birthday", profileData.get("birthday"));
            }
            if (profileData.containsKey("gender")) {
                updatedProfile.put("gender", profileData.get("gender"));
            }
            if (profileData.containsKey("address")) {
                updatedProfile.put("address", profileData.get("address"));
            }
            if (profileData.containsKey("imageUrl")) {
                updatedProfile.put("imageUrl", profileData.get("imageUrl"));
            }
            
            // Save updated profile
            userProfiles.put(userId, updatedProfile);
            
            log.info("Profile updated successfully for userId: {}", userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Cập nhật profile thành công!",
                "data", updatedProfile
            ));
        } catch (Exception e) {
            log.error("Error updating profile", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi cập nhật profile: " + e.getMessage()));
        }
    }
}
