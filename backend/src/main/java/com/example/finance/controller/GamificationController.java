package com.example.finance.controller;

import com.example.finance.entity.*;
import com.example.finance.repository.UserRepository;
import com.example.finance.service.AchievementService;
import com.example.finance.service.GamificationService;
import com.example.finance.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * Get user's gamification stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats(HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            Map<String, Object> stats = gamificationService.getUserStatistics(user);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user's achievements
     */
    @GetMapping("/achievements")
    public ResponseEntity<?> getUserAchievements(HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            List<Map<String, Object>> achievements = achievementService.getAllAchievementsWithProgress(user);
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get unnotified achievements
     */
    @GetMapping("/achievements/unnotified")
    public ResponseEntity<?> getUnnotifiedAchievements(HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            List<UserAchievement> unnotified = achievementService.getUnnotifiedAchievements(user.getId());
            return ResponseEntity.ok(unnotified);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Mark achievement as notified
     */
    @PutMapping("/achievements/{achievementId}/notify")
    public ResponseEntity<?> markAchievementNotified(@PathVariable Long achievementId) {
        try {
            achievementService.markAsNotified(achievementId);
            return ResponseEntity.ok(Map.of("message", "Achievement marked as notified"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get leaderboard
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> leaderboard = gamificationService.getLeaderboard(limit);
            return ResponseEntity.ok(leaderboard);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get streak leaderboard
     */
    @GetMapping("/leaderboard/streak")
    public ResponseEntity<?> getStreakLeaderboard(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> leaderboard = gamificationService.getStreakLeaderboard(limit);
            return ResponseEntity.ok(leaderboard);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get active challenges
     */
    @GetMapping("/challenges")
    public ResponseEntity<?> getActiveChallenges() {
        try {
            List<Challenge> challenges = gamificationService.getActiveChallenges();
            return ResponseEntity.ok(challenges);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user's active challenges
     */
    @GetMapping("/challenges/user")
    public ResponseEntity<?> getUserChallenges(HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            List<Map<String, Object>> challenges = gamificationService.getUserActiveChallenges(user.getId());
            return ResponseEntity.ok(challenges);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Join a challenge
     */
    @PostMapping("/challenges/{challengeId}/join")
    public ResponseEntity<?> joinChallenge(@PathVariable Long challengeId, HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            UserChallenge userChallenge = gamificationService.joinChallenge(user, challengeId);
            return ResponseEntity.ok(userChallenge);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Award points manually (for testing or special events)
     */
    @PostMapping("/points")
    public ResponseEntity<?> awardPoints(
            @RequestParam int points,
            @RequestParam String reason,
            HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            UserGamification gamification = gamificationService.awardPoints(user, points, reason);
            return ResponseEntity.ok(gamification);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check and unlock achievements manually
     */
    @PostMapping("/achievements/check")
    public ResponseEntity<?> checkAchievements(HttpServletRequest request) {
        try {
            User user = getCurrentUser(request);
            List<UserAchievement> newAchievements = achievementService.checkAndUnlockAchievements(user);
            return ResponseEntity.ok(Map.of(
                "message", "Achievements checked",
                "newAchievements", newAchievements
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get current user from JWT token
     */
    private User getCurrentUser(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Long userId = jwtUtil.getUserId(token);
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
