package com.example.finance.service;

import com.example.finance.entity.*;
import com.example.finance.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AchievementService {

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private UserGamificationRepository userGamificationRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private GoalRepository goalRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Check and unlock achievements for a user based on their activities
     */
    @Transactional
    public List<UserAchievement> checkAndUnlockAchievements(User user) {
        List<UserAchievement> newlyUnlocked = new ArrayList<>();
        List<Achievement> activeAchievements = achievementRepository.findByIsActiveTrue();

        for (Achievement achievement : activeAchievements) {
            // Check if user already has this achievement
            Optional<UserAchievement> existing = userAchievementRepository.findByUserAndAchievement(user, achievement);
            if (existing.isEmpty()) {
                // Evaluate criteria
                if (evaluateCriteria(user, achievement)) {
                    UserAchievement userAchievement = unlockAchievement(user, achievement);
                    newlyUnlocked.add(userAchievement);
                }
            }
        }

        return newlyUnlocked;
    }

    /**
     * Unlock a specific achievement for a user
     */
    @Transactional
    public UserAchievement unlockAchievement(User user, Achievement achievement) {
        // Create user achievement
        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setUser(user);
        userAchievement.setAchievement(achievement);
        userAchievement.setUnlockedAt(LocalDateTime.now());
        userAchievement.setProgress(100);
        userAchievement.setIsNotified(false);
        userAchievementRepository.save(userAchievement);

        // Award points to user gamification
        UserGamification gamification = userGamificationRepository.findByUserId(user.getId())
                .orElseGet(() -> createUserGamification(user));
        
        gamification.setTotalPoints(gamification.getTotalPoints() + achievement.getPoints());
        gamification.calculateLevel();
        userGamificationRepository.save(gamification);

        return userAchievement;
    }

    /**
     * Evaluate achievement criteria
     */
    private boolean evaluateCriteria(User user, Achievement achievement) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> criteria = objectMapper.readValue(achievement.getCriteria(), Map.class);
            String type = (String) criteria.get("type");

            switch (type) {
                case "transaction_count":
                    int requiredCount = (Integer) criteria.get("count");
                    long transactionCount = transactionRepository.countByUserId(user.getId());
                    return transactionCount >= requiredCount;

                case "budget_created":
                    long budgetCount = budgetRepository.countByUserId(user.getId());
                    return budgetCount >= 1;

                case "goal_achieved":
                    long completedGoals = goalRepository.countByUserIdAndStatus(user.getId(), "completed");
                    int requiredGoals = (Integer) criteria.getOrDefault("count", 1);
                    return completedGoals >= requiredGoals;

                case "saving_streak":
                    UserGamification gamification = userGamificationRepository.findByUserId(user.getId()).orElse(null);
                    if (gamification == null) return false;
                    int requiredStreak = (Integer) criteria.get("days");
                    return gamification.getCurrentStreak() >= requiredStreak;

                case "total_savings":
                    UserGamification gam = userGamificationRepository.findByUserId(user.getId()).orElse(null);
                    if (gam == null) return false;
                    double requiredSavings = ((Number) criteria.get("amount")).doubleValue();
                    return gam.getTotalSavings() >= requiredSavings;

                case "spending_category":
                    String category = (String) criteria.get("category");
                    int categoryCount = (Integer) criteria.get("count");
                    long catTransactionCount = transactionRepository.countByUserIdAndCategoryId(user.getId(), Long.parseLong(category));
                    return catTransactionCount >= categoryCount;

                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get user's achievements
     */
    public List<UserAchievement> getUserAchievements(Long userId) {
        return userAchievementRepository.findByUserId(userId);
    }

    /**
     * Get achievement progress for a user
     */
    public Map<String, Object> getAchievementProgress(User user, Achievement achievement) {
        Map<String, Object> progress = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> criteria = objectMapper.readValue(achievement.getCriteria(), Map.class);
            String type = (String) criteria.get("type");

            switch (type) {
                case "transaction_count":
                    int requiredCount = (Integer) criteria.get("count");
                    long currentCount = transactionRepository.countByUserId(user.getId());
                    progress.put("current", currentCount);
                    progress.put("required", requiredCount);
                    progress.put("percentage", Math.min(100, (currentCount * 100) / requiredCount));
                    break;

                case "budget_created":
                    long budgetCount = budgetRepository.countByUserId(user.getId());
                    progress.put("current", budgetCount);
                    progress.put("required", 1);
                    progress.put("percentage", budgetCount >= 1 ? 100 : 0);
                    break;

                case "goal_achieved":
                    long completedGoals = goalRepository.countByUserIdAndStatus(user.getId(), "completed");
                    int requiredGoals = (Integer) criteria.getOrDefault("count", 1);
                    progress.put("current", completedGoals);
                    progress.put("required", requiredGoals);
                    progress.put("percentage", Math.min(100, (completedGoals * 100) / requiredGoals));
                    break;

                case "saving_streak":
                    UserGamification gamification = userGamificationRepository.findByUserId(user.getId()).orElse(null);
                    int currentStreak = gamification != null ? gamification.getCurrentStreak() : 0;
                    int requiredStreak = (Integer) criteria.get("days");
                    progress.put("current", currentStreak);
                    progress.put("required", requiredStreak);
                    progress.put("percentage", Math.min(100, (currentStreak * 100) / requiredStreak));
                    break;

                case "total_savings":
                    UserGamification gam = userGamificationRepository.findByUserId(user.getId()).orElse(null);
                    double currentSavings = gam != null ? gam.getTotalSavings() : 0;
                    double requiredSavings = ((Number) criteria.get("amount")).doubleValue();
                    progress.put("current", currentSavings);
                    progress.put("required", requiredSavings);
                    progress.put("percentage", Math.min(100, (int)((currentSavings * 100) / requiredSavings)));
                    break;

                case "spending_category":
                    String category = (String) criteria.get("category");
                    int categoryRequired = (Integer) criteria.get("count");
                    long catCount = transactionRepository.countByUserIdAndCategoryId(user.getId(), Long.parseLong(category));
                    progress.put("current", catCount);
                    progress.put("required", categoryRequired);
                    progress.put("percentage", Math.min(100, (catCount * 100) / categoryRequired));
                    break;

                default:
                    progress.put("current", 0);
                    progress.put("required", 0);
                    progress.put("percentage", 0);
            }
        } catch (Exception e) {
            progress.put("current", 0);
            progress.put("required", 0);
            progress.put("percentage", 0);
        }

        return progress;
    }

    /**
     * Get all achievements with user's progress
     */
    public List<Map<String, Object>> getAllAchievementsWithProgress(User user) {
        List<Achievement> allAchievements = achievementRepository.findByIsActiveTrue();
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(user.getId());
        
        Map<Long, UserAchievement> userAchMap = userAchievements.stream()
                .collect(Collectors.toMap(ua -> ua.getAchievement().getId(), ua -> ua));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Achievement achievement : allAchievements) {
            Map<String, Object> item = new HashMap<>();
            item.put("achievement", achievement);
            
            UserAchievement userAch = userAchMap.get(achievement.getId());
            if (userAch != null) {
                item.put("unlocked", true);
                item.put("unlockedAt", userAch.getUnlockedAt());
                item.put("progress", 100);
            } else {
                item.put("unlocked", false);
                item.put("progress", getAchievementProgress(user, achievement));
            }
            
            result.add(item);
        }

        return result;
    }

    /**
     * Mark achievements as notified
     */
    @Transactional
    public void markAsNotified(Long userAchievementId) {
        userAchievementRepository.findById(userAchievementId).ifPresent(ua -> {
            ua.setIsNotified(true);
            userAchievementRepository.save(ua);
        });
    }

    /**
     * Get unnotified achievements
     */
    public List<UserAchievement> getUnnotifiedAchievements(Long userId) {
        return userAchievementRepository.findByUserIdAndIsNotifiedFalse(userId);
    }

    /**
     * Create user gamification profile
     */
    private UserGamification createUserGamification(User user) {
        UserGamification gamification = new UserGamification();
        gamification.setUser(user);
        gamification.setTotalPoints(0);
        gamification.setLevel(1);
        gamification.setCurrentStreak(0);
        gamification.setLongestStreak(0);
        gamification.setLastActivityDate(LocalDate.now());
        gamification.setTransactionCount(0);
        gamification.setBudgetCount(0);
        gamification.setGoalCount(0);
        gamification.setTotalSavings(0.0);
        return userGamificationRepository.save(gamification);
    }
}
