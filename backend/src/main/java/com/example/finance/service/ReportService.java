package com.example.finance.service;

import com.example.finance.entity.*;
import com.example.finance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

 private final TransactionRepository transactionRepository;
 private final BudgetRepository budgetRepository;
 private final UserRepository userRepository;

 public Long getUserIdByUsername(String username) {
 return userRepository.findByUsername(username)
 .orElseThrow(() -> new RuntimeException("User not found"))
 .getId();
 }

 public Map<String, Object> generateSummaryReport(Long userId, LocalDate fromDate, LocalDate toDate) {
 Map<String, Object> report = new HashMap<>();
 
 // Tổng thu chi
 BigDecimal totalIncome = transactionRepository.sumByUserIdAndTypeAndDateBetween(userId, "income", fromDate, toDate);
 BigDecimal totalExpense = transactionRepository.sumByUserIdAndTypeAndDateBetween(userId, "expense", fromDate, toDate);
 
 Map<String, String> period = new HashMap<>();
 period.put("from", fromDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
 period.put("to", toDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
 report.put("period", period);
 
 Map<String, Object> summary = new HashMap<>();
 summary.put("totalIncome", totalIncome != null ? totalIncome : BigDecimal.ZERO);
 summary.put("totalExpense", totalExpense != null ? totalExpense : BigDecimal.ZERO);
 summary.put("balance", (totalIncome != null ? totalIncome : BigDecimal.ZERO)
 .subtract(totalExpense != null ? totalExpense : BigDecimal.ZERO));
 report.put("summary", summary);
 
 // Chi tiêu theo danh mục
 List<Map<String, Object>> categoryStats = transactionRepository
 .findByUserIdAndTypeAndDateBetween(userId, "expense", fromDate, toDate)
 .stream()
 .collect(Collectors.groupingBy(t -> t.getCategory().getName()))
 .entrySet()
 .stream()
 .map(entry -> {
 BigDecimal total = entry.getValue().stream()
 .map(Transaction::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 Map<String, Object> stat = new HashMap<>();
 stat.put("category", entry.getKey());
 stat.put("amount", total);
 stat.put("percentage", totalExpense != null && totalExpense.compareTo(BigDecimal.ZERO) > 0 
 ? total.divide(totalExpense, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
 : BigDecimal.ZERO);
 return stat;
 })
 .sorted((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")))
 .collect(Collectors.toList());
 
 report.put("categoryStats", categoryStats);
 
 // Giao dịch gần đây
 List<Transaction> recentTransactions = transactionRepository
 .findByUserIdAndDateBetweenOrderByDateDesc(userId, fromDate, toDate);
 
 List<Map<String, Object>> recentTransactionsList = recentTransactions.stream()
 .map(t -> {
 Map<String, Object> transaction = new HashMap<>();
 transaction.put("date", t.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
 transaction.put("type", t.getType());
 transaction.put("category", t.getCategory().getName());
 transaction.put("amount", t.getAmount());
 transaction.put("note", t.getNote() != null ? t.getNote() : "");
 return transaction;
 })
 .limit(10)
 .collect(Collectors.toList());
 
 report.put("recentTransactions", recentTransactionsList);
 
 return report;
 }

 public Map<String, Object> generateTransactionReport(Long userId, LocalDate fromDate, LocalDate toDate, String type, Long categoryId) {
 Map<String, Object> report = new HashMap<>();
 
 List<Transaction> transactions;
 if (type != null && categoryId != null) {
 transactions = transactionRepository.findByUserIdAndTypeAndCategoryIdAndDateBetween(userId, type, categoryId, fromDate, toDate);
 } else if (type != null) {
 transactions = transactionRepository.findByUserIdAndTypeAndDateBetween(userId, type, fromDate, toDate);
 } else if (categoryId != null) {
 transactions = transactionRepository.findByUserIdAndCategoryIdAndDateBetween(userId, categoryId, fromDate, toDate);
 } else {
 transactions = transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, fromDate, toDate);
 }
 
 BigDecimal totalAmount = transactions.stream()
 .map(Transaction::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 Map<String, String> period = new HashMap<>();
 period.put("from", fromDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
 period.put("to", toDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
 report.put("period", period);
 
 Map<String, Object> filters = new HashMap<>();
 filters.put("type", type);
 filters.put("categoryId", categoryId);
 report.put("filters", filters);
 
 Map<String, Object> summary = new HashMap<>();
 summary.put("totalTransactions", transactions.size());
 summary.put("totalAmount", totalAmount);
 report.put("summary", summary);
 
 List<Map<String, Object>> transactionsList = transactions.stream()
 .map(t -> {
 Map<String, Object> transaction = new HashMap<>();
 transaction.put("id", t.getId());
 transaction.put("date", t.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
 transaction.put("type", t.getType());
 transaction.put("category", t.getCategory().getName());
 transaction.put("amount", t.getAmount());
 transaction.put("note", t.getNote() != null ? t.getNote() : "");
 transaction.put("wallet", t.getWallet() != null ? t.getWallet().getName() : "");
 return transaction;
 })
 .collect(Collectors.toList());
 
 report.put("transactions", transactionsList);
 
 return report;
 }

 public Map<String, Object> generateBudgetReport(Long userId, int month, int year) {
 Map<String, Object> report = new HashMap<>();
 
 List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYearAndIsDeletedFalse(userId, month, year);
 
 BigDecimal totalBudget = budgets.stream()
 .map(Budget::getAmount)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 BigDecimal totalSpent = budgets.stream()
 .map(b -> b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO)
 .reduce(BigDecimal.ZERO, BigDecimal::add);
 
 Map<String, Object> period = new HashMap<>();
 period.put("month", month);
 period.put("year", year);
 report.put("period", period);
 
 Map<String, Object> summary = new HashMap<>();
 summary.put("totalBudget", totalBudget);
 summary.put("totalSpent", totalSpent);
 summary.put("remaining", totalBudget.subtract(totalSpent));
 summary.put("usagePercentage", totalBudget.compareTo(BigDecimal.ZERO) > 0 
 ? totalSpent.divide(totalBudget, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
 : BigDecimal.ZERO);
 report.put("summary", summary);
 
 List<Map<String, Object>> budgetsList = budgets.stream()
 .map(b -> {
 Map<String, Object> budget = new HashMap<>();
 budget.put("category", b.getCategory().getName());
 budget.put("budgetAmount", b.getAmount());
 budget.put("spentAmount", b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO);
 budget.put("remaining", b.getAmount().subtract(b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO));
 budget.put("usagePercentage", b.getAmount().compareTo(BigDecimal.ZERO) > 0 
 ? (b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO)
 .divide(b.getAmount(), 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
 : BigDecimal.ZERO);
 return budget;
 })
 .collect(Collectors.toList());
 
 report.put("budgets", budgetsList);
 
 return report;
 }

 public String generateTextReport(Long userId, String reportType, String dateFrom, String dateTo, Integer month, Integer year) {
 try {
 LocalDate fromDate = null;
 LocalDate toDate = null;
 
 // Xử lý thời gian
 if (dateFrom != null && dateTo != null) {
 try {
 fromDate = LocalDate.parse(dateFrom, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
 toDate = LocalDate.parse(dateTo, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
 } catch (Exception e) {
 // Fallback to current month if date parsing fails
 LocalDate now = LocalDate.now();
 fromDate = now.withDayOfMonth(1);
 toDate = now.withDayOfMonth(now.lengthOfMonth());
 }
 } else if (month != null && year != null) {
 fromDate = LocalDate.of(year, month, 1);
 toDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth());
 } else {
 // Default to current month
 LocalDate now = LocalDate.now();
 fromDate = now.withDayOfMonth(1);
 toDate = now.withDayOfMonth(now.lengthOfMonth());
 }
 
 StringBuilder report = new StringBuilder();
 
 switch (reportType.toLowerCase()) {
 case "summary":
 report.append(generateSummaryReportText(userId, fromDate, toDate));
 break;
 case "transactions":
 report.append(generateTransactionReportText(userId, fromDate, toDate));
 break;
 case "budgets":
 if (month != null && year != null) {
 report.append(generateBudgetReportText(userId, month, year));
 } else {
 report.append(" Báo cáo ngân sách cần chỉ định tháng và năm");
 }
 break;
 default:
 report.append(generateSummaryReportText(userId, fromDate, toDate));
 }
 
 return report.toString();
 
 } catch (Exception e) {
 log.error("Error generating text report", e);
 return " Lỗi khi tạo báo cáo: " + e.getMessage();
 }
 }

 private String generateSummaryReportText(Long userId, LocalDate fromDate, LocalDate toDate) {
 Map<String, Object> report = generateSummaryReport(userId, fromDate, toDate);
 
 StringBuilder text = new StringBuilder();
 text.append(" **BÁO CÁO TỔNG HỢP TÀI CHÍNH**\n");
 text.append("========================================\n\n");
 
 @SuppressWarnings("unchecked")
 Map<String, String> period = (Map<String, String>) report.get("period");
 text.append(" **Thời gian**: Từ ").append(period.get("from")).append(" đến ").append(period.get("to")).append("\n\n");
 
 @SuppressWarnings("unchecked")
 Map<String, Object> summary = (Map<String, Object>) report.get("summary");
 text.append(" **TỔNG KẾT**:\n");
 text.append("• Tổng thu: ").append(formatCurrency((BigDecimal) summary.get("totalIncome"))).append("\n");
 text.append("• Tổng chi: ").append(formatCurrency((BigDecimal) summary.get("totalExpense"))).append("\n");
 text.append("• Số dư: ").append(formatCurrency((BigDecimal) summary.get("balance"))).append("\n\n");
 
 @SuppressWarnings("unchecked")
 List<Map<String, Object>> categoryStats = (List<Map<String, Object>>) report.get("categoryStats");
 if (!categoryStats.isEmpty()) {
 text.append(" **CHI TIÊU THEO DANH MỤC**:\n");
 for (int i = 0; i < Math.min(categoryStats.size(), 10); i++) {
 Map<String, Object> stat = categoryStats.get(i);
 text.append(i + 1).append(". ").append(stat.get("category")).append(": ")
 .append(formatCurrency((BigDecimal) stat.get("amount")))
 .append(" (").append(formatPercentage((BigDecimal) stat.get("percentage"))).append(")\n");
 }
 text.append("\n");
 }
 
 @SuppressWarnings("unchecked")
 List<Map<String, Object>> recentTransactions = (List<Map<String, Object>>) report.get("recentTransactions");
 if (!recentTransactions.isEmpty()) {
 text.append(" **GIAO DỊCH GẦN ĐÂY**:\n");
 for (Map<String, Object> t : recentTransactions) {
 String emoji = "income".equals(t.get("type")) ? "📥" : "📤";
 text.append("• ").append(t.get("date")).append(" - ").append(emoji)
 .append(" ").append(t.get("type").equals("income") ? "Thu" : "Chi").append(": ")
 .append(formatCurrency((BigDecimal) t.get("amount"))).append(" - ")
 .append(t.get("category"));
 if (t.get("note") != null && !t.get("note").toString().isEmpty()) {
 text.append(" (").append(t.get("note")).append(")");
 }
 text.append("\n");
 }
 }
 
 return text.toString();
 }

 private String generateTransactionReportText(Long userId, LocalDate fromDate, LocalDate toDate) {
 Map<String, Object> report = generateTransactionReport(userId, fromDate, toDate, null, null);
 
 StringBuilder text = new StringBuilder();
 text.append(" **BÁO CÁO GIAO DỊCH**\n");
 text.append("==============================\n\n");
 
 @SuppressWarnings("unchecked")
 Map<String, String> period = (Map<String, String>) report.get("period");
 text.append(" **Thời gian**: Từ ").append(period.get("from")).append(" đến ").append(period.get("to")).append("\n\n");
 
 @SuppressWarnings("unchecked")
 Map<String, Object> summary = (Map<String, Object>) report.get("summary");
 text.append(" **TỔNG KẾT**:\n");
 text.append("• Tổng giao dịch: ").append(summary.get("totalTransactions")).append("\n");
 text.append("• Tổng số tiền: ").append(formatCurrency((BigDecimal) summary.get("totalAmount"))).append("\n\n");
 
 @SuppressWarnings("unchecked")
 List<Map<String, Object>> transactions = (List<Map<String, Object>>) report.get("transactions");
 if (!transactions.isEmpty()) {
 text.append(" **CHI TIẾT GIAO DỊCH**:\n");
 for (int i = 0; i < Math.min(transactions.size(), 20); i++) {
 Map<String, Object> t = transactions.get(i);
 String emoji = "income".equals(t.get("type")) ? "📥" : "📤";
 text.append(i + 1).append(". ").append(t.get("date")).append(" - ").append(emoji)
 .append(" ").append(t.get("type").equals("income") ? "Thu" : "Chi").append(" - ")
 .append(formatCurrency((BigDecimal) t.get("amount"))).append(" - ")
 .append(t.get("category"));
 if (t.get("note") != null && !t.get("note").toString().isEmpty()) {
 text.append(" (").append(t.get("note")).append(")");
 }
 text.append("\n");
 }
 }
 
 return text.toString();
 }

 private String generateBudgetReportText(Long userId, int month, int year) {
 Map<String, Object> report = generateBudgetReport(userId, month, year);
 
 StringBuilder text = new StringBuilder();
 text.append(" **BÁO CÁO NGÂN SÁCH**\n");
 text.append("==============================\n\n");
 
 @SuppressWarnings("unchecked")
 Map<String, Object> period = (Map<String, Object>) report.get("period");
 text.append(" **Thời gian**: ").append(period.get("month")).append("/").append(period.get("year")).append("\n\n");
 
 @SuppressWarnings("unchecked")
 Map<String, Object> summary = (Map<String, Object>) report.get("summary");
 text.append(" **TỔNG KẾT**:\n");
 text.append("• Tổng ngân sách: ").append(formatCurrency((BigDecimal) summary.get("totalBudget"))).append("\n");
 text.append("• Đã chi: ").append(formatCurrency((BigDecimal) summary.get("totalSpent"))).append("\n");
 text.append("• Còn lại: ").append(formatCurrency((BigDecimal) summary.get("remaining"))).append("\n");
 text.append("• Phần trăm sử dụng: ").append(formatPercentage((BigDecimal) summary.get("usagePercentage"))).append("\n\n");
 
 @SuppressWarnings("unchecked")
 List<Map<String, Object>> budgets = (List<Map<String, Object>>) report.get("budgets");
 if (!budgets.isEmpty()) {
 text.append(" **CHI TIẾT NGÂN SÁCH**:\n");
 for (int i = 0; i < budgets.size(); i++) {
 Map<String, Object> b = budgets.get(i);
 text.append(i + 1).append(". ").append(b.get("category")).append(":\n");
 text.append(" • Ngân sách: ").append(formatCurrency((BigDecimal) b.get("budgetAmount"))).append("\n");
 text.append(" • Đã chi: ").append(formatCurrency((BigDecimal) b.get("spentAmount"))).append("\n");
 text.append(" • Còn lại: ").append(formatCurrency((BigDecimal) b.get("remaining"))).append("\n");
 text.append(" • Sử dụng: ").append(formatPercentage((BigDecimal) b.get("usagePercentage"))).append("\n\n");
 }
 }
 
 return text.toString();
 }

 private String formatCurrency(BigDecimal amount) {
 if (amount == null) return "0 VND";
 return String.format("%,.0f VND", amount);
 }

 private String formatPercentage(BigDecimal percentage) {
 if (percentage == null) return "0%";
 return String.format("%.1f%%", percentage);
 }
}
