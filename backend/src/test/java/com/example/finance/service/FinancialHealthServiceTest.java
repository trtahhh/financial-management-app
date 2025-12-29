package com.example.finance.service;

import com.example.finance.dto.FinancialHealthScoreDTO;
import com.example.finance.entity.*;
import com.example.finance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FinancialHealthService
 */
@ExtendWith(MockitoExtension.class)
class FinancialHealthServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private FinancialHealthHistoryRepository healthHistoryRepository;

    @InjectMocks
    private FinancialHealthService financialHealthService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
    }

    @Test
    void testCalculateHealthScore_WithNoData() {
        // Arrange
        Long userId = 1L;
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(
            eq(userId), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(new ArrayList<>());
        
        when(budgetRepository.findByUserIdAndIsDeletedFalse(userId))
            .thenReturn(new ArrayList<>());
        
        when(goalRepository.findByUserId(userId))
            .thenReturn(new ArrayList<>());

        // Act
        FinancialHealthScoreDTO result = financialHealthService.calculateHealthScore(userId);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTotalScore());
        assertNotNull(result.getRating());
        assertNotNull(result.getCalculatedAt());
        
        verify(transactionRepository, times(1)).findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any());
        verify(budgetRepository, times(1)).findByUserIdAndIsDeletedFalse(userId);
        verify(goalRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testCalculateHealthScore_WithTransactions() {
        // Arrange
        Long userId = 1L;
        
        // Create sample transaction
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAmount(new BigDecimal("100000"));
        transaction.setType("EXPENSE");
        transaction.setDate(LocalDate.now());
        
        List<Transaction> transactions = Arrays.asList(transaction);
        
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(
            eq(userId), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(transactions);
        
        when(budgetRepository.findByUserIdAndIsDeletedFalse(userId))
            .thenReturn(new ArrayList<>());
        
        when(goalRepository.findByUserId(userId))
            .thenReturn(new ArrayList<>());

        // Act
        FinancialHealthScoreDTO result = financialHealthService.calculateHealthScore(userId);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTotalScore());
        assertNotNull(result.getBreakdown());
        assertNotNull(result.getSummary());
        assertTrue(result.getTotalScore() >= 0);
        assertTrue(result.getTotalScore() <= 100);
    }

    @Test
    void testCalculateHealthScore_WithBudgetsAndGoals() {
        // Arrange
        Long userId = 1L;
        
        Budget budget = new Budget();
        budget.setId(1L);
        budget.setAmount(new BigDecimal("1000000"));
        
        Goal goal = new Goal();
        goal.setId(1L);
        goal.setTargetAmount(new BigDecimal("5000000"));
        goal.setCurrentAmount(new BigDecimal("1000000"));
        goal.setStatus("ACTIVE");
        
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(
            eq(userId), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(new ArrayList<>());
        
        when(budgetRepository.findByUserIdAndIsDeletedFalse(userId))
            .thenReturn(Arrays.asList(budget));
        
        when(goalRepository.findByUserId(userId))
            .thenReturn(Arrays.asList(goal));

        // Act
        FinancialHealthScoreDTO result = financialHealthService.calculateHealthScore(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.getTotalScore() >= 0);
        assertNotNull(result.getRecommendations());
    }

    @Test
    void testGetHealthScoreTrend_ReturnsEmptyList() {
        // Arrange
        Long userId = 1L;
        int months = 6;

        // Act
        List<Map<String, Object>> result = financialHealthService.getHealthScoreTrend(userId, months);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}
