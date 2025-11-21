package com.example.finance.service;

import com.example.finance.dto.CategorySuggestionDTO;
import com.example.finance.entity.Category;
import com.example.finance.entity.CategorySuggestion;
import com.example.finance.repository.CategoryRepository;
import com.example.finance.repository.CategorySuggestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategorySuggestionService {
    
    @Autowired
    private CategorySuggestionRepository suggestionRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private FuzzyMatchingService fuzzyMatchingService;
    
    private static final BigDecimal MIN_CONFIDENCE_FOR_SUGGESTION = new BigDecimal("0.60");
    private static final int MIN_TRANSACTION_COUNT_FOR_AUTO_SUGGEST = 3;
    
    /**
     * Analyze if a transaction pattern warrants a new category suggestion
     */
    public Optional<CategorySuggestion> analyzeAndSuggest(
            String description, 
            Long userId, 
            String transactionType,
            Long currentCategoryId) {
        
        // Only suggest if transaction fell into "Other" category (14)
        if (currentCategoryId != 14L) {
            return Optional.empty();
        }
        
        String normalized = fuzzyMatchingService.fullNormalize(description);
        
        // Extract potential category name from description
        String suggestedName = extractCategoryName(normalized, transactionType);
        if (suggestedName == null) {
            return Optional.empty();
        }
        
        // Check if similar category already exists
        if (isSimilarCategoryExists(suggestedName, userId, transactionType)) {
            System.out.println("[SUGGEST] Similar category exists: " + suggestedName);
            return Optional.empty();
        }
        
        // Check if suggestion already exists
        Optional<CategorySuggestion> existingSuggestion = 
            suggestionRepository.findPendingSuggestionByName(userId, suggestedName);
        
        if (existingSuggestion.isPresent()) {
            // Update existing suggestion with new sample
            CategorySuggestion suggestion = existingSuggestion.get();
            updateSuggestionWithNewSample(suggestion, description);
            suggestionRepository.save(suggestion);
            System.out.println("[SUGGEST] Updated existing suggestion: " + suggestedName);
            return Optional.of(suggestion);
        }
        
        // Create new suggestion
        CategorySuggestion suggestion = createSuggestion(
            userId, suggestedName, transactionType, description, normalized
        );
        
        CategorySuggestion saved = suggestionRepository.save(suggestion);
        System.out.println("[SUGGEST] New category suggested: " + suggestedName + 
                          " (confidence: " + suggestion.getConfidenceScore() + ")");
        
        return Optional.of(saved);
    }
    
    /**
     * Extract potential category name from transaction description
     */
    private String extractCategoryName(String normalized, String type) {
        // Pattern detection cho expense categories
        if ("expense".equals(type)) {
            // Pet-related
            if (normalized.matches(".*(cho|meo|cat|dog|pet|thu cung).*")) {
                return "Thú cưng";
            }
            
            // Beauty/spa
            if (normalized.matches(".*(spa|nail|lam dep|cat toc|salon|beautify).*")) {
                return "Làm đẹp";
            }
            
            // Home repair/maintenance
            if (normalized.matches(".*(sua chua|bao tri|tho|renovation|repair).*")) {
                return "Sửa chữa nhà";
            }
            
            // Charity/donation
            if (normalized.matches(".*(tu thien|donation|quyen gop|charity).*")) {
                return "Từ thiện";
            }
            
            // Insurance
            if (normalized.matches(".*(bao hiem|insurance|premium).*")) {
                return "Bảo hiểm";
            }
            
            // Subscription services
            if (normalized.matches(".*(subscription|goi cuoc|monthly|annual).*")) {
                return "Dịch vụ đăng ký";
            }
            
            // Hobbies
            if (normalized.matches(".*(hobby|so thich|collection|fan).*")) {
                return "Sở thích";
            }
        }
        
        // Income categories
        if ("income".equals(type)) {
            // Rental income
            if (normalized.matches(".*(cho thue|rental|rent).*")) {
                return "Thu nhập cho thuê";
            }
            
            // Bonus/awards
            if (normalized.matches(".*(bonus|thuong|award|prize).*")) {
                return "Thưởng";
            }
            
            // Side hustle
            if (normalized.matches(".*(parttime|lam them|side).*")) {
                return "Thu nhập phụ";
            }
        }
        
        return null;
    }
    
    /**
     * Check if similar category already exists (fuzzy match)
     */
    private boolean isSimilarCategoryExists(String suggestedName, Long userId, String type) {
        // Get all categories (system + user custom)
        List<Category> allCategories = categoryRepository.findAll();
        
        String normalizedSuggestion = fuzzyMatchingService.fullNormalize(suggestedName);
        
        for (Category category : allCategories) {
            // Skip if different type
            if (!category.getType().equals(type)) {
                continue;
            }
            
            String normalizedCategoryName = fuzzyMatchingService.fullNormalize(category.getName());
            double similarity = fuzzyMatchingService.calculateSimilarity(
                normalizedSuggestion, normalizedCategoryName
            );
            
            // If 80%+ similar, consider it exists
            if (similarity >= 0.80) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Create new category suggestion
     */
    private CategorySuggestion createSuggestion(
            Long userId, 
            String name, 
            String type, 
            String description,
            String normalized) {
        
        CategorySuggestion suggestion = new CategorySuggestion();
        suggestion.setUserId(userId);
        suggestion.setSuggestedName(name);
        suggestion.setSuggestedType(type);
        suggestion.setSuggestedColor(selectColorByType(type));
        suggestion.setSuggestedIcon(selectIconByName(name));
        suggestion.setConfidenceScore(new BigDecimal("0.65")); // Initial confidence
        suggestion.setReasoning(generateReasoning(name, normalized));
        suggestion.setSampleDescriptions(description);
        suggestion.setTransactionCount(1);
        suggestion.setStatus("pending");
        
        return suggestion;
    }
    
    /**
     * Update existing suggestion with new sample transaction
     */
    private void updateSuggestionWithNewSample(CategorySuggestion suggestion, String newDescription) {
        // Increment transaction count
        suggestion.setTransactionCount(suggestion.getTransactionCount() + 1);
        
        // Add new sample (max 3 samples)
        String currentSamples = suggestion.getSampleDescriptions();
        List<String> samples = currentSamples != null ? 
            new ArrayList<>(Arrays.asList(currentSamples.split("\\|"))) : new ArrayList<>();
        
        if (!samples.contains(newDescription)) {
            samples.add(newDescription);
            if (samples.size() > 3) {
                samples = samples.subList(samples.size() - 3, samples.size());
            }
            suggestion.setSampleDescriptions(String.join("|", samples));
        }
        
        // Increase confidence based on frequency
        BigDecimal currentConfidence = suggestion.getConfidenceScore();
        BigDecimal boost = new BigDecimal("0.05");
        BigDecimal newConfidence = currentConfidence.add(boost);
        if (newConfidence.compareTo(BigDecimal.ONE) > 0) {
            newConfidence = BigDecimal.ONE;
        }
        suggestion.setConfidenceScore(newConfidence);
        
        suggestion.setUpdatedAt(LocalDateTime.now());
    }
    
    /**
     * Generate reasoning for why this category is suggested
     */
    private String generateReasoning(String categoryName, String normalized) {
        return String.format(
            "Phát hiện pattern '%s' xuất hiện nhiều lần trong các giao dịch. " +
            "AI đề xuất tạo category riêng để tracking tốt hơn.",
            categoryName
        );
    }
    
    /**
     * Select appropriate color based on category type
     */
    private String selectColorByType(String type) {
        return "expense".equals(type) ? "#dc3545" : "#28a745";
    }
    
    /**
     * Select appropriate icon based on category name
     */
    private String selectIconByName(String name) {
        String normalized = name.toLowerCase();
        
        if (normalized.contains("thu cung") || normalized.contains("pet")) return "fa-paw";
        if (normalized.contains("lam dep") || normalized.contains("beauty")) return "fa-spa";
        if (normalized.contains("sua chua") || normalized.contains("repair")) return "fa-tools";
        if (normalized.contains("tu thien") || normalized.contains("charity")) return "fa-heart";
        if (normalized.contains("bao hiem") || normalized.contains("insurance")) return "fa-shield-alt";
        if (normalized.contains("dang ky") || normalized.contains("subscription")) return "fa-sync";
        if (normalized.contains("so thich") || normalized.contains("hobby")) return "fa-star";
        if (normalized.contains("cho thue") || normalized.contains("rental")) return "fa-home";
        if (normalized.contains("thuong") || normalized.contains("bonus")) return "fa-gift";
        
        return "fa-tag";
    }
    
    /**
     * Get all pending suggestions for user
     */
    public List<CategorySuggestionDTO> getPendingSuggestions(Long userId) {
        List<CategorySuggestion> suggestions = 
            suggestionRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "pending");
        
        return suggestions.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Approve a suggestion and create actual category
     */
    @Transactional
    public Category approveSuggestion(Long suggestionId, Long userId) {
        CategorySuggestion suggestion = suggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new RuntimeException("Suggestion not found"));
        
        if (!suggestion.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (!"pending".equals(suggestion.getStatus())) {
            throw new RuntimeException("Suggestion already processed");
        }
        
        // Create new category
        Category category = new Category();
        category.setName(suggestion.getSuggestedName());
        category.setType(suggestion.getSuggestedType());
        category.setColor(suggestion.getSuggestedColor());
        category.setIcon(suggestion.getSuggestedIcon());
        category.setIsActive(true);
        // Note: Need to add user_id and is_custom fields to Category entity
        
        Category savedCategory = categoryRepository.save(category);
        
        // Update suggestion status
        suggestion.setStatus("approved");
        suggestion.setApprovedAt(LocalDateTime.now());
        suggestion.setCreatedCategoryId(savedCategory.getId());
        suggestionRepository.save(suggestion);
        
        System.out.println("[SUGGEST] Approved & created category: " + savedCategory.getName());
        
        return savedCategory;
    }
    
    /**
     * Reject a suggestion
     */
    @Transactional
    public void rejectSuggestion(Long suggestionId, Long userId, String reason) {
        CategorySuggestion suggestion = suggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new RuntimeException("Suggestion not found"));
        
        if (!suggestion.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        suggestion.setStatus("rejected");
        suggestion.setRejectedReason(reason);
        suggestion.setUpdatedAt(LocalDateTime.now());
        suggestionRepository.save(suggestion);
        
        System.out.println("[SUGGEST] Rejected suggestion: " + suggestion.getSuggestedName());
    }
    
    /**
     * Merge suggestion with existing category
     */
    @Transactional
    public void mergeSuggestion(Long suggestionId, Long userId, Long existingCategoryId) {
        CategorySuggestion suggestion = suggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new RuntimeException("Suggestion not found"));
        
        if (!suggestion.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        suggestion.setStatus("merged");
        suggestion.setMergedWithCategoryId(existingCategoryId);
        suggestion.setUpdatedAt(LocalDateTime.now());
        suggestionRepository.save(suggestion);
        
        System.out.println("[SUGGEST] Merged suggestion with category: " + existingCategoryId);
    }
    
    private CategorySuggestionDTO convertToDTO(CategorySuggestion entity) {
        CategorySuggestionDTO dto = new CategorySuggestionDTO();
        dto.setId(entity.getId());
        dto.setSuggestedName(entity.getSuggestedName());
        dto.setSuggestedType(entity.getSuggestedType());
        dto.setSuggestedColor(entity.getSuggestedColor());
        dto.setSuggestedIcon(entity.getSuggestedIcon());
        dto.setConfidenceScore(entity.getConfidenceScore());
        dto.setReasoning(entity.getReasoning());
        
        if (entity.getSampleDescriptions() != null) {
            dto.setSampleDescriptions(Arrays.asList(entity.getSampleDescriptions().split("\\|")));
        }
        
        dto.setTransactionCount(entity.getTransactionCount());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        
        return dto;
    }
}
