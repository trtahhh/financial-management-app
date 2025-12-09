package com.example.finance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.time.LocalDateTime;

/**
 * Mock Planning Controller - For Testing Integration
 * Provides sample AI planning responses without external dependencies
 */
@RestController
@RequestMapping("/api/planning")
@CrossOrigin(origins = "*")
public class MockPlanningController {

    /**
     * Kiểm tra trạng thái Planning service
     */
    @GetMapping("/health")
    public ResponseEntity<?> checkPlanningHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("service", "mock-planning-service");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("features", Arrays.asList("Planning Analysis", "Mock Data", "Integration Testing"));
        return ResponseEntity.ok(health);
    }

    /**
     * Phân tích kế hoạch tài chính toàn diện (Mock)
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeFinancialPlan(@RequestBody Map<String, Object> planningData) {
        try {
            // Get input data
            Double monthlyIncome = Double.parseDouble(planningData.getOrDefault("monthly_income", "0").toString());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) planningData.getOrDefault("transactions", new ArrayList<>());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> goals = (List<Map<String, Object>>) planningData.getOrDefault("goals", new ArrayList<>());

            // Calculate basic stats
            double totalSpending = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.get("type")))
                .mapToDouble(t -> Double.parseDouble(t.getOrDefault("amount", "0").toString()))
                .sum();

            double savingsRate = monthlyIncome > 0 ? ((monthlyIncome - totalSpending) / monthlyIncome * 100) : 0;

            // Create mock response
            Map<String, Object> response = new HashMap<>();
            response.put("monthly_income", monthlyIncome);
            response.put("total_spending", totalSpending);
            response.put("savings_rate", Math.max(0, savingsRate));
            
            // Mock spending insights
            response.put("spending_insights", createMockSpendingInsights());
            
            // Mock savings recommendations
            response.put("savings_recommendations", createMockSavingsRecommendations());
            
            // Mock goal plans
            response.put("goal_plans", createMockGoalPlans(goals));
            
            // Mock overall score and next actions
            response.put("overall_score", calculateMockScore(savingsRate, goals.size()));
            response.put("next_actions", createMockNextActions());
            response.put("success", true);
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi phân tích kế hoạch tài chính: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Lấy insights chi tiêu nhanh (Mock)
     */
    @PostMapping("/spending-insights")
    public ResponseEntity<?> getSpendingInsights(@RequestBody Map<String, Object> requestData) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("insights", createMockSpendingInsights());
            response.put("total_spending", 5000000);
            response.put("categories", createMockCategories());
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi phân tích chi tiêu: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Lấy gợi ý tiết kiệm (Mock)
     */
    @PostMapping("/savings-recommendations")
    public ResponseEntity<?> getSavingsRecommendations(@RequestBody Map<String, Object> requestData) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("recommendations", createMockSavingsRecommendations());
            response.put("total_potential_savings", 1500000);
            response.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi tạo gợi ý tiết kiệm: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // Helper methods to create mock data
    private List<Map<String, Object>> createMockSpendingInsights() {
        List<Map<String, Object>> insights = new ArrayList<>();
        
        insights.add(createInsight("Ăn uống", 2000000, 35.5, "Tăng cao", "Chi tiêu ăn uống chiếm 35.5% ngân sách. Cần giảm bớt để cải thiện tài chính.", "high"));
        insights.add(createInsight("Di chuyển", 1200000, 21.3, "Ổn định", "Chi tiêu di chuyển ở mức trung bình. Có thể tối ưu hóa để tiết kiệm thêm.", "medium"));
        insights.add(createInsight("Mua sắm", 800000, 14.2, "Hợp lý", "Chi tiêu mua sắm ở mức hợp lý. Duy trì thói quen tốt này.", "low"));
        insights.add(createInsight("Giải trí", 600000, 10.7, "Ổn định", "Chi tiêu giải trí ở mức trung bình.", "medium"));
        insights.add(createInsight("Y tế", 400000, 7.1, "Hợp lý", "Chi tiêu y tế cần thiết và hợp lý.", "low"));
        
        return insights;
    }

    private Map<String, Object> createInsight(String category, double amount, double percentage, String trend, String recommendation, String severity) {
        Map<String, Object> insight = new HashMap<>();
        insight.put("category", category);
        insight.put("amount", amount);
        insight.put("percentage", percentage);
        insight.put("trend", trend);
        insight.put("recommendation", recommendation);
        insight.put("severity", severity);
        return insight;
    }

    private List<Map<String, Object>> createMockSavingsRecommendations() {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        Map<String, Object> rec1 = new HashMap<>();
        rec1.put("title", "Tối ưu chi phí ăn uống");
        rec1.put("description", "Chi phí ăn uống của bạn cao hơn mức khuyến nghị. Có thể tiết kiệm bằng cách nấu ăn tại nhà nhiều hơn.");
        rec1.put("potential_savings", 600000);
        rec1.put("difficulty", "Dễ");
        rec1.put("timeframe", "1-2 tuần");
        rec1.put("action_steps", Arrays.asList(
            "Lập kế hoạch menu hàng tuần",
            "Mua sắm theo danh sách định sẵn",
            "Nấu ăn tại nhà ít nhất 5 bữa/tuần",
            "Đem cơm trưa đi làm thay vì ăn ngoài"
        ));
        recommendations.add(rec1);

        Map<String, Object> rec2 = new HashMap<>();
        rec2.put("title", "Tối ưu chi phí di chuyển");
        rec2.put("description", "Sử dụng phương tiện công cộng hoặc xe đạp để giảm chi phí di chuyển hàng ngày.");
        rec2.put("potential_savings", 300000);
        rec2.put("difficulty", "Dễ");
        rec2.put("timeframe", "2-3 tuần");
        rec2.put("action_steps", Arrays.asList(
            "Sử dụng xe buýt/tàu điện thay vì taxi",
            "Đi xe đạp cho quãng đường ngắn",
            "Chia sẻ xe với đồng nghiệp",
            "Lên kế hoạch di chuyển hiệu quả"
        ));
        recommendations.add(rec2);

        Map<String, Object> rec3 = new HashMap<>();
        rec3.put("title", "Kiểm soát chi tiêu giải trí");
        rec3.put("description", "Hạn chế các khoản chi không cần thiết cho giải trí và mua sắm để tăng khả năng tiết kiệm.");
        rec3.put("potential_savings", 240000);
        rec3.put("difficulty", "Trung bình");
        rec3.put("timeframe", "1 tháng");
        rec3.put("action_steps", Arrays.asList(
            "Đặt ngân sách cố định cho giải trí mỗi tháng",
            "Tìm các hoạt động miễn phí thay thế",
            "Áp dụng quy tắc 24h trước khi mua đồ không cần thiết",
            "Sử dụng ứng dụng theo dõi chi tiêu"
        ));
        recommendations.add(rec3);

        return recommendations;
    }

    private List<Map<String, Object>> createMockGoalPlans(List<Map<String, Object>> inputGoals) {
        List<Map<String, Object>> goalPlans = new ArrayList<>();
        
        if (inputGoals.isEmpty()) {
            // Create sample goals if none provided
            goalPlans.add(createMockGoalPlan("Mua nhà", 3000000000.0, 500000000.0, 208333333.0, "2026-12-31", "Khó khăn", Arrays.asList(
                "Cần tăng thu nhập hoặc giảm chi tiêu đáng kể",
                "Xem xét vay ngân hàng với lãi suất ưu đãi",
                "Tìm hiểu các chương trình hỗ trợ mua nhà"
            )));
            
            goalPlans.add(createMockGoalPlan("Quỹ khẩn cấp", 120000000.0, 20000000.0, 8333333.0, "2025-12-31", "Khả thi", Arrays.asList(
                "Mục tiêu hoàn toàn khả thi với thu nhập hiện tại",
                "Thiết lập tự động chuyển tiền tiết kiệm",
                "Tìm tài khoản tiết kiệm lãi suất cao"
            )));
        } else {
            // Process input goals
            for (Map<String, Object> goal : inputGoals) {
                String name = (String) goal.getOrDefault("name", "Mục tiêu chưa đặt tên");
                Double targetAmount = Double.parseDouble(goal.getOrDefault("target_amount", "0").toString());
                Double currentAmount = Double.parseDouble(goal.getOrDefault("current_amount", "0").toString());
                
                // Mock calculation
                double monthlyRequired = (targetAmount - currentAmount) / 12; // 12 months
                String feasibility = monthlyRequired < 5000000 ? "Khả thi" : (monthlyRequired < 10000000 ? "Khó khăn" : "Không khả thi");
                
                List<String> recommendations = Arrays.asList(
                    "Đánh giá tính khả thi dựa trên thu nhập hiện tại",
                    "Thiết lập kế hoạch tiết kiệm định kỳ",
                    "Theo dõi tiến độ hàng tháng"
                );
                
                goalPlans.add(createMockGoalPlan(name, targetAmount, currentAmount, monthlyRequired, "2025-12-31", feasibility, recommendations));
            }
        }
        
        return goalPlans;
    }

    private Map<String, Object> createMockGoalPlan(String name, Double targetAmount, Double currentAmount, Double monthlyRequired, String deadline, String feasibility, List<String> recommendations) {
        Map<String, Object> goalPlan = new HashMap<>();
        goalPlan.put("goal_name", name);
        goalPlan.put("target_amount", targetAmount);
        goalPlan.put("current_amount", currentAmount);
        goalPlan.put("monthly_required", monthlyRequired);
        goalPlan.put("deadline", deadline);
        goalPlan.put("feasibility", feasibility);
        goalPlan.put("recommendations", recommendations);
        return goalPlan;
    }

    private Map<String, Object> createMockCategories() {
        Map<String, Object> categories = new HashMap<>();
        categories.put("Ăn uống", 2000000);
        categories.put("Di chuyển", 1200000);
        categories.put("Mua sắm", 800000);
        categories.put("Giải trí", 600000);
        categories.put("Y tế", 400000);
        return categories;
    }

    private double calculateMockScore(double savingsRate, int numGoals) {
        double savingsScore = Math.min(40, savingsRate * 2); // Max 40 points
        double goalScore = Math.min(30, numGoals * 10); // Max 30 points
        double baseScore = 30; // Base financial awareness
        return Math.round(savingsScore + goalScore + baseScore);
    }

    private List<String> createMockNextActions() {
        return Arrays.asList(
            "🎯 Thiết lập ngân sách chi tiêu hàng tháng",
            "💰 Tăng tỷ lệ tiết kiệm lên ít nhất 20%",
            "📊 Theo dõi chi tiêu bằng ứng dụng di động",
            "🏦 Mở tài khoản tiết kiệm tự động",
            "📚 Học về đầu tư cơ bản để tăng tài sản",
            "🎯 Đặt mục tiêu tài chính cụ thể cho 6 tháng tới"
        );
    }
}
