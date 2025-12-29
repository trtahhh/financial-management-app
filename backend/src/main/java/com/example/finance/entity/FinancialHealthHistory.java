package com.example.finance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity lưu lịch sử điểm sức khỏe tài chính
 */
@Entity
@Table(name = "financial_health_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinancialHealthHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "score", nullable = false)
    private Integer score;
    
    @Column(name = "rating", length = 20)
    private String rating;
    
    @Column(name = "month", nullable = false)
    private Integer month;
    
    @Column(name = "year", nullable = false)
    private Integer year;
    
    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (recordDate == null) {
            recordDate = LocalDate.now();
        }
    }
}
