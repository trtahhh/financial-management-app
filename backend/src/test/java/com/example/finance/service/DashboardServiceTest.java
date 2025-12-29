package com.example.finance.service;

import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.entity.Wallet;
import com.example.finance.repository.TransactionRepository;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private BudgetService budgetService;

    @Mock
    private GoalService goalService;

    @Mock
    private CategoryColorService categoryColorService;

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser;
    private Wallet testWallet;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testWallet = new Wallet();
        testWallet.setId(1L);
        testWallet.setName("Main Wallet");
        testWallet.setBalance(new BigDecimal("1000000"));
        testWallet.setUser(testUser);

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food");
    }

    @Test
    @DisplayName("Should get dashboard data successfully")
    void testGetDashboardData_Success() {
        // Arrange
        LocalDate dateFrom = LocalDate.now().minusDays(30);
        LocalDate dateTo = LocalDate.now();

        List<Transaction> transactions = Arrays.asList(
            createTransaction(1L, "income", new BigDecimal("5000000")),
            createTransaction(2L, "expense", new BigDecimal("1000000")),
            createTransaction(3L, "expense", new BigDecimal("500000"))
        );

        List<Wallet> wallets = Arrays.asList(
            testWallet,
            createWallet(2L, "Savings", new BigDecimal("2000000"))
        );

        when(transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(
            anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(transactions);
        when(walletRepository.findByUserId(1L)).thenReturn(wallets);

        // Act
        Map<String, Object> dashboard = dashboardService.getDashboardData(1L, dateFrom, dateTo);

        // Assert
        assertNotNull(dashboard);
        assertEquals(new BigDecimal("5000000"), dashboard.get("totalIncome"));
        assertEquals(new BigDecimal("1500000"), dashboard.get("totalExpense"));
        assertEquals(new BigDecimal("3000000"), dashboard.get("totalBalance"));
        assertNotNull(dashboard.get("wallets"));
        assertNotNull(dashboard.get("recentTransactions"));
        
        verify(transactionRepository).findByUserIdAndDateBetweenOrderByCreatedAtDesc(anyLong(), any(), any());
        verify(walletRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("Should handle empty transactions")
    void testGetDashboardData_EmptyTransactions() {
        // Arrange
        LocalDate dateFrom = LocalDate.now().minusDays(30);
        LocalDate dateTo = LocalDate.now();

        when(transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(
            anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());
        when(walletRepository.findByUserId(1L)).thenReturn(Arrays.asList(testWallet));

        // Act
        Map<String, Object> dashboard = dashboardService.getDashboardData(1L, dateFrom, dateTo);

        // Assert
        assertNotNull(dashboard);
        assertEquals(BigDecimal.ZERO, dashboard.get("totalIncome"));
        assertEquals(BigDecimal.ZERO, dashboard.get("totalExpense"));
        assertEquals(new BigDecimal("1000000"), dashboard.get("totalBalance"));
    }

    @Test
    @DisplayName("Should handle empty wallets")
    void testGetDashboardData_EmptyWallets() {
        // Arrange
        LocalDate dateFrom = LocalDate.now().minusDays(30);
        LocalDate dateTo = LocalDate.now();

        List<Transaction> transactions = Arrays.asList(
            createTransaction(1L, "income", new BigDecimal("1000000"))
        );

        when(transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(
            anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(transactions);
        when(walletRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> dashboard = dashboardService.getDashboardData(1L, dateFrom, dateTo);

        // Assert
        assertNotNull(dashboard);
        assertEquals(BigDecimal.ZERO, dashboard.get("totalBalance"));
    }

    @Test
    @DisplayName("Should handle repository exception gracefully")
    void testGetDashboardData_Exception() {
        // Arrange
        LocalDate dateFrom = LocalDate.now().minusDays(30);
        LocalDate dateTo = LocalDate.now();

        when(transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(
            anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        Map<String, Object> dashboard = dashboardService.getDashboardData(1L, dateFrom, dateTo);

        // Assert - should return default values
        assertNotNull(dashboard);
        assertEquals(BigDecimal.ZERO, dashboard.get("totalIncome"));
        assertEquals(BigDecimal.ZERO, dashboard.get("totalExpense"));
        assertEquals(BigDecimal.ZERO, dashboard.get("totalBalance"));
        assertTrue(((List<?>) dashboard.get("wallets")).isEmpty());
        assertTrue(((List<?>) dashboard.get("recentTransactions")).isEmpty());
    }

    @Test
    @DisplayName("Should get dashboard data by month successfully")
    void testGetDashboardDataByMonth_Success() {
        // Arrange
        Integer month = 12;
        Integer year = 2025;

        List<Transaction> transactions = Arrays.asList(
            createTransaction(1L, "income", new BigDecimal("10000000")),
            createTransaction(2L, "expense", new BigDecimal("3000000"))
        );

        List<Wallet> wallets = Arrays.asList(testWallet);

        when(transactionRepository.findByUserIdAndMonthAndYear(anyLong(), anyInt(), anyInt()))
            .thenReturn(transactions);
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
            .thenReturn(transactions);
        when(walletRepository.findByUserId(1L)).thenReturn(wallets);
        when(budgetService.getBudgetVsActual(anyLong(), anyInt(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(budgetService.getBudgetWarnings(anyLong(), anyInt(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(goalService.getGoalProgress(anyLong()))
            .thenReturn(new ArrayList<>());

        // Act
        Map<String, Object> dashboard = dashboardService.getDashboardDataByMonth(1L, month, year);

        // Assert
        assertNotNull(dashboard);
        assertEquals(new BigDecimal("10000000"), dashboard.get("totalIncome"));
        assertEquals(new BigDecimal("3000000"), dashboard.get("totalExpense"));
        assertEquals(new BigDecimal("7000000"), dashboard.get("netIncome"));
        assertNotNull(dashboard.get("budgetProgress"));
        assertNotNull(dashboard.get("goalProgress"));
        
        verify(transactionRepository, times(2)).findByUserIdAndMonthAndYear(1L, month, year);
        verify(budgetService).getBudgetVsActual(1L, month, year);
        verify(goalService).getGoalProgress(1L);
    }

    @Test
    @DisplayName("Should handle budget service exception")
    void testGetDashboardDataByMonth_BudgetException() {
        // Arrange
        when(transactionRepository.findByUserIdAndMonthAndYear(anyLong(), anyInt(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
            .thenReturn(new ArrayList<>());
        when(walletRepository.findByUserId(anyLong()))
            .thenReturn(Arrays.asList(testWallet));
        when(budgetService.getBudgetVsActual(anyLong(), anyInt(), anyInt()))
            .thenThrow(new RuntimeException("Budget error"));
        when(goalService.getGoalProgress(anyLong()))
            .thenReturn(new ArrayList<>());

        // Act
        Map<String, Object> dashboard = dashboardService.getDashboardDataByMonth(1L, 12, 2025);

        // Assert
        assertNotNull(dashboard);
        assertTrue(((List<?>) dashboard.get("budgetProgress")).isEmpty());
        assertTrue(((List<?>) dashboard.get("budgetWarnings")).isEmpty());
    }

    @Test
    @DisplayName("Should handle goal service exception")
    void testGetDashboardDataByMonth_GoalException() {
        // Arrange
        when(transactionRepository.findByUserIdAndMonthAndYear(anyLong(), anyInt(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
            .thenReturn(new ArrayList<>());
        when(walletRepository.findByUserId(anyLong()))
            .thenReturn(Arrays.asList(testWallet));
        when(budgetService.getBudgetVsActual(anyLong(), anyInt(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(budgetService.getBudgetWarnings(anyLong(), anyInt(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(goalService.getGoalProgress(anyLong()))
            .thenThrow(new RuntimeException("Goal error"));

        // Act
        Map<String, Object> dashboard = dashboardService.getDashboardDataByMonth(1L, 12, 2025);

        // Assert
        assertNotNull(dashboard);
        assertTrue(((List<?>) dashboard.get("goalProgress")).isEmpty());
        assertEquals(0L, dashboard.get("activeGoalsCount"));
    }

    @Test
    @DisplayName("Should limit recent transactions to 5")
    void testGetDashboardData_LimitRecentTransactions() {
        // Arrange
        LocalDate dateFrom = LocalDate.now().minusDays(30);
        LocalDate dateTo = LocalDate.now();

        List<Transaction> transactions = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            transactions.add(createTransaction((long) i, "expense", new BigDecimal("100000")));
        }

        when(transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(
            anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(transactions);
        when(walletRepository.findByUserId(1L)).thenReturn(Arrays.asList(testWallet));

        // Act
        Map<String, Object> dashboard = dashboardService.getDashboardData(1L, dateFrom, dateTo);

        // Assert
        @SuppressWarnings("unchecked")
        List<Transaction> recentTransactions = (List<Transaction>) dashboard.get("recentTransactions");
        assertEquals(5, recentTransactions.size());
    }

    @Test
    @DisplayName("Should calculate correct totals with mixed transaction types")
    void testGetDashboardData_MixedTransactions() {
        // Arrange
        LocalDate dateFrom = LocalDate.now().minusDays(30);
        LocalDate dateTo = LocalDate.now();

        List<Transaction> transactions = Arrays.asList(
            createTransaction(1L, "income", new BigDecimal("5000000")),
            createTransaction(2L, "income", new BigDecimal("3000000")),
            createTransaction(3L, "expense", new BigDecimal("2000000")),
            createTransaction(4L, "expense", new BigDecimal("1000000")),
            createTransaction(5L, "expense", new BigDecimal("500000"))
        );

        when(transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(
            anyLong(), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(transactions);
        when(walletRepository.findByUserId(1L)).thenReturn(Arrays.asList(testWallet));

        // Act
        Map<String, Object> dashboard = dashboardService.getDashboardData(1L, dateFrom, dateTo);

        // Assert
        assertEquals(new BigDecimal("8000000"), dashboard.get("totalIncome"));
        assertEquals(new BigDecimal("3500000"), dashboard.get("totalExpense"));
    }

    @Test
    @DisplayName("Should handle dashboard by month with zero transactions")
    void testGetDashboardDataByMonth_ZeroTransactions() {
        // Arrange
        when(transactionRepository.findByUserIdAndMonthAndYear(anyLong(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(anyLong()))
            .thenReturn(Collections.emptyList());
        when(walletRepository.findByUserId(anyLong()))
            .thenReturn(Arrays.asList(testWallet));
        when(budgetService.getBudgetVsActual(anyLong(), anyInt(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(budgetService.getBudgetWarnings(anyLong(), anyInt(), anyInt()))
            .thenReturn(new ArrayList<>());
        when(goalService.getGoalProgress(anyLong()))
            .thenReturn(new ArrayList<>());

        // Act
        Map<String, Object> dashboard = dashboardService.getDashboardDataByMonth(1L, 12, 2025);

        // Assert
        assertEquals(BigDecimal.ZERO, dashboard.get("totalIncome"));
        assertEquals(BigDecimal.ZERO, dashboard.get("totalExpense"));
        assertEquals(BigDecimal.ZERO, dashboard.get("netIncome"));
    }

    @Test
    @DisplayName("Should handle all exceptions in dashboard by month")
    void testGetDashboardDataByMonth_AllExceptions() {
        // Arrange
        when(transactionRepository.findByUserIdAndMonthAndYear(anyLong(), anyInt(), anyInt()))
            .thenThrow(new RuntimeException("Transaction error"));

        // Act
        Map<String, Object> dashboard = dashboardService.getDashboardDataByMonth(1L, 12, 2025);

        // Assert - should return all default values
        assertNotNull(dashboard);
        assertEquals(BigDecimal.ZERO, dashboard.get("totalIncome"));
        assertEquals(BigDecimal.ZERO, dashboard.get("totalExpense"));
        assertEquals(BigDecimal.ZERO, dashboard.get("netIncome"));
        assertEquals(BigDecimal.ZERO, dashboard.get("totalBalance"));
        assertTrue(((List<?>) dashboard.get("expensesByCategory")).isEmpty());
        assertTrue(((List<?>) dashboard.get("spendingTrend")).isEmpty());
    }

    private Transaction createTransaction(Long id, String type, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setDate(LocalDate.now());
        tx.setUser(testUser);
        tx.setWallet(testWallet);
        tx.setCategory(testCategory);
        return tx;
    }

    private Wallet createWallet(Long id, String name, BigDecimal balance) {
        Wallet wallet = new Wallet();
        wallet.setId(id);
        wallet.setName(name);
        wallet.setBalance(balance);
        wallet.setUser(testUser);
        return wallet;
    }
}
