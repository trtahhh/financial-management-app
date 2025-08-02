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
    private String currencyCode;
    private Boolean isDeleted;
}
