package com.example.finance.controller;

import com.example.finance.dto.CategorySuggestionDTO;
import com.example.finance.entity.Category;
import com.example.finance.service.CategorySuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/category-suggestions")
public class CategorySuggestionController {
    
    @Autowired
    private CategorySuggestionService suggestionService;
    
    /**
     * Get all pending category suggestions for current user
     * GET /api/category-suggestions/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<List<CategorySuggestionDTO>> getPendingSuggestions(
            @RequestParam Long userId) {
        
        List<CategorySuggestionDTO> suggestions = suggestionService.getPendingSuggestions(userId);
        return ResponseEntity.ok(suggestions);
    }
    
    /**
     * Approve a suggestion and create new category
     * POST /api/category-suggestions/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveSuggestion(
            @PathVariable Long id,
            @RequestParam Long userId) {
        
        try {
            Category createdCategory = suggestionService.approveSuggestion(id, userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Category created successfully",
                "category", Map.of(
                    "id", createdCategory.getId(),
                    "name", createdCategory.getName(),
                    "type", createdCategory.getType(),
                    "color", createdCategory.getColor(),
                    "icon", createdCategory.getIcon()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Reject a suggestion
     * POST /api/category-suggestions/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectSuggestion(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestBody(required = false) Map<String, String> body) {
        
        try {
            String reason = body != null ? body.get("reason") : "User rejected";
            suggestionService.rejectSuggestion(id, userId, reason);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Suggestion rejected"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Merge suggestion with existing category
     * POST /api/category-suggestions/{id}/merge
     */
    @PostMapping("/{id}/merge")
    public ResponseEntity<?> mergeSuggestion(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestBody Map<String, Long> body) {
        
        try {
            Long existingCategoryId = body.get("categoryId");
            if (existingCategoryId == null) {
                throw new IllegalArgumentException("categoryId is required");
            }
            
            suggestionService.mergeSuggestion(id, userId, existingCategoryId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Suggestion merged with existing category"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}
