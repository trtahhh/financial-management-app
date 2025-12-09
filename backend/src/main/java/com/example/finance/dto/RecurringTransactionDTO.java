package com.example.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RecurringTransactionDTO {
    private Long id;
    private Long userId;
    private Long walletId;
    private Long categoryId;
    private BigDecimal amount;
    private String type; // expense, income
    private String note;
    private String frequency; // daily, weekly, monthly, yearly
    private Boolean isActive;
    
    // Read-only field - calculated automatically
    private LocalDate nextExecution;
}
