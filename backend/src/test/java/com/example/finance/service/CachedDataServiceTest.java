package com.example.finance.service;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for CachedDataService
 * Service that provides caching layer for frequently accessed data
 */
@ExtendWith(MockitoExtension.class)
public class CachedDataServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserGamificationRepository userGamificationRepository;

    @InjectMocks
    private CachedDataService cachedDataService;

    private User testUser;
    private Category testCategory;
    private Transaction testTransaction;
    private Budget testBudget;
    private Achievement testAchievement;
    private UserGamification testUserGamification;

    @BeforeEach
    public void setUp() {
        // Setup test entities
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Food");
        testCategory.setType("expense");
        testCategory.setIsActive(true);

        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setUser(testUser);
        testTransaction.setCategory(testCategory);
        testTransaction.setAmount(new BigDecimal("50000"));
        testTransaction.setType("expense");
        testTransaction.setNote("Test transaction");
        testTransaction.setDate(LocalDate.now());

        testBudget = new Budget();
        testBudget.setId(1L);
        testBudget.setUser(testUser);
        testBudget.setCategory(testCategory);
        testBudget.setAmount(new BigDecimal("1000000"));
        testBudget.setMonth(12);
        testBudget.setYear(2025);
        testBudget.setIsDeleted(false);

        testAchievement = new Achievement();
        testAchievement.setId(1L);
        testAchievement.setName("First Transaction");
        testAchievement.setDescription("Complete your first transaction");
        testAchievement.setPoints(10);
        testAchievement.setIsActive(true);

        testUserGamification = new UserGamification();
        testUserGamification.setId(1L);
        testUserGamification.setUser(testUser);
        testUserGamification.setLevel(5);
        testUserGamification.setTotalPoints(250);
    }

    @Test
    public void testGetUserCategories_Success() {
        // Given: Categories exist
        List<Category> categories = Arrays.asList(testCategory);
        when(categoryRepository.findAllByIsActiveTrue()).thenReturn(categories);

        // When: Get user categories
        List<Category> result = cachedDataService.getUserCategories(1L);

        // Then: Returns categories
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Food", result.get(0).getName());
        verify(categoryRepository, times(1)).findAllByIsActiveTrue();
    }

    @Test
    public void testGetUserCategories_EmptyList() {
        // Given: No categories exist
        when(categoryRepository.findAllByIsActiveTrue()).thenReturn(new ArrayList<>());

        // When: Get user categories
        List<Category> result = cachedDataService.getUserCategories(1L);

        // Then: Returns empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(categoryRepository, times(1)).findAllByIsActiveTrue();
    }

    @Test
    public void testEvictUserCategoriesCache() {
        // When: Evict cache
        cachedDataService.evictUserCategoriesCache(1L);

        // Then: No exception thrown (cache eviction is silent)
        verifyNoInteractions(categoryRepository);
    }

    @Test
    public void testGetRecentTransactions_Success() {
        // Given: Transactions exist
        List<Transaction> transactions = Arrays.asList(testTransaction);
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(transactions);

        // When: Get recent transactions
        List<Transaction> result = cachedDataService.getRecentTransactions(1L, 5);

        // Then: Returns transactions
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test transaction", result.get(0).getNote());
        verify(transactionRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    public void testGetRecentTransactions_LimitApplied() {
        // Given: Multiple transactions exist
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Transaction tx = new Transaction();
            tx.setId((long) i);
            tx.setNote("Transaction " + i);
            transactions.add(tx);
        }
        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(transactions);

        // When: Get recent transactions with limit 5
        List<Transaction> result = cachedDataService.getRecentTransactions(1L, 5);

        // Then: Returns only 5 transactions
        assertNotNull(result);
        assertEquals(5, result.size());
        verify(transactionRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    public void testGetMonthlyBudgets_Success() {
        // Given: Budgets exist
        List<Budget> budgets = Arrays.asList(testBudget);
        when(budgetRepository.findByUserIdAndMonthAndYearAndIsDeletedFalse(1L, 12, 2025)).thenReturn(budgets);

        // When: Get monthly budgets
        List<Budget> result = cachedDataService.getMonthlyBudgets(1L, 12, 2025);

        // Then: Returns budgets
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("1000000"), result.get(0).getAmount());
        verify(budgetRepository, times(1)).findByUserIdAndMonthAndYearAndIsDeletedFalse(1L, 12, 2025);
    }

    @Test
    public void testGetMonthlyBudgets_EmptyList() {
        // Given: No budgets exist
        when(budgetRepository.findByUserIdAndMonthAndYearAndIsDeletedFalse(1L, 12, 2025)).thenReturn(new ArrayList<>());

        // When: Get monthly budgets
        List<Budget> result = cachedDataService.getMonthlyBudgets(1L, 12, 2025);

        // Then: Returns empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(budgetRepository, times(1)).findByUserIdAndMonthAndYearAndIsDeletedFalse(1L, 12, 2025);
    }

    @Test
    public void testEvictMonthlyBudgetsCache() {
        // When: Evict cache
        cachedDataService.evictMonthlyBudgetsCache(1L, 12, 2025);

        // Then: No exception thrown
        verifyNoInteractions(budgetRepository);
    }

    @Test
    public void testGetAllActiveAchievements_Success() {
        // Given: Achievements exist
        List<Achievement> achievements = Arrays.asList(testAchievement);
        when(achievementRepository.findByIsActiveTrue()).thenReturn(achievements);

        // When: Get all achievements
        List<Achievement> result = cachedDataService.getAllActiveAchievements();

        // Then: Returns achievements
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("First Transaction", result.get(0).getName());
        verify(achievementRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    public void testGetPointsLeaderboard_Success() {
        // Given: Leaderboard data exists
        List<UserGamification> leaderboard = Arrays.asList(testUserGamification);
        when(userGamificationRepository.findTopByOrderByTotalPointsDesc()).thenReturn(leaderboard);

        // When: Get leaderboard
        List<UserGamification> result = cachedDataService.getPointsLeaderboard(10);

        // Then: Returns leaderboard
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(250, result.get(0).getTotalPoints());
        verify(userGamificationRepository, times(1)).findTopByOrderByTotalPointsDesc();
    }

    @Test
    public void testGetPointsLeaderboard_LimitApplied() {
        // Given: Multiple users in leaderboard
        List<UserGamification> leaderboard = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            UserGamification ug = new UserGamification();
            ug.setId((long) i);
            User u = new User();
            u.setId((long) i);
            ug.setUser(u);
            ug.setTotalPoints(1000 - (i * 10));
            leaderboard.add(ug);
        }
        when(userGamificationRepository.findTopByOrderByTotalPointsDesc()).thenReturn(leaderboard);

        // When: Get top 10
        List<UserGamification> result = cachedDataService.getPointsLeaderboard(10);

        // Then: Returns only 10
        assertNotNull(result);
        assertEquals(10, result.size());
        verify(userGamificationRepository, times(1)).findTopByOrderByTotalPointsDesc();
    }

    @Test
    public void testEvictLeaderboardCache() {
        // When: Evict cache
        cachedDataService.evictLeaderboardCache();

        // Then: No exception thrown
        verifyNoInteractions(userGamificationRepository);
    }

    @Test
    public void testGetDashboardStatistics_Success() {
        // Given: Transactions exist
        Transaction income = new Transaction();
        income.setId(1L);
        income.setUser(testUser);
        income.setType("income");
        income.setAmount(new BigDecimal("5000000"));
        income.setDate(LocalDate.now());

        Transaction expense = new Transaction();
        expense.setId(2L);
        expense.setUser(testUser);
        expense.setType("expense");
        expense.setAmount(new BigDecimal("3000000"));
        expense.setDate(LocalDate.now());

        List<Transaction> transactions = Arrays.asList(income, expense);
        when(transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(any(), any(), any()))
            .thenReturn(transactions);

        // When: Get dashboard statistics
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        Map<String, Object> stats = cachedDataService.getDashboardStatistics(1L, startDate, endDate);

        // Then: Returns correct statistics
        assertNotNull(stats);
        assertEquals(5000000.0, stats.get("totalIncome"));
        assertEquals(3000000.0, stats.get("totalExpense"));
        assertEquals(2000000.0, stats.get("netSavings"));
        assertEquals(2, stats.get("transactionCount"));
        verify(transactionRepository, times(1)).findByUserIdAndDateBetweenOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    public void testGetDashboardStatistics_NoTransactions() {
        // Given: No transactions exist
        when(transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(any(), any(), any()))
            .thenReturn(new ArrayList<>());

        // When: Get dashboard statistics
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        Map<String, Object> stats = cachedDataService.getDashboardStatistics(1L, startDate, endDate);

        // Then: Returns zero statistics
        assertNotNull(stats);
        assertEquals(0.0, stats.get("totalIncome"));
        assertEquals(0.0, stats.get("totalExpense"));
        assertEquals(0.0, stats.get("netSavings"));
        assertEquals(0, stats.get("transactionCount"));
        verify(transactionRepository, times(1)).findByUserIdAndDateBetweenOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    public void testGetDashboardStatistics_NullAmounts() {
        // Given: Transactions with null amounts
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setUser(testUser);
        tx.setType("income");
        tx.setAmount(null); // null amount
        tx.setDate(LocalDate.now());

        when(transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(any(), any(), any()))
            .thenReturn(Arrays.asList(tx));

        // When: Get dashboard statistics
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        Map<String, Object> stats = cachedDataService.getDashboardStatistics(1L, startDate, endDate);

        // Then: Null amounts are filtered out
        assertNotNull(stats);
        assertEquals(0.0, stats.get("totalIncome"));
        assertEquals(0.0, stats.get("totalExpense"));
        assertEquals(0.0, stats.get("netSavings"));
        assertEquals(1, stats.get("transactionCount"));
        verify(transactionRepository, times(1)).findByUserIdAndDateBetweenOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    public void testEvictDashboardCache() {
        // When: Evict cache
        cachedDataService.evictDashboardCache(1L);

        // Then: No exception thrown
        verifyNoInteractions(transactionRepository);
    }

    @Test
    public void testClearAllCaches() {
        // When: Clear all caches
        cachedDataService.clearAllCaches();

        // Then: No exception thrown (cache eviction is silent)
        verifyNoInteractions(categoryRepository, transactionRepository, budgetRepository, 
                           achievementRepository, userGamificationRepository);
    }
}
