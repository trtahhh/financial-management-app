package com.example.finance.service;

import com.example.finance.entity.UserCategorizationPreference;
import com.example.finance.repository.UserCategorizationPreferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * User Learning Service - Learns from user corrections and improves predictions
 * 
 * Features:
 * 1. Track user corrections (AI predicted X, user changed to Y)
 * 2. Identify patterns ("User always changes X to Y")
 * 3. Adjust confidence scores based on learned patterns
 * 4. Suggest keyword improvements for Python AI Layer 0
 * 5. Track learning statistics
 */
@Slf4j
@Service
public class UserLearningService {
    
    @Autowired
    private UserCategorizationPreferenceRepository userPrefRepository;
    
    @Autowired
    private CategorizationMonitoringService monitoringService;
    
    // In-memory cache for quick pattern lookup
    private final Map<Long, Map<String, PatternInfo>> userPatterns = new ConcurrentHashMap<>();
    
    // Track corrections for learning
    private final Map<String, CorrectionStats> correctionStats = new ConcurrentHashMap<>();
    
    /**
     * Pattern info: tracks how often user corrects specific predictions
     */
    public static class PatternInfo {
        public Long fromCategoryId;
        public Long toCategoryId;
        public int occurrences;
        public LocalDateTime lastSeen;
        
        public PatternInfo(Long from, Long to) {
            this.fromCategoryId = from;
            this.toCategoryId = to;
            this.occurrences = 1;
            this.lastSeen = LocalDateTime.now();
        }
        
        public void increment() {
            this.occurrences++;
            this.lastSeen = LocalDateTime.now();
        }
    }
    
    /**
     * Correction statistics for a specific pattern
     */
    public static class CorrectionStats {
        public String description;
        public Long aiPredictedCategory;
        public Long userCorrectedCategory;
        public int count;
        public List<LocalDateTime> timestamps = new ArrayList<>();
        
        public CorrectionStats(String desc, Long aiPredicted, Long userCorrected) {
            this.description = desc;
            this.aiPredictedCategory = aiPredicted;
            this.userCorrectedCategory = userCorrected;
            this.count = 1;
            this.timestamps.add(LocalDateTime.now());
        }
        
        public void increment() {
            this.count++;
            this.timestamps.add(LocalDateTime.now());
            if (this.timestamps.size() > 10) {
                this.timestamps.remove(0); // Keep last 10 only
            }
        }
    }
    
    /**
     * Record a user correction
     * 
     * @param userId User who made the correction
     * @param description Transaction description
     * @param aiPredictedCategory What AI predicted
     * @param userCorrectedCategory What user changed it to
     * @param layer Which layer made the prediction (0=Python AI, 1=Keywords, etc.)
     */
    public void recordCorrection(Long userId, String description, Long aiPredictedCategory, 
                                 Long userCorrectedCategory, int layer) {
        
        // Don't record if prediction was correct
        if (aiPredictedCategory.equals(userCorrectedCategory)) {
            monitoringService.recordFeedback(true);
            log.info("✅ User confirmed AI prediction - no correction needed");
            return;
        }
        
        // Record feedback as incorrect
        monitoringService.recordFeedback(false);
        
        log.info("📝 Recording correction: User {} changed category {} → {} for '{}'", 
                userId, aiPredictedCategory, userCorrectedCategory, description);
        
        // Update user patterns
        updateUserPattern(userId, aiPredictedCategory, userCorrectedCategory);
        
        // Update global correction stats
        updateCorrectionStats(description, aiPredictedCategory, userCorrectedCategory);
        
        // Save to database for persistence
        saveUserPreference(userId, description, userCorrectedCategory);
        
        // Check if we should suggest keyword improvements
        checkForKeywordSuggestions(description, aiPredictedCategory, userCorrectedCategory, layer);
    }
    
    /**
     * Get confidence adjustment based on user patterns
     * 
     * If user frequently changes category X to Y, reduce confidence for X
     * If user rarely corrects category Z, increase confidence for Z
     * 
     * @return Adjusted confidence multiplier (0.5 to 1.5)
     */
    public double getConfidenceAdjustment(Long userId, Long categoryId, String description) {
        Map<String, PatternInfo> patterns = userPatterns.get(userId);
        if (patterns == null || patterns.isEmpty()) {
            return 1.0; // No adjustment
        }
        
        // Check if user frequently corrects this category
        String patternKey = categoryId.toString();
        PatternInfo pattern = patterns.get(patternKey);
        
        if (pattern != null && pattern.occurrences >= 3) {
            // User has corrected this category 3+ times
            // Reduce confidence by 20%
            log.debug("⚠️  Reducing confidence for category {} (user corrected {}x)", 
                     categoryId, pattern.occurrences);
            return 0.80;
        }
        
        return 1.0; // No adjustment
    }
    
    /**
     * Get suggested category based on learned patterns
     * 
     * @return Category ID if user has a strong pattern, null otherwise
     */
    public Long getSuggestedCategory(Long userId, String description, Long aiPrediction) {
        Map<String, PatternInfo> patterns = userPatterns.get(userId);
        if (patterns == null) {
            return null;
        }
        
        // Check if user has a strong correction pattern for this AI prediction
        String patternKey = aiPrediction.toString();
        PatternInfo pattern = patterns.get(patternKey);
        
        if (pattern != null && pattern.occurrences >= 5) {
            // User has corrected this pattern 5+ times - very strong signal
            log.info("💡 User learning: Suggesting category {} instead of {} based on {} corrections",
                    pattern.toCategoryId, aiPrediction, pattern.occurrences);
            return pattern.toCategoryId;
        }
        
        return null;
    }
    
    /**
     * Get learning statistics for a user
     */
    public Map<String, Object> getUserLearningStats(Long userId) {
        Map<String, PatternInfo> patterns = userPatterns.get(userId);
        
        if (patterns == null || patterns.isEmpty()) {
            return Map.of(
                "user_id", userId,
                "total_patterns", 0,
                "message", "No learning data available yet"
            );
        }
        
        List<Map<String, Object>> patternList = patterns.entrySet().stream()
            .map(entry -> {
                Map<String, Object> map = new HashMap<>();
                map.put("from_category", entry.getValue().fromCategoryId);
                map.put("to_category", entry.getValue().toCategoryId);
                map.put("occurrences", entry.getValue().occurrences);
                map.put("last_seen", entry.getValue().lastSeen.toString());
                return map;
            })
            .sorted((a, b) -> ((Integer)b.get("occurrences")).compareTo((Integer)a.get("occurrences")))
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("user_id", userId);
        result.put("total_patterns", patterns.size());
        result.put("patterns", patternList);
        result.put("top_correction", patternList.isEmpty() ? "None" : patternList.get(0));
        return result;
    }
    
    /**
     * Get global correction statistics (all users)
     */
    public Map<String, Object> getGlobalCorrectionStats() {
        List<Map<String, Object>> topCorrections = correctionStats.entrySet().stream()
            .sorted((a, b) -> b.getValue().count - a.getValue().count)
            .limit(10)
            .map(entry -> {
                Map<String, Object> map = new HashMap<>();
                map.put("description", entry.getValue().description);
                map.put("ai_predicted", entry.getValue().aiPredictedCategory);
                map.put("user_corrected", entry.getValue().userCorrectedCategory);
                map.put("count", entry.getValue().count);
                return map;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("total_unique_corrections", correctionStats.size());
        result.put("top_corrections", topCorrections);
        result.put("recommendation", topCorrections.isEmpty() ? 
            "No corrections yet - system performing well" :
            "Consider reviewing top corrections for keyword improvements");
        return result;
    }
    
    /**
     * Get keyword suggestions for Python AI improvement
     * 
     * Analyzes frequent corrections to suggest new keywords
     */
    public List<Map<String, Object>> getKeywordSuggestions() {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        
        // Analyze corrections with count >= 5
        correctionStats.entrySet().stream()
            .filter(entry -> entry.getValue().count >= 5)
            .forEach(entry -> {
                CorrectionStats stats = entry.getValue();
                
                // Extract potential keywords from description
                String[] words = stats.description.toLowerCase().split("\\s+");
                List<String> potentialKeywords = Arrays.stream(words)
                    .filter(word -> word.length() >= 3)
                    .filter(word -> !isStopWord(word))
                    .collect(Collectors.toList());
                
                suggestions.add(Map.of(
                    "description_pattern", stats.description,
                    "correct_category", stats.userCorrectedCategory,
                    "correction_count", stats.count,
                    "suggested_keywords", potentialKeywords,
                    "action", "Add these keywords to category " + stats.userCorrectedCategory
                ));
            });
        
        return suggestions;
    }
    
    /**
     * Clear learning data for a user (GDPR compliance)
     */
    public void clearUserData(Long userId) {
        userPatterns.remove(userId);
        log.info("🗑️  Cleared learning data for user {}", userId);
    }
    
    // ===== Private helper methods =====
    
    private void updateUserPattern(Long userId, Long fromCategory, Long toCategory) {
        userPatterns.putIfAbsent(userId, new ConcurrentHashMap<>());
        Map<String, PatternInfo> patterns = userPatterns.get(userId);
        
        String patternKey = fromCategory.toString();
        
        if (patterns.containsKey(patternKey)) {
            PatternInfo existing = patterns.get(patternKey);
            if (existing.toCategoryId.equals(toCategory)) {
                existing.increment();
            } else {
                // User changed their pattern - replace
                patterns.put(patternKey, new PatternInfo(fromCategory, toCategory));
            }
        } else {
            patterns.put(patternKey, new PatternInfo(fromCategory, toCategory));
        }
    }
    
    private void updateCorrectionStats(String description, Long aiPredicted, Long userCorrected) {
        String normalizedDesc = description.toLowerCase().trim();
        String key = normalizedDesc + "|" + aiPredicted + "|" + userCorrected;
        
        if (correctionStats.containsKey(key)) {
            correctionStats.get(key).increment();
        } else {
            correctionStats.put(key, new CorrectionStats(normalizedDesc, aiPredicted, userCorrected));
        }
    }
    
    private void saveUserPreference(Long userId, String description, Long categoryId) {
        try {
            UserCategorizationPreference pref = new UserCategorizationPreference();
            pref.setUserId(userId);
            pref.setDescriptionPattern(description.toLowerCase());
            pref.setCategoryId(categoryId);
            pref.setFrequency(1);
            pref.setCreatedAt(LocalDateTime.now());
            
            userPrefRepository.save(pref);
        } catch (Exception e) {
            log.error("Failed to save user preference: {}", e.getMessage());
        }
    }
    
    private void checkForKeywordSuggestions(String description, Long aiPredicted, 
                                           Long userCorrected, int layer) {
        // Only suggest for Layer 0 (Python AI) failures
        if (layer != 0) return;
        
        String key = description.toLowerCase() + "|" + aiPredicted + "|" + userCorrected;
        CorrectionStats stats = correctionStats.get(key);
        
        if (stats != null && stats.count >= 3) {
            log.warn("💡 KEYWORD SUGGESTION: '{}' corrected {}x from {} to {}. " +
                    "Consider adding keywords to category {}",
                    description, stats.count, aiPredicted, userCorrected, userCorrected);
        }
    }
    
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "của", "và", "là", "có", "được", "không", "này", "đó", "với", "từ"
        );
        return stopWords.contains(word);
    }
}
