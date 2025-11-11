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
 private String type;
 private String note;
 private String frequency;
 private LocalDate startDate;
 private LocalDate endDate;
 private LocalDate nextExecution;
 private Boolean isActive;
}
