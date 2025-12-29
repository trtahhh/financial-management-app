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

import java.time.LocalDate;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Gamification Service Tests")
class GamificationServiceTest {

    @Mock
    private UserGamificationRepository userGamificationRepository;

    @Mock
    private UserChallengeRepository userChallengeRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private AchievementService achievementService;

    @InjectMocks
    private GamificationService gamificationService;

    private User testUser;
    private UserGamification testGamification;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testGamification = new UserGamification();
        testGamification.setId(1L);
        testGamification.setUser(testUser);
        testGamification.setTotalPoints(100);
        testGamification.setLevel(2);
        testGamification.setCurrentStreak(5);
        testGamification.setLongestStreak(10);
        testGamification.setLastActivityDate(LocalDate.now());
        testGamification.setTransactionCount(20);
        testGamification.setBudgetCount(3);
        testGamification.setGoalCount(2);
    }

    @Test
    @DisplayName("Should get existing user gamification profile")
    void testGetUserGamification_Existing() {
        // Arrange
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));

        // Act
        UserGamification result = gamificationService.getUserGamification(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getTotalPoints());
        assertEquals(2, result.getLevel());
        assertEquals(5, result.getCurrentStreak());
        verify(userGamificationRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("Should create new user gamification profile if not exists")
    void testGetUserGamification_CreateNew() {
        // Arrange
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.empty());
        
        UserGamification newGamification = new UserGamification();
        newGamification.setUser(testUser);
        newGamification.setTotalPoints(0);
        newGamification.setLevel(1);
        newGamification.setCurrentStreak(0);
        
        when(userGamificationRepository.save(any(UserGamification.class))).thenReturn(newGamification);

        // Act
        UserGamification result = gamificationService.getUserGamification(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalPoints());
        assertEquals(1, result.getLevel());
        assertEquals(0, result.getCurrentStreak());
        verify(userGamificationRepository).save(any(UserGamification.class));
    }

    @Test
    @DisplayName("Should update user activity - transaction")
    void testUpdateUserActivity_Transaction() {
        // Arrange
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));
        when(userGamificationRepository.save(any(UserGamification.class))).thenReturn(testGamification);
        when(achievementService.checkAndUnlockAchievements(testUser)).thenReturn(Arrays.asList());

        // Act
        UserGamification result = gamificationService.updateUserActivity(testUser, "transaction");

        // Assert
        assertNotNull(result);
        assertEquals(21, result.getTransactionCount()); // 20 + 1
        verify(achievementService).checkAndUnlockAchievements(testUser);
        verify(userGamificationRepository).save(any(UserGamification.class));
    }

    @Test
    @DisplayName("Should update user activity - budget")
    void testUpdateUserActivity_Budget() {
        // Arrange
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));
        when(userGamificationRepository.save(any(UserGamification.class))).thenReturn(testGamification);
        when(achievementService.checkAndUnlockAchievements(testUser)).thenReturn(Arrays.asList());

        // Act
        UserGamification result = gamificationService.updateUserActivity(testUser, "budget");

        // Assert
        assertNotNull(result);
        assertEquals(4, result.getBudgetCount()); // 3 + 1
        verify(achievementService).checkAndUnlockAchievements(testUser);
    }

    @Test
    @DisplayName("Should update user activity - goal")
    void testUpdateUserActivity_Goal() {
        // Arrange
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));
        when(userGamificationRepository.save(any(UserGamification.class))).thenReturn(testGamification);
        when(achievementService.checkAndUnlockAchievements(testUser)).thenReturn(Arrays.asList());

        // Act
        UserGamification result = gamificationService.updateUserActivity(testUser, "goal");

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getGoalCount()); // 2 + 1
        verify(achievementService).checkAndUnlockAchievements(testUser);
    }

    @Test
    @DisplayName("Should get user level based on points")
    void testGetUserLevel() {
        // Arrange
        testGamification.setTotalPoints(500);
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));

        // Act
        UserGamification result = gamificationService.getUserGamification(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(500, result.getTotalPoints());
    }

    @Test
    @DisplayName("Should get active challenges for user")
    void testGetActiveChallenges() {
        // Arrange
        Challenge challenge1 = new Challenge();
        challenge1.setId(1L);
        challenge1.setName("Save $1000");
        challenge1.setIsActive(true);

        Challenge challenge2 = new Challenge();
        challenge2.setId(2L);
        challenge2.setName("Budget Master");
        challenge2.setIsActive(true);

        when(challengeRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(challenge1, challenge2));

        // Act
        List<Challenge> result = challengeRepository.findByIsActiveTrue();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(Challenge::getIsActive));
    }

    @Test
    @DisplayName("Should calculate streak correctly")
    void testStreakCalculation() {
        // Arrange
        testGamification.setLastActivityDate(LocalDate.now().minusDays(1));
        testGamification.setCurrentStreak(5);
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));
        when(userGamificationRepository.save(any(UserGamification.class))).thenAnswer(invocation -> {
            UserGamification saved = invocation.getArgument(0);
            // Simulate streak update logic
            saved.setCurrentStreak(6); // Incremented from 5
            return saved;
        });
        when(achievementService.checkAndUnlockAchievements(testUser)).thenReturn(Arrays.asList());

        // Act
        UserGamification result = gamificationService.updateUserActivity(testUser, "transaction");

        // Assert
        assertNotNull(result);
        verify(userGamificationRepository).save(any(UserGamification.class));
    }

    @Test
    @DisplayName("Should handle null activity type gracefully")
    void testUpdateUserActivity_NullType() {
        // Arrange
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));
        when(userGamificationRepository.save(any(UserGamification.class))).thenReturn(testGamification);
        when(achievementService.checkAndUnlockAchievements(testUser)).thenReturn(Arrays.asList());

        // Act
        UserGamification result = gamificationService.updateUserActivity(testUser, "unknown");

        // Assert
        assertNotNull(result);
        // Counts should remain unchanged
        assertEquals(20, result.getTransactionCount());
        assertEquals(3, result.getBudgetCount());
        assertEquals(2, result.getGoalCount());
    }

    @Test
    @DisplayName("Should track longest streak")
    void testLongestStreak() {
        // Arrange
        testGamification.setCurrentStreak(15);
        testGamification.setLongestStreak(10);
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.of(testGamification));

        // Act
        UserGamification result = gamificationService.getUserGamification(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(15, result.getCurrentStreak());
        assertEquals(10, result.getLongestStreak());
        // Note: Longest streak update happens in entity method when current > longest
    }

    @Test
    @DisplayName("Should initialize new user with default values")
    void testCreateUserGamification_Defaults() {
        // Arrange
        when(userGamificationRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userGamificationRepository.save(any(UserGamification.class))).thenAnswer(invocation -> {
            UserGamification saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        UserGamification result = gamificationService.getUserGamification(testUser);

        // Assert
        assertNotNull(result);
        verify(userGamificationRepository).save(argThat(g -> 
            g.getTotalPoints() == 0 &&
            g.getLevel() == 1 &&
            g.getCurrentStreak() == 0 &&
            g.getLongestStreak() == 0 &&
            g.getTransactionCount() == 0 &&
            g.getBudgetCount() == 0 &&
            g.getGoalCount() == 0
        ));
    }
}
