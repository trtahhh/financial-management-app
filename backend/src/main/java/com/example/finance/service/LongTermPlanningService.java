package com.example.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service tạo kế hoạch tài chính dài hạn (3/6/12 tháng)
 * Dựa trên thu nhập thực tế và lịch sử chi tiêu của người dùng
 */
@Service
public class LongTermPlanningService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Tạo kế hoạch tài chính dài hạn
     * @param userId ID người dùng
     * @param planMonths Số tháng (3, 6, 12)
     * @param targetSavings Mục tiêu tiết kiệm (VND)
     * @return Kế hoạch chi tiết từng tháng
     */
    public LongTermPlan createLongTermPlan(Long userId, int planMonths, Double targetSavings) {
        // Validate input
        if (!Arrays.asList(3, 6, 12).contains(planMonths)) {
            throw new IllegalArgumentException("Plan months must be 3, 6, or 12");
        }
        
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }
        
        // Phân tích lịch sử chi tiêu 3 tháng gần nhất
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<Transaction> recentTransactions = transactionRepository
            .findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, threeMonthsAgo);
        
        // Tính thu nhập trung bình hàng tháng
        double avgMonthlyIncome = calculateAverageMonthlyIncome(recentTransactions);
        if (avgMonthlyIncome <= 0) {
            avgMonthlyIncome = 10000000; // Default 10M VND if no income data
        }
        
        // Tính chi tiêu trung bình hàng tháng theo category
        Map<String, Double> avgCategorySpending = calculateAverageCategorySpending(recentTransactions);
        double totalAvgSpending = avgCategorySpending.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        
        // Tính tỷ lệ tiết kiệm hiện tại
        double currentSavingsRate = avgMonthlyIncome > 0 ? 
            ((avgMonthlyIncome - totalAvgSpending) / avgMonthlyIncome) * 100 : 0;
        
        // Tính monthly savings cần thiết để đạt target
        double requiredMonthlySavings = targetSavings / planMonths;
        double requiredSavingsRate = (requiredMonthlySavings / avgMonthlyIncome) * 100;
        
        // Kiểm tra tính khả thi
        PlanFeasibility feasibility = assessPlanFeasibility(
            avgMonthlyIncome, totalAvgSpending, targetSavings, planMonths
        );
        
        // Tạo kế hoạch chi tiêu tối ưu cho từng category
        Map<String, CategoryPlan> categoryPlans = createCategoryPlans(
            avgCategorySpending, avgMonthlyIncome, requiredMonthlySavings
        );
        
        // Tạo timeline theo tháng
        List<MonthlyMilestone> milestones = createMonthlyMilestones(
            planMonths, avgMonthlyIncome, requiredMonthlySavings, targetSavings
        );
        
        // Tạo lời khuyên chi tiết
        List<String> recommendations = generatePlanRecommendations(
            feasibility, currentSavingsRate, requiredSavingsRate, categoryPlans
        );
        
        // Tạo emergency strategies
        List<EmergencyStrategy> emergencyStrategies = createEmergencyStrategies(
            avgMonthlyIncome, targetSavings
        );
        
        return new LongTermPlan(
            planMonths,
            avgMonthlyIncome,
            totalAvgSpending,
            currentSavingsRate,
            targetSavings,
            requiredMonthlySavings,
            requiredSavingsRate,
            feasibility,
            categoryPlans,
            milestones,
            recommendations,
            emergencyStrategies,
            calculateSuccessProbability(feasibility, currentSavingsRate, requiredSavingsRate)
        );
    }
    
    /**
     * Get suggestions for specific savings target
     */
    public SavingsPathSuggestion suggestSavingsPath(Long userId, Double targetAmount, String purpose) {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<Transaction> recentTransactions = transactionRepository
            .findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, threeMonthsAgo);
        
        double avgMonthlyIncome = calculateAverageMonthlyIncome(recentTransactions);
        Map<String, Double> avgCategorySpending = calculateAverageCategorySpending(recentTransactions);
        double totalAvgSpending = avgCategorySpending.values().stream().mapToDouble(Double::doubleValue).sum();
        double currentMonthlySavings = avgMonthlyIncome - totalAvgSpending;
        
        // Tính thời gian cần thiết với savings hiện tại
        int monthsNeededCurrent = currentMonthlySavings > 0 ? 
            (int) Math.ceil(targetAmount / currentMonthlySavings) : 999;
        
        // Suggest 3 paths: Conservative (12mo), Balanced (6mo), Aggressive (3mo)
        List<SavingsPath> paths = new ArrayList<>();
        
        // Conservative path (12 months)
        if (targetAmount / 12 < avgMonthlyIncome * 0.3) {
            paths.add(createSavingsPath("conservative", 12, targetAmount, avgMonthlyIncome, 
                avgCategorySpending, "Tiết kiệm từ từ, ít áp lực"));
        }
        
        // Balanced path (6 months)
        if (targetAmount / 6 < avgMonthlyIncome * 0.5) {
            paths.add(createSavingsPath("balanced", 6, targetAmount, avgMonthlyIncome,
                avgCategorySpending, "Cân bằng giữa tốc độ và áp lực"));
        }
        
        // Aggressive path (3 months)
        if (targetAmount / 3 < avgMonthlyIncome * 0.7) {
            paths.add(createSavingsPath("aggressive", 3, targetAmount, avgMonthlyIncome,
                avgCategorySpending, "Tiết kiệm nhanh, cần kỷ luật cao"));
        }
        
        // Custom path based on current savings
        if (monthsNeededCurrent < 999 && monthsNeededCurrent > 0) {
            paths.add(createSavingsPath("current_pace", monthsNeededCurrent, targetAmount,
                avgMonthlyIncome, avgCategorySpending, "Duy trì tốc độ tiết kiệm hiện tại"));
        }
        
        return new SavingsPathSuggestion(
            targetAmount,
            purpose,
            paths,
            avgMonthlyIncome,
            currentMonthlySavings,
            monthsNeededCurrent
        );
    }
    
    // Helper methods
    private double calculateAverageMonthlyIncome(List<Transaction> transactions) {
        List<Transaction> incomeTransactions = transactions.stream()
            .filter(t -> "income".equalsIgnoreCase(t.getType()))
            .collect(Collectors.toList());
        
        if (incomeTransactions.isEmpty()) return 0;
        
        double totalIncome = incomeTransactions.stream()
            .mapToDouble(t -> t.getAmount().doubleValue())
            .sum();
        
        // Estimate số tháng có data
        LocalDateTime earliest = incomeTransactions.stream()
            .map(Transaction::getCreatedAt)
            .min(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(earliest, LocalDateTime.now());
        int monthsCount = Math.max(1, (int) Math.ceil(daysBetween / 30.0));
        
        return totalIncome / monthsCount;
    }
    
    private Map<String, Double> calculateAverageCategorySpending(List<Transaction> transactions) {
        List<Transaction> expenses = transactions.stream()
            .filter(t -> "expense".equalsIgnoreCase(t.getType()))
            .collect(Collectors.toList());
        
        Map<String, Double> categoryTotals = new HashMap<>();
        
        for (Transaction t : expenses) {
            String categoryName = t.getCategory() != null ? t.getCategory().getName() : "Khác";
            categoryTotals.merge(categoryName, t.getAmount().abs().doubleValue(), Double::sum);
        }
        
        // Average over months
        LocalDateTime earliest = expenses.stream()
            .map(Transaction::getCreatedAt)
            .min(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
        
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(earliest, LocalDateTime.now());
        int monthsCount = Math.max(1, (int) Math.ceil(daysBetween / 30.0));
        
        for (String key : categoryTotals.keySet()) {
            categoryTotals.put(key, categoryTotals.get(key) / monthsCount);
        }
        
        return categoryTotals;
    }
    
    private PlanFeasibility assessPlanFeasibility(double income, double spending, 
                                                 double target, int months) {
        double monthlySavingsNeeded = target / months;
        double availableForSavings = income - spending;
        
        String level;
        String description;
        double probability;
        
        if (monthlySavingsNeeded > income * 0.7) {
            level = "impossible";
            description = "Mục tiêu quá cao so với thu nhập";
            probability = 0.1;
        } else if (monthlySavingsNeeded > availableForSavings * 1.5) {
            level = "very_difficult";
            description = "Rất khó, cần cắt giảm chi tiêu mạnh";
            probability = 0.3;
        } else if (monthlySavingsNeeded > availableForSavings) {
            level = "difficult";
            description = "Khó, cần thay đổi thói quen chi tiêu";
            probability = 0.6;
        } else if (monthlySavingsNeeded > availableForSavings * 0.7) {
            level = "achievable";
            description = "Có thể đạt được với kỷ luật";
            probability = 0.8;
        } else {
            level = "easy";
            description = "Dễ dàng đạt được";
            probability = 0.95;
        }
        
        return new PlanFeasibility(level, description, probability);
    }
    
    private Map<String, CategoryPlan> createCategoryPlans(Map<String, Double> avgSpending,
                                                          double income, double requiredSavings) {
        Map<String, CategoryPlan> plans = new HashMap<>();
        double totalSpending = avgSpending.values().stream().mapToDouble(Double::doubleValue).sum();
        
        // Calculate how much we need to cut
        double targetSpending = income - requiredSavings;
        // If targetSpending < 0, we need to cut more than 100% (impossible)
        // cutPercentage should be: (current - target) / current * 100
        double cutPercentage = 0;
        if (totalSpending > 0) {
            if (targetSpending < 0) {
                cutPercentage = 100; // Need to cut everything and more
            } else {
                cutPercentage = ((totalSpending - targetSpending) / totalSpending) * 100;
                cutPercentage = Math.max(0, Math.min(cutPercentage, 100)); // Clamp between 0-100%
            }
        }
        
        for (Map.Entry<String, Double> entry : avgSpending.entrySet()) {
            String category = entry.getKey();
            double currentSpending = entry.getValue();
            
            // Apply different cut rates by category priority
            double categoryCutRate = getCategoryCutRate(category, cutPercentage);
            double targetAmount = currentSpending * (1 - categoryCutRate / 100);
            double savings = currentSpending - targetAmount;
            
            List<String> tips = getCategorySpecificCutTips(category, categoryCutRate);
            
            plans.put(category, new CategoryPlan(
                category,
                currentSpending,
                targetAmount,
                savings,
                categoryCutRate,
                tips
            ));
        }
        
        return plans;
    }
    
    private double getCategoryCutRate(String category, double baseCutRate) {
        // Essential categories: cut less
        if (category.matches("(?i).*(hóa đơn|bills|sức khỏe|health).*")) {
            return baseCutRate * 0.3;
        }
        // Semi-essential: medium cut
        if (category.matches("(?i).*(ăn uống|food|di chuyển|transport).*")) {
            return baseCutRate * 0.7;
        }
        // Non-essential: cut more
        if (category.matches("(?i).*(giải trí|entertainment|mua sắm|shopping).*")) {
            return baseCutRate * 1.5;
        }
        // Default
        return baseCutRate;
    }
    
    private List<String> getCategorySpecificCutTips(String category, double cutRate) {
        List<String> tips = new ArrayList<>();
        
        if (cutRate < 10) {
            tips.add("Duy trì mức chi tiêu hiện tại");
            return tips;
        }
        
        switch (category.toLowerCase()) {
            case "ăn uống":
            case "food":
                tips.add("🏠 Tăng tỷ lệ ăn nhà lên " + (cutRate > 30 ? "80%" : "60%"));
                tips.add("🍱 Chuẩn bị cơm trưa mang đi làm");
                tips.add("☕ Giảm cafe ngoài, pha tại nhà");
                tips.add("🛒 Mua sắm tại chợ thay vì siêu thị");
                break;
                
            case "di chuyển":
            case "transport":
                tips.add("🚌 Ưu tiên xe buýt/MRT (tiết kiệm ~60%)");
                tips.add("🚴 Xe đạp/đi bộ với quãng đường < 3km");
                tips.add("🎫 Mua vé tháng nếu đi thường xuyên");
                if (cutRate > 40) tips.add("🏠 Work from home nếu có thể");
                break;
                
            case "giải trí":
            case "entertainment":
                tips.add("🎮 Tạm dừng các subscription không cần thiết");
                tips.add("🏞️ Hoạt động miễn phí: công viên, thư viện");
                tips.add("🎬 Xem phim tại nhà thay vì rạp");
                if (cutRate > 50) tips.add("⏸️ Tạm dừng giải trí trả phí 1-2 tháng");
                break;
                
            case "mua sắm":
            case "shopping":
                tips.add("📝 Chỉ mua đồ trong danh sách cần thiết");
                tips.add("⏰ Áp dụng quy tắc 48h trước khi mua");
                tips.add("🛍️ Tận dụng đồ có sẵn");
                if (cutRate > 60) tips.add("⏸️ Ngừng hoàn toàn mua sắm không thiết yếu");
                break;
                
            default:
                tips.add("📊 Theo dõi chi tiết chi tiêu");
                tips.add("💰 Cắt giảm " + String.format("%.0f%%", cutRate));
                tips.add("📝 Lập ngân sách cụ thể");
                break;
        }
        
        return tips;
    }
    
    private List<MonthlyMilestone> createMonthlyMilestones(int totalMonths, double income,
                                                          double monthlySavings, double target) {
        List<MonthlyMilestone> milestones = new ArrayList<>();
        double cumulativeSavings = 0;
        
        for (int month = 1; month <= totalMonths; month++) {
            cumulativeSavings += monthlySavings;
            double progress = (cumulativeSavings / target) * 100;
            
            String monthlyGoal = month % 3 == 0 ? 
                "Review và điều chỉnh kế hoạch" : 
                "Duy trì tiết kiệm " + String.format("%.0f₫", monthlySavings);
            
            milestones.add(new MonthlyMilestone(
                month,
                monthlySavings,
                cumulativeSavings,
                progress,
                monthlyGoal
            ));
        }
        
        return milestones;
    }
    
    private List<String> generatePlanRecommendations(PlanFeasibility feasibility,
                                                     double currentRate, double requiredRate,
                                                     Map<String, CategoryPlan> categoryPlans) {
        List<String> recommendations = new ArrayList<>();
        
        // Overall assessment - match với logic ở assessPlanFeasibility
        String level = feasibility.getLevel();
        if (level.equals("impossible")) {
            recommendations.add("Mục tiêu không khả thi - xem xét giảm target hoặc kéo dài thời gian");
            recommendations.add("Tìm cách tăng thu nhập: làm thêm, freelance, bán đồ không dùng");
        } else if (level.equals("very_difficult")) {
            recommendations.add("Rất khó đạt được - cần cắt giảm chi tiêu mạnh và kỷ luật cao");
            recommendations.add("Xem xét lại mục tiêu hoặc kéo dài thời gian");
            recommendations.add("Tìm cách tăng thu nhập bổ sung");
        } else if (level.equals("difficult")) {
            recommendations.add("Khó đạt được - cần thay đổi thói quen chi tiêu đáng kể");
            recommendations.add("Theo dõi chi tiêu hàng ngày chặt chẽ");
            recommendations.add("Tìm accountability partner để giữ động lực");
        } else if (level.equals("achievable")) {
            recommendations.add("Mục tiêu khả thi - có thể đạt được với kỷ luật");
            recommendations.add("Lập kế hoạch chi tiêu cụ thể và tuân thủ");
        } else { // easy
            recommendations.add("Mục tiêu dễ đạt - bắt đầu ngay!");
            recommendations.add("Có thể tăng target hoặc rút ngắn thời gian nếu muốn");
        }
        
        // Savings rate recommendations
        double rateGap = requiredRate - currentRate;
        if (rateGap > 20) {
            recommendations.add("Cần tăng tỷ lệ tiết kiệm " + String.format("%.1f%%", rateGap));
            recommendations.add("Xem lại chi tiêu Giải trí và Mua sắm đầu tiên");
        }
        
        // Category-specific recommendations
        categoryPlans.entrySet().stream()
            .filter(e -> e.getValue().getCutPercentage() > 30)
            .sorted((a, b) -> Double.compare(b.getValue().getCutPercentage(), a.getValue().getCutPercentage()))
            .limit(2)
            .forEach(e -> {
                recommendations.add("Ưu tiên cắt giảm: " + e.getKey() + 
                    " (" + String.format("%.0f%%", e.getValue().getCutPercentage()) + ")");
            });
        
        // Additional tips
        recommendations.add("Tự động chuyển khoản tiết kiệm mỗi đầu tháng");
        recommendations.add("Xóa app shopping, tắt thông báo khuyến mãi");
        recommendations.add("Thưởng cho bản thân khi đạt milestone");
        
        return recommendations;
    }
    
    private List<EmergencyStrategy> createEmergencyStrategies(double income, double target) {
        List<EmergencyStrategy> strategies = new ArrayList<>();
        
        strategies.add(new EmergencyStrategy(
            "Tăng thu nhập tạm thời",
            Arrays.asList(
                "Làm thêm giờ/overtime tại công ty",
                "Freelance online (Fiverr, Upwork)",
                "Bán đồ không dùng (Facebook, Chợ Tốt)",
                "Dạy kèm, gia sư",
                "Làm part-time cuối tuần"
            ),
            "high"
        ));
        
        strategies.add(new EmergencyStrategy(
            "Cắt giảm chi tiêu khẩn cấp",
            Arrays.asList(
                "Tạm dừng toàn bộ giải trí trả phí",
                "100% ăn nhà, tự nấu",
                "Chỉ di chuyển xe buýt/đi bộ",
                "Dừng mua sắm hoàn toàn 1 tháng",
                "Hủy subscriptions không cần thiết"
            ),
            "medium"
        ));
        
        strategies.add(new EmergencyStrategy(
            "Điều chỉnh mục tiêu",
            Arrays.asList(
                "Giảm target xuống 70-80%",
                "Kéo dài thời gian thêm 2-3 tháng",
                "Chia nhỏ mục tiêu thành các giai đoạn",
                "Tìm nguồn vốn hỗ trợ (vay thân nhân)"
            ),
            "low"
        ));
        
        return strategies;
    }
    
    private double calculateSuccessProbability(PlanFeasibility feasibility, 
                                              double currentRate, double requiredRate) {
        double baseProbability = feasibility.getProbability();
        
        // Adjust based on current vs required savings rate
        if (currentRate >= requiredRate) {
            baseProbability = Math.min(0.95, baseProbability * 1.2);
        } else {
            double gap = requiredRate - currentRate;
            if (gap > 30) baseProbability *= 0.5;
            else if (gap > 20) baseProbability *= 0.7;
            else if (gap > 10) baseProbability *= 0.9;
        }
        
        return Math.max(0.05, Math.min(0.95, baseProbability));
    }
    
    private SavingsPath createSavingsPath(String type, int months, double target,
                                         double income, Map<String, Double> spending,
                                         String description) {
        double monthlySavingsNeeded = target / months;
        double totalSpending = spending.values().stream().mapToDouble(Double::doubleValue).sum();
        double targetMonthlySpending = income - monthlySavingsNeeded;
        double cutPercentage = totalSpending > 0 ? 
            ((totalSpending - targetMonthlySpending) / totalSpending) * 100 : 0;
        
        List<String> keyChanges = new ArrayList<>();
        if (cutPercentage > 50) {
            keyChanges.add("Cắt giảm mạnh chi tiêu không thiết yếu");
            keyChanges.add("Tăng thu nhập nếu có thể");
        } else if (cutPercentage > 30) {
            keyChanges.add("Giảm ăn ngoài và giải trí");
            keyChanges.add("Tối ưu chi phí di chuyển");
        } else if (cutPercentage > 10) {
            keyChanges.add("Kiểm soát chi tiêu nhỏ hàng ngày");
            keyChanges.add("Tận dụng khuyến mãi");
        } else {
            keyChanges.add("Duy trì thói quen chi tiêu hiện tại");
        }
        
        String difficulty = cutPercentage > 50 ? "Rất khó" :
                           cutPercentage > 30 ? "Khó" :
                           cutPercentage > 10 ? "Trung bình" : "Dễ";
        
        return new SavingsPath(
            type,
            months,
            monthlySavingsNeeded,
            cutPercentage,
            description,
            difficulty,
            keyChanges
        );
    }
    
    // DTO Classes
    public static class LongTermPlan {
        private int planMonths;
        private double avgMonthlyIncome;
        private double avgMonthlySpending;
        private double currentSavingsRate;
        private double targetSavings;
        private double requiredMonthlySavings;
        private double requiredSavingsRate;
        private PlanFeasibility feasibility;
        private Map<String, CategoryPlan> categoryPlans;
        private List<MonthlyMilestone> milestones;
        private List<String> recommendations;
        private List<EmergencyStrategy> emergencyStrategies;
        private double successProbability;
        
        public LongTermPlan(int planMonths, double avgMonthlyIncome, double avgMonthlySpending,
                          double currentSavingsRate, double targetSavings, double requiredMonthlySavings,
                          double requiredSavingsRate, PlanFeasibility feasibility,
                          Map<String, CategoryPlan> categoryPlans, List<MonthlyMilestone> milestones,
                          List<String> recommendations, List<EmergencyStrategy> emergencyStrategies,
                          double successProbability) {
            this.planMonths = planMonths; this.avgMonthlyIncome = avgMonthlyIncome;
            this.avgMonthlySpending = avgMonthlySpending; this.currentSavingsRate = currentSavingsRate;
            this.targetSavings = targetSavings; this.requiredMonthlySavings = requiredMonthlySavings;
            this.requiredSavingsRate = requiredSavingsRate; this.feasibility = feasibility;
            this.categoryPlans = categoryPlans; this.milestones = milestones;
            this.recommendations = recommendations; this.emergencyStrategies = emergencyStrategies;
            this.successProbability = successProbability;
        }
        
        // Getters
        public int getPlanMonths() { return planMonths; }
        public double getAvgMonthlyIncome() { return avgMonthlyIncome; }
        public double getAvgMonthlySpending() { return avgMonthlySpending; }
        public double getCurrentSavingsRate() { return currentSavingsRate; }
        public double getTargetSavings() { return targetSavings; }
        public double getRequiredMonthlySavings() { return requiredMonthlySavings; }
        public double getRequiredSavingsRate() { return requiredSavingsRate; }
        public PlanFeasibility getFeasibility() { return feasibility; }
        public Map<String, CategoryPlan> getCategoryPlans() { return categoryPlans; }
        public List<MonthlyMilestone> getMilestones() { return milestones; }
        public List<String> getRecommendations() { return recommendations; }
        public List<EmergencyStrategy> getEmergencyStrategies() { return emergencyStrategies; }
        public double getSuccessProbability() { return successProbability; }
    }
    
    public static class PlanFeasibility {
        private String level;
        private String description;
        private double probability;
        
        public PlanFeasibility(String level, String description, double probability) {
            this.level = level; this.description = description; this.probability = probability;
        }
        
        public String getLevel() { return level; }
        public String getDescription() { return description; }
        public double getProbability() { return probability; }
    }
    
    public static class CategoryPlan {
        private String categoryName;
        private double currentSpending;
        private double targetSpending;
        private double savings;
        private double cutPercentage;
        private List<String> tips;
        
        public CategoryPlan(String categoryName, double currentSpending, double targetSpending,
                          double savings, double cutPercentage, List<String> tips) {
            this.categoryName = categoryName; this.currentSpending = currentSpending;
            this.targetSpending = targetSpending; this.savings = savings;
            this.cutPercentage = cutPercentage; this.tips = tips;
        }
        
        public String getCategoryName() { return categoryName; }
        public double getCurrentSpending() { return currentSpending; }
        public double getTargetSpending() { return targetSpending; }
        public double getSavings() { return savings; }
        public double getCutPercentage() { return cutPercentage; }
        public List<String> getTips() { return tips; }
    }
    
    public static class MonthlyMilestone {
        private int month;
        private double monthlySavings;
        private double cumulativeSavings;
        private double progress;
        private String goal;
        
        public MonthlyMilestone(int month, double monthlySavings, double cumulativeSavings,
                              double progress, String goal) {
            this.month = month; this.monthlySavings = monthlySavings;
            this.cumulativeSavings = cumulativeSavings; this.progress = progress;
            this.goal = goal;
        }
        
        public int getMonth() { return month; }
        public double getMonthlySavings() { return monthlySavings; }
        public double getCumulativeSavings() { return cumulativeSavings; }
        public double getProgress() { return progress; }
        public String getGoal() { return goal; }
    }
    
    public static class EmergencyStrategy {
        private String title;
        private List<String> actions;
        private String priority;
        
        public EmergencyStrategy(String title, List<String> actions, String priority) {
            this.title = title; this.actions = actions; this.priority = priority;
        }
        
        public String getTitle() { return title; }
        public List<String> getActions() { return actions; }
        public String getPriority() { return priority; }
    }
    
    public static class SavingsPathSuggestion {
        private double targetAmount;
        private String purpose;
        private List<SavingsPath> paths;
        private double currentIncome;
        private double currentMonthlySavings;
        private int monthsNeededCurrentPace;
        
        public SavingsPathSuggestion(double targetAmount, String purpose, List<SavingsPath> paths,
                                   double currentIncome, double currentMonthlySavings,
                                   int monthsNeededCurrentPace) {
            this.targetAmount = targetAmount; this.purpose = purpose; this.paths = paths;
            this.currentIncome = currentIncome; this.currentMonthlySavings = currentMonthlySavings;
            this.monthsNeededCurrentPace = monthsNeededCurrentPace;
        }
        
        public double getTargetAmount() { return targetAmount; }
        public String getPurpose() { return purpose; }
        public List<SavingsPath> getPaths() { return paths; }
        public double getCurrentIncome() { return currentIncome; }
        public double getCurrentMonthlySavings() { return currentMonthlySavings; }
        public int getMonthsNeededCurrentPace() { return monthsNeededCurrentPace; }
    }
    
    public static class SavingsPath {
        private String type;
        private int months;
        private double monthlySavingsNeeded;
        private double spendingCutPercentage;
        private String description;
        private String difficulty;
        private List<String> keyChanges;
        
        public SavingsPath(String type, int months, double monthlySavingsNeeded,
                         double spendingCutPercentage, String description,
                         String difficulty, List<String> keyChanges) {
            this.type = type; this.months = months; this.monthlySavingsNeeded = monthlySavingsNeeded;
            this.spendingCutPercentage = spendingCutPercentage; this.description = description;
            this.difficulty = difficulty; this.keyChanges = keyChanges;
        }
        
        public String getType() { return type; }
        public int getMonths() { return months; }
        public double getMonthlySavingsNeeded() { return monthlySavingsNeeded; }
        public double getSpendingCutPercentage() { return spendingCutPercentage; }
        public String getDescription() { return description; }
        public String getDifficulty() { return difficulty; }
        public List<String> getKeyChanges() { return keyChanges; }
    }
}
