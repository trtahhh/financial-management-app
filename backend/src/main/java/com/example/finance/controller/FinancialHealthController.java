package com.example.finance.controller;

import com.example.finance.dto.FinancialHealthScoreDTO;
import com.example.finance.service.FinancialHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST API Controller cho Điểm Sức khỏe Tài chính
 */
@RestController
@RequestMapping("/api/financial-health")
@RequiredArgsConstructor
@Slf4j
public class FinancialHealthController {
    
    private final FinancialHealthService financialHealthService;
    
    /**
     * Lấy điểm sức khỏe tài chính hiện tại
     */
    @GetMapping("/score")
    public ResponseEntity<FinancialHealthScoreDTO> getHealthScore(Authentication authentication) {
        log.info("Get financial health score request from user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        FinancialHealthScoreDTO score = financialHealthService.calculateHealthScore(userId);
        
        return ResponseEntity.ok(score);
    }
    
    /**
     * Lấy lịch sử xu hướng điểm theo thời gian
     */
    @GetMapping("/trend")
    public ResponseEntity<?> getTrend(
            @RequestParam(defaultValue = "6") Integer months,
            Authentication authentication) {
        log.info("Get health score trend for {} months from user: {}", months, authentication.getName());
        
        // TODO: Implement trend history
        return ResponseEntity.ok("Tính năng lịch sử xu hướng đang được phát triển");
    }
    
    /**
     * Lấy gợi ý cải thiện chi tiết
     */
    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations(Authentication authentication) {
        log.info("Get recommendations request from user: {}", authentication.getName());
        
        Long userId = getUserIdFromAuth(authentication);
        FinancialHealthScoreDTO score = financialHealthService.calculateHealthScore(userId);
        
        return ResponseEntity.ok(score.getRecommendations());
    }
    
    /**
     * Helper method để lấy userId từ Authentication
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        try {
            com.example.finance.security.CustomUserDetails userDetails = 
                (com.example.finance.security.CustomUserDetails) authentication.getPrincipal();
            return userDetails.getId();
        } catch (Exception e) {
            log.error("Error extracting userId from authentication", e);
            throw new RuntimeException("Unable to get user ID");
        }
    }
}
