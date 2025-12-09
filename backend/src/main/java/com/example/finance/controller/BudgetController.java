package com.example.finance.controller;

import com.example.finance.security.CustomUserDetails;

import com.example.finance.dto.BudgetDTO;
import com.example.finance.service.BudgetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BudgetController {

 private final BudgetService service;

 @GetMapping
 public ResponseEntity<?> getAll() {
 try {
 Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
 CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
 Long userId = userDetails.getId();
 
 List<BudgetDTO> budgets = service.getAllBudgets(userId);
 log.info("Retrieved {} budgets for user {}", budgets.size(), userId);
 return ResponseEntity.ok(budgets);
 } catch (Exception e) {
 log.error("Error getting budgets", e);
 return ResponseEntity.badRequest()
 .body(Map.of("success", false, "message", "Lỗi lấy danh sách ngân sách: " + e.getMessage()));
 }
 }

 @GetMapping("/{id}")
 public ResponseEntity<?> getById(@PathVariable Long id) {
 try {
 BudgetDTO budget = service.getBudgetById(id);
 if (budget == null) {
 return ResponseEntity.badRequest()
 .body(Map.of("success", false, "message", "Không tìm thấy ngân sách"));
 }
 return ResponseEntity.ok(budget);
 } catch (Exception e) {
 log.error("Error getting budget by id: {}", id, e);
 return ResponseEntity.badRequest()
 .body(Map.of("success", false, "message", "Lỗi lấy thông tin ngân sách: " + e.getMessage()));
 }
 }

 @PostMapping
 public ResponseEntity<?> create(@RequestBody BudgetDTO dto) {
 try {
 Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
 CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
 Long userId = userDetails.getId();
 dto.setUserId(userId);

 log.info("Creating budget with data: {}", dto);
 BudgetDTO result = service.createBudget(dto);
 return ResponseEntity.ok(Map.of(
 "success", true,
 "message", "Tạo ngân sách thành công",
 "data", result
 ));
 } catch (Exception e) {
 log.error("Error creating budget", e);
 return ResponseEntity.badRequest()
 .body(Map.of("success", false, "message", "Lỗi tạo ngân sách: " + e.getMessage()));
 }
 }

 @PutMapping("/{id}")
 public ResponseEntity<?> update(@PathVariable Long id, @RequestBody BudgetDTO dto) {
 try {
 BudgetDTO result = service.updateBudget(id, dto);
 return ResponseEntity.ok(Map.of(
 "success", true,
 "message", "Cập nhật ngân sách thành công",
 "data", result
 ));
 } catch (Exception e) {
 log.error("Error updating budget: {}", id, e);
 return ResponseEntity.badRequest()
 .body(Map.of("success", false, "message", "Lỗi cập nhật ngân sách: " + e.getMessage()));
 }
 }

 @DeleteMapping("/{id}")
 public ResponseEntity<?> delete(@PathVariable Long id) {
 try {
 service.deleteBudget(id);
 return ResponseEntity.ok(Map.of(
 "success", true,
 "message", "Budget deleted successfully"
 ));
 } catch (RuntimeException e) {
 log.error("Error deleting budget: {}", id, e);
 if (e.getMessage().contains("not found")) {
 return ResponseEntity.status(404).body(Map.of(
 "success", false, 
 "message", "Budget not found with id: " + id
 ));
 }
 if (e.getMessage().contains("access denied")) {
 return ResponseEntity.status(403).body(Map.of(
 "success", false, 
 "message", "Access denied: Budget does not belong to current user"
 ));
 }
 return ResponseEntity.status(400).body(Map.of(
 "success", false, 
 "message", "Error deleting budget: " + e.getMessage()
 ));
 } catch (Exception e) {
 log.error("Error deleting budget: {}", id, e);
 return ResponseEntity.status(500).body(Map.of(
 "success", false, 
 "message", "Internal server error: " + e.getMessage()
 ));
 }
 }
 
 // ========================================
 // ULTRA AI ENDPOINTS FOR BUDGETS
 // ========================================
 
 /**
  * 🚀 Ultra AI: Get smart budget insights
  * Uses 9 ML libraries: XGBoost, LightGBM, Prophet, SHAP, Optuna, SMOTE, VADER, TextBlob, Word2Vec
  */
 @GetMapping("/ultra-insights")
 public ResponseEntity<?> getUltraAIInsights(
  @RequestParam(required = false) Integer month,
  @RequestParam(required = false) Integer year) {
  
  try {
   Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
   CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
   Long userId = userDetails.getId();
   
   // Default to current month/year
   if (month == null) month = java.time.LocalDate.now().getMonthValue();
   if (year == null) year = java.time.LocalDate.now().getYear();
   
   log.info("🚀 Getting Ultra AI budget insights for user {} (month: {}, year: {})", userId, month, year);
   
   Map<String, Object> insights = service.getUltraBudgetInsights(userId, month, year);
   
   return ResponseEntity.ok(Map.of(
    "success", true,
    "message", "Ultra AI insights generated",
    "data", insights,
    "aiLibraries", Map.of(
     "xgboost", true,
     "lightgbm", true,
     "prophet", true,
     "shap", true,
     "vader", true,
     "textblob", true,
     "word2vec", true
    )
   ));
   
  } catch (Exception e) {
   log.error("❌ Error getting Ultra AI insights", e);
   return ResponseEntity.status(500).body(Map.of(
    "success", false,
    "message", "Lỗi lấy thông tin AI: " + e.getMessage(),
    "fallback", "AI service unavailable"
   ));
  }
 }
 
 /**
  * 📈 Prophet Forecasting: Predict budget needs for next month
  */
 @GetMapping("/forecast/{categoryName}")
 public ResponseEntity<?> forecastBudgetNeeds(
  @PathVariable String categoryName) {
  
  try {
   Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
   CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
   Long userId = userDetails.getId();
   
   log.info("📈 Forecasting budget for category: {}", categoryName);
   
   Map<String, Object> forecast = service.forecastBudgetNeeds(userId, categoryName);
   
   return ResponseEntity.ok(Map.of(
    "success", true,
    "message", "Forecast generated using Prophet ML",
    "data", forecast
   ));
   
  } catch (Exception e) {
   log.error("❌ Error forecasting budget", e);
   return ResponseEntity.status(500).body(Map.of(
    "success", false,
    "message", "Lỗi dự đoán ngân sách: " + e.getMessage()
   ));
  }
 }
 
 /**
  * 😊 Sentiment Analysis: Analyze spending sentiment
  */
 @GetMapping("/sentiment-analysis")
 public ResponseEntity<?> analyzeSpendingSentiment(
  @RequestParam(required = false) Integer month,
  @RequestParam(required = false) Integer year) {
  
  try {
   Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
   CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
   Long userId = userDetails.getId();
   
   // Default to current month/year
   if (month == null) month = java.time.LocalDate.now().getMonthValue();
   if (year == null) year = java.time.LocalDate.now().getYear();
   
   log.info("😊 Analyzing spending sentiment for user {}", userId);
   
   Map<String, Object> sentiment = service.analyzeSpendingSentiment(userId, month, year);
   
   return ResponseEntity.ok(Map.of(
    "success", true,
    "message", "Sentiment analysis completed (TextBlob + VADER)",
    "data", sentiment
   ));
   
  } catch (Exception e) {
   log.error("❌ Error analyzing sentiment", e);
   return ResponseEntity.status(500).body(Map.of(
    "success", false,
    "message", "Lỗi phân tích cảm xúc: " + e.getMessage()
   ));
  }
 }
}
