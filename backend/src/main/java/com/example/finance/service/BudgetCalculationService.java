package com.example.finance.service;

import com.example.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 
 log.info("Calculating spent amount for budget: categoryId={}, month={}, year={}", categoryId, month, year);
 log.info("Fetching transactions for period {}/{}, found {} transactions", month, year, list.size());
 
 // Tính tổng chi tiêu cho category cụ thể
 BigDecimal totalSpent = list.stream()
 .filter(t -> {
 boolean userMatch = t.getUser() != null && t.getUser().getId().equals(userId);
 boolean catMatch = t.getCategory() != null && t.getCategory().getId().equals(categoryId);
 boolean typeMatch = "expense".equals(t.getType());
 return userMatch && catMatch && typeMatch;
 })
 .map(t -> t.getAmount())
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 log.info("Spent amount calculated for categoryId={}: {}", categoryId, totalSpent);
 
 return totalSpent;
 }
 
 /**
 * Tính tổng chi tiêu của tất cả category cho user trong tháng (sử dụng cho hiển thị tổng quát)
 */
 public BigDecimal calculateTotalSpentAmount(Long userId, int month, int year) {
 YearMonth ym = YearMonth.of(year, month);
 var list = transactionRepository.findAllByDateBetween(ym.atDay(1), ym.atEndOfMonth());
 
 log.info("Calculating TOTAL spent amount for user={}, month={}/{}", userId, month, year);
 
 BigDecimal totalSpent = list.stream()
 .filter(t -> {
 boolean userMatch = t.getUser() != null && t.getUser().getId().equals(userId);
 boolean typeMatch = "expense".equals(t.getType());
 return userMatch && typeMatch;
 })
 .map(t -> t.getAmount())
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 log.info("Total spent amount for user={}, month={}/{}: {}", userId, month, year, totalSpent);
 
 return totalSpent;
 }
 
 /**
 * Tính tổng chi tiêu theo khoảng thời gian (overload method)
 */
 public BigDecimal calculateSpentAmountByDateRange(Long userId, Long categoryId, LocalDate startDate, LocalDate endDate) {
 var list = transactionRepository.findAllByDateBetween(startDate, endDate);
 
 log.info("Calculating spent amount for budget: categoryId={}, dateRange={} to {}", categoryId, startDate, endDate);
 log.info("Fetching transactions for period, found {} transactions", list.size());
 
 BigDecimal totalSpent = list.stream()
 .filter(t -> {
 boolean userMatch = t.getUser() != null && t.getUser().getId().equals(userId);
 boolean catMatch = t.getCategory() != null && t.getCategory().getId().equals(categoryId);
 boolean typeMatch = "expense".equals(t.getType());
 return userMatch && catMatch && typeMatch;
 })
 .map(t -> t.getAmount())
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 log.info("Spent amount calculated for categoryId={} in date range: {}", categoryId, totalSpent);
 
 return totalSpent;
 }
 
 /**
 * Tính tổng chi tiêu của TẤT CẢ category theo khoảng thời gian
 */
 public BigDecimal calculateTotalSpentAmountByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
 var list = transactionRepository.findAllByDateBetween(startDate, endDate);
 
 log.info("Calculating TOTAL spent amount for user={}, dateRange={} to {}", userId, startDate, endDate);
 log.info("Fetching transactions for period, found {} transactions", list.size());
 
 BigDecimal totalSpent = list.stream()
 .filter(t -> {
 boolean userMatch = t.getUser() != null && t.getUser().getId().equals(userId);
 boolean typeMatch = "expense".equals(t.getType());
 return userMatch && typeMatch;
 })
 .map(t -> t.getAmount())
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 log.info("Total spent amount for user={} in date range: {}", userId, totalSpent);
 
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
