package com.example.finance.service;

import com.example.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetCalculationService {
 
 private final TransactionRepository transactionRepository;
 
 /**
 * Tính tổng chi tiêu cho category trong tháng cụ thể
 */
 public BigDecimal calculateSpentAmount(Long userId, Long categoryId, int month, int year) {
 YearMonth ym = YearMonth.of(year, month);
 var list = transactionRepository.findAllByDateBetween(ym.atDay(1), ym.atEndOfMonth());
 
 BigDecimal totalSpent = list.stream()
 .filter(t -> t.getUser() != null && t.getUser().getId().equals(userId))
 .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryId))
 .filter(t -> "expense".equals(t.getType()))
 .map(t -> t.getAmount())
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 log.debug(" Calculated spent amount for user {}, category {}, month {}/{}: {}", 
 userId, categoryId, month, year, totalSpent);
 
 return totalSpent;
 }
 
 /**
 * Tính phần trăm sử dụng ngân sách
 */
 public int calculateProgress(BigDecimal spentAmount, BigDecimal budgetAmount) {
 if (budgetAmount == null || budgetAmount.compareTo(BigDecimal.ZERO) <= 0) {
 return 0;
 }
 
 BigDecimal progress = spentAmount.divide(budgetAmount, 4, java.math.RoundingMode.HALF_UP)
 .multiply(BigDecimal.valueOf(100));
 
 return progress.intValue();
 }
}
