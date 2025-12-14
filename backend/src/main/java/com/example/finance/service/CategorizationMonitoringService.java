package com.example.finance.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;

/**
 * Performance Monitoring Service for AI Categorization System
 * Tracks: Layer usage, accuracy, response times, confidence scores
 */
@Slf4j
@Service
public class CategorizationMonitoringService {
    
    // Layer usage counters
    private final AtomicLong layer0Count = new AtomicLong(0);  // Python AI
    private final AtomicLong layer1Count = new AtomicLong(0);  // Keywords
    private final AtomicLong layer2Count = new AtomicLong(0);  // Fuzzy
    private final AtomicLong layer3Count = new AtomicLong(0);  // LLM
    private final AtomicLong totalCount = new AtomicLong(0);
    
    // Response time tracking (milliseconds)
    private final AtomicLong layer0TotalTime = new AtomicLong(0);
    private final AtomicLong layer1TotalTime = new AtomicLong(0);
    private final AtomicLong layer2TotalTime = new AtomicLong(0);
    private final AtomicLong layer3TotalTime = new AtomicLong(0);
    
    // Confidence score tracking
    private final Map<String, List<Double>> confidenceByLayer = new ConcurrentHashMap<>();
    
    // Accuracy tracking (requires user feedback)
    private final AtomicLong correctPredictions = new AtomicLong(0);
    private final AtomicLong totalFeedback = new AtomicLong(0);
    
    // Alert thresholds
    private static final double LLM_USAGE_ALERT_THRESHOLD = 0.10;  // 10%
    private static final double ACCURACY_ALERT_THRESHOLD = 0.80;   // 80%
    
    public CategorizationMonitoringService() {
        confidenceByLayer.put("layer0", new ArrayList<>());
        confidenceByLayer.put("layer1", new ArrayList<>());
        confidenceByLayer.put("layer2", new ArrayList<>());
        confidenceByLayer.put("layer3", new ArrayList<>());
    }
    
    /**
     * Record a successful categorization
     */
    public void recordCategorization(int layer, long responseTimeMs, double confidence) {
        totalCount.incrementAndGet();
        
        switch (layer) {
            case 0:
                layer0Count.incrementAndGet();
                layer0TotalTime.addAndGet(responseTimeMs);
                addConfidence("layer0", confidence);
                break;
            case 1:
                layer1Count.incrementAndGet();
                layer1TotalTime.addAndGet(responseTimeMs);
                addConfidence("layer1", confidence);
                break;
            case 2:
                layer2Count.incrementAndGet();
                layer2TotalTime.addAndGet(responseTimeMs);
                addConfidence("layer2", confidence);
                break;
            case 3:
                layer3Count.incrementAndGet();
                layer3TotalTime.addAndGet(responseTimeMs);
                addConfidence("layer3", confidence);
                checkLLMUsageAlert();
                break;
        }
    }
    
    /**
     * Record user feedback on categorization accuracy
     */
    public void recordFeedback(boolean wasCorrect) {
        totalFeedback.incrementAndGet();
        if (wasCorrect) {
            correctPredictions.incrementAndGet();
        }
        checkAccuracyAlert();
    }
    
    /**
     * Get comprehensive statistics
     */
    public Map<String, Object> getStats() {
        long total = totalCount.get();
        
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        // Layer usage distribution
        stats.put("layer_usage", Map.of(
            "layer0_python_ai", Map.of(
                "count", layer0Count.get(),
                "percentage", total > 0 ? (layer0Count.get() * 100.0 / total) : 0,
                "avg_time_ms", layer0Count.get() > 0 ? (layer0TotalTime.get() / layer0Count.get()) : 0,
                "avg_confidence", getAverageConfidence("layer0")
            ),
            "layer1_keywords", Map.of(
                "count", layer1Count.get(),
                "percentage", total > 0 ? (layer1Count.get() * 100.0 / total) : 0,
                "avg_time_ms", layer1Count.get() > 0 ? (layer1TotalTime.get() / layer1Count.get()) : 0,
                "avg_confidence", getAverageConfidence("layer1")
            ),
            "layer2_fuzzy", Map.of(
                "count", layer2Count.get(),
                "percentage", total > 0 ? (layer2Count.get() * 100.0 / total) : 0,
                "avg_time_ms", layer2Count.get() > 0 ? (layer2TotalTime.get() / layer2Count.get()) : 0,
                "avg_confidence", getAverageConfidence("layer2")
            ),
            "layer3_llm", Map.of(
                "count", layer3Count.get(),
                "percentage", total > 0 ? (layer3Count.get() * 100.0 / total) : 0,
                "avg_time_ms", layer3Count.get() > 0 ? (layer3TotalTime.get() / layer3Count.get()) : 0,
                "avg_confidence", getAverageConfidence("layer3")
            )
        ));
        
        // Overall metrics
        stats.put("overall", Map.of(
            "total_transactions", total,
            "avg_response_time_ms", total > 0 ? 
                ((layer0TotalTime.get() + layer1TotalTime.get() + 
                  layer2TotalTime.get() + layer3TotalTime.get()) / total) : 0
        ));
        
        // Accuracy metrics (if feedback available)
        long feedbackCount = totalFeedback.get();
        if (feedbackCount > 0) {
            double accuracy = correctPredictions.get() * 100.0 / feedbackCount;
            stats.put("accuracy", Map.of(
                "correct", correctPredictions.get(),
                "total_feedback", feedbackCount,
                "accuracy_percentage", accuracy,
                "alert", accuracy < (ACCURACY_ALERT_THRESHOLD * 100)
            ));
        }
        
        stats.put("timestamp", LocalDateTime.now().toString());
        
        return stats;
    }
    
    /**
     * Get layer performance comparison
     */
    public Map<String, Object> getLayerComparison() {
        long total = totalCount.get();
        if (total == 0) {
            return Map.of("message", "No data available yet");
        }
        
        return Map.of(
            "layer_effectiveness", Map.of(
                "layer0_coverage", String.format("%.1f%%", layer0Count.get() * 100.0 / total),
                "layer1_fallback", String.format("%.1f%%", layer1Count.get() * 100.0 / total),
                "layer2_fallback", String.format("%.1f%%", layer2Count.get() * 100.0 / total),
                "layer3_llm_usage", String.format("%.1f%%", layer3Count.get() * 100.0 / total)
            ),
            "performance_ranking", List.of(
                "Layer 0 (Python AI): Best for Vietnamese transactions",
                "Layer 1 (Keywords): Fast fallback, high confidence",
                "Layer 2 (Fuzzy): Handles typos and variations",
                "Layer 3 (LLM): Complex cases, highest cost"
            ),
            "recommendation", getLLMUsage() > 0.05 ? 
                "Layer 0 keywords may need enhancement" : 
                "System performing optimally"
        );
    }
    
    /**
     * Reset all statistics
     */
    public void reset() {
        layer0Count.set(0);
        layer1Count.set(0);
        layer2Count.set(0);
        layer3Count.set(0);
        totalCount.set(0);
        
        layer0TotalTime.set(0);
        layer1TotalTime.set(0);
        layer2TotalTime.set(0);
        layer3TotalTime.set(0);
        
        confidenceByLayer.values().forEach(List::clear);
        
        correctPredictions.set(0);
        totalFeedback.set(0);
        
        log.info("📊 Monitoring statistics reset");
    }
    
    // ===== Private helper methods =====
    
    private synchronized void addConfidence(String layer, double confidence) {
        List<Double> confidences = confidenceByLayer.get(layer);
        confidences.add(confidence);
        
        // Keep only last 1000 values to prevent memory issues
        if (confidences.size() > 1000) {
            confidences.remove(0);
        }
    }
    
    private double getAverageConfidence(String layer) {
        List<Double> confidences = confidenceByLayer.get(layer);
        if (confidences.isEmpty()) {
            return 0.0;
        }
        return confidences.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }
    
    private double getLLMUsage() {
        long total = totalCount.get();
        return total > 0 ? (layer3Count.get() * 1.0 / total) : 0.0;
    }
    
    private void checkLLMUsageAlert() {
        double llmUsage = getLLMUsage();
        if (llmUsage > LLM_USAGE_ALERT_THRESHOLD) {
            log.warn("⚠️  ALERT: LLM usage is {:.1f}% (threshold: {:.0f}%). " +
                    "Consider enhancing Layer 0 (Python AI) keywords.",
                    llmUsage * 100, LLM_USAGE_ALERT_THRESHOLD * 100);
        }
    }
    
    private void checkAccuracyAlert() {
        long feedbackCount = totalFeedback.get();
        if (feedbackCount >= 10) {  // Only alert after 10+ feedback
            double accuracy = correctPredictions.get() * 1.0 / feedbackCount;
            if (accuracy < ACCURACY_ALERT_THRESHOLD) {
                log.warn("⚠️  ALERT: Accuracy is {:.1f}% (threshold: {:.0f}%). " +
                        "Model may need retraining.",
                        accuracy * 100, ACCURACY_ALERT_THRESHOLD * 100);
            }
        }
    }
}
