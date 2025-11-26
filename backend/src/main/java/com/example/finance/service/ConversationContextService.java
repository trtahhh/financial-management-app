package com.example.finance.service;

import com.example.finance.entity.User;
import com.example.finance.entity.Transaction;
import com.example.finance.repository.UserRepository;
import com.example.finance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing conversation context and generating personalized, natural responses
 * Inspired by Momo's conversational AI approach
 */
@Service
public class ConversationContextService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    /**
     * Generate personalized greeting based on time, user profile, and recent activity
     */
    public String generatePersonalizedGreeting(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return "Xin chào! Moni có thể giúp gì cho bạn? 😊";
        }
        
        User user = userOpt.get();
        String firstName = extractFirstName(user.getUsername());
        String timeGreeting = getTimeBasedGreeting();
        String emoji = getTimeBasedEmoji();
        
        // Get recent spending insight
        String spendingInsight = getRecentSpendingInsight(userId);
        
        return String.format(
            "%s %s! %s\n\n%s",
            emoji,
            timeGreeting,
            firstName,
            spendingInsight
        );
    }
    
    /**
     * Extract first name from username (handle Vietnamese names)
     */
    private String extractFirstName(String username) {
        if (username == null || username.isEmpty()) {
            return "bạn";
        }
        
        // Remove common prefixes
        username = username.replaceAll("^(mr|ms|mrs|anh|chị|em)\\s*", "");
        
        // For Vietnamese names (Nguyen Van A -> A), Western names (John Doe -> John)
        String[] parts = username.trim().split("\\s+");
        if (parts.length > 0) {
            // Check if looks like Vietnamese full name (3+ parts)
            if (parts.length >= 3) {
                return parts[parts.length - 1]; // Last part is given name
            } else {
                return parts[0]; // First part
            }
        }
        
        return username;
    }
    
    /**
     * Get time-appropriate greeting
     */
    private String getTimeBasedGreeting() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        
        if (hour >= 5 && hour < 12) {
            return "Chào buổi sáng";
        } else if (hour >= 12 && hour < 13) {
            return "Chào buổi trưa";
        } else if (hour >= 13 && hour < 18) {
            return "Chào buổi chiều";
        } else if (hour >= 18 && hour < 22) {
            return "Chào buổi tối";
        } else {
            return "Chào bạn"; // Late night/early morning
        }
    }
    
    /**
     * Get time-based emoji
     */
    private String getTimeBasedEmoji() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        
        if (hour >= 5 && hour < 12) {
            return "🌅";
        } else if (hour >= 12 && hour < 18) {
            return "☀️";
        } else if (hour >= 18 && hour < 22) {
            return "🌆";
        } else {
            return "🌙";
        }
    }
    
    /**
     * Generate insight about recent spending
     */
    private String getRecentSpendingInsight(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        
        List<Transaction> recentTransactions = transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(
            userId, weekAgo, today
        );
        
        if (recentTransactions.isEmpty()) {
            return "Moni thấy bạn chưa có giao dịch gần đây. Hãy bắt đầu ghi chép chi tiêu nhé! 📝";
        }
        
        // Calculate total spending (BigDecimal)
        double totalSpent = recentTransactions.stream()
            .filter(t -> "expense".equalsIgnoreCase(t.getType()))
            .mapToDouble(t -> t.getAmount().doubleValue())
            .sum();
        
        double totalIncome = recentTransactions.stream()
            .filter(t -> "income".equalsIgnoreCase(t.getType()))
            .mapToDouble(t -> t.getAmount().doubleValue())
            .sum();
        
        // Find most spent category
        Map<String, Double> categorySpending = recentTransactions.stream()
            .filter(t -> "expense".equalsIgnoreCase(t.getType()))
            .collect(Collectors.groupingBy(
                t -> t.getCategory() != null ? t.getCategory().getName() : "Khác",
                Collectors.summingDouble(t -> t.getAmount().doubleValue())
            ));
        
        String topCategory = categorySpending.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("chi tiêu");
        
        // Generate natural, friendly insight
        List<String> insights = new ArrayList<>();
        
        if (totalSpent > 0) {
            insights.add(String.format(
                "Tuần này bạn đã chi **%,.0f đ**, chủ yếu cho **%s**",
                totalSpent, topCategory
            ));
        }
        
        if (totalIncome > totalSpent && totalIncome > 0) {
            insights.add("Tuyệt vời! Thu nhập vượt chi tiêu 🎉");
        } else if (totalSpent > totalIncome * 0.8 && totalIncome > 0) {
            insights.add("Hãy chú ý tiết kiệm thêm nhé! 💪");
        }
        
        return insights.isEmpty() 
            ? "Moni sẵn sàng giúp bạn quản lý tài chính thông minh hơn! 💡"
            : String.join(". ", insights) + ".";
    }
    
    /**
     * Generate quick actions based on context
     */
    public List<Map<String, String>> generateQuickActions(Long userId) {
        List<Map<String, String>> actions = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int hour = LocalTime.now().getHour();
        
        // Time-based suggestions
        if (hour >= 6 && hour < 10) {
            actions.add(createAction("☕", "Phân tích chi tiêu Ăn Uống gần đây", "category:food"));
        } else if (hour >= 11 && hour < 14) {
            actions.add(createAction("🍜", "Tìm voucher Ăn Trưa", "voucher:lunch"));
        } else if (hour >= 17 && hour < 21) {
            actions.add(createAction("🍔", "Tìm voucher Ăn Tối", "voucher:dinner"));
        }
        
        // Day-based suggestions
        int dayOfMonth = today.getDayOfMonth();
        if (dayOfMonth >= 25) {
            actions.add(createAction("💰", "Khoản chi lớn nhất tháng qua", "insight:largest"));
        } else if (dayOfMonth <= 5) {
            actions.add(createAction("✨", "Lập kế hoạch chi tiêu tháng mới", "plan:monthly"));
        }
        
        // Common actions
        actions.add(createAction("🎯", "Moni có thể làm những gì?", "help:features"));
        actions.add(createAction("😊", "Phân tích chi tiêu Ăn Uống gần đây", "category:food"));
        actions.add(createAction("🎁", "Tìm voucher Ăn Trưa", "voucher:lunch"));
        actions.add(createAction("💡", "21 tuổi, tiết kiệm như nào?", "advice:saving"));
        actions.add(createAction("💸", "Khoản chi lớn nhất tháng qua", "insight:largest"));
        
        // Get user-specific suggestions
        addPersonalizedActions(userId, actions);
        
        return actions;
    }
    
    /**
     * Add personalized action suggestions based on user behavior
     */
    private void addPersonalizedActions(Long userId, List<Map<String, String>> actions) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate monthStart = currentMonth.atDay(1);
        
        List<Transaction> monthTransactions = transactionRepository.findByUserIdAndDateBetweenOrderByDateDesc(
            userId, monthStart, today
        );
        
        if (monthTransactions.isEmpty()) {
            return;
        }
        
        // Find top spending category
        Map<String, Double> categoryTotals = monthTransactions.stream()
            .filter(t -> "expense".equalsIgnoreCase(t.getType()))
            .collect(Collectors.groupingBy(
                t -> t.getCategory() != null ? t.getCategory().getName() : "Khác",
                Collectors.summingDouble(t -> t.getAmount().doubleValue())
            ));
        
        Optional<Map.Entry<String, Double>> topCategory = categoryTotals.entrySet().stream()
            .max(Map.Entry.comparingByValue());
        
        topCategory.ifPresent(entry -> {
            String emoji = getCategoryEmoji(entry.getKey());
            actions.add(createAction(
                emoji,
                String.format("Phân tích chi tiêu %s tháng này", entry.getKey()),
                "category:" + entry.getKey().toLowerCase()
            ));
        });
    }
    
    /**
     * Get emoji for category
     */
    private String getCategoryEmoji(String category) {
        Map<String, String> emojiMap = Map.of(
            "Ăn uống", "🍜",
            "Giao thông", "🚗",
            "Giải trí", "🎮",
            "Sức khỏe", "💊",
            "Giáo dục", "📚",
            "Mua sắm", "🛍️",
            "Tiện ích", "💡",
            "Vay nợ", "💳",
            "Quà tặng", "🎁"
        );
        return emojiMap.getOrDefault(category, "📊");
    }
    
    /**
     * Create action map
     */
    private Map<String, String> createAction(String emoji, String text, String action) {
        Map<String, String> actionMap = new HashMap<>();
        actionMap.put("emoji", emoji);
        actionMap.put("text", text);
        actionMap.put("action", action);
        return actionMap;
    }
    
    /**
     * Generate natural language response with personality
     */
    public String generateNaturalResponse(String context, Map<String, Object> data) {
        // Use template-based NLG with variations for more natural feel
        Random random = new Random();
        
        switch (context) {
            case "largest_expense":
                String[] largestTemplates = {
                    "Moni tìm thấy khoản chi lớn nhất của bạn rồi! 🔍",
                    "Đây là khoản chi đáng chú ý nhất nhé! 👀",
                    "Ồ, khoản này khá lớn đấy! 💰"
                };
                return largestTemplates[random.nextInt(largestTemplates.length)];
                
            case "saving_advice":
                String[] savingTemplates = {
                    "Moni có vài gợi ý tiết kiệm cho bạn! 💡",
                    "Cùng Moni tìm cách tiết kiệm thông minh nhé! 🎯",
                    "Để Moni giúp bạn chi tiêu khôn ngoan hơn! 💪"
                };
                return savingTemplates[random.nextInt(savingTemplates.length)];
                
            case "category_analysis":
                String[] analysisTemplates = {
                    "Moni đã phân tích chi tiêu theo danh mục! 📊",
                    "Đây là bức tranh chi tiêu của bạn! 🎨",
                    "Cùng xem bạn chi nhiều nhất vào đâu nhé! 👇"
                };
                return analysisTemplates[random.nextInt(analysisTemplates.length)];
                
            default:
                return "Moni hiểu rồi! Để xem nhé... 🤔";
        }
    }
}
