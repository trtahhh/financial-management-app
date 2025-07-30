package com.example.finance.dto;

import lombok.Data;

@Data
public class CategoryStatisticDTO {
    private Long categoryId;
    private String categoryName;
    private Double totalIncome;
    private Double totalExpense;
    private Double balance;
}
