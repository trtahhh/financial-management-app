package com.example.finance.controller;

import com.example.finance.dto.LoginRequest;
import com.example.finance.dto.RegisterRequest;
import com.example.finance.dto.AuthResponse;
import com.example.finance.entity.User;
import com.example.finance.entity.UserProfile;
import com.example.finance.service.UserService;
import com.example.finance.service.EmailService;
import com.example.finance.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;

import java.time.LocalDateTime;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Value("${email.verification.enabled:true}")
    private boolean emailVerificationEnabled;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            // Kiểm tra username đã tồn tại
            if (userService.existsByUsername(registerRequest.getUsername())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Tên đăng nhập đã tồn tại!"));
            }

            // Kiểm tra email đã tồn tại (nếu có)
            if (registerRequest.getEmail() != null && !registerRequest.getEmail().isEmpty()
                && userService.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Email đã tồn tại!"));
            }

            // Tạo user mới
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
            user.setRole("USER");
            user.setIsActive(true);
            user.setEmailVerified(false); // Email chưa được xác thực
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            User savedUser = userService.save(user);

            // Tạo profile cho user (nếu có thông tin)
            if (registerRequest.getFullName() != null || registerRequest.getPhone() != null ||
                registerRequest.getBirthday() != null || registerRequest.getGender() != null ||
                registerRequest.getAddress() != null || registerRequest.getImageUrl() != null) {
                try {
                    // Tạo Map từ RegisterRequest thay vì gọi profile.toMap()
                    Map<String, Object> profileData = new HashMap<>();
                    profileData.put("fullName", registerRequest.getFullName());
                    profileData.put("phone", registerRequest.getPhone());
                    profileData.put("birthday", registerRequest.getBirthday());
                    profileData.put("gender", registerRequest.getGender());
                    profileData.put("address", registerRequest.getAddress());
                    profileData.put("imageUrl", registerRequest.getImageUrl());
                    
                    // Lưu profile vào bảng User_Profile
                    UserProfile savedProfile = userService.saveProfile(savedUser.getId(), profileData);
                    log.info("Profile created successfully for user: {} with profile: {}", savedUser.getId(), savedProfile);
                } catch (Exception e) {
                    log.error("Error creating profile for user: {}", savedUser.getId(), e);
                    // Không throw exception, chỉ log lỗi để user vẫn được tạo
                }
            }

            // Gửi email verification nếu có email và email verification được bật
            if (emailVerificationEnabled && savedUser.getEmail() != null && !savedUser.getEmail().isEmpty()) {
                try {
                    // Tạo verification token
                    String verificationToken = java.util.UUID.randomUUID().toString();
                    LocalDateTime expirationTime = LocalDateTime.now().plusHours(24);
                    
                    // Cập nhật user với token
                    savedUser.setEmailVerificationToken(verificationToken);
                    savedUser.setEmailVerificationExpires(expirationTime);
                    userService.updateUser(savedUser);
                    
                    // Tạo verification URL
                    String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;
                    
                    // Gửi email
                    emailService.sendVerificationEmail(
                        savedUser.getEmail(), 
                        savedUser.getUsername(), 
                        verificationUrl
                    );
                    
                    log.info("Verification email sent successfully to: {}", savedUser.getEmail());
                } catch (Exception e) {
                    log.error("Error sending verification email to: {}", savedUser.getEmail(), e);
                    // Không throw exception, chỉ log lỗi để user vẫn được tạo
                }
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Đăng ký thành công! Vui lòng kiểm tra email để xác thực tài khoản."));
        } catch (Exception e) {
            log.error("Error during registration", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi đăng ký: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("Login attempt for user: " + loginRequest.getUsername());
            
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            String jwt = tokenProvider.generateToken(authentication);
            User user = userService.findByUsername(loginRequest.getUsername());
            UserProfile profile = userService.findProfileByUserId(user.getId()).orElse(null);

            // Kiểm tra email verification nếu được bật
            if (emailVerificationEnabled && user.getEmail() != null && !user.getEmail().isEmpty() && !user.getEmailVerified()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Vui lòng xác thực email trước khi đăng nhập!"));
            }

            System.out.println("Login successful for user: " + loginRequest.getUsername());
            
            // Trả về cả user và profile nếu cần
            return ResponseEntity.ok(new AuthResponse(jwt, user, profile));
        } catch (Exception e) {
            log.error("Login failed for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Tên đăng nhập hoặc mật khẩu không đúng!"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            
            if (username == null || email == null || username.trim().isEmpty() || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Vui lòng nhập đầy đủ tên đăng nhập và email!"));
            }
            
            // Kiểm tra username và email có khớp không
            User user = userService.findByUsername(username);
            if (user == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Tên đăng nhập không tồn tại!"));
            }
            
            if (!email.equalsIgnoreCase(user.getEmail())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Email không khớp với tên đăng nhập!"));
            }
            
            // Tạo reset password token
            String resetToken = java.util.UUID.randomUUID().toString();
            LocalDateTime expirationTime = LocalDateTime.now().plusHours(1); // Token hết hạn sau 1 giờ
            
            // Cập nhật user với reset token
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetExpires(expirationTime);
            userService.updateUser(user);
            
            // Tạo reset URL
            String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;
            
            // Gửi email reset password
            emailService.sendPasswordResetEmail(
                user.getEmail(), 
                user.getUsername(), 
                resetUrl
            );
            
            log.info("Password reset email sent successfully to: {}", user.getEmail());
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Email đặt lại mật khẩu đã được gửi! Vui lòng kiểm tra email của bạn."
            ));
            
        } catch (Exception e) {
            log.error("Error during forgot password process", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi xử lý yêu cầu: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");
            
            if (token == null || newPassword == null || token.trim().isEmpty() || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Vui lòng nhập đầy đủ thông tin!"));
            }
            
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Mật khẩu mới phải có ít nhất 6 ký tự!"));
            }
            
            // Tìm user theo reset token
            User user = userService.findByPasswordResetToken(token);
            if (user == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Token không hợp lệ!"));
            }
            
            // Kiểm tra token có hết hạn không
            if (user.getPasswordResetExpires() == null || user.getPasswordResetExpires().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Token đã hết hạn! Vui lòng yêu cầu lại."));
            }
            
            // Cập nhật mật khẩu mới
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setPasswordResetToken(null);
            user.setPasswordResetExpires(null);
            user.setUpdatedAt(LocalDateTime.now());
            userService.updateUser(user);
            
            log.info("Password reset successfully for user: {}", user.getUsername());
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "Mật khẩu đã được đặt lại thành công! Vui lòng đăng nhập với mật khẩu mới."
            ));
            
        } catch (Exception e) {
            log.error("Error during password reset", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi đặt lại mật khẩu: " + e.getMessage()));
        }
    }

    // Inner class cho response
    public static class ApiResponse {
        private Boolean success;
        private String message;

        public ApiResponse(Boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        // Getters and setters
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}