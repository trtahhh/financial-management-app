package com.example.finance.service;

import com.example.finance.dto.GoalDTO;
import com.example.finance.entity.Goal;
import com.example.finance.exception.CustomException;
import com.example.finance.mapper.GoalMapper;
import com.example.finance.repository.GoalRepository;
import com.example.finance.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository repo;
    private final GoalMapper mapper;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final WalletRepository walletRepository;


    public List<GoalDTO> findAll() {
        return repo.findAll().stream().map(mapper::toDto).toList();
    }

    public List<GoalDTO> findByUserId(Long userId) {
        // Tính tổng số dư của user từ tất cả ví
        BigDecimal totalBalance = walletRepository.findByUserId(userId).stream()
            .map(wallet -> wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return repo.findByUserId(userId).stream().map(goal -> {
            GoalDTO dto = mapper.toDto(goal);
            dto.setCurrentBalance(totalBalance);
            
            // Tính phần trăm tiến độ dựa trên số dư hiện tại
            if (goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
                double progress = totalBalance.divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                                            .multiply(BigDecimal.valueOf(100))
                                            .doubleValue();
                dto.setProgress(Math.min(progress, 100.0)); // Giới hạn tối đa 100%
            } else {
                dto.setProgress(0.0);
            }
            
            return dto;
        }).toList();
    }

    public BigDecimal predictNextMonth(Long userId) {
        SimpleRegression regression = new SimpleRegression(true);

        YearMonth start = YearMonth.now().minusMonths(11); // last 12 months
        for (int i = 0; i < 12; i++) {
            BigDecimal total = transactionService.sumByMonth(start.plusMonths(i));
            regression.addData(i, total.doubleValue());
        }
        double next = regression.predict(12);
        if (Double.isNaN(next) || next < 0) next = 0;
        return BigDecimal.valueOf(next).setScale(0, RoundingMode.HALF_UP);
    }

    public GoalDTO save(GoalDTO dto) {
        return mapper.toDto(repo.save(mapper.toEntity(dto)));
    }   

    public GoalDTO findById(Long id) {
        return repo.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + id));
    }

    public GoalDTO update(GoalDTO dto) {
        Goal existingGoal = repo.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + dto.getId()));
        
        // Update only the necessary fields to avoid concurrent modification
        existingGoal.setName(dto.getName());
        existingGoal.setTargetAmount(dto.getTargetAmount());
        existingGoal.setCurrentAmount(dto.getCurrentAmount());
        existingGoal.setDueDate(dto.getDueDate());
        existingGoal.setStatus(dto.getStatus());
        existingGoal.setCompletedAt(dto.getCompletedAt());
        
        return mapper.toDto(repo.save(existingGoal));
    }

    public void deleteById(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Goal not found with id: " + id);
        }
        repo.deleteById(id);
    }

    public GoalDTO completeGoal(Long goalId, Long userId) {
        Goal goal = repo.findById(goalId)
            .orElseThrow(() -> new CustomException("Goal not found"));
        goal.setStatus("COMPLETED");
        goal.setCompletedAt(LocalDateTime.now());
        Goal saved = repo.save(goal);

        notificationService.createGoalCompletedNotification(userId, goalId, goal.getName());

        return mapper.toDto(saved);
    }

    // Thêm các method này vào GoalService:

    /**
     * Lấy tiến độ mục tiêu
     */
    public List<Map<String, Object>> getGoalProgress(Long userId) {
        List<Goal> activeGoals = repo.findByUserIdAndIsDeletedFalse(userId);
        
        // Tính tổng số dư của user từ tất cả ví
        BigDecimal totalBalance = walletRepository.findByUserId(userId).stream()
                .map(wallet -> wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return activeGoals.stream().map(goal -> {
            Map<String, Object> goalData = new HashMap<>();
            goalData.put("goalId", goal.getId());
            goalData.put("goalName", goal.getName());
            goalData.put("targetAmount", goal.getTargetAmount());
            
            // Sử dụng currentAmount từ goal entity (nếu cần hiển thị nơi khác)
            BigDecimal currentAmount = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
            goalData.put("currentAmount", currentAmount);
            goalData.put("currentBalance", totalBalance);
            
            // Đồng bộ với trang Mục tiêu: tính % dựa trên tổng số dư tất cả ví so với target của từng goal
            BigDecimal progressPercentage = goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0 ? 
                    totalBalance.divide(goal.getTargetAmount(), 4, java.math.RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
            
            goalData.put("progressPercentage", progressPercentage.doubleValue());
            
            // Xác định trạng thái theo tiến độ mới
            String status = "in-progress";
            if (progressPercentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
                status = "completed";
            } else if (progressPercentage.compareTo(BigDecimal.valueOf(80)) >= 0) {
                status = "near-completion";
            }
            goalData.put("status", status);
            
            // Tính số tiền còn thiếu
            BigDecimal remainingAmount = goal.getTargetAmount().subtract(currentAmount);
            if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
                remainingAmount = BigDecimal.ZERO;
            }
            goalData.put("remainingAmount", remainingAmount);
            
            return goalData;
        }).toList();
    }

    /**
     * Đếm số mục tiêu đang hoạt động
     */
    public Long countActiveGoals(Long userId) {
        return (long) repo.findByUserIdAndIsDeletedFalse(userId).size();
    }
}
