package com.example.finance.service;

import com.example.finance.entity.*;
import com.example.finance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportService reportService;

    private User testUser;
    private Category testCategory;
    private Wallet testWallet;
    private LocalDate fromDate;
    private LocalDate toDate;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food");

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setName("Main Wallet");

        fromDate = LocalDate.of(2025, 12, 1);
        toDate = LocalDate.of(2025, 12, 31);
    }

    @Test
    @DisplayName("Should get userId by username successfully")
    void testGetUserIdByUsername_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Long userId = reportService.getUserIdByUsername("testuser");

        // Assert
        assertNotNull(userId);
        assertEquals(1L, userId);
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw exception when user not found by username")
    void testGetUserIdByUsername_NotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            reportService.getUserIdByUsername("nonexistent");
        });
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should generate summary report successfully")
    void testGenerateSummaryReport_Success() {
        // Arrange
        BigDecimal totalIncome = new BigDecimal("5000000");
        BigDecimal totalExpense = new BigDecimal("3000000");

        when(transactionRepository.sumByUserIdAndTypeAndDateBetween(1L, "income", fromDate, toDate))
                .thenReturn(totalIncome);
        when(transactionRepository.sumByUserIdAndTypeAndDateBetween(1L, "expense", fromDate, toDate))
                .thenReturn(totalExpense);

        Transaction tx1 = createTransaction(1L, "expense", new BigDecimal("500000"));
        Transaction tx2 = createTransaction(2L, "income", new BigDecimal("1000000"));
        when(transactionRepository.findByUserIdAndTypeAndDateBetween(1L, "expense", fromDate, toDate))
                .thenReturn(Arrays.asList(tx1));
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(1L, fromDate, toDate))
                .thenReturn(Arrays.asList(tx1, tx2));

        // Act
        Map<String, Object> report = reportService.generateSummaryReport(1L, fromDate, toDate);

        // Assert
        assertNotNull(report);
        assertTrue(report.containsKey("period"));
        assertTrue(report.containsKey("summary"));
        assertTrue(report.containsKey("categoryStats"));
        assertTrue(report.containsKey("recentTransactions"));

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        assertEquals(totalIncome, summary.get("totalIncome"));
        assertEquals(totalExpense, summary.get("totalExpense"));
        assertEquals(new BigDecimal("2000000"), summary.get("balance"));
    }

    @Test
    @DisplayName("Should generate summary report with null income and expense")
    void testGenerateSummaryReport_NullIncomeExpense() {
        // Arrange
        when(transactionRepository.sumByUserIdAndTypeAndDateBetween(1L, "income", fromDate, toDate))
                .thenReturn(null);
        when(transactionRepository.sumByUserIdAndTypeAndDateBetween(1L, "expense", fromDate, toDate))
                .thenReturn(null);
        when(transactionRepository.findByUserIdAndTypeAndDateBetween(1L, "expense", fromDate, toDate))
                .thenReturn(Collections.emptyList());
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(1L, fromDate, toDate))
                .thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> report = reportService.generateSummaryReport(1L, fromDate, toDate);

        // Assert
        assertNotNull(report);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        assertEquals(BigDecimal.ZERO, summary.get("totalIncome"));
        assertEquals(BigDecimal.ZERO, summary.get("totalExpense"));
        assertEquals(BigDecimal.ZERO, summary.get("balance"));
    }

    @Test
    @DisplayName("Should generate transaction report with all filters")
    void testGenerateTransactionReport_WithAllFilters() {
        // Arrange
        String type = "expense";
        Long categoryId = 1L;

        Transaction tx1 = createTransaction(1L, type, new BigDecimal("100000"));
        Transaction tx2 = createTransaction(2L, type, new BigDecimal("200000"));

        when(transactionRepository.findByUserIdAndTypeAndCategoryIdAndDateBetween(
                1L, type, categoryId, fromDate, toDate))
                .thenReturn(Arrays.asList(tx1, tx2));

        // Act
        Map<String, Object> report = reportService.generateTransactionReport(1L, fromDate, toDate, type, categoryId);

        // Assert
        assertNotNull(report);
        assertTrue(report.containsKey("period"));
        assertTrue(report.containsKey("filters"));
        assertTrue(report.containsKey("summary"));
        assertTrue(report.containsKey("transactions"));

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        assertEquals(2, summary.get("totalTransactions"));
        assertEquals(new BigDecimal("300000"), summary.get("totalAmount"));
    }

    @Test
    @DisplayName("Should generate transaction report with type filter only")
    void testGenerateTransactionReport_TypeFilterOnly() {
        // Arrange
        String type = "income";

        Transaction tx = createTransaction(1L, type, new BigDecimal("500000"));
        when(transactionRepository.findByUserIdAndTypeAndDateBetween(1L, type, fromDate, toDate))
                .thenReturn(Arrays.asList(tx));

        // Act
        Map<String, Object> report = reportService.generateTransactionReport(1L, fromDate, toDate, type, null);

        // Assert
        assertNotNull(report);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        assertEquals(1, summary.get("totalTransactions"));
        assertEquals(new BigDecimal("500000"), summary.get("totalAmount"));
    }

    @Test
    @DisplayName("Should generate transaction report with category filter only")
    void testGenerateTransactionReport_CategoryFilterOnly() {
        // Arrange
        Long categoryId = 1L;

        Transaction tx = createTransaction(1L, "expense", new BigDecimal("300000"));
        when(transactionRepository.findByUserIdAndCategoryIdAndDateBetween(1L, categoryId, fromDate, toDate))
                .thenReturn(Arrays.asList(tx));

        // Act
        Map<String, Object> report = reportService.generateTransactionReport(1L, fromDate, toDate, null, categoryId);

        // Assert
        assertNotNull(report);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        assertEquals(1, summary.get("totalTransactions"));
    }

    @Test
    @DisplayName("Should generate transaction report without filters")
    void testGenerateTransactionReport_NoFilters() {
        // Arrange
        Transaction tx = createTransaction(1L, "expense", new BigDecimal("100000"));
        when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(1L, fromDate, toDate))
                .thenReturn(Arrays.asList(tx));

        // Act
        Map<String, Object> report = reportService.generateTransactionReport(1L, fromDate, toDate, null, null);

        // Assert
        assertNotNull(report);
        assertTrue(report.containsKey("transactions"));
    }

    @Test
    @DisplayName("Should generate budget report successfully")
    void testGenerateBudgetReport_Success() {
        // Arrange
        Budget budget1 = createBudget(1L, new BigDecimal("2000000"), new BigDecimal("1500000"));
        Budget budget2 = createBudget(2L, new BigDecimal("1000000"), new BigDecimal("800000"));

        when(budgetRepository.findByUserIdAndMonthAndYearAndIsDeletedFalse(1L, 12, 2025))
                .thenReturn(Arrays.asList(budget1, budget2));

        // Act
        Map<String, Object> report = reportService.generateBudgetReport(1L, 12, 2025);

        // Assert
        assertNotNull(report);
        assertTrue(report.containsKey("period"));
        assertTrue(report.containsKey("summary"));
        assertTrue(report.containsKey("budgets"));

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        assertEquals(new BigDecimal("3000000"), summary.get("totalBudget"));
        assertEquals(new BigDecimal("2300000"), summary.get("totalSpent"));
        assertEquals(new BigDecimal("700000"), summary.get("remaining"));
    }

    @Test
    @DisplayName("Should generate budget report with no budgets")
    void testGenerateBudgetReport_NoBudgets() {
        // Arrange
        when(budgetRepository.findByUserIdAndMonthAndYearAndIsDeletedFalse(1L, 12, 2025))
                .thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> report = reportService.generateBudgetReport(1L, 12, 2025);

        // Assert
        assertNotNull(report);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        assertEquals(BigDecimal.ZERO, summary.get("totalBudget"));
        assertEquals(BigDecimal.ZERO, summary.get("totalSpent"));
    }

    @Test
    @DisplayName("Should generate budget report with null spent amounts")
    void testGenerateBudgetReport_NullSpentAmounts() {
        // Arrange
        Budget budget = createBudget(1L, new BigDecimal("2000000"), null);

        when(budgetRepository.findByUserIdAndMonthAndYearAndIsDeletedFalse(1L, 12, 2025))
                .thenReturn(Arrays.asList(budget));

        // Act
        Map<String, Object> report = reportService.generateBudgetReport(1L, 12, 2025);

        // Assert
        assertNotNull(report);
        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        assertEquals(BigDecimal.ZERO, summary.get("totalSpent"));
    }

    // Helper methods
    private Transaction createTransaction(Long id, String type, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setDate(LocalDate.of(2025, 12, 15));
        tx.setCategory(testCategory);
        tx.setWallet(testWallet);
        tx.setUser(testUser);
        tx.setNote("Test transaction");
        return tx;
    }

    private Budget createBudget(Long id, BigDecimal amount, BigDecimal spentAmount) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setAmount(amount);
        budget.setSpentAmount(spentAmount);
        budget.setMonth(12);
        budget.setYear(2025);
        budget.setCategory(testCategory);
        budget.setUser(testUser);
        return budget;
    }
}
