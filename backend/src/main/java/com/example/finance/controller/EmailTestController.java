package com.example.finance.controller;

import com.example.finance.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class EmailTestController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/send-email")
    public ResponseEntity<?> sendTestEmail(@RequestBody Map<String, String> request) {
        try {
            String toEmail = request.get("email");
            
            if (toEmail == null || toEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Email is required"));
            }
            
            String subject = "Test Email from Financial Management App";
            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #007bff;">🧪 Test Email</h2>
                    <p>Đây là email test từ Financial Management App.</p>
                    <p>Nếu bạn nhận được email này, Gmail SMTP config đã hoạt động!</p>
                    <hr/>
                    <p style="color: #666; font-size: 12px;">
                        Sent from Financial Management App
                    </p>
                </body>
                </html>
            """;
            
            emailService.sendEmail(toEmail, subject, htmlContent);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email đã được gửi đến: " + toEmail
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Lỗi gửi email: " + e.getMessage()
            ));
        }
    }
}
