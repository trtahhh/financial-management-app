package com.example.finance.controller;

import com.example.finance.dto.LoginRequest;
import com.example.finance.dto.RegisterRequest;
import com.example.finance.dto.AuthResponse;
import com.example.finance.entity.User;
import com.example.finance.entity.UserProfile;
import com.example.finance.service.UserService;
import com.example.finance.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            // Kiểm tra username đã tồn tại
            if (userService.existsByUsername(registerRequest.getUsername())) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Tên đăng nhập đã tồn tại!"));
            }

            // Kiểm tra email đã tồn tại (nếu có)
            if (registerRequest.getEmail() != null && !registerRequest.getEmail().isEmpty() 
                && userService.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Email đã tồn tại!"));
            }

            // Tạo user mới
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setEmail(registerRequest.getEmail());
            user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
            user.setFullName(registerRequest.getFullName());
            user.setRole("USER");
            user.setCreatedAt(LocalDateTime.now());

            User savedUser = userService.save(user);

            // Tạo profile cho user
            if (registerRequest.getPhone() != null || registerRequest.getBirthday() != null 
                || registerRequest.getGender() != null) {
                UserProfile profile = new UserProfile();
                profile.setUser(savedUser);  // Set User object thay vì userId
                profile.setPhone(registerRequest.getPhone());
                profile.setBirthday(registerRequest.getBirthday());
                profile.setGender(registerRequest.getGender());
                userService.saveProfile(profile);
            }

            return ResponseEntity.ok(new ApiResponse(true, "Đăng ký thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Lỗi đăng ký: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            String jwt = tokenProvider.generateToken(authentication);
            User user = userService.findByUsername(loginRequest.getUsername());

            return ResponseEntity.ok(new AuthResponse(jwt, user));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new ApiResponse(false, "Tên đăng nhập hoặc mật khẩu không đúng!"));
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
