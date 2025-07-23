package com.example.finance.controller;
import com.example.finance.entity.User;
import com.example.finance.service.UserService;
import com.example.finance.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired UserService userService;
    @Autowired JwtUtil jwtUtil;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String,String> req) {
        try {
            System.out.println("Register request: " + req); // Debug log
            
            String username = req.get("username");
            String email = req.get("email");
            String password = req.get("password");
            String role = req.get("role"); // Lấy role từ request
            
            System.out.println("Role from request: " + role); // Debug log
            
            // Validate role
            if (role == null || role.trim().isEmpty()) {
                role = "user"; // Default role
                System.out.println("Role is null/empty, using default: " + role); // Debug log
            }
            
            // Đảm bảo role là lowercase
            role = role.toLowerCase();
            System.out.println("Role after toLowerCase: " + role); // Debug log
            
            // Chỉ cho phép các role hợp lệ
            if (!"user".equals(role) && !"admin".equals(role)) {
                role = "user"; // Default nếu role không hợp lệ
                System.out.println("Invalid role, using default: " + role); // Debug log
            }
            
            System.out.println("Final role: " + role); // Debug log
            
            User u = userService.register(username, email, password, role);
            String token = jwtUtil.generateToken(u);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (RuntimeException e) {
            System.out.println("Register error: " + e.getMessage()); // Debug log
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> req) {
        try {
            User u = userService.findByEmail(req.get("email")).orElseThrow(() -> new RuntimeException("Email không tồn tại"));
            if (!jwtUtil.passwordEncoder().matches(req.get("password"), u.getPasswordHash()))
                throw new RuntimeException("Sai mật khẩu");
            String token = jwtUtil.generateToken(u);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}