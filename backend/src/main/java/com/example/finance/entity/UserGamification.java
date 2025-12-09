package com.example.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_gamification")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGamification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(nullable = false)
    private Integer totalPoints = 0;
    
    @Column(nullable = false)
    private Integer level = 1;
    
    @Column(nullable = false)
    private Integer currentStreak = 0; // Consecutive days with transactions
    
    @Column(nullable = false)
    private Integer longestStreak = 0;
    
    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate; // Last day user logged a transaction
    
    @Column(nullable = false)
    private Integer transactionCount = 0;
    
    @Column(nullable = false)
    private Integer budgetCount = 0;
    
    @Column(nullable = false)
    private Integer goalCount = 0;
    
    @Column(nullable = false)
    private Double totalSavings = 0.0;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Calculate level based on total points
     * Level 1: 0-99 points
     * Level 2: 100-299 points
     * Level 3: 300-599 points
     * And so on...
     */
    public void calculateLevel() {
        if (totalPoints < 100) {
            level = 1;
        } else if (totalPoints < 300) {
            level = 2;
        } else if (totalPoints < 600) {
            level = 3;
        } else if (totalPoints < 1000) {
            level = 4;
        } else if (totalPoints < 1500) {
            level = 5;
        } else if (totalPoints < 2100) {
            level = 6;
        } else if (totalPoints < 2800) {
            level = 7;
        } else if (totalPoints < 3600) {
            level = 8;
        } else if (totalPoints < 4500) {
            level = 9;
        } else {
            level = 10;
        }
    }
    
    /**
     * Add points and recalculate level
     */
    public void addPoints(Integer points) {
        this.totalPoints += points;
        calculateLevel();
    }
    
    /**
     * Update streak based on activity date
     */
    public void updateStreak(LocalDate activityDate) {
        if (lastActivityDate == null) {
            currentStreak = 1;
        } else if (activityDate.equals(lastActivityDate)) {
            // Same day, no change
            return;
        } else if (activityDate.equals(lastActivityDate.plusDays(1))) {
            // Consecutive day
            currentStreak++;
        } else if (activityDate.isAfter(lastActivityDate.plusDays(1))) {
            // Streak broken
            currentStreak = 1;
        }
        
        lastActivityDate = activityDate;
        
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }
    }
}
