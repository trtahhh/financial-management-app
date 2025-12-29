package com.example.finance.service;

import com.example.finance.entity.*;
import com.example.finance.repository.TransactionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for BudgetCalculationService
 * Service for budget calculation utilities
 */
@ExtendWith(MockitoExtension.class)
public class BudgetCalculationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BudgetCalculationService budgetCalculationService;

    private User testUser;
    private Category testCategory;
    private List<Transaction> testTransactions;

    @BeforeEach
    public void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        // Setup test category
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food");
        testCategory.setType("expense");

        // Setup test transactions
        testTransactions = new ArrayList<>();
        
        Transaction tx1 = new Transaction();
        tx1.setId(1L);
        tx1.setUser(testUser);
        tx1.setCategory(testCategory);
        tx1.setAmount(new BigDecimal("500000"));
        tx1.setType("expense");
        tx1.setDate(LocalDate.of(2025, 12, 15));
        testTransactions.add(tx1);

        Transaction tx2 = new Transaction();
        tx2.setId(2L);
        tx2.setUser(testUser);
        tx2.setCategory(testCategory);
        tx2.setAmount(new BigDecimal("300000"));
        tx2.setType("expense");
        tx2.setDate(LocalDate.of(2025, 12, 20));
        testTransactions.add(tx2);
    }

    @Test
    public void testCalculateSpentAmount_Success() {
        // Given: Transactions exist for user and category
        when(transactionRepository.findAllByDateBetween(any(), any())).thenReturn(testTransactions);

        // When: Calculate spent amount
        BigDecimal spent = budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025);

        // Then: Returns correct sum
        assertEquals(new BigDecimal("800000"), spent);
        verify(transactionRepository, times(1)).findAllByDateBetween(any(), any());
    }

    @Test
    public void testCalculateSpentAmount_NoTransactions() {
        // Given: No transactions exist
        when(transactionRepository.findAllByDateBetween(any(), any())).thenReturn(new ArrayList<>());

        // When: Calculate spent amount
        BigDecimal spent = budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025);

        // Then: Returns zero
        assertEquals(BigDecimal.ZERO, spent);
        verify(transactionRepository, times(1)).findAllByDateBetween(any(), any());
    }

    @Test
    public void testCalculateSpentAmount_FiltersByCategory() {
        // Given: Transactions from different categories
        Category otherCategory = new Category();
        otherCategory.setId(2L);
        otherCategory.setName("Transport");

        Transaction tx3 = new Transaction();
        tx3.setId(3L);
        tx3.setUser(testUser);
        tx3.setCategory(otherCategory);
        tx3.setAmount(new BigDecimal("200000"));
        tx3.setType("expense");
        tx3.setDate(LocalDate.of(2025, 12, 25));

        List<Transaction> mixedTransactions = new ArrayList<>(testTransactions);
        mixedTransactions.add(tx3);
        when(transactionRepository.findAllByDateBetween(any(), any())).thenReturn(mixedTransactions);

        // When: Calculate spent for category 1 only
        BigDecimal spent = budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025);

        // Then: Returns sum only for category 1 (excludes category 2)
        assertEquals(new BigDecimal("800000"), spent);
    }

    @Test
    public void testCalculateSpentAmount_FiltersByUser() {
        // Given: Transactions from different users
        User otherUser = new User();
        otherUser.setId(2L);

        Transaction tx3 = new Transaction();
        tx3.setId(3L);
        tx3.setUser(otherUser);
        tx3.setCategory(testCategory);
        tx3.setAmount(new BigDecimal("400000"));
        tx3.setType("expense");
        tx3.setDate(LocalDate.of(2025, 12, 25));

        List<Transaction> mixedTransactions = new ArrayList<>(testTransactions);
        mixedTransactions.add(tx3);
        when(transactionRepository.findAllByDateBetween(any(), any())).thenReturn(mixedTransactions);

        // When: Calculate spent for user 1 only
        BigDecimal spent = budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025);

        // Then: Returns sum only for user 1 (excludes user 2)
        assertEquals(new BigDecimal("800000"), spent);
    }

    @Test
    public void testCalculateSpentAmount_FiltersByType() {
        // Given: Mix of income and expense transactions
        Transaction income = new Transaction();
        income.setId(3L);
        income.setUser(testUser);
        income.setCategory(testCategory);
        income.setAmount(new BigDecimal("1000000"));
        income.setType("income"); // Income, not expense
        income.setDate(LocalDate.of(2025, 12, 25));

        List<Transaction> mixedTransactions = new ArrayList<>(testTransactions);
        mixedTransactions.add(income);
        when(transactionRepository.findAllByDateBetween(any(), any())).thenReturn(mixedTransactions);

        // When: Calculate spent amount (expenses only)
        BigDecimal spent = budgetCalculationService.calculateSpentAmount(1L, 1L, 12, 2025);

        // Then: Returns sum only for expenses (excludes income)
        assertEquals(new BigDecimal("800000"), spent);
    }

    @Test
    public void testCalculateTotalSpentAmount_Success() {
        // Given: Transactions from multiple categories
        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("Transport");

        Transaction tx3 = new Transaction();
        tx3.setId(3L);
        tx3.setUser(testUser);
        tx3.setCategory(category2);
        tx3.setAmount(new BigDecimal("200000"));
        tx3.setType("expense");
        tx3.setDate(LocalDate.of(2025, 12, 25));

        List<Transaction> allTransactions = new ArrayList<>(testTransactions);
        allTransactions.add(tx3);
        when(transactionRepository.findAllByDateBetween(any(), any())).thenReturn(allTransactions);

        // When: Calculate total spent (all categories)
        BigDecimal totalSpent = budgetCalculationService.calculateTotalSpentAmount(1L, 12, 2025);

        // Then: Returns sum across all categories
        assertEquals(new BigDecimal("1000000"), totalSpent);
        verify(transactionRepository, times(1)).findAllByDateBetween(any(), any());
    }

    @Test
    public void testCalculateTotalSpentAmount_NoTransactions() {
        // Given: No transactions exist
        when(transactionRepository.findAllByDateBetween(any(), any())).thenReturn(new ArrayList<>());

        // When: Calculate total spent
        BigDecimal totalSpent = budgetCalculationService.calculateTotalSpentAmount(1L, 12, 2025);

        // Then: Returns zero
        assertEquals(BigDecimal.ZERO, totalSpent);
    }

    @Test
    public void testCalculateSpentAmountByDateRange_Success() {
        // Given: Transactions in date range
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        when(transactionRepository.findAllByDateBetween(startDate, endDate)).thenReturn(testTransactions);

        // When: Calculate spent by date range
        BigDecimal spent = budgetCalculationService.calculateSpentAmountByDateRange(1L, 1L, startDate, endDate);

        // Then: Returns correct sum
        assertEquals(new BigDecimal("800000"), spent);
        verify(transactionRepository, times(1)).findAllByDateBetween(startDate, endDate);
    }

    @Test
    public void testCalculateSpentAmountByDateRange_NoTransactions() {
        // Given: No transactions in date range
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        when(transactionRepository.findAllByDateBetween(startDate, endDate)).thenReturn(new ArrayList<>());

        // When: Calculate spent by date range
        BigDecimal spent = budgetCalculationService.calculateSpentAmountByDateRange(1L, 1L, startDate, endDate);

        // Then: Returns zero
        assertEquals(BigDecimal.ZERO, spent);
    }

    @Test
    public void testCalculateTotalSpentAmountByDateRange_Success() {
        // Given: Transactions from multiple categories in date range
        Category category2 = new Category();
        category2.setId(2L);

        Transaction tx3 = new Transaction();
        tx3.setId(3L);
        tx3.setUser(testUser);
        tx3.setCategory(category2);
        tx3.setAmount(new BigDecimal("300000"));
        tx3.setType("expense");
        tx3.setDate(LocalDate.of(2025, 12, 10));

        List<Transaction> allTransactions = new ArrayList<>(testTransactions);
        allTransactions.add(tx3);

        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        when(transactionRepository.findAllByDateBetween(startDate, endDate)).thenReturn(allTransactions);

        // When: Calculate total spent by date range
        BigDecimal totalSpent = budgetCalculationService.calculateTotalSpentAmountByDateRange(1L, startDate, endDate);

        // Then: Returns sum across all categories
        assertEquals(new BigDecimal("1100000"), totalSpent);
    }

    @Test
    public void testCalculateTotalSpentAmountByDateRange_NoTransactions() {
        // Given: No transactions in date range
        LocalDate startDate = LocalDate.of(2025, 12, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        when(transactionRepository.findAllByDateBetween(startDate, endDate)).thenReturn(new ArrayList<>());

        // When: Calculate total spent by date range
        BigDecimal totalSpent = budgetCalculationService.calculateTotalSpentAmountByDateRange(1L, startDate, endDate);

        // Then: Returns zero
        assertEquals(BigDecimal.ZERO, totalSpent);
    }

    @Test
    public void testCalculateProgress_NormalUsage() {
        // When: Calculate progress with normal values
        int progress = budgetCalculationService.calculateProgress(
            new BigDecimal("500000"), 
            new BigDecimal("1000000")
        );

        // Then: Returns 50%
        assertEquals(50, progress);
    }

    @Test
    public void testCalculateProgress_OverBudget() {
        // When: Spent exceeds budget
        int progress = budgetCalculationService.calculateProgress(
            new BigDecimal("1500000"), 
            new BigDecimal("1000000")
        );

        // Then: Returns 150%
        assertEquals(150, progress);
    }

    @Test
    public void testCalculateProgress_ZeroBudget() {
        // When: Budget is zero
        int progress = budgetCalculationService.calculateProgress(
            new BigDecimal("500000"), 
            BigDecimal.ZERO
        );

        // Then: Returns 0 (avoid division by zero)
        assertEquals(0, progress);
    }

    @Test
    public void testCalculateProgress_NullBudget() {
        // When: Budget is null
        int progress = budgetCalculationService.calculateProgress(
            new BigDecimal("500000"), 
            null
        );

        // Then: Returns 0 (null budget = 0%)
        assertEquals(0, progress);
    }

    @Test
    public void testCalculateProgress_NegativeBudget() {
        // When: Budget is negative
        int progress = budgetCalculationService.calculateProgress(
            new BigDecimal("500000"), 
            new BigDecimal("-1000000")
        );

        // Then: Returns 0 (negative budget = invalid)
        assertEquals(0, progress);
    }

    @Test
    public void testCalculateProgress_ZeroSpent() {
        // When: No spending yet
        int progress = budgetCalculationService.calculateProgress(
            BigDecimal.ZERO, 
            new BigDecimal("1000000")
        );

        // Then: Returns 0%
        assertEquals(0, progress);
    }

    @Test
    public void testCalculateProgress_SmallDecimal() {
        // When: Calculate with small decimal values
        int progress = budgetCalculationService.calculateProgress(
            new BigDecimal("123.45"), 
            new BigDecimal("1000.00")
        );

        // Then: Returns correct percentage
        assertEquals(12, progress); // 12.345% -> 12%
    }

    @Test
    public void testCalculateProgress_ExactMatch() {
        // When: Spent exactly equals budget
        int progress = budgetCalculationService.calculateProgress(
            new BigDecimal("1000000"), 
            new BigDecimal("1000000")
        );

        // Then: Returns 100%
        assertEquals(100, progress);
    }
}
