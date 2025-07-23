package com.example.finance.controller;

import com.example.finance.entity.User;
import com.example.finance.entity.UserProfile;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    @Autowired 
    UserRepository userRepo;
    
    @Autowired
    UserProfileRepository userProfileRepo;

    // Lấy thông tin profile đầy đủ
    @GetMapping
    public ResponseEntity<?> getProfile() {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // Lấy thông tin profile
        UserProfile profile = userProfileRepo.findByUserId(user.getId()).orElse(null);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("fullName", user.getFullName());
        response.put("imageUrl", user.getImageUrl());
        response.put("createdAt", user.getCreatedAt());
        
        // Thêm thông tin profile nếu có
        if (profile != null) {
            response.put("birthday", profile.getBirthday());
            response.put("gender", profile.getGender());
            response.put("address", profile.getAddress());
        } else {
            response.put("birthday", null);
            response.put("gender", null);
            response.put("address", null);
        }
        
        return ResponseEntity.ok(response);
    }

    // Cập nhật thông tin profile
    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> req) {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // Cập nhật thông tin cơ bản của User
        if (req.containsKey("username")) {
            user.setUsername(req.get("username"));
        }
        if (req.containsKey("email")) {
            user.setEmail(req.get("email"));
        }
        if (req.containsKey("fullName")) {
            user.setFullName(req.get("fullName"));
        }
        if (req.containsKey("imageUrl")) {
            user.setImageUrl(req.get("imageUrl"));
        }
        userRepo.save(user);

        // Lấy hoặc tạo mới UserProfile
        UserProfile profile = userProfileRepo.findByUserId(user.getId()).orElse(new UserProfile());
        profile.setUser(user);

        // Cập nhật thông tin profile
        if (req.containsKey("gender")) {
            profile.setGender(req.get("gender"));
        }
        if (req.containsKey("address")) {
            profile.setAddress(req.get("address"));
        }
        if (req.containsKey("birthday")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date birthday = sdf.parse(req.get("birthday"));
                profile.setBirthday(birthday);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Định dạng ngày sinh không hợp lệ (yyyy-MM-dd)"));
            }
        }

        userProfileRepo.save(profile);
        
        return ResponseEntity.ok(Map.of("message", "Cập nhật profile thành công"));
    }

    // Xóa tài khoản user hiện tại (soft delete)
    @DeleteMapping
    public ResponseEntity<?> deleteProfile() {
        String email = getCurrentEmail();
        if (email == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Soft delete - chỉ đánh dấu xóa
        user.setIsDeleted(true);
        user.setDeletedAt(new Date());
        userRepo.save(user);
        
        return ResponseEntity.ok(Map.of("message", "Đã xóa tài khoản"));
    }

    private String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }
} 