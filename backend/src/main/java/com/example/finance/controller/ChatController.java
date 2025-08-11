package com.example.finance.controller;

import com.example.finance.service.OpenRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ChatController {

    private final OpenRouterService openRouterService;
    
    @Value("${ai.provider:openrouter}")
    private String aiProvider;

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> request) {
        try {
            String message = request.get("message");
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Message cannot be empty"));
            }

            log.info("Received chat message: {}", message);
            
            String response;
            // Sử dụng OpenRouter AI
            if (openRouterService.isAvailable()) {
                log.info("Using OpenRouter AI for response");
                response = openRouterService.chat(message);
            } else {
                log.error("OpenRouter service not available");
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "OpenRouter AI không khả dụng. Vui lòng kiểm tra cấu hình."));
            }
            
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
    
    @GetMapping("/models")
    public ResponseEntity<?> getAvailableModels() {
        try {
            String[] models = openRouterService.getAvailableModels();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "models", models,
                "provider", "openrouter",
                "default_model", openRouterService.getDefaultModel()
            ));
        } catch (Exception e) {
            log.error("Error getting available models", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Không thể tải danh sách models"));
        }
    }
    
    @PostMapping("/models/pull")
    public ResponseEntity<?> pullModel(@RequestBody Map<String, String> request) {
        try {
            String modelName = request.get("model");
            if (modelName == null || modelName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Model name is required"));
            }
            
            // OpenRouter không cần pull model, chỉ cần API key
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OpenRouter model " + modelName + " đã sẵn sàng sử dụng"
            ));
        } catch (Exception e) {
            log.error("Error with model", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Model error: " + e.getMessage()));
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> getAIStatus() {
        try {
            Map<String, Object> status = Map.of(
                "provider", aiProvider,
                "openrouter_available", openRouterService.isAvailable(),
                "default_model", openRouterService.getDefaultModel(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "status", status
            ));
        } catch (Exception e) {
            log.error("Error getting AI status", e);
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Cannot get AI status"));
        }
    }
}
