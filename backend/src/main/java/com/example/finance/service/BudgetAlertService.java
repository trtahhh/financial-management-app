package com.example.finance.service;

import com.example.finance.entity.Budget;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetAlertService {

 private final BudgetRepository budgetRepository;
 private final NotificationService notificationService;
 private final EmailService emailService;
 private final BudgetCalculationService budgetCalculationService;
 private final TransactionRepository transactionRepository;

 @Value("${email.verification.enabled:true}")
 private boolean emailVerificationEnabled;

 @Value("${notification.email.budget-alerts:true}")
 private boolean budgetAlertsEnabled;

 @Value("${notification.email.budget-warning-threshold:80}")
 private int budgetWarningThreshold;

 @Value("${notification.email.budget-exceeded-threshold:100}")
 private int budgetExceededThreshold;

 /**
 * Kiểm tra và tạo cảnh báo ngân sách khi có giao dịch mới
 */
 @Transactional
 public void checkBudgetAlert(Transaction transaction) {
 log.info(" BudgetAlertService.checkBudgetAlert called for transaction: ID={}, Type={}, Amount={}, Category={}", 
 transaction.getId(), transaction.getType(), transaction.getAmount(), 
 transaction.getCategory() != null ? transaction.getCategory().getName() : "NULL");
 
 if (!"expense".equals(transaction.getType())) {
 log.info(" Skipping budget check - not an expense transaction");
 return; // Chỉ check cho chi tiêu
 }

 LocalDate transDate = transaction.getDate();
 Optional<Budget> budgetOpt = budgetRepository.findByUserAndMonthAndCategoryAndIsDeletedFalse(
 transaction.getUser().getId(),
 transDate.getMonthValue(),
 transDate.getYear(),
 transaction.getCategory().getId()
 );

 if (budgetOpt.isEmpty()) {
 return; // Không có ngân sách cho category này
 }

 Budget budget = budgetOpt.get();
 
 // Tính tổng chi tiêu trong tháng cho category này
 BigDecimal totalSpent = budgetCalculationService.calculateSpentAmount(
 transaction.getUser().getId(),
 transaction.getCategory().getId(),
 transDate.getMonthValue(),
 transDate.getYear()
 );

 // Tính phần trăm sử dụng
 BigDecimal usagePercent = totalSpent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
 .multiply(BigDecimal.valueOf(100));

 log.info(" Budget check - Category: {}, Spent: {}, Budget: {}, Usage: {}%", 
 transaction.getCategory().getName(), totalSpent, budget.getAmount(), usagePercent);

 // Kiểm tra các mức cảnh báo dựa trên cấu hình
 if (usagePercent.compareTo(BigDecimal.valueOf(budgetExceededThreshold)) >= 0) {
 // Vượt ngân sách
 log.info(" Budget exceeded for category {}: {}% >= {}%", 
 transaction.getCategory().getName(), usagePercent, budgetExceededThreshold);
 createBudgetNotification(budget, totalSpent, usagePercent, "BUDGET_EXCEEDED");
 } else if (usagePercent.compareTo(BigDecimal.valueOf(budgetWarningThreshold)) >= 0) {
 // Gần đạt ngân sách
 log.info(" Budget warning for category {}: {}% >= {}%", 
 transaction.getCategory().getName(), usagePercent, budgetWarningThreshold);
 createBudgetNotification(budget, totalSpent, usagePercent, "BUDGET_WARNING");
 } else {
 log.info(" Budget usage normal for category {}: {}% < {}%", 
 transaction.getCategory().getName(), usagePercent, budgetWarningThreshold);
 }

 // Cập nhật spent amount trong budget
 budget.setSpentAmount(totalSpent);
 budgetRepository.save(budget);
 
 // Gửi email cảnh báo nếu được bật
 if (emailVerificationEnabled && budgetAlertsEnabled) {
 User user = transaction.getUser();
 log.info("📧 Budget alert email check - User: {}, Email: {}, EmailVerified: {}, BudgetAlertsEnabled: {}", 
 user.getUsername(), user.getEmail(), user.getEmailVerified(), budgetAlertsEnabled);
 
 // Gửi email ngay cả khi chưa verify (để test)
 if (user.getEmail() != null && !user.getEmail().isEmpty()) {
 try {
 emailService.sendBudgetAlertEmail(
 user.getEmail(),
 user.getUsername(),
 budget.getCategory().getName(),
 totalSpent.doubleValue(),
 budget.getAmount().doubleValue()
 );
 log.info("📧 Budget alert email sent to: {}", user.getEmail());
 } catch (Exception e) {
 log.error(" Failed to send budget alert email: {}", e.getMessage());
 }
 } else {
 log.warn(" User {} không có email để gửi thông báo", user.getUsername());
 }
 } else {
 log.info("📧 Budget alert email disabled - EmailVerification: {}, BudgetAlerts: {}", 
 emailVerificationEnabled, budgetAlertsEnabled);
 }
 }

 private void createBudgetNotification(Budget budget, BigDecimal totalSpent, BigDecimal usagePercent, String type) {
 String message;
 if ("BUDGET_EXCEEDED".equals(type)) {
 message = String.format(" Bạn đã vượt ngân sách %s! Đã chi %.0f%% (%.0f VND/%.0f VND)",
 budget.getCategory().getName(),
 usagePercent.doubleValue(),
 totalSpent.doubleValue(),
 budget.getAmount().doubleValue());
 } else {
 message = String.format(" Cảnh báo ngân sách %s: Đã sử dụng %.0f%% (%.0f VND/%.0f VND)",
 budget.getCategory().getName(),
 usagePercent.doubleValue(),
 totalSpent.doubleValue(),
 budget.getAmount().doubleValue());
 }

 // Kiểm tra xem đã có notification tương tự chưa
 boolean exists = notificationService.existsByBudgetAndType(
 budget.getId(), 
 type, 
 budget.getMonth(), 
 budget.getYear()
 );

 if (!exists) {
 notificationService.createBudgetNotification(
 budget.getUser().getId(),
 budget.getId(),
 budget.getCategory().getId(),
 message,
 type,
 budget.getMonth(),
 budget.getYear()
 );

 // Gửi email cảnh báo nếu được bật
 if (emailVerificationEnabled && budgetAlertsEnabled) {
 try {
 User user = budget.getUser();
 log.info("📧 Budget notification email check - User: {}, Email: {}, EmailVerified: {}, BudgetAlertsEnabled: {}", 
 user.getUsername(), user.getEmail(), user.getEmailVerified(), budgetAlertsEnabled);
 
 // Gửi email ngay cả khi chưa verify (để test)
 if (user.getEmail() != null && !user.getEmail().isEmpty()) {
 emailService.sendBudgetAlertEmail(
 user.getEmail(),
 user.getUsername(),
 budget.getCategory().getName(),
 totalSpent.doubleValue(),
 budget.getAmount().doubleValue()
 );
 log.info("📧 Budget notification email sent to: {}", user.getEmail());
 } else {
 log.warn(" User {} không có email để gửi thông báo", user.getUsername());
 }
 } catch (Exception e) {
 log.error(" Error sending budget notification email: {}", e.getMessage());
 // Không throw exception, chỉ log lỗi
 }
 } else {
 log.info("📧 Budget notification email disabled - EmailVerification: {}, BudgetAlerts: {}", 
 emailVerificationEnabled, budgetAlertsEnabled);
 }
 }
 }

 /**
 * Tính tổng chi tiêu cho category trong tháng cụ thể
 * KHÔNG sử dụng spent_amount từ budget để tránh bug cộng dồn
 */
 @SuppressWarnings("unused")
 private BigDecimal calculateTotalSpentForCategory(Long userId, Long categoryId, int month, int year) {
 YearMonth ym = YearMonth.of(year, month);
 var list = transactionRepository.findAllByDateBetween(ym.atDay(1), ym.atEndOfMonth());
 
 BigDecimal totalSpent = list.stream()
 .filter(t -> t.getUser() != null && t.getUser().getId().equals(userId))
 .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(categoryId))
 .filter(t -> "expense".equals(t.getType()))
 .map(t -> t.getAmount())
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 return totalSpent;
 }

}
