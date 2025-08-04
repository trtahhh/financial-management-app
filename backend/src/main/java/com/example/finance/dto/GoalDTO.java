package com.example.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GoalDTO {
    private Long id;
    private Long walletId;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate dueDate;
    private Long userId;
    private String status; 
    private LocalDateTime completedAt;
    private BigDecimal currentBalance; // Số dư hiện tại từ ví
    private Double progress; // Phần trăm tiến độ
}
