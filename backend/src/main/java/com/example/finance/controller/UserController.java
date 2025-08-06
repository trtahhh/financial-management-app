package com.example.finance.controller;

import com.example.finance.entity.User;
import com.example.finance.entity.UserProfile;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.UserProfileRepository;
import com.example.finance.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Long extractUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        return jwtUtil.getUserId(token);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            log.info("Getting profile for userId: {}", userId);

            User user = userRepository.findById(userId).orElse(null);
            UserProfile profile = userProfileRepository.findById(userId).orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest()
                    .body(java.util.Map.of("success", false, "message", "User not found"));
            }

            return ResponseEntity.ok(java.util.Map.of(
                "user", user,
                "profile", profile
            ));
        } catch (Exception e) {
            log.error("Error getting profile", e);
            return ResponseEntity.badRequest()
                .body(java.util.Map.of("success", false, "message", "Lỗi lấy thông tin profile: " + e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody UserProfile profileData, HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            log.info("Updating profile for userId: {} with data: {}", userId, profileData);

            UserProfile existingProfile = userProfileRepository.findById(userId).orElse(null);

            if (existingProfile == null) {
                // Nếu chưa có profile thì tạo mới
                profileData.setUserId(userId);
                userProfileRepository.save(profileData);
            } else {
                // Update only the fields provided in the request
                if (profileData.getFullName() != null) {
                    existingProfile.setFullName(profileData.getFullName());
                }
                if (profileData.getPhone() != null) {
                    existingProfile.setPhone(profileData.getPhone());
                }
                if (profileData.getBirthday() != null) {
                    existingProfile.setBirthday(profileData.getBirthday());
                }
                if (profileData.getGender() != null) {
                    existingProfile.setGender(profileData.getGender());
                }
                if (profileData.getAddress() != null) {
                    existingProfile.setAddress(profileData.getAddress());
                }
                if (profileData.getImageUrl() != null) {
                    existingProfile.setImageUrl(profileData.getImageUrl());
                }
                userProfileRepository.save(existingProfile);
            }

            log.info("Profile updated successfully for userId: {}", userId);

            return ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Cập nhật profile thành công!"
            ));
        } catch (Exception e) {
            log.error("Error updating profile", e);
            return ResponseEntity.badRequest()
                .body(java.util.Map.of("success", false, "message", "Lỗi cập nhật profile: " + e.getMessage()));
        }
    }
}