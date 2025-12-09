package com.example.finance.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "Recurring_Transactions")
public class RecurringTransaction {
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name = "user_id", nullable = false)
 @JsonIgnore
 private User user;

 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name = "wallet_id", nullable = false)
 @JsonIgnore
 private Wallet wallet;

 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name = "category_id", nullable = false)
 @JsonIgnore
 private Category category;

 @Column(nullable = false, precision = 18, scale = 2)
 private BigDecimal amount;

 @Column(nullable = false)
 private String type; // expense, income

    private String note;

    @Column(nullable = false)
    private String frequency; // daily, weekly, monthly, yearly

    @Column(name = "next_execution", nullable = false)
    private LocalDate nextExecution;
    
    @Column(name = "is_active")
    private Boolean isActive = true;

 @Column(name = "created_at")
 private LocalDateTime createdAt = LocalDateTime.now();
}
