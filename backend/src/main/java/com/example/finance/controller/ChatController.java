package com.example.finance.controller;

import com.example.finance.service.ChatGPTService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatGPTService chatGPTService;

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Message cannot be empty"));
            }

            log.info("Received chat message: {}", message);
            String response = chatGPTService.getChatResponse(message);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", response,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Có lỗi xảy ra khi xử lý tin nhắn"));
        }
    }

    @GetMapping("/suggestions")
    public ResponseEntity<?> getChatSuggestions() {
        try {
            String[] suggestions = {
                "Làm thế nào để tiết kiệm tiền hiệu quả?",
                "Tôi nên đầu tư như thế nào?",
                "Cách lập ngân sách gia đình?",
                "Quản lý nợ thông minh ra sao?",
                "Bảo hiểm nào cần thiết?"
            };
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "suggestions", suggestions
            ));
        } catch (Exception e) {
            log.error("Error getting chat suggestions", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Không thể tải gợi ý"));
        }
    }
}
