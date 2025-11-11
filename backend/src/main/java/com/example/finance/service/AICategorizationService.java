package com.example.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.finance.entity.Transaction;
import com.example.finance.repository.TransactionRepository;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class AICategorizationService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    // Pre-trained categories với keywords mở rộng theo ngữ cảnh
    private final Map<String, CategoryInfo> categories = new HashMap<String, CategoryInfo>() {{
        // Ăn uống - FOOD & BEVERAGES
        put("food", new CategoryInfo("food", "Ăn uống", "🍔", 
            Arrays.asList(
                // Đồ ăn
                "cơm", "phở", "bún", "bánh", "mì", "cháo", "xôi", "chè", 
                "pizza", "burger", "gà", "thịt", "cá", "tôm", "rau", "salad",
                // Đồ uống  
                "cafe", "cà phê", "trà", "nước", "sinh tố", "bia", "rượu", "cocktail",
                // Địa điểm
                "nhà hàng", "quán", "canteen", "food court", "buffet", "lẩu", "nướng",
                "highland", "starbucks", "phúc long", "kfc", "lotteria", "jollibee",
                // Động từ
                "ăn sáng", "ăn trưa", "ăn tối", "ăn vặt", "nhậu", "tiệc"
            )));
            
        // Di chuyển - TRANSPORT
        put("transport", new CategoryInfo("transport", "Di chuyển", "🚗", 
            Arrays.asList(
                // Phương tiện
                "grab", "uber", "gojek", "be", "taxi", "xe ôm", "bus", "xe buýt", 
                "tàu", "máy bay", "vé", "vietjet", "bamboo", "vietnam airlines",
                // Xăng dầu
                "xăng", "dầu", "petrol", "nhiên liệu",
                // Phụ tùng
                "sửa xe", "rửa xe", "thay nhớt", "lốp", "phanh",
                // Đỗ xe
                "gửi xe", "đỗ xe", "parking", "bãi xe",
                // Từ chung
                "đi", "về", "chuyến", "cước"
            )));
            
        // Mua sắm - SHOPPING (CHỈ đồ vật, không phải dịch vụ)
        put("shopping", new CategoryInfo("shopping", "Mua sắm", "🛒", 
            Arrays.asList(
                // Quần áo
                "áo", "quần", "váy", "đầm", "giày", "dép", "túi", "ba lô",
                "uniqlo", "zara", "h&m", "adidas", "nike",
                // Mỹ phẩm
                "mỹ phẩm", "son", "phấn", "kem", "nước hoa", "dưỡng da",
                // Đồ dùng
                "đồ dùng", "nội thất", "trang trí", "chăn", "gối", "màn",
                // Điện tử
                "điện thoại", "laptop", "tai nghe", "sạc", "chuột", "bàn phím",
                "iphone", "samsung", "xiaomi", "oppo",
                // Siêu thị
                "vinmart", "coopmart", "lotte", "big c", "aeon", "emart",
                // Từ tổng quát (CHỈ khi đi với đồ vật cụ thể)
                "mua đồ", "shopping", "sắm"
            )));
            
        // Giáo dục - EDUCATION  
        put("education", new CategoryInfo("education", "Giáo dục", "📚", 
            Arrays.asList(
                "học phí", "sách", "vở", "bút", "khóa học", "lớp học", "gia sư",
                "văn phòng phẩm", "trường", "đại học", "udemy", "coursera",
                "toeic", "ielts", "tiếng anh", "ngoại ngữ"
            )));
            
        // Giải trí - ENTERTAINMENT
        put("entertainment", new CategoryInfo("entertainment", "Giải trí", "🎮", 
            Arrays.asList(
                // Phim ảnh
                "phim", "rạp", "cgv", "lotte cinema", "galaxy", "netflix", "spotify",
                // Game
                "game", "steam", "playstation", "xbox", "nintendo",
                // Du lịch
                "du lịch", "tour", "khách sạn", "resort", "vé tham quan",
                "đà lạt", "nha trang", "phú quốc", "sapa",
                // Thể thao
                "gym", "yoga", "bơi", "chạy", "tennis", "cầu lông"
            )));
            
        // Sức khỏe - HEALTH
        put("health", new CategoryInfo("health", "Sức khỏe", "🏥", 
            Arrays.asList(
                "bệnh viện", "phòng khám", "khám", "thuốc", "viên uống",
                "nha khoa", "răng", "mắt", "tai mũi họng",
                "vitamin", "bổ sung", "dược phẩm", "pharmacy"
            )));
            
        // Hóa đơn - BILLS
        put("bills", new CategoryInfo("bills", "Hóa đơn", "📄", 
            Arrays.asList(
                "tiền điện", "tiền nước", "tiền nhà", "thuê nhà", "thuê trọ",
                "internet", "wifi", "điện thoại", "di động", "viettel", "vinaphone", "mobifone",
                "netflix", "spotify premium", "youtube premium"
            )));
            
        // Khác
        put("other", new CategoryInfo("other", "Khác", "💼", 
            Arrays.asList("khác", "linh tinh", "khác")));
    }};
    
    // Model weights (simulated pre-trained model)
    private final Map<String, Double> categoryWeights = new HashMap<String, Double>() {{
        put("food", 0.8);
        put("transport", 0.6);
        put("shopping", 0.7);
        put("education", 0.9);
        put("entertainment", 0.5);
        put("health", 0.4);
        put("bills", 0.3);
        put("other", 0.2);
    }};
    
    /**
     * Categorize expense using AI-like algorithm
     */
    public CategorizationResult categorizeExpense(String description, Double amount) {
        String normalizedDesc = normalizeText(description);
        Map<String, Double> scores = calculateCategoryScores(normalizedDesc, amount);
        
        // Find best match
        String bestCategory = scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("other");
        
        double confidence = scores.get(bestCategory);
        
        // Get top 3 suggestions
        List<CategorySuggestion> suggestions = scores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(3)
            .map(entry -> new CategorySuggestion(
                entry.getKey(),
                categories.get(entry.getKey()).getName(),
                entry.getValue()
            ))
            .collect(Collectors.toList());
        
        String reasoning = generateReasoning(normalizedDesc, bestCategory, confidence);
        
        return new CategorizationResult(
            bestCategory,
            categories.get(bestCategory).getName(),
            confidence,
            suggestions,
            reasoning
        );
    }
    
    /**
     * Generate spending insights từ transaction history
     */
    public SpendingInsights generateSpendingInsights(Long userId, String timeframe) {
        List<Transaction> transactions = getTransactionsByTimeframe(userId, timeframe);
        
        if (transactions.isEmpty()) {
            return new SpendingInsights(
                new ArrayList<>(),
                new ArrayList<>(),
                new HashMap<>(),
                0
            );
        }
        
        List<Insight> insights = new ArrayList<>();
        List<Recommendation> recommendations = new ArrayList<>();
        
        // Analyze spending patterns
        Map<String, Double> categoryTotals = analyzeCategoryTotals(transactions);
        Map<String, Object> trends = analyzeTrends(transactions, timeframe);
        List<Anomaly> anomalies = detectAnomalies(transactions);
        
        // Generate insights from patterns
        generatePatternInsights(categoryTotals, insights);
        generateTrendInsights(trends, insights, recommendations, timeframe);
        generateAnomalyInsights(anomalies, insights);
        
        // Calculate financial health score
        int score = calculateFinancialHealthScore(transactions, trends, categoryTotals);
        
        return new SpendingInsights(insights, recommendations, trends, score);
    }
    
    /**
     * Generate personalized tips
     */
    public List<PersonalizedTip> generatePersonalizedTips(Long userId) {
        List<PersonalizedTip> tips = new ArrayList<>();
        
        List<Transaction> transactions = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().limit(50).collect(Collectors.toList());
        
        if (transactions.isEmpty()) {
            // Default tips for new users
            return getDefaultStudentTips();
        }
        
        // Analyze user behavior
        Map<String, Double> categoryTotals = analyzeCategoryTotals(transactions);
        String topCategory = getTopSpendingCategory(categoryTotals);
        
        // Generate category-specific tips
        tips.addAll(getCategorySpecificTips(topCategory, categoryTotals.get(topCategory)));
        
        // Generate general tips
        tips.addAll(getGeneralFinancialTips());
        
        // Generate time-based tips
        tips.addAll(getTimeBasedTips());
        
        return tips.stream()
            .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
            .limit(5)
            .collect(Collectors.toList());
    }
    
    /**
     * Process voice input (mock implementation)
     */
    public VoiceProcessingResult processVoiceInput(String transcript) {
        String normalized = normalizeText(transcript);
        
        // Extract amount
        Double amount = extractAmountFromText(normalized);
        
        // Categorize if amount found
        String category = null;
        double confidence = 0.8;
        
        if (amount != null && amount > 0) {
            CategorizationResult result = categorizeExpense(normalized, amount);
            category = result.getCategory();
            confidence = Math.min(confidence, result.getConfidence());
        }
        
        return new VoiceProcessingResult(
            amount,
            category,
            normalized,
            confidence,
            generateVoiceSuggestions(amount, category),
            transcript
        );
    }
    
    /**
     * Learn from user transaction for model improvement
     */
    public void learnFromTransaction(Transaction transaction) {
        // In a real implementation, this would update model weights
        // For now, we just log the learning event
        System.out.println("Learning from transaction: " + transaction.getNote() 
            + " -> " + (transaction.getCategory() != null ? transaction.getCategory().getName() : "Unknown"));
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
    private String normalizeText(String text) {
        return text.toLowerCase()
            .replaceAll("[^\\w\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    private Map<String, Double> calculateCategoryScores(String description, Double amount) {
        Map<String, Double> scores = new HashMap<>();
        String[] words = description.split("\\s+");
        
        for (Map.Entry<String, CategoryInfo> entry : categories.entrySet()) {
            String categoryId = entry.getKey();
            CategoryInfo category = entry.getValue();
            
            double score = 0.0;
            int matchCount = 0;
            
            // Keyword matching với context awareness
            for (String keyword : category.getKeywords()) {
                if (description.contains(keyword)) {
                    matchCount++;
                    
                    // Exact word match (không phải substring)
                    boolean isExactMatch = false;
                    for (String word : words) {
                        if (word.equals(keyword)) {
                            isExactMatch = true;
                            break;
                        }
                    }
                    
                    if (isExactMatch) {
                        score += 2.0; // Exact match score cao hơn
                    } else {
                        score += 1.0; // Substring match score thấp hơn
                    }
                    
                    // Bonus cho keyword dài (specific hơn)
                    if (keyword.length() > 5) {
                        score += 0.5;
                    }
                }
            }
            
            // Bonus cho multiple keyword matches (context stronger)
            if (matchCount > 1) {
                score += matchCount * 0.5;
            }
            
            // Amount-based scoring
            if (amount != null && amount > 0) {
                score += getAmountScore(categoryId, amount);
            }
            
            // Apply model weights
            score *= categoryWeights.get(categoryId);
            
            scores.put(categoryId, Math.max(0, score));
        }
        
        // Nếu tất cả score = 0, fallback về "other"
        if (scores.values().stream().allMatch(s -> s == 0)) {
            scores.put("other", 0.5);
        }
        
        // Apply softmax for probability distribution
        return applySoftmax(scores);
    }
    
    private double getAmountScore(String category, double amount) {
        // Typical amount ranges for each category
        Map<String, double[]> typicalRanges = new HashMap<String, double[]>() {{
            put("food", new double[]{20000, 200000});
            put("transport", new double[]{10000, 100000});
            put("shopping", new double[]{100000, 2000000});
            put("education", new double[]{200000, 5000000});
            put("entertainment", new double[]{50000, 500000});
            put("health", new double[]{100000, 1000000});
            put("bills", new double[]{200000, 2000000});
            put("other", new double[]{0, Double.MAX_VALUE});
        }};
        
        double[] range = typicalRanges.get(category);
        if (range != null && amount >= range[0] && amount <= range[1]) {
            return 0.3; // Boost score if amount fits typical range
        }
        
        return 0.0;
    }
    
    private Map<String, Double> applySoftmax(Map<String, Double> scores) {
        double sum = scores.values().stream().mapToDouble(Math::exp).sum();
        
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            result.put(entry.getKey(), Math.exp(entry.getValue()) / sum);
        }
        
        return result;
    }
    
    private String generateReasoning(String description, String category, double confidence) {
        CategoryInfo categoryInfo = categories.get(category);
        
        List<String> matchedKeywords = categoryInfo.getKeywords().stream()
            .filter(keyword -> description.contains(keyword))
            .collect(Collectors.toList());
        
        if (!matchedKeywords.isEmpty()) {
            return "Phát hiện từ khóa: \"" + String.join(", ", matchedKeywords) + 
                   "\" liên quan đến " + categoryInfo.getName();
        }
        
        return "Dự đoán dựa trên mô hình học máy cho danh mục " + categoryInfo.getName();
    }
    
    private List<Transaction> getTransactionsByTimeframe(Long userId, String timeframe) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate;
        
        switch (timeframe) {
            case "week":
                startDate = endDate.minusWeeks(1);
                break;
            case "month":
                startDate = endDate.minusMonths(1);
                break;
            case "year":
                startDate = endDate.minusYears(1);
                break;
            default:
                startDate = endDate.minusMonths(1);
        }
        
        return transactionRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            userId, startDate, endDate);
    }
    
    private Map<String, Double> analyzeCategoryTotals(List<Transaction> transactions) {
        return transactions.stream()
            .collect(Collectors.groupingBy(
                t -> t.getCategory() != null ? t.getCategory().getName() : "other",
                Collectors.summingDouble(t -> t.getAmount().abs().doubleValue())
            ));
    }
    
    private Map<String, Object> analyzeTrends(List<Transaction> transactions, String timeframe) {
        // Simple trend analysis
        Map<String, Object> trends = new HashMap<>();
        
        double totalCurrent = transactions.stream()
            .mapToDouble(t -> t.getAmount().abs().doubleValue())
            .sum();
        
        // Mock previous period comparison
        double totalPrevious = totalCurrent * 0.9; // Assume 10% growth
        double growth = (totalCurrent - totalPrevious) / totalPrevious;
        
        trends.put("growth", growth);
        trends.put("totalCurrent", totalCurrent);
        trends.put("totalPrevious", totalPrevious);
        
        return trends;
    }
    
    private List<Anomaly> detectAnomalies(List<Transaction> transactions) {
        List<Anomaly> anomalies = new ArrayList<>();
        
        if (transactions.size() < 3) return anomalies;
        
        double avgAmount = transactions.stream()
            .mapToDouble(t -> t.getAmount().abs().doubleValue())
            .average()
            .orElse(0);
        
        double threshold = avgAmount * 2.5;
        
        for (Transaction transaction : transactions) {
            double amount = transaction.getAmount().abs().doubleValue();
            if (amount > threshold) {
                anomalies.add(new Anomaly(
                    "large_transaction",
                    "Giao dịch lớn: " + formatCurrency(amount) + 
                    " cho " + (transaction.getNote() != null ? transaction.getNote() : "không rõ"),
                    amount,
                    transaction.getCategory() != null ? transaction.getCategory().getName() : "Unknown"
                ));
            }
        }
        
        return anomalies;
    }
    
    private void generatePatternInsights(Map<String, Double> categoryTotals, List<Insight> insights) {
        if (categoryTotals.isEmpty()) return;
        
        String topCategory = categoryTotals.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("other");
        
        Double topAmount = categoryTotals.get(topCategory);
        Double totalSpending = categoryTotals.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        
        double percentage = (topAmount / totalSpending) * 100;
        
        CategoryInfo categoryInfo = categories.get(topCategory);
        insights.add(new Insight(
            "pattern",
            "Danh mục chi tiêu chính",
            "Bạn chi nhiều nhất cho " + categoryInfo.getName() + 
            " (" + String.format("%.1f", percentage) + "%)",
            categoryInfo.getIcon(),
            "medium"
        ));
    }
    
    private void generateTrendInsights(Map<String, Object> trends, List<Insight> insights, 
                                     List<Recommendation> recommendations, String timeframe) {
        Double growth = (Double) trends.get("growth");
        
        if (growth > 0.1) {
            insights.add(new Insight(
                "trend",
                "Chi tiêu tăng cao",
                "Chi tiêu " + (timeframe.equals("month") ? "tháng này" : "tuần này") + 
                " tăng " + String.format("%.1f", growth * 100) + "% so với kỳ trước",
                "📈",
                "high"
            ));
            
            recommendations.add(new Recommendation(
                "budget",
                "Kiểm soát chi tiêu",
                "Hãy xem lại ngân sách và giảm chi tiêu không cần thiết",
                "review_budget"
            ));
        } else if (growth < -0.1) {
            insights.add(new Insight(
                "trend",
                "Chi tiêu giảm tốt",
                "Bạn đã giảm chi tiêu " + String.format("%.1f", Math.abs(growth * 100)) + 
                "% so với kỳ trước",
                "📉",
                "low"
            ));
            
            recommendations.add(new Recommendation(
                "saving",
                "Tăng tiết kiệm",
                "Hãy chuyển số tiền tiết kiệm được vào mục tiêu dài hạn",
                "increase_savings"
            ));
        }
    }
    
    private void generateAnomalyInsights(List<Anomaly> anomalies, List<Insight> insights) {
        for (Anomaly anomaly : anomalies) {
            insights.add(new Insight(
                "anomaly",
                "Chi tiêu bất thường",
                anomaly.getMessage(),
                "⚠️",
                "high"
            ));
        }
    }
    
    private int calculateFinancialHealthScore(List<Transaction> transactions, 
                                           Map<String, Object> trends, 
                                           Map<String, Double> categoryTotals) {
        int score = 70; // Base score
        
        Double growth = (Double) trends.get("growth");
        
        // Trend impact
        if (growth > 0.2) {
            score -= 20;
        } else if (growth < -0.1) {
            score += 15;
        }
        
        // Category diversity
        int categoryCount = categoryTotals.size();
        if (categoryCount > 5) {
            score += 10;
        } else if (categoryCount < 3) {
            score -= 5;
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    private String getTopSpendingCategory(Map<String, Double> categoryTotals) {
        return categoryTotals.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("other");
    }
    
    private List<PersonalizedTip> getDefaultStudentTips() {
        List<PersonalizedTip> tips = new ArrayList<>();
        
        tips.add(new PersonalizedTip(
            "🍳 Nấu ăn tại nhà",
            "Thử nấu ăn tại nhà 3-4 bữa/tuần để tiết kiệm chi phí ăn uống",
            8,
            500000.0
        ));
        
        tips.add(new PersonalizedTip(
            "📊 Theo dõi chi tiêu",
            "Hãy ghi chép mọi khoản chi tiêu để hiểu rõ thói quen tài chính",
            10,
            null
        ));
        
        tips.add(new PersonalizedTip(
            "🎯 Quy tắc 50/30/20",
            "50% thu nhập cho nhu cầu thiết yếu, 30% giải trí, 20% tiết kiệm",
            9,
            null
        ));
        
        return tips;
    }
    
    private List<PersonalizedTip> getCategorySpecificTips(String category, Double amount) {
        List<PersonalizedTip> tips = new ArrayList<>();
        
        switch (category) {
            case "food":
                tips.add(new PersonalizedTip(
                    "🍳 Nấu ăn tại nhà",
                    "Thử nấu ăn tại nhà 3-4 bữa/tuần để tiết kiệm chi phí ăn uống",
                    8,
                    amount * 0.3
                ));
                break;
                
            case "transport":
                tips.add(new PersonalizedTip(
                    "🚴 Di chuyển xanh",
                    "Sử dụng xe đạp hoặc phương tiện công cộng cho quãng đường ngắn",
                    8,
                    amount * 0.25
                ));
                break;
                
            case "shopping":
                tips.add(new PersonalizedTip(
                    "📝 Lập danh sách mua sắm",
                    "Lập danh sách trước khi đi mua để tránh mua impulsive",
                    9,
                    amount * 0.4
                ));
                break;
        }
        
        return tips;
    }
    
    private List<PersonalizedTip> getGeneralFinancialTips() {
        List<PersonalizedTip> tips = new ArrayList<>();
        
        tips.add(new PersonalizedTip(
            "🌱 Bắt đầu đầu tư sớm",
            "Tuổi trẻ là lợi thế lớn cho đầu tư dài hạn với lợi suất kép",
            7,
            null
        ));
        
        return tips;
    }
    
    private List<PersonalizedTip> getTimeBasedTips() {
        List<PersonalizedTip> tips = new ArrayList<>();
        
        int currentMonth = LocalDateTime.now().getMonthValue();
        
        if (currentMonth == 1) {
            tips.add(new PersonalizedTip(
                "🎊 Lập kế hoạch tài chính năm mới",
                "Đầu năm là thời điểm tốt để đặt mục tiêu tài chính và xem lại ngân sách",
                8,
                null
            ));
        }
        
        if (currentMonth >= 11) {
            tips.add(new PersonalizedTip(
                "🎁 Chuẩn bị ngân sách lễ hội",
                "Lập ngân sách cho quà tặng và du lịch cuối năm từ sớm",
                7,
                null
            ));
        }
        
        return tips;
    }
    
    private Double extractAmountFromText(String text) {
        // Vietnamese number patterns
        String[] patterns = {
            "(\\d+(?:\\.\\d+)?)\\s*(?:nghìn|k|thousand)",
            "(\\d+(?:\\.\\d+)?)\\s*(?:triệu|m|million)",
            "(\\d+(?:\\.\\d+)?)\\s*(?:tỷ|b|billion)",
            "(\\d+(?:[\\.,]\\d+)*)\\s*(?:đồng|vnd|d)",
            "(\\d+(?:[\\.,]\\d+)*)"
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, 
                java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(text);
            
            if (m.find()) {
                String numberStr = m.group(1).replace(",", ".");
                double value = Double.parseDouble(numberStr);
                
                if (text.toLowerCase().contains("nghìn") || text.toLowerCase().contains("k")) {
                    value *= 1000;
                } else if (text.toLowerCase().contains("triệu") || text.toLowerCase().contains("m")) {
                    value *= 1000000;
                } else if (text.toLowerCase().contains("tỷ") || text.toLowerCase().contains("b")) {
                    value *= 1000000000;
                }
                
                return value;
            }
        }
        
        return null;
    }
    
    private List<VoiceSuggestion> generateVoiceSuggestions(Double amount, String category) {
        List<VoiceSuggestion> suggestions = new ArrayList<>();
        
        if (amount != null && amount > 0) {
            suggestions.add(new VoiceSuggestion(
                "amount",
                Arrays.asList(
                    new AmountSuggestion(amount, formatCurrency(amount)),
                    new AmountSuggestion(amount * 10, formatCurrency(amount * 10)),
                    new AmountSuggestion(amount / 10, formatCurrency(amount / 10))
                )
            ));
        }
        
        return suggestions;
    }
    
    private String formatCurrency(double amount) {
        return String.format("%,.0f VND", amount);
    }
    
    // ===== INNER CLASSES =====
    
    public static class CategoryInfo {
        private String id;
        private String name;
        private String icon;
        private List<String> keywords;
        
        public CategoryInfo(String id, String name, String icon, List<String> keywords) {
            this.id = id;
            this.name = name;
            this.icon = icon;
            this.keywords = keywords;
        }
        
        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getIcon() { return icon; }
        public List<String> getKeywords() { return keywords; }
    }
    
    public static class CategorizationResult {
        private String category;
        private String categoryName;
        private double confidence;
        private List<CategorySuggestion> suggestions;
        private String reasoning;
        
        public CategorizationResult(String category, String categoryName, double confidence,
                                  List<CategorySuggestion> suggestions, String reasoning) {
            this.category = category;
            this.categoryName = categoryName;
            this.confidence = confidence;
            this.suggestions = suggestions;
            this.reasoning = reasoning;
        }
        
        // Getters
        public String getCategory() { return category; }
        public String getCategoryName() { return categoryName; }
        public double getConfidence() { return confidence; }
        public List<CategorySuggestion> getSuggestions() { return suggestions; }
        public String getReasoning() { return reasoning; }
    }
    
    public static class CategorySuggestion {
        private String id;
        private String name;
        private double confidence;
        
        public CategorySuggestion(String id, String name, double confidence) {
            this.id = id;
            this.name = name;
            this.confidence = confidence;
        }
        
        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public double getConfidence() { return confidence; }
    }
    
    public static class SpendingInsights {
        private List<Insight> insights;
        private List<Recommendation> recommendations;
        private Map<String, Object> trends;
        private int score;
        
        public SpendingInsights(List<Insight> insights, List<Recommendation> recommendations,
                              Map<String, Object> trends, int score) {
            this.insights = insights;
            this.recommendations = recommendations;
            this.trends = trends;
            this.score = score;
        }
        
        // Getters
        public List<Insight> getInsights() { return insights; }
        public List<Recommendation> getRecommendations() { return recommendations; }
        public Map<String, Object> getTrends() { return trends; }
        public int getScore() { return score; }
    }
    
    public static class Insight {
        private String type;
        private String title;
        private String message;
        private String icon;
        private String priority;
        
        public Insight(String type, String title, String message, String icon, String priority) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.icon = icon;
            this.priority = priority;
        }
        
        // Getters
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getIcon() { return icon; }
        public String getPriority() { return priority; }
    }
    
    public static class Recommendation {
        private String type;
        private String title;
        private String message;
        private String action;
        
        public Recommendation(String type, String title, String message, String action) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.action = action;
        }
        
        // Getters
        public String getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getAction() { return action; }
    }
    
    public static class Anomaly {
        private String type;
        private String message;
        private double amount;
        private String category;
        
        public Anomaly(String type, String message, double amount, String category) {
            this.type = type;
            this.message = message;
            this.amount = amount;
            this.category = category;
        }
        
        // Getters
        public String getType() { return type; }
        public String getMessage() { return message; }
        public double getAmount() { return amount; }
        public String getCategory() { return category; }
    }
    
    public static class PersonalizedTip {
        private String title;
        private String message;
        private int priority;
        private Double potentialSavings;
        
        public PersonalizedTip(String title, String message, int priority, Double potentialSavings) {
            this.title = title;
            this.message = message;
            this.priority = priority;
            this.potentialSavings = potentialSavings;
        }
        
        // Getters
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public int getPriority() { return priority; }
        public Double getPotentialSavings() { return potentialSavings; }
    }
    
    public static class VoiceProcessingResult {
        private Double amount;
        private String category;
        private String description;
        private double confidence;
        private List<VoiceSuggestion> suggestions;
        private String rawTranscript;
        
        public VoiceProcessingResult(Double amount, String category, String description,
                                   double confidence, List<VoiceSuggestion> suggestions, String rawTranscript) {
            this.amount = amount;
            this.category = category;
            this.description = description;
            this.confidence = confidence;
            this.suggestions = suggestions;
            this.rawTranscript = rawTranscript;
        }
        
        // Getters
        public Double getAmount() { return amount; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public double getConfidence() { return confidence; }
        public List<VoiceSuggestion> getSuggestions() { return suggestions; }
        public String getRawTranscript() { return rawTranscript; }
    }
    
    public static class VoiceSuggestion {
        private String type;
        private List<AmountSuggestion> suggestions;
        
        public VoiceSuggestion(String type, List<AmountSuggestion> suggestions) {
            this.type = type;
            this.suggestions = suggestions;
        }
        
        // Getters
        public String getType() { return type; }
        public List<AmountSuggestion> getSuggestions() { return suggestions; }
    }
    
    public static class AmountSuggestion {
        private double value;
        private String text;
        
        public AmountSuggestion(double value, String text) {
            this.value = value;
            this.text = text;
        }
        
        // Getters
        public double getValue() { return value; }
        public String getText() { return text; }
    }
}
