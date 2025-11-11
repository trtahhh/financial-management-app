package com.example.finance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import com.example.finance.service.AICategorizationService;
import com.example.finance.service.SmartBudgetService;
import com.example.finance.service.OverspendingDetectionService;
import com.example.finance.service.LongTermPlanningService;
import com.example.finance.service.SavingsKnowledgeBase;
import com.example.finance.entity.User;
import com.example.finance.entity.Transaction;
import com.example.finance.security.CustomUserDetails;
import com.example.finance.repository.TransactionRepository;

import java.util.*;

/**
 * AI Controller - Local AI Processing với MoMo-like features
 * (1) Auto-categorize real-time
 * (2) Overspending detection & alerts
 * (3) Long-term financial planning (3/6/12 months)
 * (4) Smart savings recommendations từ knowledge base
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIController {

    @Autowired
    private AICategorizationService aiCategorizationService;
    
    @Autowired
    private SmartBudgetService smartBudgetService;
    
    @Autowired
    private OverspendingDetectionService overspendingDetectionService;
    
    @Autowired
    private LongTermPlanningService longTermPlanningService;
    
    @Autowired
    private SavingsKnowledgeBase savingsKnowledgeBase;
    
    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Categorize expense using AI
     */
    @PostMapping("/categorize")
    public ResponseEntity<?> categorizeExpense(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        try {
            String description = (String) request.get("description");
            Double amount = request.get("amount") != null ? 
                Double.valueOf(request.get("amount").toString()) : null;
            
            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Description is required")
                );
            }
            
            var result = aiCategorizationService.categorizeExpense(description, amount);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "category", result.getCategory(),
                "categoryName", result.getCategoryName(),
                "confidence", result.getConfidence(),
                "suggestions", result.getSuggestions(),
                "reasoning", result.getReasoning()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Categorization failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * Generate spending insights
     */
    @GetMapping("/insights")
    public ResponseEntity<?> getSpendingInsights(
            @RequestParam(defaultValue = "month") String timeframe,
            Authentication authentication) {
        
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            var insights = aiCategorizationService.generateSpendingInsights(userId, timeframe);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "insights", insights.getInsights(),
                "recommendations", insights.getRecommendations(),
                "trends", insights.getTrends(),
                "financialHealthScore", insights.getScore()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to generate insights: " + e.getMessage())
            );
        }
    }
    
    /**
     * Get personalized tips
     */
    @GetMapping("/tips")
    public ResponseEntity<?> getPersonalizedTips(Authentication authentication) {
        
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            var tips = aiCategorizationService.generatePersonalizedTips(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "tips", tips
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to generate tips: " + e.getMessage())
            );
        }
    }
    
    /**
     * Process voice input
     */
    @PostMapping("/voice/process")
    public ResponseEntity<?> processVoiceInput(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        try {
            String transcript = request.get("transcript");
            
            if (transcript == null || transcript.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Transcript is required")
                );
            }
            
            var result = aiCategorizationService.processVoiceInput(transcript);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "amount", result.getAmount(),
                "category", result.getCategory(),
                "description", result.getDescription(),
                "confidence", result.getConfidence(),
                "suggestions", result.getSuggestions(),
                "rawTranscript", result.getRawTranscript()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Voice processing failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * Smart budget generation
     */
    @PostMapping("/budget/generate")
    public ResponseEntity<?> generateSmartBudget(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            Double monthlyIncome = request.get("monthlyIncome") != null ? 
                Double.valueOf(request.get("monthlyIncome").toString()) : null;
            String budgetType = (String) request.getOrDefault("budgetType", "balanced");
            
            if (monthlyIncome == null || monthlyIncome <= 0) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Valid monthly income is required")
                );
            }
            
            var recommendation = smartBudgetService.generateSmartBudget(
                userId, monthlyIncome, budgetType);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "allocations", recommendation.getAllocations(),
                "insights", recommendation.getInsights(),
                "recommendations", recommendation.getRecommendations(),
                "optimizationTips", recommendation.getTips(),
                "healthScore", recommendation.getHealthScore(),
                "spendingPattern", recommendation.getPattern()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Smart budget generation failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * Budget performance analysis
     */
    @GetMapping("/budget/performance")
    public ResponseEntity<?> analyzeBudgetPerformance(
            @RequestParam(defaultValue = "month") String period,
            Authentication authentication) {
        
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            var analysis = smartBudgetService.analyzeBudgetPerformance(userId, period);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "metrics", analysis.getMetrics(),
                "achievements", analysis.getAchievements(),
                "warnings", analysis.getWarnings(),
                "suggestions", analysis.getSuggestions(),
                "trends", analysis.getTrends(),
                "forecast", analysis.getForecast()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Budget analysis failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * Budget optimization
     */
    @PostMapping("/budget/optimize")
    public ResponseEntity<?> optimizeBudget(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            Double targetSavings = request.get("targetSavings") != null ? 
                Double.valueOf(request.get("targetSavings").toString()) : null;
            
            if (targetSavings == null || targetSavings <= 0) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Valid target savings amount is required")
                );
            }
            
            var optimization = smartBudgetService.optimizeBudget(userId, targetSavings);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "opportunities", optimization.getOpportunities(),
                "plan", optimization.getPlan(),
                "impact", optimization.getImpact(),
                "targetSavings", optimization.getTargetSavings()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Budget optimization failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * Student budget guide
     */
    @PostMapping("/budget/student-guide")
    public ResponseEntity<?> generateStudentBudgetGuide(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        try {
            Double monthlyIncome = request.get("monthlyIncome") != null ? 
                Double.valueOf(request.get("monthlyIncome").toString()) : null;
            String studentLevel = (String) request.getOrDefault("studentLevel", "university");
            
            if (monthlyIncome == null || monthlyIncome <= 0) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Valid monthly income is required")
                );
            }
            
            var guide = smartBudgetService.generateStudentBudgetGuide(monthlyIncome, studentLevel);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "allocations", guide.getAllocations(),
                "ratios", guide.getRatios(),
                "tips", guide.getTips(),
                "emergencyGuide", guide.getEmergencyGuide(),
                "savingGoals", guide.getSavingGoals(),
                "seasonalMultiplier", guide.getSeasonalMultiplier()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Student budget guide failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * Spending alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<?> getSpendingAlerts(Authentication authentication) {
        
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            var alerts = smartBudgetService.analyzeSpendingAlerts(userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "alerts", alerts.getAlerts(),
                "warnings", alerts.getWarnings(),
                "info", alerts.getInfo(),
                "recommendations", alerts.getRecommendations(),
                "hasUrgentAlerts", !alerts.getAlerts().isEmpty(),
                "totalAlerts", alerts.getAlerts().size() + alerts.getWarnings().size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to get spending alerts: " + e.getMessage())
            );
        }
    }
    
    /**
     * Batch categorize multiple transactions
     */
    @PostMapping("/categorize/batch")
    public ResponseEntity<?> batchCategorizeTransactions(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) request.get("transactions");
            
            if (transactions == null || transactions.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Transactions list is required")
                );
            }
            
            List<Map<String, Object>> results = new ArrayList<>();
            
            for (Map<String, Object> transaction : transactions) {
                String description = (String) transaction.get("description");
                Double amount = transaction.get("amount") != null ? 
                    Double.valueOf(transaction.get("amount").toString()) : null;
                
                if (description != null && !description.trim().isEmpty()) {
                    var result = aiCategorizationService.categorizeExpense(description, amount);
                    
                    results.add(Map.of(
                        "originalIndex", transaction.get("index"),
                        "category", result.getCategory(),
                        "categoryName", result.getCategoryName(),
                        "confidence", result.getConfidence(),
                        "reasoning", result.getReasoning()
                    ));
                } else {
                    results.add(Map.of(
                        "originalIndex", transaction.get("index"),
                        "error", "Description is required"
                    ));
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "results", results,
                "processed", results.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Batch categorization failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * Get AI system status and health
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAISystemStatus() {
        
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("status", "active");
            status.put("version", "1.0.0");
            status.put("features", Arrays.asList(
                "expense_categorization",
                "spending_insights",
                "smart_budgeting",
                "voice_processing",
                "personalized_tips"
            ));
            status.put("supported_languages", Arrays.asList("vi", "en"));
            status.put("model_info", Map.of(
                "type", "local_neural_network",
                "categories", 8,
                "accuracy", 0.85
            ));
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "aiSystem", status
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to analyze spending alerts: " + e.getMessage())
            );
        }
    }
    
    // ===== NEW MOMO-LIKE FEATURES =====
    
    /**
     * 🆕 AUTO-CATEGORIZE REAL-TIME (like MoMo)
     * Tự động phân loại khi tạo transaction - trả về suggestion với confidence
     */
    @PostMapping("/auto-categorize")
    public ResponseEntity<?> autoCategorizeTransaction(@RequestBody Map<String, Object> request) {
        try {
            String description = (String) request.get("description");
            Double amount = request.get("amount") != null ? 
                Double.valueOf(request.get("amount").toString()) : null;
            
            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Description is required")
                );
            }
            
            var result = aiCategorizationService.categorizeExpense(description, amount);
            
            // Format like MoMo UI
            return ResponseEntity.ok(Map.of(
                "success", true,
                "autoCategor categorized", true,
                "primaryCategory", Map.of(
                    "id", result.getCategory(),
                    "name", result.getCategoryName(),
                    "confidence", result.getConfidence(),
                    "icon", getCategoryIcon(result.getCategory())
                ),
                "suggestions", result.getSuggestions(),
                "reasoning", result.getReasoning(),
                "message", String.format("Tự động phân loại: %s (%.0f%% chắc chắn)", 
                    result.getCategoryName(), result.getConfidence())
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Auto-categorization failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * 🆕 OVERSPENDING DETECTION (real-time alert)
     * Phát hiện chi tiêu quá đà ngay khi tạo transaction
     */
    @PostMapping("/detect-overspending")
    public ResponseEntity<?> detectOverspending(
            @RequestParam Long transactionId,
            Authentication authentication) {
        try {
            Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
            
            var alert = overspendingDetectionService.detectOverspending(transaction);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "hasAlert", !alert.getSeverity().equals("none"),
                "alert", alert
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Overspending detection failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * 🆕 GET ALL OVERSPENDING ALERTS (dashboard)
     * Lấy tất cả cảnh báo chi tiêu quá đà
     */
    @GetMapping("/overspending-alerts")
    public ResponseEntity<?> getOverspendingAlerts(Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            var alerts = overspendingDetectionService.getAllUserAlerts(userId);
            
            // Count by severity
            long critical = alerts.stream().filter(a -> a.getSeverity().equals("critical")).count();
            long warning = alerts.stream().filter(a -> a.getSeverity().equals("warning")).count();
            long info = alerts.stream().filter(a -> a.getSeverity().equals("info")).count();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "totalAlerts", alerts.size(),
                "summary", Map.of(
                    "critical", critical,
                    "warning", warning,
                    "info", info
                ),
                "alerts", alerts
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to get alerts: " + e.getMessage())
            );
        }
    }
    
    /**
     * 🆕 LONG-TERM FINANCIAL PLAN (3/6/12 months)
     * Tạo kế hoạch tài chính dài hạn dựa trên thu nhập
     */
    @PostMapping("/long-term-plan")
    public ResponseEntity<?> createLongTermPlan(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            int planMonths = Integer.parseInt(request.get("months").toString());
            double targetSavings = Double.parseDouble(request.get("targetSavings").toString());
            
            var plan = longTermPlanningService.createLongTermPlan(userId, planMonths, targetSavings);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "plan", plan
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to create plan: " + e.getMessage())
            );
        }
    }
    
    /**
     * 🆕 SAVINGS PATH SUGGESTIONS
     * Gợi ý nhiều lộ trình tiết kiệm (conservative/balanced/aggressive)
     */
    @PostMapping("/savings-path")
    public ResponseEntity<?> suggestSavingsPath(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long userId = userDetails.getId();
            
            double targetAmount = Double.parseDouble(request.get("targetAmount").toString());
            String purpose = (String) request.getOrDefault("purpose", "Mục tiêu tiết kiệm");
            
            var suggestion = longTermPlanningService.suggestSavingsPath(userId, targetAmount, purpose);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "suggestion", suggestion
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to suggest savings path: " + e.getMessage())
            );
        }
    }
    
    /**
     * 🆕 SAVINGS TIPS BY CATEGORY
     * Lấy tips tiết kiệm theo category từ knowledge base
     */
    @GetMapping("/savings-tips/{category}")
    public ResponseEntity<?> getSavingsTips(
            @PathVariable String category,
            @RequestParam(defaultValue = "50") double spendingLevel) {
        try {
            var tips = savingsKnowledgeBase.getTipsForCategory(category, spendingLevel);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "category", category,
                "spendingLevel", spendingLevel,
                "tips", tips,
                "totalTips", tips.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to get savings tips: " + e.getMessage())
            );
        }
    }
    
    /**
     * 🆕 EMERGENCY SAVINGS TIPS
     * Lấy tips khẩn cấp khi overspending nghiêm trọng
     */
    @GetMapping("/emergency-tips")
    public ResponseEntity<?> getEmergencyTips() {
        try {
            var tips = savingsKnowledgeBase.getEmergencyTips();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "tips", tips,
                "message", "Các giải pháp khẩn cấp để giảm chi tiêu nhanh"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to get emergency tips: " + e.getMessage())
            );
        }
    }
    
    /**
     * 🆕 GENERAL SAVINGS TIPS
     * Lấy tips chung áp dụng cho mọi người
     */
    @GetMapping("/general-tips")
    public ResponseEntity<?> getGeneralTips(@RequestParam(defaultValue = "5") int count) {
        try {
            var tips = savingsKnowledgeBase.getGeneralTips(count);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "tips", tips
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to get general tips: " + e.getMessage())
            );
        }
    }
    
    /**
     * 🆕 SEARCH SAVINGS TIPS
     * Tìm kiếm tips theo keyword
     */
    @GetMapping("/search-tips")
    public ResponseEntity<?> searchSavingsTips(@RequestParam String keyword) {
        try {
            var tips = savingsKnowledgeBase.searchTips(keyword);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "keyword", keyword,
                "tips", tips,
                "resultsCount", tips.size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Search failed: " + e.getMessage())
            );
        }
    }
    
    // Helper method
    private String getCategoryIcon(String category) {
        Map<String, String> icons = new HashMap<>();
        icons.put("food", "🍔");
        icons.put("transport", "🚗");
        icons.put("shopping", "🛒");
        icons.put("education", "📚");
        icons.put("entertainment", "🎮");
        icons.put("health", "🏥");
        icons.put("bills", "📄");
        icons.put("other", "💼");
        return icons.getOrDefault(category.toLowerCase(), "💼");
    }
}

