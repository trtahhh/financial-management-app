package com.example.finance.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
public class BudgetDTO {
    private Long id;
    private Long userId;
    private Long categoryId;
    private int month;
    private int year;
    private BigDecimal amount;
    private BigDecimal usedAmount; // Amount already spent
    private String currencyCode;
    private Boolean isDeleted;
    private Integer progress; // Progress percentage (0-100)
}
