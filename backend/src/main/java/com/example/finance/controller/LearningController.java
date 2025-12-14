package com.example.finance.controller;

import com.example.finance.service.UserLearningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for User Learning System
 * 
 * Endpoints:
 * - POST /api/learning/correction - Record user correction
 * - GET /api/learning/stats/{userId} - Get user learning statistics
 * - GET /api/learning/global-stats - Get global correction statistics
 * - GET /api/learning/keyword-suggestions - Get keyword improvement suggestions
 * - DELETE /api/learning/user/{userId} - Clear user learning data (GDPR)
 */
@RestController
@RequestMapping("/api/learning")
@CrossOrigin(origins = "*")
public class LearningController {
    
    @Autowired
    private UserLearningService learningService;
    
    /**
     * Record a user correction
     * 
     * Called when user manually changes a transaction category
     * 
     * Request body:
     * {
     *   "userId": 1,
     *   "description": "Cafe Highlands",
     *   "aiPredictedCategory": 14,
     *   "userCorrectedCategory": 5,
     *   "layer": 0
     * }
     * 
     * Response:
     * {
     *   "message": "Correction recorded successfully",
     *   "learning_active": true,
     *   "suggestion": "Pattern detected: Consider adding keywords to category 5"
     * }
     */
    @PostMapping("/correction")
    public ResponseEntity<Map<String, Object>> recordCorrection(
            @RequestBody Map<String, Object> request) {
        
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String description = request.get("description").toString();
            Long aiPredicted = Long.valueOf(request.get("aiPredictedCategory").toString());
            Long userCorrected = Long.valueOf(request.get("userCorrectedCategory").toString());
            int layer = Integer.parseInt(request.get("layer").toString());
            
            learningService.recordCorrection(userId, description, aiPredicted, userCorrected, layer);
            
            // Check if user has a suggestion
            Long suggested = learningService.getSuggestedCategory(userId, description, aiPredicted);
            
            Map<String, Object> response;
            if (suggested != null) {
                response = Map.of(
                    "message", "Correction recorded successfully",
                    "learning_active", true,
                    "suggestion", "Based on your history, category " + suggested + " might be better for similar transactions",
                    "suggested_category", suggested
                );
            } else {
                response = Map.of(
                    "message", "Correction recorded successfully",
                    "learning_active", true
                );
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }
    
    /**
     * Get user-specific learning statistics
     * 
     * Shows what patterns the system has learned for this user
     * 
     * Response:
     * {
     *   "user_id": 1,
     *   "total_patterns": 3,
     *   "patterns": [
     *     {
     *       "from_category": 14,
     *       "to_category": 5,
     *       "occurrences": 7,
     *       "last_seen": "2025-12-01T07:15:00"
     *     },
     *     ...
     *   ],
     *   "top_correction": { ... }
     * }
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable Long userId) {
        Map<String, Object> stats = learningService.getUserLearningStats(userId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get global correction statistics (all users)
     * 
     * Useful for identifying system-wide categorization issues
     * 
     * Response:
     * {
     *   "total_unique_corrections": 25,
     *   "top_corrections": [
     *     {
     *       "description": "cafe highlands",
     *       "ai_predicted": 14,
     *       "user_corrected": 5,
     *       "count": 12
     *     },
     *     ...
     *   ],
     *   "recommendation": "Consider reviewing top corrections for keyword improvements"
     * }
     */
    @GetMapping("/global-stats")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        Map<String, Object> stats = learningService.getGlobalCorrectionStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get keyword suggestions for Python AI improvement
     * 
     * Analyzes frequent corrections and suggests keywords to add
     * 
     * Response:
     * {
     *   "suggestions": [
     *     {
     *       "description_pattern": "cafe highlands",
     *       "correct_category": 5,
     *       "correction_count": 12,
     *       "suggested_keywords": ["cafe", "highlands"],
     *       "action": "Add these keywords to category 5"
     *     },
     *     ...
     *   ],
     *   "total_suggestions": 5
     * }
     */
    @GetMapping("/keyword-suggestions")
    public ResponseEntity<Map<String, Object>> getKeywordSuggestions() {
        List<Map<String, Object>> suggestions = learningService.getKeywordSuggestions();
        
        return ResponseEntity.ok(Map.of(
            "suggestions", suggestions,
            "total_suggestions", suggestions.size(),
            "instruction", "Add suggested keywords to simple_vietnamese_nlp.py in ai-service folder"
        ));
    }
    
    /**
     * Clear learning data for a user (GDPR compliance)
     * 
     * Deletes all learned patterns for the specified user
     * 
     * Response:
     * {
     *   "message": "User learning data cleared successfully",
     *   "user_id": 1
     * }
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> clearUserData(@PathVariable Long userId) {
        learningService.clearUserData(userId);
        
        return ResponseEntity.ok(Map.of(
            "message", "User learning data cleared successfully",
            "user_id", userId
        ));
    }
    
    /**
     * Health check for learning system
     * 
     * Response:
     * {
     *   "status": "healthy",
     *   "learning_enabled": true
     * }
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "learning_enabled", true,
            "features", List.of(
                "User correction tracking",
                "Pattern detection",
                "Confidence adjustment",
                "Keyword suggestions"
            )
        ));
    }
}
