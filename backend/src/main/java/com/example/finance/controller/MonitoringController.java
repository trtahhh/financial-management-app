package com.example.finance.controller;

import com.example.finance.service.CategorizationMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for AI Categorization Performance Monitoring
 * 
 * Endpoints:
 * - GET /api/monitoring/stats - Get comprehensive statistics
 * - GET /api/monitoring/layers - Get layer comparison
 * - POST /api/monitoring/feedback - Record user feedback
 * - POST /api/monitoring/reset - Reset statistics
 */
@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringController {
    
    @Autowired
    private CategorizationMonitoringService monitoringService;
    
    /**
     * Get comprehensive monitoring statistics
     * 
     * Returns:
     * - Layer usage distribution (%, count, avg time, avg confidence)
     * - Overall metrics (total transactions, avg response time)
     * - Accuracy metrics (if feedback available)
     * 
     * Example response:
     * {
     *   "layer_usage": {
     *     "layer0_python_ai": {
     *       "count": 75,
     *       "percentage": 75.0,
     *       "avg_time_ms": 45,
     *       "avg_confidence": 0.82
     *     },
     *     "layer1_keywords": { ... },
     *     ...
     *   },
     *   "overall": {
     *     "total_transactions": 100,
     *     "avg_response_time_ms": 52
     *   },
     *   "accuracy": {
     *     "correct": 45,
     *     "total_feedback": 50,
     *     "accuracy_percentage": 90.0,
     *     "alert": false
     *   }
     * }
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = monitoringService.getStats();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get layer performance comparison
     * 
     * Returns:
     * - Layer effectiveness (coverage %)
     * - Performance ranking
     * - Recommendations
     * 
     * Example response:
     * {
     *   "layer_effectiveness": {
     *     "layer0_coverage": "75.0%",
     *     "layer1_fallback": "20.0%",
     *     "layer2_fallback": "4.0%",
     *     "layer3_llm_usage": "1.0%"
     *   },
     *   "performance_ranking": [
     *     "Layer 0 (Python AI): Best for Vietnamese transactions",
     *     ...
     *   ],
     *   "recommendation": "System performing optimally"
     * }
     */
    @GetMapping("/layers")
    public ResponseEntity<Map<String, Object>> getLayerComparison() {
        Map<String, Object> comparison = monitoringService.getLayerComparison();
        return ResponseEntity.ok(comparison);
    }
    
    /**
     * Record user feedback on categorization accuracy
     * 
     * Used when user manually changes a category or confirms AI suggestion
     * 
     * Request body:
     * {
     *   "wasCorrect": true
     * }
     * 
     * Response:
     * {
     *   "message": "Feedback recorded successfully",
     *   "total_feedback": 51,
     *   "current_accuracy": 90.2
     * }
     */
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> recordFeedback(@RequestBody Map<String, Boolean> request) {
        Boolean wasCorrect = request.get("wasCorrect");
        if (wasCorrect == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Missing 'wasCorrect' field"));
        }
        
        monitoringService.recordFeedback(wasCorrect);
        
        Map<String, Object> stats = monitoringService.getStats();
        Map<String, Object> accuracy = (Map<String, Object>) stats.get("accuracy");
        
        Map<String, Object> response = Map.of(
            "message", "Feedback recorded successfully",
            "total_feedback", accuracy != null ? accuracy.get("total_feedback") : 0,
            "current_accuracy", accuracy != null ? accuracy.get("accuracy_percentage") : "N/A"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Reset all monitoring statistics
     * 
     * WARNING: This will clear all accumulated statistics
     * 
     * Response:
     * {
     *   "message": "Statistics reset successfully"
     * }
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetStats() {
        monitoringService.reset();
        return ResponseEntity.ok(Map.of("message", "Statistics reset successfully"));
    }
    
    /**
     * Get monitoring health status
     * 
     * Checks if monitoring is active and functioning
     * 
     * Response:
     * {
     *   "status": "healthy",
     *   "monitoring_active": true,
     *   "total_transactions_tracked": 100
     * }
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> stats = monitoringService.getStats();
        Map<String, Object> overall = (Map<String, Object>) stats.get("overall");
        
        Map<String, Object> health = Map.of(
            "status", "healthy",
            "monitoring_active", true,
            "total_transactions_tracked", overall != null ? overall.get("total_transactions") : 0
        );
        
        return ResponseEntity.ok(health);
    }
}
