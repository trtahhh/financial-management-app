package com.example.finance.controller;

import com.example.finance.service.DashboardService;
import com.example.finance.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
@RequiredArgsConstructor
public class DashboardController {

 private final DashboardService dashboardService;

 /**
 * Lấy toàn bộ dữ liệu dashboard
 */
 @GetMapping("/user/{userId}")
 // @Cacheable(value = "dashboard", key = "#userId + '_' + #month + '_' + #year") // Disabled for testing
 public ResponseEntity<Map<String, Object>> getDashboard(
 @PathVariable Long userId,
 @RequestParam(required = false) Integer month,
 @RequestParam(required = false) Integer year) {
 
 LocalDate now = LocalDate.now();
 if (month == null) month = now.getMonthValue();
 if (year == null) year = now.getYear();
 
 // Mặc định lấy từ đầu tháng đến ngày hôm nay
 LocalDate startDate = LocalDate.of(year, month, 1);
 LocalDate endDate = now; // Ngày hôm nay
 
 System.out.println("=== DASHBOARD REQUEST ===");
 System.out.println("User ID: " + userId + ", Month: " + month + ", Year: " + year);
 System.out.println("Date range: " + startDate + " to " + endDate);
 
 // Sử dụng getDashboardDataByDate để lấy dữ liệu từ đầu tháng đến hôm nay
 Map<String, Object> dashboard = dashboardService.getDashboardDataByDate(userId, startDate, endDate);
 System.out.println("Dashboard totalBalance: " + dashboard.get("totalBalance"));
 
 return ResponseEntity.ok(dashboard);
 }

 /**
 * API riêng cho mobile hoặc lightweight requests
 */
 @GetMapping("/stats/{userId}")
 public ResponseEntity<Map<String, Object>> getBasicStats(
 @PathVariable Long userId,
 @RequestParam(required = false) Integer month,
 @RequestParam(required = false) Integer year) {
 
 LocalDate now = LocalDate.now();
 if (month == null) month = now.getMonthValue();
 if (year == null) year = now.getYear();
 
 // Mặc định lấy từ đầu tháng đến ngày hôm nay
 LocalDate startDate = LocalDate.of(year, month, 1);
 LocalDate endDate = now; // Ngày hôm nay
 
 // Chỉ trả về stats cơ bản, không cache
 Map<String, Object> basicStats = dashboardService.getDashboardDataByDate(userId, startDate, endDate);
 return ResponseEntity.ok(basicStats);
 }

 /**
 * Endpoint mới để hỗ trợ frontend với month và year parameters
 */
 @GetMapping("/data")
 public ResponseEntity<Map<String, Object>> getDashboardData(
 @RequestParam(required = false) Integer month,
 @RequestParam(required = false) Integer year) {
 
 try {
 LocalDate now = LocalDate.now();
 if (month == null) month = now.getMonthValue();
 if (year == null) year = now.getYear();
 
 // Extract userId from JWT token
 Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
 CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
 Long userId = userDetails.getId();
 
 System.out.println("Dashboard request for userId: " + userId + ", month: " + month + ", year: " + year);
 
 // Simple response for testing
 Map<String, Object> dashboard = new java.util.HashMap<>();
 dashboard.put("userId", userId);
 dashboard.put("month", month);
 dashboard.put("year", year);
 dashboard.put("status", "success");
 
 // Try to get data safely
 try {
 // Mặc định lấy từ đầu tháng đến ngày hôm nay
 LocalDate startDate = LocalDate.of(year, month, 1);
 LocalDate endDate = now; // Ngày hôm nay
 
 Map<String, Object> fullDashboard = dashboardService.getDashboardDataByDate(userId, startDate, endDate);
 dashboard.putAll(fullDashboard);
 } catch (Exception e) {
 System.err.println("Error getting dashboard data: " + e.getMessage());
 e.printStackTrace();
 dashboard.put("error", e.getMessage());
 }
 
 return ResponseEntity.ok(dashboard);
 } catch (Exception e) {
 System.err.println("Error in dashboard controller: " + e.getMessage());
 e.printStackTrace();
 Map<String, Object> errorResponse = new java.util.HashMap<>();
 errorResponse.put("error", e.getMessage());
 return ResponseEntity.status(500).body(errorResponse);
 }
 }

 /**
 * Endpoint mới để hỗ trợ frontend với date range parameters
 */
 @GetMapping("/data-by-date")
 public ResponseEntity<Map<String, Object>> getDashboardDataByDate(
 @RequestParam Long userId,
 @RequestParam String dateFrom,
 @RequestParam String dateTo) {
 
 try {
 // Parse date strings to LocalDate
 LocalDate startDate = LocalDate.parse(dateFrom);
 LocalDate endDate = LocalDate.parse(dateTo);
 
 System.out.println("Dashboard data-by-date request for userId: " + userId + 
 ", from: " + startDate + ", to: " + endDate);
 
 // Lấy dữ liệu dashboard theo khoảng thời gian
 Map<String, Object> dashboard = dashboardService.getDashboardDataByDate(userId, startDate, endDate);
 
 System.out.println("Dashboard data-by-date response: " + dashboard.get("totalBalance"));
 
 return ResponseEntity.ok(dashboard);
 
 } catch (Exception e) {
 System.err.println("Error in getDashboardDataByDate: " + e.getMessage());
 e.printStackTrace();
 
 // Trả về response lỗi
 Map<String, Object> errorResponse = new HashMap<>();
 errorResponse.put("error", "Failed to get dashboard data");
 errorResponse.put("message", e.getMessage());
 errorResponse.put("totalIncome", 0);
 errorResponse.put("totalExpense", 0);
 errorResponse.put("totalBalance", 0);
 errorResponse.put("recentTransactions", new ArrayList<>());
 errorResponse.put("wallets", new ArrayList<>());
 errorResponse.put("expensesByCategory", new ArrayList<>());
 errorResponse.put("weeklyTrend", new ArrayList<>());
 errorResponse.put("budgetProgress", new ArrayList<>());
 errorResponse.put("budgetWarnings", new ArrayList<>());
 errorResponse.put("goalProgress", new ArrayList<>());
 errorResponse.put("activeGoalsCount", 0L);
 
 // Thêm thông tin ngân sách mặc định
 Map<String, Object> totalBudgetInfo = new HashMap<>();
 totalBudgetInfo.put("totalBudgetAmount", 0);
 totalBudgetInfo.put("totalBudgetSpent", 0);
 totalBudgetInfo.put("budgetUsagePercent", 0.0);
 errorResponse.put("totalBudgetInfo", totalBudgetInfo);
 
 return ResponseEntity.status(500).body(errorResponse);
 }
 }
}
