package com.example.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long walletId;
    private Long categoryId;
    private Long budgetId;
    private Long goalId;
    private Long transactionId;

    private String type;
    private String message;
    private Boolean isRead;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    private Integer month;
    private Integer year;
}
