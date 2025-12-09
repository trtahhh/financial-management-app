package com.example.finance.repository;

import com.example.finance.entity.CategorySuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorySuggestionRepository extends JpaRepository<CategorySuggestion, Long> {
    
    // Get all pending suggestions for a user
    List<CategorySuggestion> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    
    // Get all suggestions for a user (all statuses)
    List<CategorySuggestion> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Check if similar suggestion already exists
    @Query("SELECT cs FROM CategorySuggestion cs WHERE cs.userId = :userId " +
           "AND LOWER(cs.suggestedName) = LOWER(:name) " +
           "AND cs.status = 'pending'")
    Optional<CategorySuggestion> findPendingSuggestionByName(
        @Param("userId") Long userId, 
        @Param("name") String name
    );
    
    // Get suggestions by confidence score threshold
    @Query("SELECT cs FROM CategorySuggestion cs WHERE cs.userId = :userId " +
           "AND cs.status = 'pending' " +
           "AND cs.confidenceScore >= :minConfidence " +
           "ORDER BY cs.confidenceScore DESC, cs.transactionCount DESC")
    List<CategorySuggestion> findHighConfidenceSuggestions(
        @Param("userId") Long userId,
        @Param("minConfidence") java.math.BigDecimal minConfidence
    );
    
    // Count pending suggestions for user
    long countByUserIdAndStatus(Long userId, String status);
}
