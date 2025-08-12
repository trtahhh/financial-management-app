package com.example.finance.controller;

import com.example.finance.dto.ApiResponse;
import com.example.finance.service.EmailService;
import com.example.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class EmailVerificationController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @Value("${email.verification.enabled:true}")
    private boolean emailVerificationEnabled;

    @Value("${email.verification.token.expiration:24h}")
    private String tokenExpiration;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Gửi email xác thực
     */
    @PostMapping("/send-verification")
    public ResponseEntity<ApiResponse> sendVerificationEmail(@RequestParam String email) {
        try {
            if (!emailVerificationEnabled) {
                return ResponseEntity.ok(new ApiResponse(false, "Email verification is disabled", null));
            }

            // Kiểm tra user có tồn tại không
            var user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Email không tồn tại trong hệ thống", null));
            }

            if (user.getEmailVerified()) {
                return ResponseEntity.ok(new ApiResponse(false, "Email đã được xác thực", null));
            }

            // Tạo verification token
            String verificationToken = UUID.randomUUID().toString();
            LocalDateTime expirationTime = LocalDateTime.now().plusHours(24);

            // Cập nhật user với token
            user.setEmailVerificationToken(verificationToken);
            user.setEmailVerificationExpires(expirationTime);
            userService.updateUser(user);

            // Tạo verification URL
            String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;

            // Gửi email
            emailService.sendVerificationEmail(
                email, 
                user.getUsername(), 
                verificationUrl
            );

            return ResponseEntity.ok(new ApiResponse(true, "Email xác thực đã được gửi", null));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse(false, "Lỗi khi gửi email: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Xác thực email
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse> verifyEmail(@RequestBody EmailVerificationRequest request) {
        try {
            if (!emailVerificationEnabled) {
                return ResponseEntity.ok(new ApiResponse(false, "Email verification is disabled", null));
            }

            // Tìm user với token
            var user = userService.findByVerificationToken(request.getToken());
            if (user == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Token xác thực không hợp lệ", null));
            }

            // Kiểm tra token có hết hạn không
            if (user.getEmailVerificationExpires().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Token xác thực đã hết hạn", null));
            }

            // Xác thực email
            user.setEmailVerified(true);
            user.setEmailVerificationToken(null);
            user.setEmailVerificationExpires(null);
            userService.updateUser(user);

            return ResponseEntity.ok(new ApiResponse(true, "Email đã được xác thực thành công", null));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse(false, "Lỗi khi xác thực email: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Gửi lại email xác thực
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse> resendVerificationEmail(@RequestParam String email) {
        try {
            if (!emailVerificationEnabled) {
                return ResponseEntity.ok(new ApiResponse(false, "Email verification is disabled", null));
            }

            // Kiểm tra user có tồn tại không
            var user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Email không tồn tại trong hệ thống", null));
            }

            if (user.getEmailVerified()) {
                return ResponseEntity.ok(new ApiResponse(false, "Email đã được xác thực", null));
            }

            // Tạo verification token mới
            String verificationToken = UUID.randomUUID().toString();
            LocalDateTime expirationTime = LocalDateTime.now().plusHours(24);

            // Cập nhật user với token mới
            user.setEmailVerificationToken(verificationToken);
            user.setEmailVerificationExpires(expirationTime);
            userService.updateUser(user);

            // Tạo verification URL
            String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;

            // Gửi email
            emailService.sendVerificationEmail(
                email, 
                user.getUsername(), 
                verificationUrl
            );

            return ResponseEntity.ok(new ApiResponse(true, "Email xác thực đã được gửi lại", null));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse(false, "Lỗi khi gửi lại email: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Kiểm tra trạng thái xác thực email
     */
    @GetMapping("/verification-status")
    public ResponseEntity<ApiResponse> getVerificationStatus(@RequestParam String email) {
        try {
            var user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Email không tồn tại trong hệ thống", null));
            }

            var status = new VerificationStatus(
                user.getEmailVerified(),
                user.getEmailVerificationToken() != null,
                user.getEmailVerificationExpires()
            );

            return ResponseEntity.ok(new ApiResponse(true, "Thông tin trạng thái xác thực", status));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse(false, "Lỗi khi kiểm tra trạng thái: " + e.getMessage(), null)
            );
        }
    }

    /**
     * Test endpoint để gửi email thông báo (chỉ dùng để test)
     */
    @PostMapping("/test-email")
    public ResponseEntity<ApiResponse> testEmail(@RequestBody TestEmailRequest request) {
        try {
            if (!emailVerificationEnabled) {
                return ResponseEntity.ok(new ApiResponse(false, "Email verification is disabled", null));
            }

            // Gửi test email
            emailService.sendNotificationEmail(
                request.getEmail(),
                request.getSubject(),
                request.getMessage()
            );

            return ResponseEntity.ok(new ApiResponse(true, "Test email đã được gửi thành công", null));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse(false, "Lỗi khi gửi test email: " + e.getMessage(), null)
            );
        }
    }

    // Inner class để trả về thông tin trạng thái
    public static class VerificationStatus {
        private boolean verified;
        private boolean hasToken;
        private LocalDateTime tokenExpires;

        public VerificationStatus(boolean verified, boolean hasToken, LocalDateTime tokenExpires) {
            this.verified = verified;
            this.hasToken = hasToken;
            this.tokenExpires = tokenExpires;
        }

        // Getters
        public boolean isVerified() { return verified; }
        public boolean isHasToken() { return hasToken; }
        public LocalDateTime getTokenExpires() { return tokenExpires; }
    }

    // Inner class để nhận request body
    public static class EmailVerificationRequest {
        private String token;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }

    // Inner class để nhận test email request
    public static class TestEmailRequest {
        private String email;
        private String subject;
        private String message;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
