package com.example.finance.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.finance.repository.*;
import com.example.finance.entity.*;
import java.time.LocalDate;
import java.util.*;

@Service
public class CachedDataService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserGamificationRepository userGamificationRepository;

    /**
     * Get user categories with caching
     * Categories are global (shared), not user-specific
     */
    @Cacheable(value = "categories", key = "'all_active'")
    public List<Category> getUserCategories(Long userId) {
        return categoryRepository.findAllByIsActiveTrue();
    }

    /**
     * Invalidate category cache when updated
     */
    @CacheEvict(value = "categories", key = "'all_active'")
    public void evictUserCategoriesCache(Long userId) {
        // Cache will be evicted automatically
    }

    /**
     * Get recent transactions with caching (short-lived)
     */
    @Cacheable(value = "transactions", key = "#userId + '_recent_' + #limit")
    public List<Transaction> getRecentTransactions(Long userId, int limit) {
        // This should be optimized with pagination
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return transactions.stream().limit(limit).toList();
    }

    /**
     * Get monthly budgets with caching
     */
    @Cacheable(value = "budgets", key = "#userId + '_' + #month + '_' + #year")
    public List<Budget> getMonthlyBudgets(Long userId, Integer month, Integer year) {
        return budgetRepository.findByUserIdAndMonthAndYearAndIsDeletedFalse(userId, month, year);
    }

    /**
     * Invalidate budget cache
     */
    @CacheEvict(value = "budgets", key = "#userId + '_' + #month + '_' + #year")
    public void evictMonthlyBudgetsCache(Long userId, Integer month, Integer year) {
        // Cache will be evicted automatically
    }

    /**
     * Get all achievements with caching (rarely changes)
     */
    @Cacheable(value = "achievements")
    public List<Achievement> getAllActiveAchievements() {
        return achievementRepository.findByIsActiveTrue();
    }

    /**
     * Get leaderboard with caching
     */
    @Cacheable(value = "leaderboard", key = "'points_' + #limit")
    public List<UserGamification> getPointsLeaderboard(int limit) {
        List<UserGamification> all = userGamificationRepository.findTopByOrderByTotalPointsDesc();
        return all.stream().limit(limit).toList();
    }

    /**
     * Invalidate leaderboard cache
     */
    @CacheEvict(value = "leaderboard", allEntries = true)
    public void evictLeaderboardCache() {
        // Cache will be evicted automatically
    }

    /**
     * Get dashboard statistics with caching
     */
    @Cacheable(value = "dashboard", key = "#userId + '_stats_' + #startDate + '_' + #endDate")
    public Map<String, Object> getDashboardStatistics(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        // These queries should be optimized with indexes
        List<Transaction> transactions = transactionRepository.findByUserIdAndDateBetweenOrderByCreatedAtDesc(
            userId, startDate, endDate);
        
        double totalIncome = transactions.stream()
            .filter(t -> "income".equals(t.getType()))
            .map(Transaction::getAmount)
            .filter(amount -> amount != null)
            .mapToDouble(amount -> amount.doubleValue())
            .sum();
        
        double totalExpense = transactions.stream()
            .filter(t -> "expense".equals(t.getType()))
            .map(Transaction::getAmount)
            .filter(amount -> amount != null)
            .mapToDouble(amount -> amount.doubleValue())
            .sum();
        
        stats.put("totalIncome", totalIncome);
        stats.put("totalExpense", totalExpense);
        stats.put("netSavings", totalIncome - totalExpense);
        stats.put("transactionCount", transactions.size());
        
        return stats;
    }

    /**
     * Invalidate dashboard cache
     */
    @CacheEvict(value = "dashboard", key = "#userId + '_stats_*'")
    public void evictDashboardCache(Long userId) {
        // Cache will be evicted automatically
    }

    /**
     * Clear all caches (admin function)
     */
    @CacheEvict(value = {"categories", "transactions", "budgets", "achievements", "leaderboard", "dashboard"}, allEntries = true)
    public void clearAllCaches() {
        // All caches will be evicted
    }
}
