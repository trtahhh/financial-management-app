package com.example.finance.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Financial Planning Controller
 * Tích hợp AI Planning Service để tư vấn kế hoạch tài chính thông minh
 */
//@RestController
//@RequestMapping("/api/planning")
@CrossOrigin(origins = "*")
public class PlanningController_OLD {

    @Value("${app.ai.service.url:http://localhost:8001}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Kiểm tra trạng thái Planning service
     */
    @GetMapping("/health")
    public ResponseEntity<?> checkPlanningHealth() {
        try {
            String url = aiServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Planning service không khả dụng: " + e.getMessage());
            return ResponseEntity.status(503).body(error);
        }
    }

    /**
     * Phân tích kế hoạch tài chính toàn diện
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeFinancialPlan(@RequestBody Map<String, Object> planningData) {
        try {
            // Validate input data
            if (!planningData.containsKey("transactions") || !planningData.containsKey("monthly_income")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Thiếu thông tin giao dịch hoặc thu nhập hàng tháng");
                return ResponseEntity.badRequest().body(error);
            }

            // Prepare request for AI service
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("transactions", planningData.get("transactions"));
            requestBody.put("monthly_income", planningData.get("monthly_income"));
            requestBody.put("goals", planningData.getOrDefault("goals", new ArrayList<>()));
            requestBody.put("user_id", planningData.getOrDefault("user_id", null));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = aiServiceUrl + "/planning/analyze";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            // Parse response
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            return ResponseEntity.ok(jsonResponse);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi phân tích kế hoạch tài chính: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Lấy insights chi tiêu nhanh
     */
    @PostMapping("/spending-insights")
    public ResponseEntity<?> getSpendingInsights(@RequestBody Map<String, Object> requestData) {
        try {
            // Validate input
            if (!requestData.containsKey("transactions") || !requestData.containsKey("income")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Thiếu thông tin giao dịch hoặc thu nhập");
                return ResponseEntity.badRequest().body(error);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("transactions", requestData.get("transactions"));
            requestBody.put("income", requestData.get("income"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = aiServiceUrl + "/planning/spending-insights";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return ResponseEntity.ok(jsonResponse);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi phân tích chi tiêu: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Lấy gợi ý tiết kiệm
     */
    @PostMapping("/savings-recommendations")
    public ResponseEntity<?> getSavingsRecommendations(@RequestBody Map<String, Object> requestData) {
        try {
            // Validate input
            if (!requestData.containsKey("transactions") || !requestData.containsKey("income")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Thiếu thông tin giao dịch hoặc thu nhập");
                return ResponseEntity.badRequest().body(error);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("transactions", requestData.get("transactions"));
            requestBody.put("income", requestData.get("income"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = aiServiceUrl + "/planning/savings-recommendations";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            return ResponseEntity.ok(jsonResponse);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi tạo gợi ý tiết kiệm: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Tạo kế hoạch tài chính cá nhân hóa
     */
    @PostMapping("/create-personal-plan")
    public ResponseEntity<?> createPersonalPlan(@RequestBody Map<String, Object> planData) {
        try {
            // Extract user financial data
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) planData.getOrDefault("transactions", new ArrayList<>());
            Double monthlyIncome = Double.parseDouble(planData.getOrDefault("monthly_income", "0").toString());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> goals = (List<Map<String, Object>>) planData.getOrDefault("goals", new ArrayList<>());

            // Validate minimum requirements
            if (monthlyIncome <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Thu nhập hàng tháng phải lớn hơn 0");
                return ResponseEntity.badRequest().body(error);
            }

            // Prepare comprehensive request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("transactions", transactions);
            requestBody.put("monthly_income", monthlyIncome);
            requestBody.put("goals", goals);
            requestBody.put("user_id", planData.getOrDefault("user_id", null));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = aiServiceUrl + "/planning/analyze";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            // Add additional metadata
            Map<String, Object> enhancedResponse = new HashMap<>();
            enhancedResponse.put("plan", jsonResponse);
            enhancedResponse.put("created_at", System.currentTimeMillis());
            enhancedResponse.put("plan_type", "comprehensive_financial_plan");
            enhancedResponse.put("status", "success");
            
            return ResponseEntity.ok(enhancedResponse);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi tạo kế hoạch tài chính: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Đánh giá khả năng thực hiện mục tiêu tài chính
     */
    @PostMapping("/evaluate-goals")
    public ResponseEntity<?> evaluateFinancialGoals(@RequestBody Map<String, Object> evaluationData) {
        try {
            // This endpoint processes goal feasibility using the planning service
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> goals = (List<Map<String, Object>>) evaluationData.getOrDefault("goals", new ArrayList<>());
            Double monthlyIncome = Double.parseDouble(evaluationData.getOrDefault("monthly_income", "0").toString());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> currentSpending = (List<Map<String, Object>>) evaluationData.getOrDefault("current_spending", new ArrayList<>());

            if (goals.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Vui lòng cung cấp ít nhất một mục tiêu tài chính");
                return ResponseEntity.badRequest().body(error);
            }

            // Prepare request for goal evaluation
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("transactions", currentSpending);
            requestBody.put("monthly_income", monthlyIncome);
            requestBody.put("goals", goals);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = aiServiceUrl + "/planning/analyze";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());
            
            // Extract only goal-related information
            Map<String, Object> goalEvaluation = new HashMap<>();
            goalEvaluation.put("goal_plans", jsonResponse.get("goal_plans"));
            goalEvaluation.put("overall_feasibility", jsonResponse.get("overall_score"));
            goalEvaluation.put("recommendations", jsonResponse.get("next_actions"));
            goalEvaluation.put("evaluation_timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(goalEvaluation);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Lỗi đánh giá mục tiêu tài chính: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
