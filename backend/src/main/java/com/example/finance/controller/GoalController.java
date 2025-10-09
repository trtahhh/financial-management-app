package com.example.finance.controller;

import com.example.finance.dto.GoalDTO;
import com.example.finance.service.GoalService;
import lombok.RequiredArgsConstructor;
import com.example.finance.security.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService service;

    @Autowired
    private JwtUtil jwtUtil;

    private Long extractUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").replace("Bearer ", "");
        return jwtUtil.getUserId(token);
    }

    @GetMapping
    public List<GoalDTO> list(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return service.findByUserId(userId);
    }

    @GetMapping("/predict")
    public Map<String, String> predict(HttpServletRequest request) {
        Long userId = extractUserId(request);
        BigDecimal predicted = service.predictNextMonth(userId);
        String message = String.format("Dự đoán thu nhập tháng tới: %,d VND", predicted.longValue());
        return Map.of("message", message);
    }

    @PostMapping
    public GoalDTO create(@RequestBody GoalDTO dto, HttpServletRequest request) {
        if (dto.getUserId() == null) {
            Long userId = extractUserId(request);
            dto.setUserId(userId);
        }
        return service.save(dto);
    }

    @GetMapping("/{id}")
    public GoalDTO getById(@PathVariable("id") Long id) {   
        return service.findById(id);
    }   

    @PutMapping("/{id}")
    public GoalDTO update(@PathVariable("id") Long id, @RequestBody GoalDTO dto, HttpServletRequest request) {
        dto.setId(id);
        if (dto.getUserId() == null) {
            Long userId = extractUserId(request);
            dto.setUserId(userId);
        }
        return service.update(dto);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable("id") Long id) {
        try {
            service.deleteById(id);
            return Map.of("message", "Đã xóa mục tiêu thành công");
        } catch (Exception e) {
            return Map.of("error", "Lỗi khi xóa mục tiêu: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách mục tiêu đã hoàn thành
     */
    @GetMapping("/completed")
    public List<GoalDTO> getCompletedGoals(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return service.getCompletedGoals(userId);
    }

    /**
     * Lấy thống kê về mục tiêu
     */
    @GetMapping("/stats")
    public Map<String, Object> getGoalStats(HttpServletRequest request) {
        Long userId = extractUserId(request);
        
        Long activeGoals = service.countActiveGoals(userId);
        Long completedGoals = service.countCompletedGoals(userId);
        BigDecimal totalSaved = service.getTotalSavedAmount(userId);
        
        return Map.of(
            "activeGoals", activeGoals,
            "completedGoals", completedGoals,
            "totalSavedAmount", totalSaved != null ? totalSaved : BigDecimal.ZERO
        );
    }

    /**
     * Lấy tiến độ mục tiêu
     */
    @GetMapping("/progress")
    public Map<String, Object> getGoalProgress(HttpServletRequest request) {
        Long userId = extractUserId(request);
        List<GoalDTO> goals = service.findByUserId(userId);
        
        // Tính toán tiến độ cho từng mục tiêu
        List<Map<String, Object>> progressData = goals.stream()
            .map(goal -> {
                double progress = goal.getProgress() != null ? goal.getProgress() : 0.0;
                String status = progress >= 100 ? "Hoàn thành" : 
                               progress >= 80 ? "Gần hoàn thành" :
                               progress >= 50 ? "Nửa chặng đường" :
                               progress >= 25 ? "Bắt đầu tốt" : "Mới bắt đầu";
                
                Map<String, Object> goalProgress = new HashMap<>();
                goalProgress.put("id", goal.getId());
                goalProgress.put("name", goal.getName());
                goalProgress.put("progress", progress);
                goalProgress.put("status", status);
                goalProgress.put("targetAmount", goal.getTargetAmount());
                goalProgress.put("currentAmount", goal.getCurrentAmount());
                
                return goalProgress;
            })
            .toList();
        
        Map<String, Object> result = new HashMap<>();
        result.put("goals", progressData);
        return result;
    }

    /**
     * Kiểm tra trạng thái mục tiêu thủ công (để test)
     */
    @PostMapping("/check-status")
    public Map<String, String> checkGoalStatus(HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            service.manualCheckGoalStatus(userId);
            return Map.of("message", "Đã kiểm tra trạng thái mục tiêu thành công");
        } catch (Exception e) {
            return Map.of("error", "Lỗi khi kiểm tra trạng thái mục tiêu: " + e.getMessage());
        }
    }

    /**
     * Hoàn thành mục tiêu thủ công
     */
    @PostMapping("/{id}/complete")
    public GoalDTO completeGoal(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = extractUserId(request);
        return service.completeGoal(id, userId);
    }

    /**
     * Thực hiện mục tiêu: trừ tiền và tạo giao dịch
     */
    @PostMapping("/{id}/execute")
    public Map<String, Object> executeGoal(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            Long userId = extractUserId(request);
            return service.executeGoal(id, userId);
        } catch (Exception e) {
            return Map.of("success", false, "error", "Lỗi khi thực hiện mục tiêu: " + e.getMessage());
        }
    }
    
    /**
     * Lấy danh sách mục tiêu đã thực hiện
     */
    @GetMapping("/executed")
    public List<GoalDTO> getExecutedGoals(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return service.getExecutedGoals(userId);
    }
    
    /**
     * Lấy danh sách mục tiêu đang thực hiện
     */
    @GetMapping("/active")
    public List<GoalDTO> getActiveGoals(HttpServletRequest request) {
        Long userId = extractUserId(request);
        return service.getActiveGoals(userId);
    }
}
