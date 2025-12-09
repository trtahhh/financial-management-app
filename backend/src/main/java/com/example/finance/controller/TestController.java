package com.example.finance.controller;

import com.example.finance.service.AICategorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for AI categorization (no auth required)
 * For development/testing purposes only
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private AICategorizationService aiService;

    @PostMapping("/categorize")
    public ResponseEntity<?> testCategorize(@RequestBody Map<String, Object> request) {
        try {
            String description = (String) request.get("description");
            Double amount = request.get("amount") != null ? 
                          ((Number) request.get("amount")).doubleValue() : null;
            
            if (description == null || description.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Description is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Call AI service (no userId for testing)
            AICategorizationService.CategorizationResult result = 
                aiService.categorizeExpense(description, amount);
            
            // Convert to response format
            Map<String, Object> response = new HashMap<>();
            response.put("categoryId", result.getCategory());
            response.put("categoryName", result.getCategoryName());
            response.put("confidence", String.format("%.2f%%", result.getConfidence() * 100));
            response.put("reasoning", result.getReasoning());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
