package com.example.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.Category;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class SmartBudgetService {
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    // Student financial health thresholds
    private final Map<String, Double> studentBudgetRatios = new HashMap<String, Double>() {{
        put("food", 0.25);          // 25% cho ăn uống
        put("transport", 0.10);     // 10% cho di chuyển
        put("education", 0.30);     // 30% cho giáo dục
        put("entertainment", 0.15); // 15% cho giải trí
        put("shopping", 0.10);      // 10% cho mua sắm
        put("health", 0.05);        // 5% cho sức khỏe
        put("bills", 0.05);         // 5% cho hóa đơn
    }};
    
    // Seasonal multipliers
    private final Map<Integer, Double> seasonalMultipliers = new HashMap<Integer, Double>() {{
        put(1, 1.2);    // Tết
        put(2, 1.1);    
        put(3, 1.0);    
        put(4, 1.0);    
        put(5, 1.0);    
        put(6, 1.0);    
        put(7, 1.1);    // Hè
        put(8, 1.1);    
        put(9, 1.2);    // Khai giảng
        put(10, 1.0);   
        put(11, 1.1);   
        put(12, 1.3);   // Cuối năm
    }};
    
    /**
     * Tạo ngân sách thông minh dựa trên thu nhập và thói quen chi tiêu
     */
    public SmartBudgetRecommendation generateSmartBudget(Long userId, Double monthlyIncome, 
                                                       String budgetType) {
        
        // Phân tích lịch sử chi tiêu
        List<Transaction> recentTransactions = getRecentTransactions(userId, 3); // 3 tháng gần nhất
        SpendingPattern pattern = analyzeSpendingPattern(recentTransactions);
        
        // Tính toán ngân sách đề xuất
        Map<String, BudgetAllocation> allocations = calculateBudgetAllocations(
            monthlyIncome, budgetType, pattern);
        
        // Tạo insights và recommendations
        List<BudgetInsight> insights = generateBudgetInsights(pattern, allocations, monthlyIncome);
        List<BudgetRecommendation> recommendations = generateBudgetRecommendations(
            pattern, allocations, monthlyIncome);
        
        // Tính financial health score
        int healthScore = calculateBudgetHealthScore(pattern, allocations, monthlyIncome);
        
        // Tạo optimization tips
        List<OptimizationTip> tips = generateOptimizationTips(pattern, allocations);
        
        return new SmartBudgetRecommendation(
            allocations,
            insights,
            recommendations,
            tips,
            healthScore,
            pattern
        );
    }
    
    /**
     * Phân tích hiệu suất ngân sách hiện tại
     */
    public BudgetPerformanceAnalysis analyzeBudgetPerformance(Long userId, String period) {
        
        List<Budget> budgets = getBudgetsByPeriod(userId, period);
        List<Transaction> transactions = getTransactionsByPeriod(userId, period);
        
        if (budgets.isEmpty()) {
            return createNoBudgetAnalysis();
        }
        
        Map<String, PerformanceMetric> metrics = calculatePerformanceMetrics(budgets, transactions);
        List<String> achievements = findAchievements(metrics);
        List<String> warnings = findWarnings(metrics);
        List<ImprovementSuggestion> suggestions = generateImprovementSuggestions(metrics);
        
        // Trend analysis
        TrendAnalysis trends = analyzeTrends(transactions, period);
        
        // Forecast next period
        BudgetForecast forecast = generateBudgetForecast(metrics, trends);
        
        return new BudgetPerformanceAnalysis(
            metrics,
            achievements,
            warnings,
            suggestions,
            trends,
            forecast
        );
    }
    
    /**
     * Tối ưu hóa ngân sách dựa trên AI
     */
    public BudgetOptimization optimizeBudget(Long userId, Double targetSavings) {
        
        List<Transaction> transactions = getRecentTransactions(userId, 3);
        Map<String, Double> categorySpending = analyzeCategorySpending(transactions);
        
        // Tìm các khoản chi tiêu có thể cắt giảm
        List<OptimizationOpportunity> opportunities = findOptimizationOpportunities(
            categorySpending, targetSavings);
        
        // Tạo kế hoạch tối ưu
        OptimizationPlan plan = createOptimizationPlan(opportunities, targetSavings);
        
        // Ước tính tác động
        ImpactEstimation impact = estimateOptimizationImpact(plan, transactions);
        
        return new BudgetOptimization(
            opportunities,
            plan,
            impact,
            targetSavings
        );
    }
    
    /**
     * Gợi ý ngân sách thông minh cho học sinh/sinh viên
     */
    public StudentBudgetGuide generateStudentBudgetGuide(Double monthlyIncome, 
                                                       String studentLevel) {
        
        // Điều chỉnh tỷ lệ theo cấp độ học sinh
        Map<String, Double> adjustedRatios = adjustRatiosForStudentLevel(studentLevel);
        
        // Tính toán budget allocations
        Map<String, Double> allocations = new HashMap<>();
        for (Map.Entry<String, Double> entry : adjustedRatios.entrySet()) {
            allocations.put(entry.getKey(), monthlyIncome * entry.getValue());
        }
        
        // Seasonal adjustments
        int currentMonth = LocalDateTime.now().getMonthValue();
        double seasonalMultiplier = seasonalMultipliers.getOrDefault(currentMonth, 1.0);
        
        // Apply seasonal adjustments to entertainment and shopping
        allocations.put("entertainment", allocations.get("entertainment") * seasonalMultiplier);
        allocations.put("shopping", allocations.get("shopping") * seasonalMultiplier);
        
        // Generate tips specific to students
        List<StudentTip> tips = generateStudentSpecificTips(monthlyIncome, studentLevel);
        
        // Emergency fund recommendations
        EmergencyFundGuide emergencyGuide = createEmergencyFundGuide(monthlyIncome, studentLevel);
        
        // Saving goals suggestions
        List<SavingGoal> savingGoals = suggestSavingGoals(monthlyIncome, studentLevel);
        
        return new StudentBudgetGuide(
            allocations,
            adjustedRatios,
            tips,
            emergencyGuide,
            savingGoals,
            seasonalMultiplier
        );
    }
    
    /**
     * Phân tích và cảnh báo overspending
     */
    public SpendingAlert analyzeSpendingAlerts(Long userId) {
        
        List<Budget> currentBudgets = getCurrentBudgets(userId);
        List<Transaction> currentMonthTransactions = getCurrentMonthTransactions(userId);
        
        List<AlertItem> alerts = new ArrayList<>();
        List<AlertItem> warnings = new ArrayList<>();
        List<AlertItem> info = new ArrayList<>();
        
        for (Budget budget : currentBudgets) {
            Category category = budget.getCategory();
            BigDecimal budgetAmount = budget.getAmount();
            
            Double spent = currentMonthTransactions.stream()
                .filter(t -> category != null && t.getCategory() != null && category.getId().equals(t.getCategory().getId()))
                .mapToDouble(t -> t.getAmount().abs().doubleValue())
                .sum();
            
            double percentage = (spent / budgetAmount.doubleValue()) * 100;
            
            if (percentage >= 100) {
                alerts.add(new AlertItem(
                    "overspend",
                    category != null ? category.getName() : "Unknown",
                    "Đã vượt ngân sách " + (category != null ? category.getName() : "Unknown"),
                    spent,
                    budgetAmount.doubleValue(),
                    percentage,
                    "high"
                ));
            } else if (percentage >= 80) {
                warnings.add(new AlertItem(
                    "warning",
                    category != null ? category.getName() : "Unknown",
                    "Sắp vượt ngân sách " + (category != null ? category.getName() : "Unknown"),
                    spent,
                    budgetAmount.doubleValue(),
                    percentage,
                    "medium"
                ));
            } else if (percentage >= 50) {
                info.add(new AlertItem(
                    "info",
                    category != null ? category.getName() : "Unknown",
                    "Đã sử dụng " + String.format("%.0f", percentage) + "% ngân sách " + (category != null ? category.getName() : "Unknown"),
                    spent,
                    budgetAmount.doubleValue(),
                    percentage,
                    "low"
                ));
            }
        }
        
        // Generate recommendations based on alerts
        List<AlertRecommendation> recommendations = generateAlertRecommendations(alerts, warnings);
        
        return new SpendingAlert(alerts, warnings, info, recommendations);
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    private List<Transaction> getRecentTransactions(Long userId, int months) {
        LocalDateTime startDate = LocalDateTime.now().minusMonths(months);
        return transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, startDate);
    }
    
    private SpendingPattern analyzeSpendingPattern(List<Transaction> transactions) {
        Map<String, Double> categoryAverages = new HashMap<>();
        Map<String, List<Double>> categoryAmounts = new HashMap<>();
        
        // Group transactions by category
        Map<String, List<Transaction>> categoryTransactions = transactions.stream()
            .collect(Collectors.groupingBy(t -> t.getCategory() != null ? t.getCategory().getName() : "other"));
        
        for (Map.Entry<String, List<Transaction>> entry : categoryTransactions.entrySet()) {
            String category = entry.getKey();
            List<Transaction> categoryTxns = entry.getValue();
            
            List<Double> amounts = categoryTxns.stream()
                .map(t -> t.getAmount().abs().doubleValue())
                .collect(Collectors.toList());
            
            double average = amounts.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            
            categoryAverages.put(category, average);
            categoryAmounts.put(category, amounts);
        }
        
        // Calculate total monthly spending
        double totalMonthlySpending = categoryAverages.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        
        // Calculate spending frequency
        Map<String, Double> spendingFrequency = calculateSpendingFrequency(categoryTransactions);
        
        return new SpendingPattern(
            categoryAverages,
            categoryAmounts,
            totalMonthlySpending,
            spendingFrequency
        );
    }
    
    private Map<String, BudgetAllocation> calculateBudgetAllocations(Double monthlyIncome, 
                                                                  String budgetType, 
                                                                  SpendingPattern pattern) {
        Map<String, BudgetAllocation> allocations = new HashMap<>();
        
        // Base allocations theo 50/30/20 rule (modified for students)
        Map<String, Double> baseRatios;
        
        switch (budgetType.toLowerCase()) {
            case "conservative":
                baseRatios = getConservativeBudgetRatios();
                break;
            case "balanced":
                baseRatios = studentBudgetRatios;
                break;
            case "flexible":
                baseRatios = getFlexibleBudgetRatios();
                break;
            default:
                baseRatios = studentBudgetRatios;
        }
        
        // Adjust based on spending pattern
        Map<String, Double> adjustedRatios = adjustRatiosBasedOnPattern(baseRatios, pattern);
        
        // Calculate allocations
        for (Map.Entry<String, Double> entry : adjustedRatios.entrySet()) {
            String category = entry.getKey();
            double ratio = entry.getValue();
            double amount = monthlyIncome * ratio;
            
            // Get historical data for this category
            double historicalAvg = pattern.getCategoryAverages().getOrDefault(category, 0.0);
            double confidence = calculateConfidence(historicalAvg, amount);
            
            allocations.put(category, new BudgetAllocation(
                category,
                amount,
                ratio,
                historicalAvg,
                confidence,
                generateCategoryTips(category, amount, historicalAvg)
            ));
        }
        
        return allocations;
    }
    
    private List<BudgetInsight> generateBudgetInsights(SpendingPattern pattern, 
                                                      Map<String, BudgetAllocation> allocations, 
                                                      Double monthlyIncome) {
        List<BudgetInsight> insights = new ArrayList<>();
        
        // Top spending category insight
        String topCategory = pattern.getCategoryAverages().entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("unknown");
        
        double topAmount = pattern.getCategoryAverages().get(topCategory);
        double percentage = (topAmount / pattern.getTotalMonthlySpending()) * 100;
        
        insights.add(new BudgetInsight(
            "spending_pattern",
            "Chi tiêu chính",
            "Bạn chi nhiều nhất cho " + topCategory + " (" + String.format("%.1f", percentage) + "%)",
            "📊",
            "medium"
        ));
        
        // Saving potential insight
        double totalAllocated = allocations.values().stream()
            .mapToDouble(BudgetAllocation::getAmount)
            .sum();
        
        double savingPotential = monthlyIncome - totalAllocated;
        
        if (savingPotential > 0) {
            insights.add(new BudgetInsight(
                "saving_potential",
                "Tiềm năng tiết kiệm",
                "Bạn có thể tiết kiệm " + formatCurrency(savingPotential) + "/tháng",
                "💰",
                "high"
            ));
        }
        
        return insights;
    }
    
    private List<BudgetRecommendation> generateBudgetRecommendations(SpendingPattern pattern,
                                                                   Map<String, BudgetAllocation> allocations,
                                                                   Double monthlyIncome) {
        List<BudgetRecommendation> recommendations = new ArrayList<>();
        
        // Emergency fund recommendation - 3 months worth of income
        recommendations.add(new BudgetRecommendation(
            "emergency_fund",
            "Quỹ khẩn cấp",
            "Hãy dành " + formatCurrency(monthlyIncome * 0.1) + "/tháng cho quỹ khẩn cấp",
            "create_emergency_fund",
            monthlyIncome * 0.1
        ));
        
        // Investment recommendation for students
        if (monthlyIncome > 5000000) { // If income > 5M
            recommendations.add(new BudgetRecommendation(
                "investment",
                "Đầu tư sớm",
                "Với thu nhập ổn định, hãy bắt đầu đầu tư " + formatCurrency(monthlyIncome * 0.05) + "/tháng",
                "start_investing",
                monthlyIncome * 0.05
            ));
        }
        
        return recommendations;
    }
    
    private int calculateBudgetHealthScore(SpendingPattern pattern, 
                                         Map<String, BudgetAllocation> allocations, 
                                         Double monthlyIncome) {
        int score = 70; // Base score
        
        // Savings rate impact
        double totalAllocated = allocations.values().stream()
            .mapToDouble(BudgetAllocation::getAmount)
            .sum();
        
        double savingsRate = (monthlyIncome - totalAllocated) / monthlyIncome;
        
        if (savingsRate >= 0.2) {
            score += 20;
        } else if (savingsRate >= 0.1) {
            score += 10;
        } else if (savingsRate < 0) {
            score -= 30;
        }
        
        // Category balance impact
        int balancedCategories = 0;
        for (BudgetAllocation allocation : allocations.values()) {
            if (allocation.getConfidence() > 0.7) {
                balancedCategories++;
            }
        }
        
        if (balancedCategories >= 5) {
            score += 10;
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    private List<OptimizationTip> generateOptimizationTips(SpendingPattern pattern,
                                                          Map<String, BudgetAllocation> allocations) {
        List<OptimizationTip> tips = new ArrayList<>();
        
        // Find overspending categories
        for (BudgetAllocation allocation : allocations.values()) {
            if (allocation.getHistoricalAvg() > allocation.getAmount() * 1.2) {
                tips.add(new OptimizationTip(
                    "reduce_spending",
                    allocation.getCategory(),
                    "Giảm chi tiêu " + allocation.getCategory(),
                    "Bạn đang chi " + formatCurrency(allocation.getHistoricalAvg() - allocation.getAmount()) + 
                    " quá mức cho " + allocation.getCategory(),
                    allocation.getHistoricalAvg() - allocation.getAmount()
                ));
            }
        }
        
        return tips;
    }
    
    private Map<String, Double> getConservativeBudgetRatios() {
        return new HashMap<String, Double>() {{
            put("food", 0.20);          
            put("transport", 0.08);     
            put("education", 0.35);     
            put("entertainment", 0.10); 
            put("shopping", 0.07);      
            put("health", 0.05);        
            put("bills", 0.05);         
            put("savings", 0.10);       // More savings in conservative
        }};
    }
    
    private Map<String, Double> getFlexibleBudgetRatios() {
        return new HashMap<String, Double>() {{
            put("food", 0.30);          
            put("transport", 0.12);     
            put("education", 0.25);     
            put("entertainment", 0.20); 
            put("shopping", 0.13);      
            put("health", 0.05);        
            put("bills", 0.05);         
        }};
    }
    
    private Map<String, Double> adjustRatiosBasedOnPattern(Map<String, Double> baseRatios, 
                                                          SpendingPattern pattern) {
        Map<String, Double> adjusted = new HashMap<>(baseRatios);
        
        // If user spends significantly more in a category, adjust slightly
        for (Map.Entry<String, Double> entry : pattern.getCategoryAverages().entrySet()) {
            String category = entry.getKey();
            double avgSpending = entry.getValue();
            
            if (adjusted.containsKey(category)) {
                double currentRatio = adjusted.get(category);
                double historicalRatio = avgSpending / pattern.getTotalMonthlySpending();
                
                // Gradual adjustment (don't change too drastically)
                double adjustedRatio = (currentRatio * 0.7) + (historicalRatio * 0.3);
                adjusted.put(category, Math.min(adjustedRatio, currentRatio * 1.5));
            }
        }
        
        return adjusted;
    }
    
    private double calculateConfidence(double historical, double recommended) {
        if (historical == 0) return 0.5; // Medium confidence for new categories
        
        double ratio = Math.min(historical, recommended) / Math.max(historical, recommended);
        return ratio; // Higher when amounts are similar
    }
    
    private List<String> generateCategoryTips(String category, double amount, double historical) {
        List<String> tips = new ArrayList<>();
        
        switch (category) {
            case "food":
                tips.add("Nấu ăn tại nhà 3-4 bữa/tuần");
                tips.add("Mua thực phẩm theo mùa");
                break;
            case "transport":
                tips.add("Sử dụng phương tiện công cộng");
                tips.add("Đi xe đạp cho quãng đường ngắn");
                break;
            case "education":
                tips.add("Tận dụng tài liệu miễn phí online");
                tips.add("Mua sách cũ hoặc thuê sách");
                break;
        }
        
        return tips;
    }
    
    // More helper methods...
    private List<Budget> getBudgetsByPeriod(Long userId, String period) {
        // Implementation for getting budgets by period
        return budgetRepository.findByUserId(userId);
    }
    
    private List<Transaction> getTransactionsByPeriod(Long userId, String period) {
        LocalDateTime startDate;
        LocalDateTime endDate = LocalDateTime.now();
        
        switch (period) {
            case "week":
                startDate = endDate.minusWeeks(1);
                break;
            case "month":
                startDate = endDate.minusMonths(1);
                break;
            case "year":
                startDate = endDate.minusYears(1);
                break;
            default:
                startDate = endDate.minusMonths(1);
        }
        
        return transactionRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            userId, startDate, endDate);
    }
    
    private BudgetPerformanceAnalysis createNoBudgetAnalysis() {
        return new BudgetPerformanceAnalysis(
            new HashMap<>(),
            Arrays.asList("Chưa có ngân sách nào được tạo"),
            Arrays.asList("Hãy tạo ngân sách để theo dõi chi tiêu"),
            Arrays.asList(new ImprovementSuggestion("create_budget", "Tạo ngân sách đầu tiên", "Bắt đầu với ngân sách đơn giản")),
            new TrendAnalysis(new HashMap<>(), 0.0),
            new BudgetForecast(new HashMap<>(), 0.0)
        );
    }
    
    private Map<String, PerformanceMetric> calculatePerformanceMetrics(List<Budget> budgets, 
                                                                     List<Transaction> transactions) {
        Map<String, PerformanceMetric> metrics = new HashMap<>();
        
        for (Budget budget : budgets) {
            Category category = budget.getCategory();
            BigDecimal budgetAmount = budget.getAmount();
            
            Double spent = transactions.stream()
                .filter(t -> category != null && t.getCategory() != null && category.getId().equals(t.getCategory().getId()))
                .mapToDouble(t -> t.getAmount().abs().doubleValue())
                .sum();
            
            double utilizationRate = spent / budgetAmount.doubleValue();
            String status = getPerformanceStatus(utilizationRate);
            
            metrics.put(category.getName(), new PerformanceMetric(category.getName(),
                budgetAmount.doubleValue(),
                spent,
                budgetAmount.doubleValue() - spent,
                utilizationRate,
                status
            ));
        }
        
        return metrics;
    }
    
    private String getPerformanceStatus(double utilizationRate) {
        if (utilizationRate <= 0.8) return "good";
        else if (utilizationRate <= 1.0) return "warning";
        else return "overspent";
    }
    
    private List<String> findAchievements(Map<String, PerformanceMetric> metrics) {
        List<String> achievements = new ArrayList<>();
        
        for (PerformanceMetric metric : metrics.values()) {
            if ("good".equals(metric.getStatus())) {
                achievements.add("Kiểm soát tốt chi tiêu " + metric.getCategory());
            }
        }
        
        return achievements;
    }
    
    private List<String> findWarnings(Map<String, PerformanceMetric> metrics) {
        List<String> warnings = new ArrayList<>();
        
        for (PerformanceMetric metric : metrics.values()) {
            if ("overspent".equals(metric.getStatus())) {
                warnings.add("Vượt ngân sách " + metric.getCategory());
            }
        }
        
        return warnings;
    }
    
    private List<ImprovementSuggestion> generateImprovementSuggestions(Map<String, PerformanceMetric> metrics) {
        List<ImprovementSuggestion> suggestions = new ArrayList<>();
        
        for (PerformanceMetric metric : metrics.values()) {
            if ("overspent".equals(metric.getStatus())) {
                suggestions.add(new ImprovementSuggestion(
                    "reduce_" + metric.getCategory(),
                    "Giảm chi tiêu " + metric.getCategory(),
                    "Hãy cắt giảm " + formatCurrency(metric.getSpent() - metric.getBudgeted()) + 
                    " cho " + metric.getCategory()
                ));
            }
        }
        
        return suggestions;
    }
    
    private TrendAnalysis analyzeTrends(List<Transaction> transactions, String period) {
        // Simple trend analysis
        Map<String, Double> categoryTrends = new HashMap<>();
        double overallTrend = 0.0;
        
        return new TrendAnalysis(categoryTrends, overallTrend);
    }
    
    private BudgetForecast generateBudgetForecast(Map<String, PerformanceMetric> metrics, 
                                                TrendAnalysis trends) {
        Map<String, Double> forecastedSpending = new HashMap<>();
        
        for (PerformanceMetric metric : metrics.values()) {
            // Simple forecast based on current spending
            forecastedSpending.put(metric.getCategory(), metric.getSpent() * 1.05);
        }
        
        return new BudgetForecast(forecastedSpending, 0.05);
    }
    
    private Map<String, Double> analyzeCategorySpending(List<Transaction> transactions) {
        return transactions.stream()
            .collect(Collectors.groupingBy(
                t -> t.getCategory() != null ? t.getCategory().getName() : "other",
                Collectors.summingDouble(t -> t.getAmount().abs().doubleValue())
            ));
    }
    
    private List<OptimizationOpportunity> findOptimizationOpportunities(Map<String, Double> categorySpending,
                                                                       Double targetSavings) {
        List<OptimizationOpportunity> opportunities = new ArrayList<>();
        
        // Sort categories by spending amount
        List<Map.Entry<String, Double>> sortedCategories = categorySpending.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        for (Map.Entry<String, Double> entry : sortedCategories) {
            String category = entry.getKey();
            Double spending = entry.getValue();
            
            // Skip essential categories
            if (!Arrays.asList("bills", "health", "education").contains(category)) {
                double potentialSavings = spending * 0.2; // 20% reduction potential
                
                opportunities.add(new OptimizationOpportunity(
                    category,
                    spending,
                    potentialSavings,
                    "medium",
                    "Giảm 20% chi tiêu " + category
                ));
            }
        }
        
        return opportunities;
    }
    
    private OptimizationPlan createOptimizationPlan(List<OptimizationOpportunity> opportunities,
                                                   Double targetSavings) {
        List<OptimizationStep> steps = new ArrayList<>();
        double totalPotentialSavings = 0;
        
        for (OptimizationOpportunity opp : opportunities) {
            if (totalPotentialSavings < targetSavings) {
                steps.add(new OptimizationStep(
                    opp.getCategory(),
                    opp.getPotentialSavings(),
                    opp.getDescription(),
                    1 // Priority
                ));
                totalPotentialSavings += opp.getPotentialSavings();
            }
        }
        
        return new OptimizationPlan(steps, totalPotentialSavings, targetSavings);
    }
    
    private ImpactEstimation estimateOptimizationImpact(OptimizationPlan plan, 
                                                       List<Transaction> transactions) {
        double monthlySavings = plan.getTotalSavings();
        double yearlySavings = monthlySavings * 12;
        
        return new ImpactEstimation(
            monthlySavings,
            yearlySavings,
            "medium" // confidence level
        );
    }
    
    private Map<String, Double> adjustRatiosForStudentLevel(String studentLevel) {
        Map<String, Double> adjusted = new HashMap<>(studentBudgetRatios);
        
        switch (studentLevel.toLowerCase()) {
            case "highschool":
                // High school students - more education focus
                adjusted.put("education", 0.35);
                adjusted.put("entertainment", 0.12);
                break;
            case "university":
                // University students - more flexible
                adjusted.put("food", 0.30);
                adjusted.put("entertainment", 0.18);
                break;
            case "graduate":
                // Graduate students - more professional
                adjusted.put("education", 0.25);
                adjusted.put("transport", 0.15);
                break;
        }
        
        return adjusted;
    }
    
    private List<StudentTip> generateStudentSpecificTips(Double monthlyIncome, String studentLevel) {
        List<StudentTip> tips = new ArrayList<>();
        
        tips.add(new StudentTip(
            "🎓 Học bổng",
            "Tìm kiếm các chương trình học bổng để giảm chi phí giáo dục",
            "high"
        ));
        
        tips.add(new StudentTip(
            "👥 Chia sẻ chi phí",
            "Chia sẻ chi phí sách vở, đồ dùng học tập với bạn bè",
            "medium"
        ));
        
        if (monthlyIncome < 3000000) {
            tips.add(new StudentTip(
                "💼 Thu nhập thêm",
                "Tìm công việc part-time phù hợp để tăng thu nhập",
                "high"
            ));
        }
        
        return tips;
    }
    
    private EmergencyFundGuide createEmergencyFundGuide(Double monthlyIncome, String studentLevel) {
        double targetAmount = monthlyIncome * 2; // 2 months for students
        double monthlyContribution = monthlyIncome * 0.05; // 5%
        
        return new EmergencyFundGuide(
            targetAmount,
            monthlyContribution,
            "Học sinh nên có quỹ khẩn cấp ít nhất 2 tháng chi tiêu"
        );
    }
    
    private List<SavingGoal> suggestSavingGoals(Double monthlyIncome, String studentLevel) {
        List<SavingGoal> goals = new ArrayList<>();
        
        goals.add(new SavingGoal(
            "laptop",
            "Laptop mới",
            15000000.0,
            monthlyIncome * 0.1,
            "Dành tiền mua laptop cho học tập"
        ));
        
        goals.add(new SavingGoal(
            "vacation",
            "Du lịch hè",
            5000000.0,
            monthlyIncome * 0.05,
            "Tiết kiệm cho chuyến du lịch hè"
        ));
        
        return goals;
    }
    
    private List<Budget> getCurrentBudgets(Long userId) {
        return budgetRepository.findByUserId(userId);
    }
    
    private List<Transaction> getCurrentMonthTransactions(Long userId) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        return transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, startOfMonth);
    }
    
    private List<AlertRecommendation> generateAlertRecommendations(List<AlertItem> alerts, 
                                                                  List<AlertItem> warnings) {
        List<AlertRecommendation> recommendations = new ArrayList<>();
        
        for (AlertItem alert : alerts) {
            recommendations.add(new AlertRecommendation(
                "urgent",
                "Giảm chi tiêu " + alert.getCategory(),
                "Hãy dừng chi tiêu cho " + alert.getCategory() + " trong tuần này",
                alert.getCategory()
            ));
        }
        
        for (AlertItem warning : warnings) {
            recommendations.add(new AlertRecommendation(
                "moderate",
                "Kiểm soát " + warning.getCategory(),
                "Hạn chế chi tiêu " + warning.getCategory() + " trong thời gian còn lại của tháng",
                warning.getCategory()
            ));
        }
        
        return recommendations;
    }
    
    private Map<String, Double> calculateSpendingFrequency(Map<String, List<Transaction>> categoryTransactions) {
        Map<String, Double> frequency = new HashMap<>();
        
        for (Map.Entry<String, List<Transaction>> entry : categoryTransactions.entrySet()) {
            String category = entry.getKey();
            int transactionCount = entry.getValue().size();
            
            // Calculate frequency per month (assuming 3 months of data)
            double monthlyFrequency = transactionCount / 3.0;
            frequency.put(category, monthlyFrequency);
        }
        
        return frequency;
    }
    
    private String formatCurrency(double amount) {
        return String.format("%,.0f VND", amount);
    }
    
    // ===== INNER CLASSES =====
    
    // All the inner classes for return types...
    public static class SpendingPattern {
        private Map<String, Double> categoryAverages;
        private Map<String, List<Double>> categoryAmounts;
        private double totalMonthlySpending;
        private Map<String, Double> spendingFrequency;
        
        public SpendingPattern(Map<String, Double> categoryAverages, 
                             Map<String, List<Double>> categoryAmounts,
                             double totalMonthlySpending,
                             Map<String, Double> spendingFrequency) {
            this.categoryAverages = categoryAverages;
            this.categoryAmounts = categoryAmounts;
            this.totalMonthlySpending = totalMonthlySpending;
            this.spendingFrequency = spendingFrequency;
        }
        
        // Getters
        public Map<String, Double> getCategoryAverages() { return categoryAverages; }
        public Map<String, List<Double>> getCategoryAmounts() { return categoryAmounts; }
        public double getTotalMonthlySpending() { return totalMonthlySpending; }
        public Map<String, Double> getSpendingFrequency() { return spendingFrequency; }
    }
    
    public static class BudgetAllocation {
        private String category;
        private double amount;
        private double ratio;
        private double historicalAvg;
        private double confidence;
        private List<String> tips;
        
        public BudgetAllocation(String category, double amount, double ratio, 
                              double historicalAvg, double confidence, List<String> tips) {
            this.category = category;
            this.amount = amount;
            this.ratio = ratio;
            this.historicalAvg = historicalAvg;
            this.confidence = confidence;
            this.tips = tips;
        }
        
        // Getters
        public String getCategory() { return category; }
        public double getAmount() { return amount; }
        public double getRatio() { return ratio; }
        public double getHistoricalAvg() { return historicalAvg; }
        public double getConfidence() { return confidence; }
        public List<String> getTips() { return tips; }
    }
    
    public static class SmartBudgetRecommendation {
        private Map<String, BudgetAllocation> allocations;
        private List<BudgetInsight> insights;
        private List<BudgetRecommendation> recommendations;
        private List<OptimizationTip> tips;
        private int healthScore;
        private SpendingPattern pattern;
        
        public SmartBudgetRecommendation(Map<String, BudgetAllocation> allocations,
                                       List<BudgetInsight> insights,
                                       List<BudgetRecommendation> recommendations,
                                       List<OptimizationTip> tips,
                                       int healthScore,
                                       SpendingPattern pattern) {
            this.allocations = allocations;
            this.insights = insights;
            this.recommendations = recommendations;
            this.tips = tips;
            this.healthScore = healthScore;
            this.pattern = pattern;
        }
        
        // Getters
        public Map<String, BudgetAllocation> getAllocations() { return allocations; }
        public List<BudgetInsight> getInsights() { return insights; }
        public List<BudgetRecommendation> getRecommendations() { return recommendations; }
        public List<OptimizationTip> getTips() { return tips; }
        public int getHealthScore() { return healthScore; }
        public SpendingPattern getPattern() { return pattern; }
    }
    
    // Additional inner classes...
    public static class BudgetInsight {
        private String type, title, message, icon, priority;
        
        public BudgetInsight(String type, String title, String message, String icon, String priority) {
            this.type = type; this.title = title; this.message = message;
            this.icon = icon; this.priority = priority;
        }
        
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getIcon() { return icon; }
        public String getPriority() { return priority; }
    }
    
    public static class BudgetRecommendation {
        private String type, title, message, action;
        private Double amount;
        
        public BudgetRecommendation(String type, String title, String message, String action, Double amount) {
            this.type = type; this.title = title; this.message = message;
            this.action = action; this.amount = amount;
        }
        
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getAction() { return action; }
        public Double getAmount() { return amount; }
    }
    
    public static class OptimizationTip {
        private String type, category, title, message;
        private Double savings;
        
        public OptimizationTip(String type, String category, String title, String message, Double savings) {
            this.type = type; this.category = category; this.title = title;
            this.message = message; this.savings = savings;
        }
        
        public String getType() { return type; }
        public String getCategory() { return category; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public Double getSavings() { return savings; }
    }
    
    public static class BudgetPerformanceAnalysis {
        private Map<String, PerformanceMetric> metrics;
        private List<String> achievements;
        private List<String> warnings;
        private List<ImprovementSuggestion> suggestions;
        private TrendAnalysis trends;
        private BudgetForecast forecast;
        
        public BudgetPerformanceAnalysis(Map<String, PerformanceMetric> metrics,
                                       List<String> achievements,
                                       List<String> warnings,
                                       List<ImprovementSuggestion> suggestions,
                                       TrendAnalysis trends,
                                       BudgetForecast forecast) {
            this.metrics = metrics;
            this.achievements = achievements;
            this.warnings = warnings;
            this.suggestions = suggestions;
            this.trends = trends;
            this.forecast = forecast;
        }
        
        // Getters...
        public Map<String, PerformanceMetric> getMetrics() { return metrics; }
        public List<String> getAchievements() { return achievements; }
        public List<String> getWarnings() { return warnings; }
        public List<ImprovementSuggestion> getSuggestions() { return suggestions; }
        public TrendAnalysis getTrends() { return trends; }
        public BudgetForecast getForecast() { return forecast; }
    }
    
    public static class PerformanceMetric {
        private String category, status;
        private double budgeted, spent, remaining, utilizationRate;
        
        public PerformanceMetric(String category, double budgeted, double spent, 
                               double remaining, double utilizationRate, String status) {
            this.category = category; this.budgeted = budgeted; this.spent = spent;
            this.remaining = remaining; this.utilizationRate = utilizationRate; this.status = status;
        }
        
        public String getCategory() { return category; }
        public double getBudgeted() { return budgeted; }
        public double getSpent() { return spent; }
        public double getRemaining() { return remaining; }
        public double getUtilizationRate() { return utilizationRate; }
        public String getStatus() { return status; }
    }
    
    public static class ImprovementSuggestion {
        private String id, title, message;
        
        public ImprovementSuggestion(String id, String title, String message) {
            this.id = id; this.title = title; this.message = message;
        }
        
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
    }
    
    public static class TrendAnalysis {
        private Map<String, Double> categoryTrends;
        private double overallTrend;
        
        public TrendAnalysis(Map<String, Double> categoryTrends, double overallTrend) {
            this.categoryTrends = categoryTrends; this.overallTrend = overallTrend;
        }
        
        public Map<String, Double> getCategoryTrends() { return categoryTrends; }
        public double getOverallTrend() { return overallTrend; }
    }
    
    public static class BudgetForecast {
        private Map<String, Double> forecastedSpending;
        private double confidence;
        
        public BudgetForecast(Map<String, Double> forecastedSpending, double confidence) {
            this.forecastedSpending = forecastedSpending; this.confidence = confidence;
        }
        
        public Map<String, Double> getForecastedSpending() { return forecastedSpending; }
        public double getConfidence() { return confidence; }
    }
    
    public static class BudgetOptimization {
        private List<OptimizationOpportunity> opportunities;
        private OptimizationPlan plan;
        private ImpactEstimation impact;
        private Double targetSavings;
        
        public BudgetOptimization(List<OptimizationOpportunity> opportunities,
                                OptimizationPlan plan,
                                ImpactEstimation impact,
                                Double targetSavings) {
            this.opportunities = opportunities; this.plan = plan;
            this.impact = impact; this.targetSavings = targetSavings;
        }
        
        public List<OptimizationOpportunity> getOpportunities() { return opportunities; }
        public OptimizationPlan getPlan() { return plan; }
        public ImpactEstimation getImpact() { return impact; }
        public Double getTargetSavings() { return targetSavings; }
    }
    
    public static class OptimizationOpportunity {
        private String category, difficulty, description;
        private double currentSpending, potentialSavings;
        
        public OptimizationOpportunity(String category, double currentSpending, 
                                     double potentialSavings, String difficulty, String description) {
            this.category = category; this.currentSpending = currentSpending;
            this.potentialSavings = potentialSavings; this.difficulty = difficulty;
            this.description = description;
        }
        
        public String getCategory() { return category; }
        public double getCurrentSpending() { return currentSpending; }
        public double getPotentialSavings() { return potentialSavings; }
        public String getDifficulty() { return difficulty; }
        public String getDescription() { return description; }
    }
    
    public static class OptimizationPlan {
        private List<OptimizationStep> steps;
        private double totalSavings, targetSavings;
        
        public OptimizationPlan(List<OptimizationStep> steps, double totalSavings, double targetSavings) {
            this.steps = steps; this.totalSavings = totalSavings; this.targetSavings = targetSavings;
        }
        
        public List<OptimizationStep> getSteps() { return steps; }
        public double getTotalSavings() { return totalSavings; }
        public double getTargetSavings() { return targetSavings; }
    }
    
    public static class OptimizationStep {
        private String category, description;
        private double savings;
        private int priority;
        
        public OptimizationStep(String category, double savings, String description, int priority) {
            this.category = category; this.savings = savings;
            this.description = description; this.priority = priority;
        }
        
        public String getCategory() { return category; }
        public double getSavings() { return savings; }
        public String getDescription() { return description; }
        public int getPriority() { return priority; }
    }
    
    public static class ImpactEstimation {
        private double monthlySavings, yearlySavings;
        private String confidence;
        
        public ImpactEstimation(double monthlySavings, double yearlySavings, String confidence) {
            this.monthlySavings = monthlySavings; this.yearlySavings = yearlySavings;
            this.confidence = confidence;
        }
        
        public double getMonthlySavings() { return monthlySavings; }
        public double getYearlySavings() { return yearlySavings; }
        public String getConfidence() { return confidence; }
    }
    
    public static class StudentBudgetGuide {
        private Map<String, Double> allocations;
        private Map<String, Double> ratios;
        private List<StudentTip> tips;
        private EmergencyFundGuide emergencyGuide;
        private List<SavingGoal> savingGoals;
        private double seasonalMultiplier;
        
        public StudentBudgetGuide(Map<String, Double> allocations,
                                Map<String, Double> ratios,
                                List<StudentTip> tips,
                                EmergencyFundGuide emergencyGuide,
                                List<SavingGoal> savingGoals,
                                double seasonalMultiplier) {
            this.allocations = allocations; this.ratios = ratios;
            this.tips = tips; this.emergencyGuide = emergencyGuide;
            this.savingGoals = savingGoals; this.seasonalMultiplier = seasonalMultiplier;
        }
        
        public Map<String, Double> getAllocations() { return allocations; }
        public Map<String, Double> getRatios() { return ratios; }
        public List<StudentTip> getTips() { return tips; }
        public EmergencyFundGuide getEmergencyGuide() { return emergencyGuide; }
        public List<SavingGoal> getSavingGoals() { return savingGoals; }
        public double getSeasonalMultiplier() { return seasonalMultiplier; }
    }
    
    public static class StudentTip {
        private String title, message, priority;
        
        public StudentTip(String title, String message, String priority) {
            this.title = title; this.message = message; this.priority = priority;
        }
        
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getPriority() { return priority; }
    }
    
    public static class EmergencyFundGuide {
        private double targetAmount, monthlyContribution;
        private String description;
        
        public EmergencyFundGuide(double targetAmount, double monthlyContribution, String description) {
            this.targetAmount = targetAmount; this.monthlyContribution = monthlyContribution;
            this.description = description;
        }
        
        public double getTargetAmount() { return targetAmount; }
        public double getMonthlyContribution() { return monthlyContribution; }
        public String getDescription() { return description; }
    }
    
    public static class SavingGoal {
        private String id, name, description;
        private double targetAmount, monthlyContribution;
        
        public SavingGoal(String id, String name, double targetAmount, 
                         double monthlyContribution, String description) {
            this.id = id; this.name = name; this.targetAmount = targetAmount;
            this.monthlyContribution = monthlyContribution; this.description = description;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public double getTargetAmount() { return targetAmount; }
        public double getMonthlyContribution() { return monthlyContribution; }
        public String getDescription() { return description; }
    }
    
    public static class SpendingAlert {
        private List<AlertItem> alerts;
        private List<AlertItem> warnings;
        private List<AlertItem> info;
        private List<AlertRecommendation> recommendations;
        
        public SpendingAlert(List<AlertItem> alerts, List<AlertItem> warnings,
                           List<AlertItem> info, List<AlertRecommendation> recommendations) {
            this.alerts = alerts; this.warnings = warnings;
            this.info = info; this.recommendations = recommendations;
        }
        
        public List<AlertItem> getAlerts() { return alerts; }
        public List<AlertItem> getWarnings() { return warnings; }
        public List<AlertItem> getInfo() { return info; }
        public List<AlertRecommendation> getRecommendations() { return recommendations; }
    }
    
    public static class AlertItem {
        private String type, category, message, severity;
        private double spent, budget, percentage;
        
        public AlertItem(String type, String category, String message,
                        double spent, double budget, double percentage, String severity) {
            this.type = type; this.category = category; this.message = message;
            this.spent = spent; this.budget = budget; this.percentage = percentage;
            this.severity = severity;
        }
        
        public String getType() { return type; }
        public String getCategory() { return category; }
        public String getMessage() { return message; }
        public double getSpent() { return spent; }
        public double getBudget() { return budget; }
        public double getPercentage() { return percentage; }
        public String getSeverity() { return severity; }
    }
    
    public static class AlertRecommendation {
        private String priority, title, message, category;
        
        public AlertRecommendation(String priority, String title, String message, String category) {
            this.priority = priority; this.title = title;
            this.message = message; this.category = category;
        }
        
        public String getPriority() { return priority; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getCategory() { return category; }
    }
}
