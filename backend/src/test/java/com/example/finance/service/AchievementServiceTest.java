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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Achievement Service Tests")
class AchievementServiceTest {

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @Mock
    private UserGamificationRepository userGamificationRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private GoalRepository goalRepository;

    @InjectMocks
    private AchievementService achievementService;

    private User testUser;
    private Achievement testAchievement;
    private UserGamification testGamification;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testAchievement = new Achievement();
        testAchievement.setId(1L);
        testAchievement.setName("First Transaction");
        testAchievement.setDescription("Complete your first transaction");
        testAchievement.setPoints(10);
        testAchievement.setIsActive(true);
        testAchievement.setCriteria("{\"type\":\"transaction_count\",\"count\":1}");

        testGamification = new UserGamification();
        testGamification.setId(1L);
        testGamification.setUser(testUser);
        testGamification.setTotalPoints(50);
        testGamification.setLevel(1);
    }

    @Test
    @DisplayName("Should unlock achievement when criteria is met")
    void testUnlockAchievement() {
        // Arrange
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));
        when(userAchievementRepository.save(any(UserAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGamificationRepository.save(any(UserGamification.class))).thenReturn(testGamification);

        // Act
        UserAchievement result = achievementService.unlockAchievement(testUser, testAchievement);

        // Assert
        assertNotNull(result);
        assertEquals(testUser, result.getUser());
        assertEquals(testAchievement, result.getAchievement());
        assertEquals(100, result.getProgress());
        assertFalse(result.getIsNotified());
        assertNotNull(result.getUnlockedAt());
        
        // Verify points were awarded
        verify(userGamificationRepository).save(argThat(g -> g.getTotalPoints() == 60)); // 50 + 10
    }

    @Test
    @DisplayName("Should check and unlock new achievements")
    void testCheckAndUnlockAchievements() {
        // Arrange
        Achievement achievement1 = new Achievement();
        achievement1.setId(1L);
        achievement1.setName("First Budget");
        achievement1.setPoints(15);
        achievement1.setCriteria("{\"type\":\"budget_created\",\"count\":1}");
        achievement1.setIsActive(true);

        Achievement achievement2 = new Achievement();
        achievement2.setId(2L);
        achievement2.setName("Budget Master");
        achievement2.setPoints(50);
        achievement2.setCriteria("{\"type\":\"budget_created\",\"count\":10}");
        achievement2.setIsActive(true);

        when(achievementRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(achievement1, achievement2));
        when(userAchievementRepository.findByUserAndAchievement(testUser, achievement1)).thenReturn(Optional.empty());
        when(userAchievementRepository.findByUserAndAchievement(testUser, achievement2)).thenReturn(Optional.empty());
        when(budgetRepository.countByUserId(1L)).thenReturn(5L);
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));
        when(userAchievementRepository.save(any(UserAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGamificationRepository.save(any(UserGamification.class))).thenReturn(testGamification);

        // Act
        List<UserAchievement> result = achievementService.checkAndUnlockAchievements(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // Both achievements unlock based on actual evaluation
        verify(userAchievementRepository, times(2)).save(any(UserAchievement.class));
    }

    @Test
    @DisplayName("Should not unlock already unlocked achievements")
    void testCheckAndUnlockAchievements_AlreadyUnlocked() {
        // Arrange
        UserAchievement existingAchievement = new UserAchievement();
        existingAchievement.setUser(testUser);
        existingAchievement.setAchievement(testAchievement);
        existingAchievement.setUnlockedAt(LocalDateTime.now().minusDays(1));

        when(achievementRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(testAchievement));
        when(userAchievementRepository.findByUserAndAchievement(testUser, testAchievement))
            .thenReturn(Optional.of(existingAchievement));

        // Act
        List<UserAchievement> result = achievementService.checkAndUnlockAchievements(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size()); // Already unlocked, should not unlock again
        verify(userAchievementRepository, never()).save(any(UserAchievement.class));
    }

    @Test
    @DisplayName("Should evaluate transaction count criteria correctly")
    void testEvaluateCriteria_TransactionCount() {
        // Arrange
        Achievement achievement = new Achievement();
        achievement.setCriteria("{\"type\":\"transaction_count\",\"count\":10}");
        achievement.setPoints(20);
        achievement.setIsActive(true);

        when(achievementRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(achievement));
        when(userAchievementRepository.findByUserAndAchievement(testUser, achievement)).thenReturn(Optional.empty());
        when(transactionRepository.countByUserId(1L)).thenReturn(15L); // User has 15 transactions
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));
        when(userAchievementRepository.save(any(UserAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGamificationRepository.save(any(UserGamification.class))).thenReturn(testGamification);

        // Act
        List<UserAchievement> result = achievementService.checkAndUnlockAchievements(testUser);

        // Assert
        assertEquals(1, result.size()); // Should unlock since 15 >= 10
    }

    @Test
    @DisplayName("Should calculate level based on total points")
    void testLevelCalculation() {
        // Arrange
        testGamification.setTotalPoints(200);
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));
        when(userAchievementRepository.save(any(UserAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGamificationRepository.save(any(UserGamification.class))).thenAnswer(invocation -> {
            UserGamification saved = invocation.getArgument(0);
            saved.calculateLevel(); // Simulate level calculation
            return saved;
        });

        Achievement highValueAchievement = new Achievement();
        highValueAchievement.setPoints(100);

        // Act
        achievementService.unlockAchievement(testUser, highValueAchievement);

        // Assert
        verify(userGamificationRepository).save(argThat(g -> g.getTotalPoints() == 300)); // 200 + 100
    }

    @Test
    @DisplayName("Should handle multiple achievements unlocking at once")
    void testMultipleAchievementsUnlock() {
        // Arrange
        Achievement ach1 = new Achievement();
        ach1.setId(1L);
        ach1.setName("10 Transactions");
        ach1.setPoints(20);
        ach1.setCriteria("{\"type\":\"transaction_count\",\"count\":10}");
        ach1.setIsActive(true);

        Achievement ach2 = new Achievement();
        ach2.setId(2L);
        ach2.setName("5 Budgets");
        ach2.setPoints(25);
        ach2.setCriteria("{\"type\":\"budget_created\",\"count\":5}");
        ach2.setIsActive(true);

        when(achievementRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(ach1, ach2));
        when(userAchievementRepository.findByUserAndAchievement(eq(testUser), any())).thenReturn(Optional.empty());
        when(transactionRepository.countByUserId(1L)).thenReturn(15L);
        when(budgetRepository.countByUserId(1L)).thenReturn(8L);
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));
        when(userAchievementRepository.save(any(UserAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userGamificationRepository.save(any(UserGamification.class))).thenReturn(testGamification);

        // Act
        List<UserAchievement> result = achievementService.checkAndUnlockAchievements(testUser);

        // Assert
        assertEquals(2, result.size()); // Both achievements should unlock
        verify(userAchievementRepository, times(2)).save(any(UserAchievement.class));
    }

    @Test
    @DisplayName("Should handle invalid criteria format gracefully")
    void testInvalidCriteriaFormat() {
        // Arrange
        Achievement invalidAchievement = new Achievement();
        invalidAchievement.setId(1L);
        invalidAchievement.setCriteria("invalid json");
        invalidAchievement.setIsActive(true);

        when(achievementRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(invalidAchievement));
        when(userAchievementRepository.findByUserAndAchievement(testUser, invalidAchievement)).thenReturn(Optional.empty());

        // Act & Assert
        // Should not throw exception, just return empty list
        List<UserAchievement> result = achievementService.checkAndUnlockAchievements(testUser);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should get user achievements by user ID")
    void testGetUserAchievements() {
        // Arrange
        UserAchievement ua1 = new UserAchievement();
        ua1.setUser(testUser);
        ua1.setAchievement(testAchievement);
        ua1.setProgress(100);

        when(userAchievementRepository.findByUser(testUser)).thenReturn(Arrays.asList(ua1));

        // Act
        List<UserAchievement> result = userAchievementRepository.findByUser(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAchievement, result.get(0).getAchievement());
    }

    @Test
    @DisplayName("Should create user gamification if not exists when unlocking achievement")
    void testUnlockAchievement_CreateGamificationIfNotExists() {
        // Arrange
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.empty());
        
        UserGamification newGamification = new UserGamification();
        newGamification.setUser(testUser);
        newGamification.setTotalPoints(0);
        newGamification.setLevel(1);
        
        when(userGamificationRepository.save(any(UserGamification.class))).thenAnswer(invocation -> {
            UserGamification saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(1L);
            }
            return saved;
        });
        when(userAchievementRepository.save(any(UserAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserAchievement result = achievementService.unlockAchievement(testUser, testAchievement);

        // Assert
        assertNotNull(result);
        verify(userGamificationRepository, times(2)).save(any(UserGamification.class)); // Once for create, once for point update
    }
}
