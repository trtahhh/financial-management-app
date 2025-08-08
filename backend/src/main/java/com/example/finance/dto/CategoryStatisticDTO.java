package com.example.finance.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class CategoryStatisticDTO {
    private String categoryName;
    private String categoryColor;
    private BigDecimal totalAmount;
    private Long transactionCount;

    // Constructor for JPQL: SELECT NEW CategoryStatisticDTO(c.name, c.color, SUM(t.amount), COUNT(t))
    public CategoryStatisticDTO(String categoryName, String categoryColor, BigDecimal totalAmount, Long transactionCount) {
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
    }
}
