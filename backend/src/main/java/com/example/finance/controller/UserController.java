package com.example.finance.controller;

import com.example.finance.entity.User;
import com.example.finance.entity.UserProfile;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.UserProfileRepository;
import com.example.finance.security.JwtUtil;
import com.example.finance.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.Collections;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@Slf4j
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    private Long extractUserId(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization").replace("Bearer ", "");
            Long userId = jwtUtil.getUserId(token);
            if (userId == null) {
                // Fallback: get userId from username
                String username = jwtUtil.getUsername(token);
                if (username != null) {
                    return userService.findByUsername(username).getId();
                }
            }
            return userId;
        } catch (Exception e) {
            log.error("Error extracting userId from token", e);
            return null;
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            log.info("Getting profile for userId: {}", userId);

            User user = userRepository.findById(userId).orElse(null);
            UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("success", false));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", user);
            response.put("profile", profile);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting profile", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi lấy thông tin profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody java.util.Map<String, Object> profileData, HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            log.info("Updating profile for userId: {} with data: {}", userId, profileData);

            // Update User entity if email is provided
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            
            if (profileData.containsKey("email") && profileData.get("email") != null) {
                String newEmail = profileData.get("email").toString().trim();
                if (!newEmail.isEmpty()) {
                    // Validate email format
                    if (!newEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "Định dạng email không hợp lệ. Ví dụ: example@domain.com");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                    
                    // Check email length
                    if (newEmail.length() > 255) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "Email không được quá 255 ký tự");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                    
                    // Check if email is already used by another user
                    Optional<User> existingUserWithEmail = userRepository.findByEmail(newEmail);
                    if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getId().equals(userId)) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "Email này đã được sử dụng bởi người dùng khác");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                    user.setEmail(newEmail);
                    userRepository.save(user);
                }
            }

            // Update UserProfile entity
            UserProfile existingProfile = userProfileRepository.findByUserId(userId).orElse(null);

            if (existingProfile == null) {
                // Nếu chưa có profile thì tạo mới
                existingProfile = new UserProfile();
                existingProfile.setUserId(userId);
                existingProfile.setUser(user);
            }

            // Update profile fields
            if (profileData.containsKey("fullName")) {
                String fullName = profileData.get("fullName") != null ? profileData.get("fullName").toString().trim() : null;
                if (fullName != null && !fullName.isEmpty()) {
                    if (fullName.length() > 100) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "Họ và tên không được quá 100 ký tự");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                    if (fullName.length() < 2) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "Họ và tên phải có ít nhất 2 ký tự");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                    // Check if fullName contains only letters, spaces, and Vietnamese characters
                    if (!fullName.matches("^[\\p{L}\\s]+$")) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "Họ và tên chỉ được chứa chữ cái và khoảng trắng. Không được chứa số hoặc ký tự đặc biệt");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                }
                existingProfile.setFullName(fullName);
            }
            if (profileData.containsKey("phone")) {
                String phone = profileData.get("phone") != null ? profileData.get("phone").toString().trim() : null;
                if (phone != null && !phone.isEmpty()) {
                    if (phone.length() > 20) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "Số điện thoại không được quá 20 ký tự");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                    // Validate phone format (Vietnamese phone number)
                    if (!phone.matches("^(0|\\+84)[0-9]{9,10}$")) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "Định dạng số điện thoại không hợp lệ. Ví dụ: 0123456789 hoặc +84123456789");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                }
                existingProfile.setPhone(phone);
            }
            if (profileData.containsKey("birthday")) {
                if (profileData.get("birthday") != null && !profileData.get("birthday").toString().isEmpty()) {
                    try {
                        java.time.LocalDate birthday = java.time.LocalDate.parse(profileData.get("birthday").toString());
                        java.time.LocalDate now = java.time.LocalDate.now();
                        
                        // Check if birthday is in the future
                        if (birthday.isAfter(now)) {
                            Map<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("success", false);
                            errorResponse.put("message", "Ngày sinh không thể là ngày trong tương lai");
                            return ResponseEntity.badRequest().body(errorResponse);
                        }
                        
                        // Check if birthday is too far in the past (e.g., more than 150 years ago)
                        if (birthday.isBefore(now.minusYears(150))) {
                            Map<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("success", false);
                            errorResponse.put("message", "Ngày sinh không hợp lệ");
                            return ResponseEntity.badRequest().body(errorResponse);
                        }
                        
                        existingProfile.setBirthday(birthday);
                    } catch (Exception e) {
                        log.warn("Invalid birthday format: {}", profileData.get("birthday"));
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "Định dạng ngày sinh không hợp lệ");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                } else {
                    existingProfile.setBirthday(null);
                }
            }
            if (profileData.containsKey("gender")) {
                String gender = profileData.get("gender") != null ? profileData.get("gender").toString().trim() : null;
                if (gender != null && !gender.isEmpty()) {
                    if (!gender.matches("^(Nam|Nữ|Khác)$")) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "Giới tính không hợp lệ. Chỉ chấp nhận: Nam, Nữ, Khác");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                }
                existingProfile.setGender(gender);
            }
            if (profileData.containsKey("address")) {
                String address = profileData.get("address") != null ? profileData.get("address").toString().trim() : null;
                if (address != null && !address.isEmpty()) {
                    if (address.length() > 255) {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("success", false);
                        errorResponse.put("message", "Địa chỉ không được quá 255 ký tự");
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                    // Bỏ validation độ dài tối thiểu để cho phép địa chỉ ngắn
                }
                existingProfile.setAddress(address);
            }
            if (profileData.containsKey("imageUrl") && profileData.get("imageUrl") != null) {
                String imageUrl = profileData.get("imageUrl").toString().trim();
                if (imageUrl.length() > 500) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "URL ảnh không được quá 500 ký tự");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                existingProfile.setImageUrl(imageUrl);
            }
            // Lưu profile vào database bằng UserService để tránh lỗi Hibernate
            UserProfile savedProfile = userService.saveProfile(userId, profileData);

            log.info("Profile updated successfully for userId: {}", userId);
            log.info("Updated profile data: {}", savedProfile);

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Cập nhật profile thành công!");
            
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            log.error("Error updating profile", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Lỗi cập nhật profile: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}