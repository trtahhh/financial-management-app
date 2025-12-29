package com.example.finance.service;

import com.example.finance.entity.Budget;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.repository.BudgetRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.service.OverspendingDetectionService.OverspendingAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Overspending Detection Service Tests")
class OverspendingDetectionServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private OverspendingDetectionService overspendingDetectionService;

    private User testUser;
    private Category testCategory;
    private Budget testBudget;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food");

        testBudget = new Budget();
        testBudget.setId(1L);
        testBudget.setUser(testUser);
        testBudget.setCategory(testCategory);
        testBudget.setAmount(new BigDecimal("5000000"));
        testBudget.setMonth(12);
        testBudget.setYear(2025);

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setUser(testUser);
        testTransaction.setCategory(testCategory);
        testTransaction.setAmount(new BigDecimal("1000000"));
        testTransaction.setType("expense");
        testTransaction.setDate(LocalDate.of(2025, 12, 15));
        testTransaction.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should detect critical overspending (>=100%)")
    void testDetectOverspending_Critical() {
        // Arrange
        List<Budget> budgets = Arrays.asList(testBudget);
        
        Transaction existingTx = createTransaction(new BigDecimal("4000000"));
        existingTx.setCreatedAt(LocalDateTime.now().minusDays(5));
        
        when(budgetRepository.findByUserId(1L)).thenReturn(budgets);
        when(transactionRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(existingTx));

        // Act - new transaction adds 1M, total becomes 5M (100%)
        testTransaction.setAmount(new BigDecimal("1000000"));
        OverspendingAlert result = overspendingDetectionService.detectOverspending(testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("critical", result.getSeverity());
        assertTrue(result.getSpentPercentage() >= 100.0);
        assertEquals("Food", result.getCategoryName());
        assertNotNull(result.getRecommendations());
        assertFalse(result.getRecommendations().isEmpty());
    }

    @Test
    @DisplayName("Should detect warning overspending (>=80%)")
    void testDetectOverspending_Warning() {
        // Arrange
        List<Budget> budgets = Arrays.asList(testBudget);
        
        Transaction existingTx = createTransaction(new BigDecimal("3000000"));
        existingTx.setCreatedAt(LocalDateTime.now().minusDays(5));
        
        when(budgetRepository.findByUserId(1L)).thenReturn(budgets);
        when(transactionRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(existingTx));

        // Act - new transaction adds 1M, total becomes 4M (80%)
        testTransaction.setAmount(new BigDecimal("1000000"));
        OverspendingAlert result = overspendingDetectionService.detectOverspending(testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("warning", result.getSeverity());
        assertTrue(result.getSpentPercentage() >= 80.0);
        assertTrue(result.getSpentPercentage() < 100.0);
        assertNotNull(result.getRecommendations());
    }

    @Test
    @DisplayName("Should detect info level (>=60%)")
    void testDetectOverspending_Info() {
        // Arrange
        List<Budget> budgets = Arrays.asList(testBudget);
        
        Transaction existingTx = createTransaction(new BigDecimal("2000000"));
        existingTx.setCreatedAt(LocalDateTime.now().minusDays(5));
        
        when(budgetRepository.findByUserId(1L)).thenReturn(budgets);
        when(transactionRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(existingTx));

        // Act - new transaction adds 1M, total becomes 3M (60%)
        testTransaction.setAmount(new BigDecimal("1000000"));
        OverspendingAlert result = overspendingDetectionService.detectOverspending(testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("info", result.getSeverity());
        assertTrue(result.getSpentPercentage() >= 60.0);
        assertTrue(result.getSpentPercentage() < 80.0);
    }

    @Test
    @DisplayName("Should return no alert when spending is below 60%")
    void testDetectOverspending_BelowThreshold() {
        // Arrange
        List<Budget> budgets = Arrays.asList(testBudget);
        
        Transaction existingTx = createTransaction(new BigDecimal("1000000"));
        existingTx.setCreatedAt(LocalDateTime.now().minusDays(5));
        
        when(budgetRepository.findByUserId(1L)).thenReturn(budgets);
        when(transactionRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(existingTx));

        // Act - new transaction adds 1M, total becomes 2M (40% < 60%)
        testTransaction.setAmount(new BigDecimal("1000000"));
        OverspendingAlert result = overspendingDetectionService.detectOverspending(testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("none", result.getSeverity());
    }

    @Test
    @DisplayName("Should return no budget alert when no budget exists")
    void testDetectOverspending_NoBudget() {
        // Arrange
        when(budgetRepository.findByUserId(1L)).thenReturn(Arrays.asList());

        // Act
        OverspendingAlert result = overspendingDetectionService.detectOverspending(testTransaction);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCategoryName());
        assertEquals("Food", result.getCategoryName());
    }

    @Test
    @DisplayName("Should handle over 100% spending correctly")
    void testDetectOverspending_Over100Percent() {
        // Arrange
        List<Budget> budgets = Arrays.asList(testBudget);
        
        Transaction existingTx = createTransaction(new BigDecimal("6000000"));
        existingTx.setCreatedAt(LocalDateTime.now().minusDays(5));
        
        when(budgetRepository.findByUserId(1L)).thenReturn(budgets);
        when(transactionRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(existingTx));

        // Act - new transaction adds 1.5M, total becomes 7.5M (150%)
        testTransaction.setAmount(new BigDecimal("1500000"));
        OverspendingAlert result = overspendingDetectionService.detectOverspending(testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("critical", result.getSeverity());
        assertTrue(result.getSpentPercentage() > 100.0);
        assertTrue(result.getRemaining() < 0); // Negative remaining
    }

    @Test
    @DisplayName("Should calculate remaining budget correctly")
    void testDetectOverspending_RemainingBudget() {
        // Arrange
        List<Budget> budgets = Arrays.asList(testBudget);
        
        Transaction existingTx = createTransaction(new BigDecimal("2500000"));
        existingTx.setCreatedAt(LocalDateTime.now().minusDays(5));
        
        when(budgetRepository.findByUserId(1L)).thenReturn(budgets);
        when(transactionRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(existingTx));

        // Act - new transaction adds 1M, total becomes 3.5M (70%), remaining = 1.5M
        testTransaction.setAmount(new BigDecimal("1000000"));
        OverspendingAlert result = overspendingDetectionService.detectOverspending(testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("info", result.getSeverity()); // 70% is info level
        assertEquals(1500000.0, result.getRemaining(), 0.01);
    }

    @Test
    @DisplayName("Should only detect for expense transactions")
    void testDetectOverspending_IncomeTransaction() {
        // Arrange
        testTransaction.setType("income");

        // Act
        OverspendingAlert result = overspendingDetectionService.detectOverspending(testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("none", result.getSeverity());
    }

    @Test
    @DisplayName("Should handle null category gracefully")
    void testDetectOverspending_NullCategory() {
        // Arrange
        testTransaction.setCategory(null);

        // Act
        OverspendingAlert result = overspendingDetectionService.detectOverspending(testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("none", result.getSeverity());
    }

    @Test
    @DisplayName("Should include category name in alert")
    void testDetectOverspending_CategoryName() {
        // Arrange
        List<Budget> budgets = Arrays.asList(testBudget);
        
        Transaction existingTx = createTransaction(new BigDecimal("3000000"));
        existingTx.setCreatedAt(LocalDateTime.now().minusDays(5));
        
        when(budgetRepository.findByUserId(1L)).thenReturn(budgets);
        when(transactionRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(Arrays.asList(existingTx));

        // Act - new transaction adds 1M, total becomes 4M (80%)
        testTransaction.setAmount(new BigDecimal("1000000"));
        OverspendingAlert result = overspendingDetectionService.detectOverspending(testTransaction);

        // Assert
        assertNotNull(result);
        assertEquals("Food", result.getCategoryName());
    }

    private Transaction createTransaction(BigDecimal amount) {
        Transaction t = new Transaction();
        t.setUser(testUser);
        t.setCategory(testCategory);
        t.setAmount(amount);
        t.setType("expense");
        t.setDate(LocalDate.of(2025, 12, 15));
        return t;
    }
}
