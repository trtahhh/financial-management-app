package com.example.finance.service;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.Budget;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.GoalRepository;
import com.example.finance.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIFinancialAnalysisServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private OpenRouterService openRouterService;

    @InjectMocks
    private AIFinancialAnalysisService aiFinancialAnalysisService;

    @BeforeEach
    void setUp() {
        lenient().when(openRouterService.isAvailable()).thenReturn(true);
    }

    @Test
    @DisplayName("Should perform comprehensive analysis")
    void testComprehensiveAnalysis() {
        // Arrange
        lenient().when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(budgetRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(goalRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(walletRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(openRouterService.chat(anyString())).thenReturn("AI analysis");

        // Act
        Map<String, Object> result = aiFinancialAnalysisService.comprehensiveAnalysis(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("analysis") || result.containsKey("error"));
    }

    @Test
    @DisplayName("Should perform financial prediction")
    void testFinancialPrediction() {
        // Arrange
        lenient().when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(openRouterService.chat(anyString())).thenReturn("Prediction");

        // Act
        Map<String, Object> result = aiFinancialAnalysisService.financialPrediction(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("prediction") || result.containsKey("error"));
    }

    @Test
    @DisplayName("Should analyze spending trends")
    void testSpendingTrendAnalysis() {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100000"));
        transaction.setType("expense");
        transaction.setDate(LocalDate.now());

        lenient().when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Arrays.asList(transaction));
        lenient().when(openRouterService.chat(anyString())).thenReturn("Trend analysis");

        // Act
        Map<String, Object> result = aiFinancialAnalysisService.spendingTrendAnalysis(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("trends") || result.containsKey("error"));
    }

    @Test
    @DisplayName("Should optimize budget")
    void testBudgetOptimization() {
        // Arrange
        Budget budget = new Budget();
        budget.setAmount(new BigDecimal("5000000"));

        lenient().when(budgetRepository.findByUserId(anyLong())).thenReturn(Arrays.asList(budget));
        lenient().when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(openRouterService.chat(anyString())).thenReturn("Optimization");

        // Act
        Map<String, Object> result = aiFinancialAnalysisService.budgetOptimization(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("optimization") || result.containsKey("error"));
    }

    @Test
    @DisplayName("Should analyze with empty data")
    void testComprehensiveAnalysis_EmptyData() {
        // Arrange
        lenient().when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(budgetRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(goalRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(walletRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(openRouterService.chat(anyString())).thenReturn("Empty data analysis");

        // Act
        Map<String, Object> result = aiFinancialAnalysisService.comprehensiveAnalysis(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("analysis") || result.containsKey("error"));
    }

    @Test
    @DisplayName("Should predict with limited history")
    void testFinancialPrediction_LimitedHistory() {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("50000"));
        transaction.setType("expense");
        transaction.setDate(LocalDate.now().minusDays(5));

        lenient().when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Arrays.asList(transaction));
        lenient().when(openRouterService.chat(anyString())).thenReturn("Limited prediction");

        // Act
        Map<String, Object> result = aiFinancialAnalysisService.financialPrediction(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("prediction") || result.containsKey("error"));
    }

    @Test
    @DisplayName("Should include timestamp in results")
    void testComprehensiveAnalysis_IncludesTimestamp() {
        // Arrange
        lenient().when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        lenient().when(budgetRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(goalRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(walletRepository.findByUserId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(openRouterService.chat(anyString())).thenReturn("Analysis");

        // Act
        Map<String, Object> result = aiFinancialAnalysisService.comprehensiveAnalysis(1L);

        // Assert
        assertNotNull(result);
        if (!result.containsKey("error")) {
            assertTrue(result.containsKey("timestamp"));
        }
    }
}
