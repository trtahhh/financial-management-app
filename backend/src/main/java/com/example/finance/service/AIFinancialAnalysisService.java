package com.example.finance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.finance.entity.*;
import com.example.finance.repository.*;
import com.example.finance.dto.*;
import com.example.finance.service.OpenRouterService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Slf4j
public class AIFinancialAnalysisService {

    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private WalletRepository walletRepository;
    
    @Autowired
    private OpenRouterService openRouterService;

    /**
     * Phân tích tài chính toàn diện
     */
    public Map<String, Object> comprehensiveAnalysis(Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("analysis", analyzePersonalFinance(userId));
            result.put("timestamp", System.currentTimeMillis());
            result.put("type", "comprehensive");
            return result;
        } catch (Exception e) {
            log.error("Error in comprehensive analysis", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Không thể thực hiện phân tích toàn diện: " + e.getMessage());
            return error;
        }
    }

    /**
     * Dự báo tài chính
     */
    public Map<String, Object> financialPrediction(Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("prediction", predictFinancialFuture(userId, 12));
            result.put("timestamp", System.currentTimeMillis());
            result.put("type", "prediction");
            return result;
        } catch (Exception e) {
            log.error("Error in financial prediction", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Không thể thực hiện dự báo: " + e.getMessage());
            return error;
        }
    }

    /**
     * Phân tích xu hướng chi tiêu
     */
    public Map<String, Object> spendingTrendAnalysis(Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("trends", analyzeSpendingTrends(userId, 6));
            result.put("timestamp", System.currentTimeMillis());
            result.put("type", "trend");
            return result;
        } catch (Exception e) {
            log.error("Error in spending trend analysis", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Không thể phân tích xu hướng: " + e.getMessage());
            return error;
        }
    }

    /**
     * Tối ưu hóa ngân sách
     */
    public Map<String, Object> budgetOptimization(Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("optimization", optimizeBudget(userId));
            result.put("timestamp", System.currentTimeMillis());
            result.put("type", "budget");
            return result;
        } catch (Exception e) {
            log.error("Error in budget optimization", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Không thể tối ưu ngân sách: " + e.getMessage());
            return error;
        }
    }

    /**
     * Đánh giá rủi ro tài chính
     */
    public Map<String, Object> riskAssessment(Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("risk", assessFinancialRisk(userId));
            result.put("timestamp", System.currentTimeMillis());
            result.put("type", "risk");
            return result;
        } catch (Exception e) {
            log.error("Error in risk assessment", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Không thể đánh giá rủi ro: " + e.getMessage());
            return error;
        }
    }

    /**
     * Lời khuyên đầu tư
     */
    public Map<String, Object> investmentAdvice(Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("advice", getPersonalizedInvestmentAdvice(userId));
            result.put("timestamp", System.currentTimeMillis());
            result.put("type", "investment");
            return result;
        } catch (Exception e) {
            log.error("Error in investment advice", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Không thể đưa ra lời khuyên đầu tư: " + e.getMessage());
            return error;
        }
    }

    /**
     * Phân tích tài chính toàn diện dựa trên dữ liệu thực tế
     */
    @Transactional(readOnly = true)
    public String analyzePersonalFinance(Long userId) {
        try {
            Map<String, Object> financialData = collectFinancialData(userId);
            String prompt = createAnalysisPrompt(financialData);
            String aiAnalysis = openRouterService.chat(prompt);
            return combineAnalysisWithData(aiAnalysis, financialData);
        } catch (Exception e) {
            log.error("Error analyzing personal finance: ", e);
            return "Xin lỗi, không thể phân tích tài chính lúc này. Vui lòng thử lại sau.";
        }
    }

    /**
     * Dự báo tài chính dựa trên AI
     */
    @Transactional(readOnly = true)
    public String predictFinancialFuture(Long userId, int months) {
        try {
            Map<String, Object> historicalData = collectHistoricalData(userId, months);
            String prompt = createPredictionPrompt(historicalData, months);
            String aiPrediction = openRouterService.chat(prompt);
            return formatPredictionResponse(aiPrediction, historicalData);
        } catch (Exception e) {
            log.error("Error predicting financial future: ", e);
            return "Xin lỗi, không thể dự báo tài chính lúc này. Vui lòng thử lại sau.";
        }
    }

    /**
     * Phân tích xu hướng chi tiêu thông minh
     */
    @Transactional(readOnly = true)
    public String analyzeSpendingTrends(Long userId, int months) {
        try {
                    List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(
            userId,
            LocalDate.now().minusMonths(months),
            LocalDate.now()
        );
            
            Map<String, Object> trendData = analyzeTrends(transactions);
            String prompt = createTrendAnalysisPrompt(trendData);
            String aiAnalysis = openRouterService.chat(prompt);
            
            return formatTrendAnalysis(aiAnalysis, trendData);
        } catch (Exception e) {
            log.error("Error analyzing spending trends: ", e);
            return "Xin lỗi, không thể phân tích xu hướng chi tiêu lúc này.";
        }
    }

    /**
     * Tư vấn tối ưu hóa ngân sách
     */
    @Transactional(readOnly = true)
    public String optimizeBudget(Long userId) {
        try {
                    List<Budget> budgets = budgetRepository.findByUserIdAndIsDeletedFalse(userId);
        List<Transaction> recentTransactions = transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(
            userId,
            LocalDate.now().minusMonths(3),
            LocalDate.now()
        );
            
            Map<String, Object> budgetData = analyzeBudgetEfficiency(budgets, recentTransactions);
            String prompt = createBudgetOptimizationPrompt(budgetData);
            String aiAdvice = openRouterService.chat(prompt);
            
            return formatBudgetAdvice(aiAdvice, budgetData);
        } catch (Exception e) {
            log.error("Error optimizing budget: ", e);
            return "Xin lỗi, không thể tối ưu hóa ngân sách lúc này.";
        }
    }

    /**
     * Phân tích rủi ro tài chính
     */
    @Transactional(readOnly = true)
    public String analyzeFinancialRisk(Long userId) {
        try {
            Map<String, Object> riskData = assessFinancialRisk(userId);
            String prompt = createRiskAnalysisPrompt(riskData);
            String aiRiskAnalysis = openRouterService.chat(prompt);
            
            return formatRiskAnalysis(aiRiskAnalysis, riskData);
        } catch (Exception e) {
            log.error("Error analyzing financial risk: ", e);
            return "Xin lỗi, không thể phân tích rủi ro tài chính lúc này.";
        }
    }

    /**
     * Gợi ý đầu tư cá nhân hóa
     */
    @Transactional(readOnly = true)
    public String getPersonalizedInvestmentAdvice(Long userId) {
        try {
            Map<String, Object> investmentProfile = createInvestmentProfile(userId);
            String prompt = createInvestmentAdvicePrompt(investmentProfile);
            String aiAdvice = openRouterService.chat(prompt);
            
            return formatInvestmentAdvice(aiAdvice, investmentProfile);
        } catch (Exception e) {
            log.error("Error getting investment advice: ", e);
            return "Xin lỗi, không thể đưa ra lời khuyên đầu tư lúc này.";
        }
    }

    // Helper methods
    private Map<String, Object> collectFinancialData(Long userId) {
        Map<String, Object> data = new HashMap<>();
        
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(
            userId,
            LocalDate.now().minusMonths(6),
            LocalDate.now()
        );
        
        BigDecimal totalIncome = transactions.stream()
            .filter(t -> t.getType().equals("INCOME"))
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        BigDecimal totalExpense = transactions.stream()
            .filter(t -> t.getType().equals("EXPENSE"))
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        data.put("totalIncome", totalIncome);
        data.put("totalExpense", totalExpense);
        data.put("netIncome", totalIncome.subtract(totalExpense));
        data.put("savingsRate", totalIncome.compareTo(BigDecimal.ZERO) > 0 ? 
            totalIncome.subtract(totalExpense).divide(totalIncome, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) : 
            BigDecimal.ZERO);
        
        List<Budget> budgets = budgetRepository.findByUserIdAndIsDeletedFalse(userId);
        List<Goal> goals = goalRepository.findByUserId(userId);
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        
        data.put("budgetCount", budgets.size());
        data.put("goalCount", goals.size());
        data.put("totalBalance", wallets.stream()
            .map(Wallet::getBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        data.put("walletCount", wallets.size());
        
        return data;
    }

    private Map<String, Object> collectHistoricalData(Long userId, int months) {
        Map<String, Object> data = new HashMap<>();
        data.put("months", months);
        return data;
    }

    private Map<String, Object> analyzeTrends(List<Transaction> transactions) {
        Map<String, Object> trends = new HashMap<>();
        trends.put("transactionCount", transactions.size());
        return trends;
    }

    private Map<String, Object> analyzeBudgetEfficiency(List<Budget> budgets, List<Transaction> transactions) {
        Map<String, Object> efficiency = new HashMap<>();
        efficiency.put("totalBudgets", budgets.size());
        return efficiency;
    }

    private Map<String, Object> assessFinancialRisk(Long userId) {
        Map<String, Object> risk = new HashMap<>();
        risk.put("riskLevel", "TRUNG BÌNH");
        risk.put("savingsRate", new BigDecimal("15"));
        return risk;
    }

    private Map<String, Object> createInvestmentProfile(Long userId) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("investmentProfile", "CÂN BẰNG");
        profile.put("monthlySavings", new BigDecimal("5000000"));
        return profile;
    }

    // Prompt creation methods
    private String createAnalysisPrompt(Map<String, Object> data) {
        return "Bạn là chuyên gia tài chính cá nhân. Hãy phân tích tình hình tài chính và đưa ra lời khuyên cụ thể.";
    }

    private String createPredictionPrompt(Map<String, Object> data, int months) {
        return "Bạn là chuyên gia dự báo tài chính. Hãy đưa ra dự báo và lời khuyên cho " + months + " tháng tới.";
    }

    private String createTrendAnalysisPrompt(Map<String, Object> data) {
        return "Bạn là chuyên gia phân tích xu hướng tài chính. Hãy phân tích xu hướng và đưa ra lời khuyên.";
    }

    private String createBudgetOptimizationPrompt(Map<String, Object> data) {
        return "Bạn là chuyên gia tối ưu hóa ngân sách. Hãy đưa ra lời khuyên tối ưu hóa ngân sách.";
    }

    private String createRiskAnalysisPrompt(Map<String, Object> data) {
        return "Bạn là chuyên gia đánh giá rủi ro tài chính. Hãy phân tích rủi ro và đưa ra biện pháp giảm thiểu.";
    }

    private String createInvestmentAdvicePrompt(Map<String, Object> data) {
        return "Bạn là chuyên gia tư vấn đầu tư. Hãy đưa ra lời khuyên đầu tư phù hợp với profile.";
    }

    // Response formatting methods
    private String combineAnalysisWithData(String aiAnalysis, Map<String, Object> data) {
        return String.format(
            "**PHÂN TÍCH TÀI CHÍNH TOÀN DIỆN**\n\n" +
            "%s\n\n" +
            "**📊 DỮ LIỆU THỰC TẾ:**\n" +
            "• Tổng thu nhập: %s VND\n" +
            "• Tổng chi tiêu: %s VND\n" +
            "• Thu nhập ròng: %s VND\n" +
            "• Tỷ lệ tiết kiệm: %s%%\n" +
            "• Tổng số dư: %s VND\n\n" +
            "**💡 GỢI Ý TIẾP THEO:**\n" +
            "• 'Dự báo tài chính 6 tháng tới'\n" +
            "• 'Phân tích xu hướng chi tiêu'\n" +
            "• 'Tối ưu hóa ngân sách'\n" +
            "• 'Phân tích rủi ro tài chính'\n" +
            "• 'Lời khuyên đầu tư cá nhân'",
            aiAnalysis,
            data.get("totalIncome"),
            data.get("totalExpense"),
            data.get("netIncome"),
            data.get("savingsRate"),
            data.get("totalBalance")
        );
    }

    private String formatPredictionResponse(String aiPrediction, Map<String, Object> data) {
        return String.format(
            "**🔮 DỰ BÁO TÀI CHÍNH TƯƠNG LAI**\n\n" +
            "%s\n\n" +
            "**💡 GỢI Ý TIẾP THEO:**\n" +
            "• 'Phân tích xu hướng chi tiêu'\n" +
            "• 'Tối ưu hóa ngân sách'\n" +
            "• 'Phân tích rủi ro tài chính'\n" +
            "• 'Lời khuyên đầu tư cá nhân'",
            aiPrediction
        );
    }

    private String formatTrendAnalysis(String aiAnalysis, Map<String, Object> data) {
        return String.format(
            "**📊 PHÂN TÍCH XU HƯỚNG CHI TIÊU**\n\n" +
            "%s\n\n" +
            "**💡 GỢI Ý TIẾP THEO:**\n" +
            "• 'Dự báo tài chính tương lai'\n" +
            "• 'Tối ưu hóa ngân sách'\n" +
            "• 'Phân tích rủi ro tài chính'\n" +
            "• 'Lời khuyên đầu tư cá nhân'",
            aiAnalysis
        );
    }

    private String formatBudgetAdvice(String aiAdvice, Map<String, Object> data) {
        return String.format(
            "**💰 TỐI ƯU HÓA NGÂN SÁCH**\n\n" +
            "%s\n\n" +
            "**💡 GỢI Ý TIẾP THEO:**\n" +
            "• 'Phân tích xu hướng chi tiêu'\n" +
            "• 'Dự báo tài chính tương lai'\n" +
            "• 'Phân tích rủi ro tài chính'\n" +
            "• 'Lời khuyên đầu tư cá nhân'",
            aiAdvice
        );
    }

    private String formatRiskAnalysis(String aiAnalysis, Map<String, Object> data) {
        return String.format(
            "**⚠️ PHÂN TÍCH RỦI RO TÀI CHÍNH**\n\n" +
            "%s\n\n" +
            "**💡 GỢI Ý TIẾP THEO:**\n" +
            "• 'Tối ưu hóa ngân sách'\n" +
            "• 'Dự báo tài chính tương lai'\n" +
            "• 'Lời khuyên đầu tư cá nhân'",
            aiAnalysis
        );
    }

    private String formatInvestmentAdvice(String aiAdvice, Map<String, Object> data) {
        return String.format(
            "**💎 LỜI KHUYÊN ĐẦU TƯ CÁ NHÂN**\n\n" +
            "%s\n\n" +
            "**💡 GỢI Ý TIẾP THEO:**\n" +
            "• 'Phân tích xu hướng chi tiêu'\n" +
            "• 'Tối ưu hóa ngân sách'\n" +
            "• 'Dự báo tài chính tương lai'\n" +
            "• 'Phân tích rủi ro tài chính'",
            aiAdvice
        );
    }
}
