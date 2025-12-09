package com.example.finance.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Confidence Calibration Service
 * - Calibrates model confidence scores to match actual accuracy
 * - Provides uncertainty estimates
 * - Flags predictions requiring human review
 */
@Service
public class ConfidenceCalibrationService {
    
    // Temperature parameter for calibration (learned from validation set)
    private double temperature = 1.5; // Default, should be tuned
    
    /**
     * Calibrate raw model scores using temperature scaling
     */
    public CalibratedPrediction calibrate(
            Long predictedCategory,
            double[] rawScores,
            Map<Long, String> categoryNames) {
        
        // Apply temperature scaling
        double[] calibratedScores = temperatureScaling(rawScores, temperature);
        
        // Get top-K predictions
        List<CategoryScore> topK = getTopKPredictions(calibratedScores, categoryNames, 3);
        
        double confidence = topK.get(0).score;
        Long category = topK.get(0).categoryId;
        
        // Determine if human review is needed
        boolean needsReview = requiresHumanReview(confidence, topK);
        
        // Generate explanation
        String explanation = generateExplanation(topK, needsReview);
        
        return CalibratedPrediction.builder()
            .predictedCategory(category)
            .confidence(confidence)
            .alternativeSuggestions(topK.subList(1, Math.min(topK.size(), 3)))
            .requiresHumanReview(needsReview)
            .uncertaintyScore(calculateUncertainty(calibratedScores))
            .explanation(explanation)
            .build();
    }
    
    /**
     * Temperature scaling - scales logits to calibrate confidence
     * Higher temperature = more conservative (lower confidence)
     */
    private double[] temperatureScaling(double[] logits, double T) {
        // Scale logits by temperature
        double[] scaledLogits = new double[logits.length];
        for (int i = 0; i < logits.length; i++) {
            scaledLogits[i] = logits[i] / T;
        }
        
        // Apply softmax
        return softmax(scaledLogits);
    }
    
    /**
     * Softmax function - convert logits to probabilities
     */
    private double[] softmax(double[] logits) {
        double max = Arrays.stream(logits).max().orElse(0.0);
        double[] expScores = new double[logits.length];
        double sum = 0.0;
        
        for (int i = 0; i < logits.length; i++) {
            expScores[i] = Math.exp(logits[i] - max);
            sum += expScores[i];
        }
        
        for (int i = 0; i < logits.length; i++) {
            expScores[i] /= sum;
        }
        
        return expScores;
    }
    
    /**
     * Get top-K predictions sorted by confidence
     */
    private List<CategoryScore> getTopKPredictions(
            double[] scores, 
            Map<Long, String> categoryNames, 
            int k) {
        
        List<CategoryScore> allScores = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            Long categoryId = (long) (i + 1); // Assuming 1-indexed categories
            allScores.add(new CategoryScore(
                categoryId,
                categoryNames.getOrDefault(categoryId, "Unknown"),
                scores[i]
            ));
        }
        
        return allScores.stream()
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(k)
            .collect(Collectors.toList());
    }
    
    /**
     * Determine if prediction needs human review
     */
    private boolean requiresHumanReview(double confidence, List<CategoryScore> topK) {
        // Low confidence threshold
        if (confidence < 0.60) {
            return true;
        }
        
        // Close competition between top 2 categories
        if (topK.size() >= 2) {
            double topScore = topK.get(0).score;
            double secondScore = topK.get(1).score;
            
            // If difference is less than 20%, uncertain
            if (topScore - secondScore < 0.20) {
                return true;
            }
        }
        
        // High entropy (distributed across many categories)
        double entropy = calculateEntropy(topK);
        if (entropy > 1.5) { // Threshold learned from validation
            return true;
        }
        
        return false;
    }
    
    /**
     * Calculate Shannon entropy as uncertainty measure
     */
    private double calculateEntropy(List<CategoryScore> predictions) {
        double entropy = 0.0;
        for (CategoryScore pred : predictions) {
            if (pred.score > 0) {
                entropy -= pred.score * Math.log(pred.score) / Math.log(2);
            }
        }
        return entropy;
    }
    
    /**
     * Calculate overall uncertainty score (0-1)
     */
    private double calculateUncertainty(double[] scores) {
        // Use normalized entropy
        double entropy = 0.0;
        for (double score : scores) {
            if (score > 0) {
                entropy -= score * Math.log(score) / Math.log(2);
            }
        }
        
        // Normalize by max entropy (log2(N))
        double maxEntropy = Math.log(scores.length) / Math.log(2);
        return entropy / maxEntropy;
    }
    
    /**
     * Generate human-readable explanation
     */
    private String generateExplanation(List<CategoryScore> topK, boolean needsReview) {
        CategoryScore top = topK.get(0);
        
        if (needsReview) {
            if (topK.size() >= 2) {
                CategoryScore second = topK.get(1);
                return String.format(
                    "Moni không chắc chắn lắm. Có thể là **%s** (%.0f%%) hoặc **%s** (%.0f%%). Bạn kiểm tra lại nhé!",
                    top.categoryName, top.score * 100,
                    second.categoryName, second.score * 100
                );
            } else {
                return String.format(
                    "Moni nghĩ là **%s** nhưng không chắc lắm (%.0f%%). Bạn xác nhận lại nhé!",
                    top.categoryName, top.score * 100
                );
            }
        } else {
            return String.format(
                "Moni khá chắc đây là **%s** (%.0f%% confidence)",
                top.categoryName, top.score * 100
            );
        }
    }
    
    /**
     * Update temperature parameter based on validation data
     * Should be called periodically with calibration dataset
     */
    public void updateTemperature(List<CalibrationExample> examples) {
        // Use Expected Calibration Error (ECE) to find optimal temperature
        double bestT = 1.0;
        double minECE = Double.MAX_VALUE;
        
        for (double T = 0.5; T <= 3.0; T += 0.1) {
            double ece = calculateECE(examples, T);
            if (ece < minECE) {
                minECE = ece;
                bestT = T;
            }
        }
        
        this.temperature = bestT;
        System.out.println("Updated calibration temperature to: " + bestT);
    }
    
    /**
     * Calculate Expected Calibration Error
     */
    private double calculateECE(List<CalibrationExample> examples, double T) {
        // Implementation of ECE metric
        // Bins predictions by confidence and compares to actual accuracy
        // This would be implemented based on validation set
        return 0.0; // Placeholder
    }
    
    // === Inner Classes ===
    
    public static class CategoryScore {
        public Long categoryId;
        public String categoryName;
        public double score;
        
        public CategoryScore(Long categoryId, String categoryName, double score) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.score = score;
        }
    }
    
    public static class CalibratedPrediction {
        private Long predictedCategory;
        private double confidence;
        private List<CategoryScore> alternativeSuggestions;
        private boolean requiresHumanReview;
        private double uncertaintyScore;
        private String explanation;
        
        private CalibratedPrediction(Builder builder) {
            this.predictedCategory = builder.predictedCategory;
            this.confidence = builder.confidence;
            this.alternativeSuggestions = builder.alternativeSuggestions;
            this.requiresHumanReview = builder.requiresHumanReview;
            this.uncertaintyScore = builder.uncertaintyScore;
            this.explanation = builder.explanation;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public Long getPredictedCategory() { return predictedCategory; }
        public double getConfidence() { return confidence; }
        public List<CategoryScore> getAlternativeSuggestions() { return alternativeSuggestions; }
        public boolean isRequiresHumanReview() { return requiresHumanReview; }
        public double getUncertaintyScore() { return uncertaintyScore; }
        public String getExplanation() { return explanation; }
        
        public static class Builder {
            private Long predictedCategory;
            private double confidence;
            private List<CategoryScore> alternativeSuggestions;
            private boolean requiresHumanReview;
            private double uncertaintyScore;
            private String explanation;
            
            public Builder predictedCategory(Long val) { predictedCategory = val; return this; }
            public Builder confidence(double val) { confidence = val; return this; }
            public Builder alternativeSuggestions(List<CategoryScore> val) { alternativeSuggestions = val; return this; }
            public Builder requiresHumanReview(boolean val) { requiresHumanReview = val; return this; }
            public Builder uncertaintyScore(double val) { uncertaintyScore = val; return this; }
            public Builder explanation(String val) { explanation = val; return this; }
            
            public CalibratedPrediction build() {
                return new CalibratedPrediction(this);
            }
        }
    }
    
    public static class CalibrationExample {
        public double[] logits;
        public Long trueCategory;
        
        public CalibrationExample(double[] logits, Long trueCategory) {
            this.logits = logits;
            this.trueCategory = trueCategory;
        }
    }
}
