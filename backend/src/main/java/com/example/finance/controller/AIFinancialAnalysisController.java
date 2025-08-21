package com.example.finance.controller;

import com.example.finance.service.AIFinancialAnalysisService;
import com.example.finance.dto.AIChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-analysis")
@Slf4j
@CrossOrigin(origins = "*")
public class AIFinancialAnalysisController {

    @Autowired
    private AIFinancialAnalysisService aiFinancialAnalysisService;

    /**
     * Phân tích tài chính toàn diện
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> analyzePersonalFinance(@RequestBody AIChatRequest request) {
        try {
            log.info("Received personal finance analysis request");
            
            // TODO: Lấy userId thực tế từ JWT token
            Long userId = 1L; // Tạm thời hardcode
            
            String analysis = aiFinancialAnalysisService.analyzePersonalFinance(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("analysis", analysis);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in personal finance analysis: ", e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Không thể phân tích tài chính lúc này");
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Dự báo tài chính tương lai
     */
    @PostMapping("/predict")
    public ResponseEntity<Map<String, String>> predictFinancialFuture(@RequestBody AIChatRequest request) {
        try {
            log.info("Received financial prediction request");
            
            // TODO: Lấy userId thực tế từ JWT token
            Long userId = 1L; // Tạm thời hardcode
            
            // Mặc định dự báo 6 tháng
            int months = 6;
            if (request.getMessage().toLowerCase().contains("12 tháng") || 
                request.getMessage().toLowerCase().contains("1 năm")) {
                months = 12;
            } else if (request.getMessage().toLowerCase().contains("3 tháng")) {
                months = 3;
            }
            
            String prediction = aiFinancialAnalysisService.predictFinancialFuture(userId, months);
            
            Map<String, String> response = new HashMap<>();
            response.put("prediction", prediction);
            response.put("months", String.valueOf(months));
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in financial prediction: ", e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Không thể dự báo tài chính lúc này");
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Phân tích xu hướng chi tiêu
     */
    @PostMapping("/trends")
    public ResponseEntity<Map<String, String>> analyzeSpendingTrends(@RequestBody AIChatRequest request) {
        try {
            log.info("Received spending trends analysis request");
            
            // TODO: Lấy userId thực tế từ JWT token
            Long userId = 1L; // Tạm thời hardcode
            
            // Mặc định phân tích 6 tháng
            int months = 6;
            if (request.getMessage().toLowerCase().contains("12 tháng") || 
                request.getMessage().toLowerCase().contains("1 năm")) {
                months = 12;
            } else if (request.getMessage().toLowerCase().contains("3 tháng")) {
                months = 3;
            }
            
            String trends = aiFinancialAnalysisService.analyzeSpendingTrends(userId, months);
            
            Map<String, String> response = new HashMap<>();
            response.put("trends", trends);
            response.put("months", String.valueOf(months));
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in spending trends analysis: ", e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Không thể phân tích xu hướng chi tiêu lúc này");
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Tối ưu hóa ngân sách
     */
    @PostMapping("/optimize-budget")
    public ResponseEntity<Map<String, String>> optimizeBudget(@RequestBody AIChatRequest request) {
        try {
            log.info("Received budget optimization request");
            
            // TODO: Lấy userId thực tế từ JWT token
            Long userId = 1L; // Tạm thời hardcode
            
            String optimization = aiFinancialAnalysisService.optimizeBudget(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("optimization", optimization);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in budget optimization: ", e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Không thể tối ưu hóa ngân sách lúc này");
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Phân tích rủi ro tài chính
     */
    @PostMapping("/risk-analysis")
    public ResponseEntity<Map<String, String>> analyzeFinancialRisk(@RequestBody AIChatRequest request) {
        try {
            log.info("Received financial risk analysis request");
            
            // TODO: Lấy userId thực tế từ JWT token
            Long userId = 1L; // Tạm thời hardcode
            
            String riskAnalysis = aiFinancialAnalysisService.analyzeFinancialRisk(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("riskAnalysis", riskAnalysis);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in financial risk analysis: ", e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Không thể phân tích rủi ro tài chính lúc này");
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lời khuyên đầu tư cá nhân hóa
     */
    @PostMapping("/investment-advice")
    public ResponseEntity<Map<String, String>> getInvestmentAdvice(@RequestBody AIChatRequest request) {
        try {
            log.info("Received investment advice request");
            
            // TODO: Lấy userId thực tế từ JWT token
            Long userId = 1L; // Tạm thời hardcode
            
            String investmentAdvice = aiFinancialAnalysisService.getPersonalizedInvestmentAdvice(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("investmentAdvice", investmentAdvice);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in investment advice: ", e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Không thể đưa ra lời khuyên đầu tư lúc này");
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("AI Financial Analysis Controller test endpoint called");
        return ResponseEntity.ok("AI Financial Analysis Controller is working!");
    }
}
