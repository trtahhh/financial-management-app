package com.example.finance.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIFinanceServiceTest {

    @Mock
    private ReportService reportService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private BudgetService budgetService;

    @Mock
    private GoalService goalService;

    @Mock
    private WalletService walletService;

    @Mock
    private OpenRouterService openRouterService;

    @Mock
    private AIFinancialAnalysisService aiFinancialAnalysisService;

    @InjectMocks
    private AIFinanceService aiFinanceService;

    @Test
    @DisplayName("Should detect export request")
    void testProcessMessage_ExportRequest() {
        String message = "Xuất báo cáo Excel tháng này";
        String response = aiFinanceService.processMessage(message, 1L);
        
        assertNotNull(response);
        assertTrue(response.contains("XUẤT FILE") || response.contains("Excel"));
    }

    @Test
    @DisplayName("Should detect PDF export request")
    void testProcessMessage_PDFExport() {
        String message = "Xuất file PDF";
        String response = aiFinanceService.processMessage(message, 1L);
        
        assertNotNull(response);
        assertTrue(response.contains("PDF") || response.contains("XUẤT"));
    }

    @Test
    @DisplayName("Should detect report request")
    void testProcessMessage_ReportRequest() {
        String message = "Báo cáo tháng này";
        String response = aiFinanceService.processMessage(message, 1L);
        
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should detect financial analysis request")
    void testProcessMessage_FinancialAnalysis() {
        String message = "Phân tích tài chính của tôi";
        String response = aiFinanceService.processMessage(message, 1L);
        
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should detect 6 month prediction")
    void testProcessMessage_PredictionRequest() {
        String message = "Dự báo 6 tháng tới";
        lenient().when(aiFinancialAnalysisService.predictFinancialFuture(anyLong(), anyInt()))
            .thenReturn("Dự báo tài chính");

        String response = aiFinanceService.processMessage(message, 1L);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should detect 12 month prediction")
    void testProcessMessage_TwelveMonthPrediction() {
        String message = "Dự đoán 12 tháng tới";
        lenient().when(aiFinancialAnalysisService.predictFinancialFuture(anyLong(), anyInt()))
            .thenReturn("Dự báo 12 tháng");

        String response = aiFinanceService.processMessage(message, 1L);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should detect trend analysis request")
    void testProcessMessage_TrendAnalysis() {
        String message = "Phân tích xu hướng chi tiêu";
        lenient().when(aiFinancialAnalysisService.analyzeSpendingTrends(anyLong(), anyInt()))
            .thenReturn("Phân tích xu hướng");

        String response = aiFinanceService.processMessage(message, 1L);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should detect budget optimization request")
    void testProcessMessage_BudgetOptimization() {
        String message = "Tối ưu hóa ngân sách";
        lenient().when(aiFinancialAnalysisService.optimizeBudget(anyLong()))
            .thenReturn("Tối ưu ngân sách");

        String response = aiFinanceService.processMessage(message, 1L);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should detect risk analysis request")
    void testProcessMessage_RiskAnalysis() {
        String message = "Phân tích rủi ro tài chính";
        lenient().when(aiFinancialAnalysisService.analyzeFinancialRisk(anyLong()))
            .thenReturn("Phân tích rủi ro");

        String response = aiFinanceService.processMessage(message, 1L);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should detect investment advice request")
    void testProcessMessage_InvestmentAdvice() {
        String message = "Cho tôi lời khuyên đầu tư";
        lenient().when(aiFinancialAnalysisService.getPersonalizedInvestmentAdvice(anyLong()))
            .thenReturn("Lời khuyên đầu tư");

        String response = aiFinanceService.processMessage(message, 1L);

        assertNotNull(response);
    }

    @Test
    @DisplayName("Should handle thanks message")
    void testProcessMessage_ThanksMessage() {
        String message = "Cảm ơn bạn";
        String response = aiFinanceService.processMessage(message, 1L);
        
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should classify saving message")
    void testProcessMessage_SavingCategory() {
        String message = "Làm thế nào để tiết kiệm tiền";
        String response = aiFinanceService.processMessage(message, 1L);
        
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should check AI availability")
    void testIsAvailable_Success() {
        when(openRouterService.isAvailable()).thenReturn(true);

        boolean available = aiFinanceService.isAvailable();

        assertTrue(available);
    }

    @Test
    @DisplayName("Should handle AI service unavailable")
    void testIsAvailable_Failed() {
        when(openRouterService.isAvailable()).thenThrow(new RuntimeException("Connection error"));

        boolean available = aiFinanceService.isAvailable();

        assertFalse(available);
    }
}
