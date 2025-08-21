package com.example.finance.service;

import com.example.finance.dto.GoalDTO;
import com.example.finance.entity.Goal;
import com.example.finance.exception.CustomException;
import com.example.finance.mapper.GoalMapper;
import com.example.finance.repository.GoalRepository;
import com.example.finance.repository.WalletRepository;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.entity.Wallet;
import com.example.finance.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import com.example.finance.entity.User;
import com.example.finance.service.EmailService;
import com.example.finance.service.UserService;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import com.example.finance.entity.Category;
import com.example.finance.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalService.class);

    private final GoalRepository repo;
    private final GoalMapper mapper;
    private final NotificationService notificationService;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final EmailService emailService;
    private final UserService userService;
    private final CategoryRepository categoryRepository;

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
        // Tạm thời trả về 0 để tránh circular dependency
        // Có thể implement sau khi đã giải quyết dependency
        log.info("Goal prediction temporarily disabled to avoid circular dependency");
        return BigDecimal.ZERO;
    }

    public GoalDTO save(GoalDTO dto) {
        Goal savedGoal = repo.save(mapper.toEntity(dto));
        
        // Kiểm tra và cập nhật trạng thái mục tiêu sau khi tạo
        try {
            checkAndUpdateGoalStatus(savedGoal.getUser().getId());
            log.info("✅ Goal status check completed after creation for goal: {}", savedGoal.getName());
        } catch (Exception e) {
            log.warn("⚠️ Goal status check failed after creation for goal: {} - Error: {}", savedGoal.getName(), e.getMessage());
        }
        
        return mapper.toDto(savedGoal);
    }   

    public GoalDTO findById(Long id) {
        return repo.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + id));
    }

    /**
     * Cập nhật mục tiêu
     */
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
        
        // Lưu mục tiêu trước
        Goal savedGoal = repo.save(existingGoal);
        
        // Kiểm tra và cập nhật trạng thái mục tiêu
        try {
            checkAndUpdateGoalStatus(savedGoal.getUser().getId());
            log.info("✅ Goal status check completed after update for goal: {}", savedGoal.getName());
        } catch (Exception e) {
            log.warn("⚠️ Goal status check failed after update for goal: {} - Error: {}", savedGoal.getName(), e.getMessage());
        }
        
        return mapper.toDto(savedGoal);
    }

    public void deleteById(Long id) {
        Goal goal = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found with id: " + id));
        
        try {
            // Xóa tất cả notifications liên quan đến goal này trước
            notificationService.deleteAllNotificationsByGoalId(id);
            log.info("✅ Deleted all notifications for goal ID: {}", id);
            
            // Sau đó xóa goal
            repo.deleteById(id);
            log.info("✅ Successfully deleted goal ID: {}", id);
            
        } catch (Exception e) {
            log.error("❌ Failed to delete goal ID: {} - Error: {}", id, e.getMessage());
            throw new RuntimeException("Không thể xóa mục tiêu: " + e.getMessage(), e);
        }
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

    /**
     * Thực hiện mục tiêu: trừ tiền từ ví và tạo giao dịch chi tiêu
     */
    public Map<String, Object> executeGoal(Long goalId, Long userId) {
        try {
            // Tìm mục tiêu
            Goal goal = repo.findById(goalId)
                .orElseThrow(() -> new CustomException("Mục tiêu không tìm thấy"));
            
            // Kiểm tra mục tiêu đã hoàn thành chưa
            if (!"COMPLETED".equals(goal.getStatus())) {
                throw new CustomException("Mục tiêu chưa hoàn thành, không thể thực hiện");
            }
            
            // Kiểm tra mục tiêu đã được thực hiện chưa
            if (goal.getIsExecuted() != null && goal.getIsExecuted()) {
                throw new CustomException("Mục tiêu đã được thực hiện rồi");
            }
            
            // Lấy danh sách ví của user
            List<Wallet> userWallets = walletRepository.findByUserId(userId);
            if (userWallets.isEmpty()) {
                throw new CustomException("Không có ví nào để thực hiện mục tiêu");
            }
            
            // Tìm ví có đủ tiền (ưu tiên ví đầu tiên có đủ tiền)
            Wallet targetWallet = null;
            for (Wallet wallet : userWallets) {
                if (wallet.getBalance() != null && wallet.getBalance().compareTo(goal.getTargetAmount()) >= 0) {
                    targetWallet = wallet;
                    break;
                }
            }
            
            if (targetWallet == null) {
                throw new CustomException("Không đủ tiền trong ví để thực hiện mục tiêu");
            }
            
            // Trừ tiền từ ví
            BigDecimal newBalance = targetWallet.getBalance().subtract(goal.getTargetAmount());
            targetWallet.setBalance(newBalance);
            walletRepository.save(targetWallet);
            
            // Tạo giao dịch chi tiêu
            Transaction transaction = new Transaction();
            transaction.setUser(goal.getUser());
            transaction.setWallet(targetWallet);
            transaction.setAmount(goal.getTargetAmount());
            transaction.setType("expense");
            
            // Tìm category mặc định cho goal execution
            Category defaultCategory = null;
            try {
                // Tìm category có tên "Khác" hoặc "Mục tiêu" hoặc category đầu tiên
                List<Category> allCategories = categoryRepository.findAll();
                defaultCategory = allCategories.stream()
                    .filter(c -> "Khác".equals(c.getName()) || "Mục tiêu".equals(c.getName()))
                    .findFirst()
                    .orElse(allCategories.isEmpty() ? null : allCategories.get(0));
            } catch (Exception e) {
                log.warn("⚠️ Không thể tìm category mặc định: {}", e.getMessage());
            }
            
            transaction.setCategory(defaultCategory);
            transaction.setNote("Thực hiện mục tiêu: " + goal.getName());
            transaction.setDate(LocalDate.now());
            transaction.setCreatedAt(LocalDateTime.now());
            // Transaction entity mặc định isDeleted = false, không cần set
                    
            // Lưu giao dịch
            transactionRepository.save(transaction);
            
            // Đánh dấu mục tiêu đã được thực hiện
            goal.setIsExecuted(true);
            goal.setExecutedAt(LocalDateTime.now());
            goal.setExecutedTransactionId(transaction.getId());
            
            // Đánh dấu mục tiêu đã hoàn thành và thực hiện
            goal.setStatus("EXECUTED");
            goal.setCompletedAt(LocalDateTime.now());
            goal.setCurrentAmount(goal.getTargetAmount());
            
            // Lưu mục tiêu đã cập nhật
            repo.save(goal);
            
            // Tạo thông báo
            notificationService.createGoalNotification(userId, goalId, "GOAL_EXECUTED", 
                "Mục tiêu '" + goal.getName() + "' đã được thực hiện thành công!");
            
            log.info("✅ Goal executed successfully: {} for user ID: {}, amount: {}, wallet: {}", 
                goal.getName(), userId, goal.getTargetAmount(), targetWallet.getName());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Thực hiện mục tiêu thành công!");
            result.put("goal", mapper.toDto(goal));
            result.put("transaction", transaction);
            result.put("newBalance", newBalance);
            result.put("walletName", targetWallet.getName());
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Failed to execute goal ID: {} for user ID: {} - Error: {}", goalId, userId, e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Lấy tiến độ mục tiêu
     */
    public List<Map<String, Object>> getGoalProgress(Long userId) {
        log.info("=== GoalService.getGoalProgress called for userId: {} ===", userId);
        List<Goal> activeGoals = repo.findByUserIdAndIsDeletedFalse(userId);
        log.info("Found {} active goals for user {}", activeGoals.size(), userId);
        
        // Tính tổng số dư của user từ tất cả ví
        BigDecimal totalBalance = walletRepository.findByUserId(userId).stream()
                .map(wallet -> wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("Total balance for user {}: {}", userId, totalBalance);
        
        return activeGoals.stream().map(goal -> {
            log.info("Processing goal: {} (ID: {}) with target: {}", goal.getName(), goal.getId(), goal.getTargetAmount());
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

    /**
     * Kiểm tra và cập nhật trạng thái mục tiêu tự động
     * Được gọi khi có thay đổi về số dư ví hoặc giao dịch
     */
    public void checkAndUpdateGoalStatus(Long userId) {
        List<Goal> activeGoals = repo.findByUserIdAndIsDeletedFalse(userId);
        
        // Tính tổng số dư hiện tại của user
        BigDecimal totalBalance = walletRepository.findByUserId(userId).stream()
            .map(wallet -> wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        for (Goal goal : activeGoals) {
            // Chỉ xử lý các mục tiêu chưa hoàn thành
            if (!"COMPLETED".equals(goal.getStatus())) {
                // Kiểm tra xem mục tiêu có hoàn thành không
                if (totalBalance.compareTo(goal.getTargetAmount()) >= 0) {
                    // Cập nhật trạng thái mục tiêu
                    goal.setStatus("COMPLETED");
                    goal.setCompletedAt(LocalDateTime.now());
                    goal.setCurrentAmount(goal.getTargetAmount()); // Đặt currentAmount = targetAmount
                    
                    // Lưu mục tiêu đã cập nhật
                    Goal savedGoal = repo.save(goal);
                    
                    // Tạo thông báo hoàn thành mục tiêu
                    notificationService.createGoalCompletedNotification(userId, goal.getId(), goal.getName());
                    
                    // Tạo thông báo bổ sung về tiến độ
                    notificationService.createGoalNotification(userId, goal.getId(), "GOAL_PROGRESS", 
                        "Mục tiêu '" + goal.getName() + "' đã hoàn thành 100%! Chúc mừng bạn!");
                    
                    // Gửi email thông báo hoàn thành mục tiêu
                    try {
                        sendGoalCompletionEmail(goal.getUser(), goal);
                        log.info("✅ Goal completion email sent for user ID: {} and goal: {}", userId, goal.getName());
                    } catch (Exception e) {
                        log.warn("⚠️ Failed to send goal completion email for user ID: {} - Error: {}", userId, e.getMessage());
                    }
                } else {
                    // Tính phần trăm tiến độ
                    double progress = totalBalance.divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100))
                                                .doubleValue();
                    
                    // Cập nhật currentAmount
                    goal.setCurrentAmount(totalBalance);
                    
                    // Kiểm tra và tạo thông báo cho các mốc quan trọng
                    if (progress >= 80 && progress < 100) {
                        // Kiểm tra xem đã có thông báo 80% chưa
                        if (!notificationService.existsGoalNotificationByType(userId, goal.getId(), "GOAL_80_PERCENT")) {
                            notificationService.createGoalNotification(userId, goal.getId(), "GOAL_80_PERCENT", 
                                "Mục tiêu '" + goal.getName() + "' đã đạt " + String.format("%.1f", progress) + "%! Gần hoàn thành rồi!");
                            
                            // Gửi email thông báo mốc 80%
                            try {
                                sendGoalMilestoneEmail(goal.getUser(), goal, 80, progress);
                                log.info("✅ Goal 80% milestone email sent for user ID: {} and goal: {}", userId, goal.getName());
                            } catch (Exception e) {
                                log.warn("⚠️ Failed to send goal 80% milestone email for user ID: {} - Error: {}", userId, e.getMessage());
                            }
                        }
                    } else if (progress >= 50 && progress < 80) {
                        // Kiểm tra xem đã có thông báo 50% chưa
                        if (!notificationService.existsGoalNotificationByType(userId, goal.getId(), "GOAL_50_PERCENT")) {
                            notificationService.createGoalNotification(userId, goal.getId(), "GOAL_50_PERCENT", 
                                "Mục tiêu '" + goal.getName() + "' đã đạt " + String.format("%.1f", progress) + "%! Tiếp tục phấn đấu!");
                            
                            // Gửi email thông báo mốc 50%
                            try {
                                sendGoalMilestoneEmail(goal.getUser(), goal, 50, progress);
                                log.info("✅ Goal 50% milestone email sent for user ID: {} and goal: {}", userId, goal.getName());
                            } catch (Exception e) {
                                log.warn("⚠️ Failed to send goal 50% milestone email for user ID: {} - Error: {}", userId, e.getMessage());
                            }
                        }
                    } else if (progress >= 25 && progress < 50) {
                        // Kiểm tra xem đã có thông báo 25% chưa
                        if (!notificationService.existsGoalNotificationByType(userId, goal.getId(), "GOAL_25_PERCENT")) {
                            notificationService.createGoalNotification(userId, goal.getId(), "GOAL_25_PERCENT", 
                                "Mục tiêu '" + goal.getName() + "' đã đạt " + String.format("%.1f", progress) + "%! Bắt đầu tốt!");
                            
                            // Gửi email thông báo mốc 25%
                            try {
                                sendGoalMilestoneEmail(goal.getUser(), goal, 25, progress);
                                log.info("✅ Goal 25% milestone email sent for user ID: {} and goal: {}", userId, goal.getName());
                            } catch (Exception e) {
                                log.warn("⚠️ Failed to send goal 25% milestone email for user ID: {} - Error: {}", userId, e.getMessage());
                            }
                        }
                    }
                    
                    // Lưu mục tiêu đã cập nhật
                    repo.save(goal);
                }
            }
        }
    }

    /**
     * Kiểm tra và cập nhật trạng thái mục tiêu thủ công
     */
    public void manualCheckGoalStatus(Long userId) {
        try {
            log.info("🔍 Manual goal status check requested for user ID: {}", userId);
            checkAndUpdateGoalStatus(userId);
            log.info("✅ Manual goal status check completed for user ID: {}", userId);
        } catch (Exception e) {
            log.error("❌ Manual goal status check failed for user ID: {} - Error: {}", userId, e.getMessage());
        }
    }

    /**
     * Lấy danh sách mục tiêu đã hoàn thành
     */
    public List<GoalDTO> getCompletedGoals(Long userId) {
        try {
            List<Goal> completedGoals = repo.findByUserIdAndStatusAndIsDeletedFalse(userId, "COMPLETED");
            log.info("Found {} completed goals for user ID: {}", completedGoals.size(), userId);
            return completedGoals.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting completed goals for user ID: {}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Lấy danh sách mục tiêu đã thực hiện (đã hoàn thành và thực hiện)
     */
    public List<GoalDTO> getExecutedGoals(Long userId) {
        try {
            List<Goal> executedGoals = repo.findByUserIdAndStatusAndIsDeletedFalse(userId, "EXECUTED");
            log.info("Found {} executed goals for user ID: {}", executedGoals.size(), userId);
            return executedGoals.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting executed goals for user ID: {}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Lấy danh sách mục tiêu đang thực hiện (chưa hoàn thành)
     */
    public List<GoalDTO> getActiveGoals(Long userId) {
        try {
            List<Goal> activeGoals = repo.findByUserIdAndStatusNotInAndIsDeletedFalse(userId, 
                Arrays.asList("COMPLETED", "EXECUTED"));
            log.info("Found {} active goals for user ID: {}", activeGoals.size(), userId);
            return activeGoals.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting active goals for user ID: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Lấy số lượng mục tiêu đã hoàn thành
     */
    public Long countCompletedGoals(Long userId) {
        return repo.countByUserIdAndStatusAndIsDeletedFalse(userId, "COMPLETED");
    }

    /**
     * Lấy tổng số tiền đã tiết kiệm từ các mục tiêu hoàn thành
     */
    public BigDecimal getTotalSavedAmount(Long userId) {
        return repo.findByUserIdAndStatusAndIsDeletedFalse(userId, "COMPLETED")
                  .stream()
                  .map(Goal::getTargetAmount)
                  .filter(Objects::nonNull)
                  .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Gửi email thông báo hoàn thành mục tiêu
     */
    private void sendGoalCompletionEmail(User user, Goal goal) {
        try {
            // Fetch user details fresh to avoid proxy issues
            Optional<User> userOpt = userService.findById(user.getId());
            if (userOpt.isEmpty()) {
                log.warn("⚠️ User not found for user ID: {}", user.getId());
                return;
            }
            User freshUser = userOpt.get();
            if (freshUser.getEmail() == null) {
                log.warn("⚠️ User email is null for user ID: {}", user.getId());
                return;
            }
            
            String subject = "🎉 Chúc mừng! Bạn đã hoàn thành mục tiêu tài chính";
            String content = createGoalCompletionEmailContent(freshUser, goal);
            
            emailService.sendEmail(freshUser.getEmail(), subject, content);
            log.info("✅ Goal completion email sent successfully to user: {} for goal: {}", freshUser.getUsername(), goal.getName());
        } catch (Exception e) {
            log.error("❌ Failed to send goal completion email to user: {} for goal: {}", user.getUsername(), goal.getName(), e);
        }
    }

    /**
     * Gửi email thông báo đạt mốc mục tiêu
     */
    private void sendGoalMilestoneEmail(User user, Goal goal, int milestone, double progress) {
        try {
            // Fetch user details fresh to avoid proxy issues
            Optional<User> userOpt = userService.findById(user.getId());
            if (userOpt.isEmpty()) {
                log.warn("⚠️ User not found for user ID: {}", user.getId());
                return;
            }
            User freshUser = userOpt.get();
            if (freshUser.getEmail() == null) {
                log.warn("⚠️ User email is null for user ID: {}", user.getId());
                return;
            }
            
            String subject = String.format("🎯 Mục tiêu '%s' đã đạt %d%%!", goal.getName(), milestone);
            String content = createGoalMilestoneEmailContent(freshUser, goal, milestone, progress);
            
            emailService.sendEmail(freshUser.getEmail(), subject, content);
            log.info("✅ Goal milestone email sent successfully to user: {} for goal: {}", freshUser.getUsername(), goal.getName(), milestone);
        } catch (Exception e) {
            log.error("❌ Failed to send goal milestone email to user: {} for goal: {} at {}%", user.getUsername(), goal.getName(), milestone, e);
        }
    }

    /**
     * Tạo nội dung email hoàn thành mục tiêu
     */
    private String createGoalCompletionEmailContent(User user, Goal goal) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Chúc mừng hoàn thành mục tiêu!</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }
                    .header { background: linear-gradient(135deg, #28a745, #20c997); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 300; }
                    .header p { margin: 10px 0 0 0; font-size: 16px; opacity: 0.9; }
                    .content { padding: 40px 30px; }
                    .greeting { font-size: 20px; color: #333; margin-bottom: 20px; }
                    .goal-details { background: #f8f9fa; border-radius: 15px; padding: 25px; margin: 25px 0; border-left: 5px solid #28a745; }
                    .goal-name { font-size: 22px; font-weight: bold; color: #28a745; margin-bottom: 15px; }
                    .goal-info { display: flex; justify-content: space-between; margin: 10px 0; }
                    .goal-label { font-weight: 600; color: #666; }
                    .goal-value { font-weight: bold; color: #333; }
                    .cta-section { text-align: center; margin: 35px 0; }
                    .cta-button { display: inline-block; background: #28a745; color: white; padding: 15px 35px; text-decoration: none; border-radius: 25px; font-weight: bold; font-size: 16px; transition: all 0.3s ease; }
                    .cta-button:hover { background: #218838; transform: translateY(-2px); box-shadow: 0 5px 15px rgba(40, 167, 69, 0.3); }
                    .footer { background: #f8f9fa; padding: 25px 30px; text-align: center; color: #666; font-size: 14px; }
                    @media (max-width: 600px) { .container { margin: 10px; } .header, .content, .footer { padding: 20px; } }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Chúc mừng!</h1>
                        <p>Bạn đã hoàn thành mục tiêu tài chính</p>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">Xin chào <strong>%s</strong>!</div>
                        
                        <p>Chúng tôi rất vui mừng thông báo rằng bạn đã đạt được mục tiêu tài chính của mình! Đây là một thành tựu tuyệt vời đáng để ăn mừng.</p>
                        
                        <div class="goal-details">
                            <div class="goal-name">🎯 %s</div>
                            <div class="goal-info">
                                <span class="goal-label">Số tiền tiết kiệm:</span>
                                <span class="goal-value">%,.0f VNĐ</span>
                            </div>
                            <div class="goal-info">
                                <span class="goal-label">Thời gian hoàn thành:</span>
                                <span class="goal-value">%s</span>
                            </div>
                        </div>
                        
                        <p>Bạn đã chứng minh rằng với sự kiên trì và kế hoạch tài chính tốt, mọi mục tiêu đều có thể đạt được. Hãy tiếp tục duy trì thói quen tài chính tuyệt vời này!</p>
                        
                        <div class="cta-section">
                            <a href="http://localhost:3000/goals" class="cta-button">Xem mục tiêu của bạn</a>
                        </div>
                        
                        <p><strong>Lời khuyên:</strong> Hãy xem xét đặt mục tiêu mới để tiếp tục hành trình tài chính thành công của bạn.</p>
                    </div>
                    
                    <div class="footer">
                        <p>Trân trọng,<br><strong>Đội ngũ Finance AI</strong></p>
                        <p style="margin-top: 15px; font-size: 12px; color: #999;">
                            Email này được gửi tự động. Vui lòng không trả lời email này.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            user.getUsername(),
            goal.getName(),
            goal.getTargetAmount(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'lúc' HH:mm"))
        );
    }

    /**
     * Tạo nội dung email đạt mốc mục tiêu
     */
    private String createGoalMilestoneEmailContent(User user, Goal goal, int milestone, double progress) {
        String milestoneText = "";
        String encouragementText = "";
        
        switch (milestone) {
            case 25:
                milestoneText = "Bắt đầu tốt!";
                encouragementText = "Bạn đã có một khởi đầu tuyệt vời. Hãy tiếp tục duy trì động lực!";
                break;
            case 50:
                milestoneText = "Nửa chặng đường!";
                encouragementText = "Bạn đã đi được nửa chặng đường. Hãy kiên trì để đạt đến đích!";
                break;
            case 80:
                milestoneText = "Gần hoàn thành!";
                encouragementText = "Chỉ còn một chút nữa thôi! Bạn đang rất gần với mục tiêu của mình.";
                break;
        }
        
        return String.format("""
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Mục tiêu đạt mốc %d%%</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }
                    .header { background: linear-gradient(135deg, #ffc107, #fd7e14); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 300; }
                    .header p { margin: 10px 0 0 0; font-size: 16px; opacity: 0.9; }
                    .content { padding: 40px 30px; }
                    .greeting { font-size: 20px; color: #333; margin-bottom: 20px; }
                    .milestone-badge { display: inline-block; background: #ffc107; color: #333; padding: 8px 20px; border-radius: 20px; font-weight: bold; font-size: 18px; margin: 20px 0; }
                    .goal-details { background: #fff3cd; border-radius: 15px; padding: 25px; margin: 25px 0; border-left: 5px solid #ffc107; }
                    .goal-name { font-size: 22px; font-weight: bold; color: #856404; margin-bottom: 15px; }
                    .progress-bar { background: #e9ecef; border-radius: 10px; height: 20px; margin: 15px 0; overflow: hidden; }
                    .progress-fill { background: linear-gradient(90deg, #ffc107, #fd7e14); height: 100%%; width: %.1f%%; transition: width 0.3s ease; }
                    .goal-info { display: flex; justify-content: space-between; margin: 10px 0; }
                    .goal-label { font-weight: 600; color: #666; }
                    .goal-value { font-weight: bold; color: #333; }
                    .cta-section { text-align: center; margin: 35px 0; }
                    .cta-button { display: inline-block; background: #ffc107; color: #333; padding: 15px 35px; text-decoration: none; border-radius: 25px; font-weight: bold; font-size: 16px; transition: all 0.3s ease; }
                    .cta-button:hover { background: #e0a800; transform: translateY(-2px); box-shadow: 0 5px 15px rgba(255, 193, 7, 0.3); }
                    .footer { background: #f8f9fa; padding: 25px 30px; text-align: center; color: #666; font-size: 14px; }
                    @media (max-width: 600px) { .container { margin: 10px; } .header, .content, .footer { padding: 20px; } }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎯 Mục tiêu đạt mốc!</h1>
                        <p>%s</p>
                    </div>
                    
                    <div class="content">
                        <div class="greeting">Xin chào <strong>%s</strong>!</div>
                        
                        <div style="text-align: center;">
                            <div class="milestone-badge">%d%% Hoàn thành</div>
                        </div>
                        
                        <p>%s</p>
                        
                        <div class="goal-details">
                            <div class="goal-name">🎯 %s</div>
                            <div class="progress-bar">
                                <div class="progress-fill"></div>
                            </div>
                            <div class="goal-info">
                                <span class="goal-label">Tiến độ hiện tại:</span>
                                <span class="goal-value">%.1f%%</span>
                            </div>
                            <div class="goal-info">
                                <span class="goal-label">Số tiền đã tiết kiệm:</span>
                                <span class="goal-value">%,.0f VNĐ</span>
                            </div>
                            <div class="goal-info">
                                <span class="goal-label">Còn lại:</span>
                                <span class="goal-value">%,.0f VNĐ</span>
                            </div>
                        </div>
                        
                        <p><strong>Lời khuyên:</strong> Hãy duy trì thói quen tiết kiệm hiện tại. Bạn đang làm rất tốt!</p>
                        
                        <div class="cta-section">
                            <a href="http://localhost:3000/goals" class="cta-button">Xem tiến độ mục tiêu</a>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <p>Trân trọng,<br><strong>Đội ngũ Finance AI</strong></p>
                        <p style="margin-top: 15px; font-size: 12px; color: #999;">
                            Email này được gửi tự động. Vui lòng không trả lời email này.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            milestone,
            milestoneText,
            user.getUsername(),
            milestone,
            encouragementText,
            goal.getName(),
            progress,
            goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO,
            goal.getTargetAmount().subtract(goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO)
        );
    }
}
