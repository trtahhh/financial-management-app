package com.example.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionDTO {
    private Long    id;
    private Long    userId;
    private Long    walletId;
    private Long    categoryId;
    private CategoryDTO category; // Add category object
    private WalletDTO wallet; // Add wallet object  
    private BigDecimal amount;
    private String  type;
    private String  note;
    private LocalDate   date;
    private String  filePath;
    private String  status;
    private String  tags;
    private Boolean isDeleted;
    private LocalDate deletedAt;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
