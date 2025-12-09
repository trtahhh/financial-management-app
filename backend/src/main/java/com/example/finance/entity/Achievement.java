package com.example.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "achievements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String code; // FIRST_TRANSACTION, BUDGET_MASTER, etc.
    
    @Column(nullable = false)
    private String name; // "Giao dịch đầu tiên"
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String icon; // emoji or icon class
    
    @Column(nullable = false)
    private String category; // transaction, budget, saving, streak, goal
    
    @Column(nullable = false)
    private Integer points; // Points awarded for this achievement
    
    @Column(nullable = false)
    private String tier; // bronze, silver, gold, platinum, diamond
    
    @Column(columnDefinition = "TEXT")
    private String criteria; // JSON string describing unlock criteria
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
