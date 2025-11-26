package com.example.finance.service;

import com.example.finance.dto.BudgetDTO;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Transaction;
import com.example.finance.mapper.BudgetMapper;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

 private final BudgetRepository budgetRepository;
 private final BudgetMapper budgetMapper;
 private final CategoryRepository categoryRepository;
 private final BudgetCalculationService budgetCalculationService;
 
 @Autowired
 private TransactionRepository transactionRepository;
 
 @Autowired(required = false)
 private RestTemplate restTemplate;
 
 // Ultra AI Service URL
 private static final String AI_SERVICE_URL = "http://localhost:8001";

 @Cacheable(value = "budgets", key = "#userId")
 public List<BudgetDTO> getAllBudgets(Long userId) {
 List<Budget> budgets = budgetRepository.findByUserIdAndIsDeletedFalse(userId);
 List<BudgetDTO> budgetDTOs = budgetMapper.toDTOs(budgets);
 
 // Calculate progress for each budget
 for (BudgetDTO budget : budgetDTOs) {
 BigDecimal usedAmount = budgetCalculationService.calculateSpentAmount(
 userId, budget.getCategoryId(), budget.getMonth(), budget.getYear());
 
 // Set spent amount
 budget.setSpentAmount(usedAmount);
 
 if (budget.getAmount() != null && budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
 BigDecimal progress = usedAmount.divide(budget.getAmount(), 4, RoundingMode.HALF_UP)
 .multiply(BigDecimal.valueOf(100));
 budget.setProgress(progress.intValue()); // Không giới hạn tối đa để hiển thị vượt ngân sách
 } else {
 budget.setProgress(0);
 }
 }
 
 return budgetDTOs;
 }

 /**
 * Clear cache cho user cụ thể khi có thay đổi transaction
 */
 @CacheEvict(value = "budgets", key = "#userId")
 public void clearBudgetCache(Long userId) {
 // Method này chỉ để clear cache, không cần logic gì
 }

 @Cacheable(value = "budgets", key = "#id")
 public BudgetDTO getBudgetById(Long id) {
 Budget budget = budgetRepository.findByIdAndIsDeletedFalse(id)
 .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + id));
 return budgetMapper.toDTO(budget);
 }

 @CacheEvict(value = "budgets", allEntries = true)
 public BudgetDTO createBudget(BudgetDTO dto) {
 if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
 throw new IllegalArgumentException("Budget amount must be greater than 0");
 }
 if (dto.getCategoryId() == null) {
 throw new IllegalArgumentException("Category is required");
 }
 if (dto.getMonth() <= 0 || dto.getMonth() > 12) {
 throw new IllegalArgumentException("Month must be between 1 and 12");
 }
 if (dto.getYear() <= 0) {
 throw new IllegalArgumentException("Year must be greater than 0");
 }
 if (dto.getUserId() == null) {
 throw new IllegalArgumentException("UserId is required");
 }

 Budget budget = budgetMapper.toEntity(dto);
 budget.setCreatedAt(LocalDateTime.now());
 budget.setIsDeleted(false);
 Budget saved = budgetRepository.save(budget);
 return budgetMapper.toDTO(saved);
 }

 @CacheEvict(value = "budgets", allEntries = true)
 public BudgetDTO updateBudget(Long id, BudgetDTO dto) {
 Budget budget = budgetRepository.findByIdAndIsDeletedFalse(id)
 .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + id));

 budget.setAmount(dto.getAmount());
 budget.setMonth(dto.getMonth());
 budget.setYear(dto.getYear());
 // Currency code removed from entity
 budget.setUpdatedAt(LocalDateTime.now());

 var category = categoryRepository.findById(dto.getCategoryId())
 .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
 budget.setCategory(category);

 Budget updated = budgetRepository.save(budget);
 return budgetMapper.toDTO(updated);
 }

 @CacheEvict(value = "budgets", allEntries = true)
 public void deleteBudget(Long id) {
 Budget budget = budgetRepository.findByIdAndIsDeletedFalse(id)
 .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + id));
 budget.setIsDeleted(true); // Fixed: should be true to mark as deleted
 budget.setDeletedAt(LocalDateTime.now());
 budgetRepository.save(budget);
 }

 // Thêm các method này vào BudgetService:

 /**
 * Lấy budget vs actual comparison
 */
 public List<Map<String, Object>> getBudgetVsActual(Long userId, Integer month, Integer year) {
 List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYearAndIsDeletedFalse(userId, month, year);
 
 return budgets.stream().map(budget -> {
 // Tính tổng chi tiêu thực tế
 BigDecimal actualSpent = budgetCalculationService.calculateSpentAmount(
 userId, budget.getCategory().getId(), month, year);
 
 if (actualSpent == null) actualSpent = BigDecimal.ZERO;
 
 // Tính phần trăm sử dụng
 BigDecimal usagePercent = budget.getAmount().compareTo(BigDecimal.ZERO) > 0 ? 
 actualSpent.divide(budget.getAmount(), 4, java.math.RoundingMode.HALF_UP)
 .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
 
 // Xác định status
 String status = "OK";
 if (usagePercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
 status = "EXCEEDED";
 } else if (usagePercent.compareTo(BigDecimal.valueOf(80)) >= 0) {
 status = "WARNING";
 }
 
 Map<String, Object> result = new HashMap<>();
 result.put("budgetId", budget.getId());
 result.put("categoryName", budget.getCategory().getName());
 result.put("categoryColor", budget.getCategory().getColor());
 result.put("budgetAmount", budget.getAmount());
 result.put("spentAmount", actualSpent);
 result.put("usagePercent", usagePercent.doubleValue());
 result.put("alertThreshold", 80.0);
 result.put("status", status);
 
 return result;
 }).toList();
 }

 /**
 * Lấy cảnh báo ngân sách
 */
 public List<Map<String, Object>> getBudgetWarnings(Long userId, Integer month, Integer year) {
 List<Map<String, Object>> budgetVsActual = getBudgetVsActual(userId, month, year);
 
 // Chỉ lấy những budget có warning hoặc exceeded
 // budgetVsActual là List<Map<String,Object>>, không cast về Budget
 return budgetVsActual.stream()
 .filter(item -> {
 String status = (String) item.get("status");
 return "WARNING".equals(status) || "EXCEEDED".equals(status);
 })
 .map(item -> {
 Map<String, Object> warning = new HashMap<>();
 warning.put("categoryId", item.get("categoryId"));
 warning.put("categoryName", item.get("categoryName"));
 warning.put("budgetAmount", item.get("budgetAmount"));
 warning.put("spentAmount", item.get("spentAmount"));
 warning.put("usagePercent", item.get("usagePercent"));
 warning.put("status", item.get("status"));
 warning.put("message", "EXCEEDED".equals(item.get("status")) ? "Đã vượt ngân sách!" : "Sắp vượt ngân sách!");
 return warning;
 })
 .toList();
 }

 /**
 * Đếm số budget đang hoạt động
 */
 public Long countActiveBudgets(Long userId, int month, int year) {
 return (long) budgetRepository.findByUserIdAndMonthAndYearAndIsDeletedFalse(userId, month, year).size();
 }

 /**
 * Lấy tiến độ ngân sách trong khoảng ngày (date range)
 */
 public List<Map<String, Object>> getBudgetVsActualByDate(Long userId, LocalDate startDate, LocalDate endDate) {
 int startMonth = startDate.getMonthValue();
 int startYear = startDate.getYear();
 int endMonth = endDate.getMonthValue();
 int endYear = endDate.getYear();
 List<Budget> budgets = budgetRepository.findByUserIdAndMonthYearRangeAndIsDeletedFalse(userId, startMonth, startYear, endMonth, endYear);
 
 return budgets.stream().map(budget -> {
 // Tính tổng chi tiêu thực tế cho đúng tháng của ngân sách này
 BigDecimal actualSpent = budgetCalculationService.calculateSpentAmount(
 userId, budget.getCategory().getId(), budget.getMonth(), budget.getYear());

 if (actualSpent == null) actualSpent = BigDecimal.ZERO;

 // Tính phần trăm sử dụng
 BigDecimal usagePercent = budget.getAmount().compareTo(BigDecimal.ZERO) > 0 ?
 actualSpent.divide(budget.getAmount(), 4, java.math.RoundingMode.HALF_UP)
 .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;

 // Xác định status
 String status = "OK";
 if (usagePercent.compareTo(BigDecimal.valueOf(100)) >= 0) {
 status = "EXCEEDED";
 } else if (usagePercent.compareTo(BigDecimal.valueOf(80)) >= 0) {
 status = "WARNING";
 }

 Map<String, Object> result = new HashMap<>();
 result.put("budgetId", budget.getId());
 // Truy cập an toàn thuộc tính category (đã JOIN FETCH nhưng vẫn phòng hờ)
 try {
 result.put("categoryName", budget.getCategory() != null ? budget.getCategory().getName() : "");
 result.put("categoryColor", budget.getCategory() != null ? budget.getCategory().getColor() : "#6c757d");
 } catch (Exception e) {
 result.put("categoryName", "");
 result.put("categoryColor", "#6c757d");
 }
 result.put("budgetAmount", budget.getAmount());
 result.put("spentAmount", actualSpent);
 result.put("actualAmount", actualSpent);
 result.put("remainingAmount", budget.getAmount().subtract(actualSpent));
 result.put("usagePercent", usagePercent.doubleValue());
 result.put("alertThreshold", 80.0);
 result.put("status", status);

 return result;
 }).toList();
 }
 
 /**
  * 🚀 ULTRA AI: Get smart budget insights using 9 ML libraries
  * Features: Ensemble predictions, Prophet forecasting, Sentiment analysis, SHAP explanations
  */
 public Map<String, Object> getUltraBudgetInsights(Long userId, Integer month, Integer year) {
  try {
   log.info("🚀 Generating Ultra AI budget insights for user {} (month: {}, year: {})", userId, month, year);
   
   // 1. Get all transactions for the period
   List<Transaction> transactions = getTransactionsForPeriod(userId, month, year);
   
   if (transactions.isEmpty()) {
    log.warn("No transactions found for Ultra AI analysis");
    return Map.of(
     "success", false,
     "message", "Không đủ dữ liệu để phân tích AI"
    );
   }
   
   // 2. Prepare transaction data for AI service
   List<Map<String, Object>> txnData = transactions.stream()
    .map(txn -> {
     Map<String, Object> data = new HashMap<>();
     data.put("category", txn.getCategory().getName());
     data.put("amount", txn.getAmount().doubleValue());
     data.put("description", txn.getNote());
     data.put("date", txn.getDate().format(DateTimeFormatter.ISO_DATE));
     return data;
    })
    .collect(Collectors.toList());
   
   // 3. Calculate monthly income (estimate from transactions)
   double monthlyIncome = estimateMonthlyIncome(userId);
   
   // 4. Call Ultra AI Service
   Map<String, Object> insights = callUltraAIService(txnData, monthlyIncome);
   
   // 5. Add budget-specific data
   List<Budget> budgets = budgetRepository.findByUserIdAndMonthAndYearAndIsDeletedFalse(userId, month, year);
   insights.put("budgetCount", budgets.size());
   insights.put("budgetSummary", getBudgetVsActual(userId, month, year));
   
   log.info("✅ Ultra AI insights generated successfully");
   return insights;
   
  } catch (Exception e) {
   log.error("❌ Error generating Ultra AI budget insights: {}", e.getMessage(), e);
   return Map.of(
    "success", false,
    "error", e.getMessage(),
    "fallback", "AI service unavailable, using basic insights"
   );
  }
 }
 
 /**
  * 🔮 Prophet Forecasting: Predict budget needs for next month
  */
 public Map<String, Object> forecastBudgetNeeds(Long userId, String categoryName) {
  try {
   log.info("📈 Forecasting budget needs for category: {}", categoryName);
   
   // Get historical transactions (last 3 months)
   List<Transaction> transactions = getTransactionsForCategory(userId, categoryName, 3);
   
   if (transactions.size() < 7) {
    return Map.of(
     "success", false,
     "message", "Cần ít nhất 7 giao dịch để dự đoán xu hướng"
    );
   }
   
   // Prepare data for Prophet
   List<Map<String, Object>> txnData = transactions.stream()
    .map(txn -> {
     Map<String, Object> data = new HashMap<>();
     data.put("category", txn.getCategory().getName());
     data.put("amount", txn.getAmount().doubleValue());
     data.put("date", txn.getDate().format(DateTimeFormatter.ISO_DATE));
     return data;
    })
    .collect(Collectors.toList());
   
   // Call Prophet forecast endpoint
   Map<String, Object> forecast = callProphetForecast(categoryName, txnData);
   
   log.info("✅ Prophet forecast completed");
   return forecast;
   
  } catch (Exception e) {
   log.error("❌ Error forecasting budget: {}", e.getMessage());
   return Map.of(
    "success", false,
    "error", e.getMessage()
   );
  }
 }
 
 /**
  * 😊 Sentiment Analysis: Analyze spending sentiment
  */
 public Map<String, Object> analyzeSpendingSentiment(Long userId, Integer month, Integer year) {
  try {
   log.info("😊 Analyzing spending sentiment for user {}", userId);
   
   List<Transaction> transactions = getTransactionsForPeriod(userId, month, year);
   
   if (transactions.isEmpty()) {
    return Map.of("success", false, "message", "No transactions to analyze");
   }
   
   // Get descriptions
   List<String> descriptions = transactions.stream()
    .map(Transaction::getNote)
    .filter(desc -> desc != null && !desc.isEmpty())
    .collect(Collectors.toList());
   
   if (descriptions.isEmpty()) {
    return Map.of("success", false, "message", "No descriptions found");
   }
   
   // Call sentiment analysis
   Map<String, Object> sentiment = callSentimentAnalysis(descriptions);
   
   // Add summary
   double avgSentiment = (double) sentiment.get("average_sentiment");
   String overallMood = avgSentiment > 0.2 ? "Tích cực 😊" : 
                         avgSentiment < -0.2 ? "Tiêu cực 😢" : "Trung tính 😐";
   
   sentiment.put("overallMood", overallMood);
   sentiment.put("interpretation", interpretSentiment(avgSentiment));
   
   log.info("✅ Sentiment analysis completed: {}", overallMood);
   return sentiment;
   
  } catch (Exception e) {
   log.error("❌ Error analyzing sentiment: {}", e.getMessage());
   return Map.of("success", false, "error", e.getMessage());
  }
 }
 
 // ============= Private Helper Methods =============
 
 private List<Transaction> getTransactionsForPeriod(Long userId, Integer month, Integer year) {
  LocalDate startDate = LocalDate.of(year, month, 1);
  LocalDate endDate = startDate.plusMonths(1).minusDays(1);
  return transactionRepository.findByUserIdAndTypeAndDateBetween(userId, "EXPENSE", startDate, endDate);
 }
 
 private List<Transaction> getTransactionsForCategory(Long userId, String categoryName, int months) {
  LocalDate endDate = LocalDate.now();
  LocalDate startDate = endDate.minusMonths(months);
  return transactionRepository.findByUserIdAndTypeAndDateBetween(userId, "EXPENSE", startDate, endDate)
   .stream()
   .filter(txn -> txn.getCategory() != null && txn.getCategory().getName().equalsIgnoreCase(categoryName))
   .collect(Collectors.toList());
 }
 
 private double estimateMonthlyIncome(Long userId) {
  // Simple estimation: average income transactions
  LocalDate endDate = LocalDate.now();
  LocalDate startDate = endDate.minusMonths(3);
  List<Transaction> incomeTransactions = transactionRepository.findByUserIdAndTypeAndDateBetween(userId, "INCOME", startDate, endDate);
  
  if (incomeTransactions.isEmpty()) {
   return 10000000.0; // Default 10M VND
  }
  
  double totalIncome = incomeTransactions.stream()
   .mapToDouble(txn -> txn.getAmount().doubleValue())
   .sum();
  
  return totalIncome / 3.0; // Average over 3 months
 }
 
 private Map<String, Object> callUltraAIService(List<Map<String, Object>> transactions, double monthlyIncome) {
  if (restTemplate == null) {
   log.warn("RestTemplate not available, returning mock data");
   return Map.of("success", false, "message", "AI service unavailable");
  }
  
  try {
   String url = AI_SERVICE_URL + "/ultra/generate-insights";
   
   Map<String, Object> request = new HashMap<>();
   request.put("transactions", transactions);
   request.put("monthly_income", monthlyIncome);
   request.put("enable_shap", true);
   request.put("enable_prophet", true);
   request.put("enable_optuna", false); // Slow, disable by default
   
   HttpHeaders headers = new HttpHeaders();
   headers.setContentType(MediaType.APPLICATION_JSON);
   
   HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
   @SuppressWarnings("rawtypes")
   ResponseEntity<Map> rawResponse = restTemplate.postForEntity(url, entity, Map.class);
   
   @SuppressWarnings("unchecked")
   Map<String, Object> result = rawResponse.getBody();
   return result;
   
  } catch (Exception e) {
   log.error("Error calling Ultra AI service: {}", e.getMessage());
   throw new RuntimeException("AI service call failed: " + e.getMessage());
  }
 }
 
 private Map<String, Object> callProphetForecast(String category, List<Map<String, Object>> transactions) {
  if (restTemplate == null) {
   return Map.of("success", false, "message", "AI service unavailable");
  }
  
  try {
   String url = AI_SERVICE_URL + "/ultra/prophet-forecast?category=" + category + "&periods_ahead=3";
   
   HttpHeaders headers = new HttpHeaders();
   headers.setContentType(MediaType.APPLICATION_JSON);
   
   HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(transactions, headers);
   @SuppressWarnings("rawtypes")
   ResponseEntity<Map> rawResponse = restTemplate.postForEntity(url, entity, Map.class);
   
   @SuppressWarnings("unchecked")
   Map<String, Object> result = rawResponse.getBody();
   return result;
   
  } catch (Exception e) {
   log.error("Error calling Prophet forecast: {}", e.getMessage());
   throw new RuntimeException("Prophet forecast failed: " + e.getMessage());
  }
 }
 
 private Map<String, Object> callSentimentAnalysis(List<String> texts) {
  if (restTemplate == null) {
   return Map.of("success", false, "message", "AI service unavailable");
  }
  
  try {
   String url = AI_SERVICE_URL + "/ultra/sentiment-analysis";
   
   Map<String, Object> request = Map.of("texts", texts);
   
   HttpHeaders headers = new HttpHeaders();
   headers.setContentType(MediaType.APPLICATION_JSON);
   
   HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
   @SuppressWarnings("rawtypes")
   ResponseEntity<Map> rawResponse = restTemplate.postForEntity(url, entity, Map.class);
   
   @SuppressWarnings("unchecked")
   Map<String, Object> result = rawResponse.getBody();
   return result;
   
  } catch (Exception e) {
   log.error("Error calling sentiment analysis: {}", e.getMessage());
   throw new RuntimeException("Sentiment analysis failed: " + e.getMessage());
  }
 }
 
 private String interpretSentiment(double sentiment) {
  if (sentiment > 0.5) {
   return "Chi tiêu chủ yếu cho những điều tích cực và vui vẻ";
  } else if (sentiment > 0.2) {
   return "Chi tiêu tương đối tích cực";
  } else if (sentiment > -0.2) {
   return "Chi tiêu trung tính, cân bằng giữa nhu cầu và mong muốn";
  } else if (sentiment > -0.5) {
   return "Chi tiêu chủ yếu cho các khoản bắt buộc hoặc không mong muốn";
  } else {
   return "Chi tiêu chủ yếu cho các khoản bắt buộc, áp lực tài chính cao";
  }
 }
}
