package com.example.finance.controller;

import com.example.finance.service.DashboardService;
import com.example.finance.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Lấy toàn bộ dữ liệu dashboard
     */
    @GetMapping("/user/{userId}")
    @Cacheable(value = "dashboard", key = "#userId + '_' + #month + '_' + #year")
    public ResponseEntity<Map<String, Object>> getDashboard(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        LocalDate now = LocalDate.now();
        if (month == null) month = now.getMonthValue();
        if (year == null) year = now.getYear();
        
        Map<String, Object> dashboard = dashboardService.getDashboardData(userId, month, year);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * API riêng cho mobile hoặc lightweight requests
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getBasicStats(
            @PathVariable Long userId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        LocalDate now = LocalDate.now();
        if (month == null) month = now.getMonthValue();
        if (year == null) year = now.getYear();
        
        // Chỉ trả về stats cơ bản, không cache
        Map<String, Object> basicStats = dashboardService.getDashboardData(userId, month, year);
        return ResponseEntity.ok(basicStats);
    }

    /**
     * Endpoint mới để hỗ trợ frontend với month và year parameters
     */
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        LocalDate now = LocalDate.now();
        if (month == null) month = now.getMonthValue();
        if (year == null) year = now.getYear();
        
        // Extract userId from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();
        
        Map<String, Object> dashboard = dashboardService.getDashboardData(userId, month, year);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/data-by-date")
    public ResponseEntity<Map<String, Object>> getDashboardByDate(
            @RequestParam Long userId,
            @RequestParam String dateFrom,
            @RequestParam String dateTo) {

        LocalDate from = LocalDate.parse(dateFrom);
        LocalDate to = LocalDate.parse(dateTo);

        Map<String, Object> dashboard = dashboardService.getDashboardDataByDate(userId, from, to);
        return ResponseEntity.ok(dashboard);
    }
}
