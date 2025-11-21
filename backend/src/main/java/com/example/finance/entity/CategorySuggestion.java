package com.example.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "Category_Suggestions")
public class CategorySuggestion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    // Suggested category info
    @Column(name = "suggested_name", nullable = false)
    private String suggestedName;
    
    @Column(name = "suggested_type", nullable = false)
    private String suggestedType; // "income" or "expense"
    
    @Column(name = "suggested_color")
    private String suggestedColor = "#6c757d";
    
    @Column(name = "suggested_icon")
    private String suggestedIcon = "fa-tag";
    
    // AI reasoning
    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;
    
    @Column(name = "reasoning", length = 500)
    private String reasoning;
    
    @Column(name = "sample_descriptions", length = 1000)
    private String sampleDescriptions;
    
    @Column(name = "transaction_count")
    private Integer transactionCount = 1;
    
    // Approval status
    @Column(name = "status")
    private String status = "pending"; // pending, approved, rejected, merged
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "rejected_reason")
    private String rejectedReason;
    
    @Column(name = "created_category_id")
    private Long createdCategoryId;
    
    @Column(name = "merged_with_category_id")
    private Long mergedWithCategoryId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
