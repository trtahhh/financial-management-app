package com.example.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_categorization_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCategorizationPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "description_pattern", nullable = false, length = 500)
    private String descriptionPattern;
    
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    
    @Column(name = "frequency", nullable = false)
    private Integer frequency = 1;
    
    @Column(name = "last_used", nullable = false)
    private LocalDateTime lastUsed;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUsed = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastUsed = LocalDateTime.now();
    }
}
