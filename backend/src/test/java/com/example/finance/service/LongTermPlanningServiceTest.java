package com.example.finance.service;

import com.example.finance.entity.Transaction;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LongTermPlanningServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LongTermPlanningService longTermPlanningService;

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.existsById(anyLong())).thenReturn(true);
    }

    @Test
    @DisplayName("Should create 3-month long term plan")
    void testCreateLongTermPlan_ThreeMonths() {
        // Arrange
        List<Transaction> transactions = createMockTransactions();
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            anyLong(), any(LocalDateTime.class)))
            .thenReturn(transactions);

        // Act
        LongTermPlanningService.LongTermPlan plan = 
            longTermPlanningService.createLongTermPlan(1L, 3, 5000000.0);

        // Assert
        assertNotNull(plan);
        assertEquals(3, plan.getPlanMonths());
        assertEquals(5000000.0, plan.getTargetSavings());
        assertNotNull(plan.getFeasibility());
        assertNotNull(plan.getCategoryPlans());
        assertNotNull(plan.getMilestones());
    }

    @Test
    @DisplayName("Should create 6-month long term plan")
    void testCreateLongTermPlan_SixMonths() {
        // Arrange
        List<Transaction> transactions = createMockTransactions();
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            anyLong(), any(LocalDateTime.class)))
            .thenReturn(transactions);

        // Act
        LongTermPlanningService.LongTermPlan plan = 
            longTermPlanningService.createLongTermPlan(1L, 6, 10000000.0);

        // Assert
        assertNotNull(plan);
        assertEquals(6, plan.getPlanMonths());
        assertEquals(10000000.0, plan.getTargetSavings());
    }

    @Test
    @DisplayName("Should create 12-month long term plan")
    void testCreateLongTermPlan_TwelveMonths() {
        // Arrange
        List<Transaction> transactions = createMockTransactions();
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            anyLong(), any(LocalDateTime.class)))
            .thenReturn(transactions);

        // Act
        LongTermPlanningService.LongTermPlan plan = 
            longTermPlanningService.createLongTermPlan(1L, 12, 20000000.0);

        // Assert
        assertNotNull(plan);
        assertEquals(12, plan.getPlanMonths());
        assertEquals(20000000.0, plan.getTargetSavings());
    }

    @Test
    @DisplayName("Should throw exception for invalid plan months")
    void testCreateLongTermPlan_InvalidMonths() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            longTermPlanningService.createLongTermPlan(1L, 5, 5000000.0)
        );
    }

    @Test
    @DisplayName("Should throw exception for non-existent user")
    void testCreateLongTermPlan_UserNotFound() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            longTermPlanningService.createLongTermPlan(1L, 6, 5000000.0)
        );
    }

    @Test
    @DisplayName("Should handle empty transaction history")
    void testCreateLongTermPlan_EmptyHistory() {
        // Arrange
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            anyLong(), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // Act
        LongTermPlanningService.LongTermPlan plan = 
            longTermPlanningService.createLongTermPlan(1L, 6, 5000000.0);

        // Assert
        assertNotNull(plan);
        assertTrue(plan.getAvgMonthlyIncome() >= 0);
    }

    @Test
    @DisplayName("Should assess plan feasibility")
    void testCreateLongTermPlan_Feasibility() {
        // Arrange
        List<Transaction> transactions = createMockTransactions();
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            anyLong(), any(LocalDateTime.class)))
            .thenReturn(transactions);

        // Act
        LongTermPlanningService.LongTermPlan plan = 
            longTermPlanningService.createLongTermPlan(1L, 6, 3000000.0);

        // Assert
        assertNotNull(plan.getFeasibility());
        assertNotNull(plan.getFeasibility().getLevel());
        assertTrue(plan.getSuccessProbability() >= 0 && plan.getSuccessProbability() <= 100);
    }

    @Test
    @DisplayName("Should suggest savings path")
    void testSuggestSavingsPath() {
        // Arrange
        List<Transaction> transactions = createMockTransactions();
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            anyLong(), any(LocalDateTime.class)))
            .thenReturn(transactions);

        // Act
        LongTermPlanningService.SavingsPathSuggestion suggestion = 
            longTermPlanningService.suggestSavingsPath(1L, 5000000.0, "Mua xe");

        // Assert
        assertNotNull(suggestion);
        assertEquals(5000000.0, suggestion.getTargetAmount());
        assertEquals("Mua xe", suggestion.getPurpose());
        assertNotNull(suggestion.getPaths());
        assertFalse(suggestion.getPaths().isEmpty());
    }

    @Test
    @DisplayName("Should create multiple savings paths")
    void testSuggestSavingsPath_MultiplePaths() {
        // Arrange
        List<Transaction> transactions = createHighIncomeTransactions();
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            anyLong(), any(LocalDateTime.class)))
            .thenReturn(transactions);

        // Act
        LongTermPlanningService.SavingsPathSuggestion suggestion = 
            longTermPlanningService.suggestSavingsPath(1L, 10000000.0, "Du lịch");

        // Assert
        assertNotNull(suggestion);
        assertTrue(suggestion.getPaths().size() >= 1);
    }

    @Test
    @DisplayName("Should calculate required monthly savings")
    void testCreateLongTermPlan_RequiredSavings() {
        // Arrange
        List<Transaction> transactions = createMockTransactions();
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            anyLong(), any(LocalDateTime.class)))
            .thenReturn(transactions);

        double targetSavings = 6000000.0;
        int planMonths = 6;

        // Act
        LongTermPlanningService.LongTermPlan plan = 
            longTermPlanningService.createLongTermPlan(1L, planMonths, targetSavings);

        // Assert
        double expectedMonthlySavings = targetSavings / planMonths;
        assertEquals(expectedMonthlySavings, plan.getRequiredMonthlySavings());
    }

    @Test
    @DisplayName("Should generate recommendations")
    void testCreateLongTermPlan_Recommendations() {
        // Arrange
        List<Transaction> transactions = createMockTransactions();
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            anyLong(), any(LocalDateTime.class)))
            .thenReturn(transactions);

        // Act
        LongTermPlanningService.LongTermPlan plan = 
            longTermPlanningService.createLongTermPlan(1L, 6, 5000000.0);

        // Assert
        assertNotNull(plan.getRecommendations());
        assertFalse(plan.getRecommendations().isEmpty());
    }

    @Test
    @DisplayName("Should create emergency strategies")
    void testCreateLongTermPlan_EmergencyStrategies() {
        // Arrange
        List<Transaction> transactions = createMockTransactions();
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            anyLong(), any(LocalDateTime.class)))
            .thenReturn(transactions);

        // Act
        LongTermPlanningService.LongTermPlan plan = 
            longTermPlanningService.createLongTermPlan(1L, 6, 5000000.0);

        // Assert
        assertNotNull(plan.getEmergencyStrategies());
        assertFalse(plan.getEmergencyStrategies().isEmpty());
    }

    @Test
    @DisplayName("Should create monthly milestones")
    void testCreateLongTermPlan_Milestones() {
        // Arrange
        List<Transaction> transactions = createMockTransactions();
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            anyLong(), any(LocalDateTime.class)))
            .thenReturn(transactions);

        // Act
        LongTermPlanningService.LongTermPlan plan = 
            longTermPlanningService.createLongTermPlan(1L, 6, 6000000.0);

        // Assert
        assertNotNull(plan.getMilestones());
        assertEquals(6, plan.getMilestones().size());
    }

    @Test
    @DisplayName("Should handle high savings target")
    void testCreateLongTermPlan_HighTarget() {
        // Arrange
        List<Transaction> transactions = createMockTransactions();
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(
            anyLong(), any(LocalDateTime.class)))
            .thenReturn(transactions);

        // Act
        LongTermPlanningService.LongTermPlan plan = 
            longTermPlanningService.createLongTermPlan(1L, 12, 50000000.0);

        // Assert
        assertNotNull(plan);
        assertEquals(50000000.0, plan.getTargetSavings());
        // Feasibility should indicate difficulty
        assertNotNull(plan.getFeasibility());
    }

    // Helper methods
    private List<Transaction> createMockTransactions() {
        Transaction income1 = createTransaction("income", "10000000", LocalDateTime.now().minusDays(30));
        Transaction income2 = createTransaction("income", "10000000", LocalDateTime.now().minusDays(60));
        Transaction expense1 = createTransaction("expense", "3000000", LocalDateTime.now().minusDays(25));
        Transaction expense2 = createTransaction("expense", "2500000", LocalDateTime.now().minusDays(55));

        return Arrays.asList(income1, income2, expense1, expense2);
    }

    private List<Transaction> createHighIncomeTransactions() {
        Transaction income1 = createTransaction("income", "20000000", LocalDateTime.now().minusDays(30));
        Transaction income2 = createTransaction("income", "20000000", LocalDateTime.now().minusDays(60));
        Transaction expense1 = createTransaction("expense", "5000000", LocalDateTime.now().minusDays(25));
        Transaction expense2 = createTransaction("expense", "4500000", LocalDateTime.now().minusDays(55));

        return Arrays.asList(income1, income2, expense1, expense2);
    }

    private Transaction createTransaction(String type, String amount, LocalDateTime createdAt) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCreatedAt(createdAt);
        return transaction;
    }
}
