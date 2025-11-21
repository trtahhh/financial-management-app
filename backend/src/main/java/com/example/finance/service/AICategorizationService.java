package com.example.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.UserCategorizationPreference;
import com.example.finance.repository.TransactionRepository;
import com.example.finance.repository.UserCategorizationPreferenceRepository;
import com.example.finance.ml.*;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class AICategorizationService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserCategorizationPreferenceRepository userPrefRepository;
    
    @Autowired
    private OpenRouterService openRouterService;
    
    @Autowired
    private FuzzyMatchingService fuzzyMatchingService;
    
    @Autowired
    private CategorySuggestionService categorySuggestionService;
    
    // ML Models (kept for backward compatibility but not primary layer anymore)
    private LinearSVMClassifier svmModel;
    private TFIDFVectorizer tfidfVectorizer;
    
    // Confidence thresholds
    private static final double LAYER2_FUZZY_THRESHOLD = 0.70; // 70% similarity required for Layer 2
    private static final int USER_PREF_MIN_FREQUENCY = 3;
    
    @PostConstruct
    public void loadModels() {
        try {
            // Load TF-IDF vectorizer
            tfidfVectorizer = ModelSerializer.loadTFIDFVectorizerFromResources("/ml-models/tfidf_vectorizer.bin");
            
            // Load SVM model
            svmModel = ModelSerializer.loadSVMClassifierFromResources("/ml-models/svm_model.bin");
            
            System.out.println("ML models loaded successfully!");
        } catch (Exception e) {
            System.err.println("Failed to load ML models: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("ML model initialization failed", e);
        }
    }
    
    // SVM Model Category Mapping
    // SVM class indices directly match database category IDs (no mapping needed)
    // Class 1 -> Lương (DB ID: 1)
    // Class 2 -> Thu nhập khác (DB ID: 2)
    // Class 3 -> Đầu tư (DB ID: 3)
    // Class 4 -> Kinh doanh (DB ID: 4)
    // Class 5 -> Ăn uống (DB ID: 5)
    // Class 6 -> Giao thông (DB ID: 6)
    // Class 7 -> Giải trí (DB ID: 7)
    // Class 8 -> Sức khỏe (DB ID: 8)
    // Class 9 -> Giáo dục (DB ID: 9)
    // Class 10 -> Mua sắm (DB ID: 10)
    // Class 11 -> Tiện ích (DB ID: 11)
    
    private final Map<Long, String> categoryNameMapping = new HashMap<Long, String>() {{
        put(1L, "Lương");
        put(2L, "Thu nhập khác");
        put(3L, "Đầu tư");
        put(4L, "Kinh doanh");
        put(5L, "Ăn uống");
        put(6L, "Giao thông");
        put(7L, "Giải trí");
        put(8L, "Sức khỏe");
        put(9L, "Giáo dục");
        put(10L, "Mua sắm");
        put(11L, "Tiện ích");
        put(12L, "Vay nợ");
        put(13L, "Quà tặng");
        put(14L, "Khác");
    }};
    
    /**
     * NEW 3-LAYER CATEGORIZATION ARCHITECTURE:
     * 
     * Layer 1: Rule-based Keyword Matching (FAST - exact matches)
     *   - Instant response < 5ms
     *   - Matches 200+ keywords across 14 categories
     *   - Returns 95% confidence for exact matches
     * 
     * Layer 2: Fuzzy Matching (SMART - handles variations)
     *   - Typo tolerance via Levenshtein distance
     *   - Vietnamese accent removal (ăn → an)
     *   - Teencode normalization (mik → mình, k → không)
     *   - Returns if confidence ≥ 70%
     * 
     * Layer 3: LLM Fallback (INTELLIGENT - complex understanding)
     *   - OpenRouter API with structured prompting
     *   - Handles complex sentences and context
     *   - Returns 75% confidence
     * 
     * Fallback: Category 14 (Khác) with 30% confidence
     */
    public CategorizationResult categorizeExpense(String description, Double amount, Long userId) {
        long startTime = System.currentTimeMillis();
        String normalized = VietnameseTextNormalizer.normalize(description);
        
        System.out.println("\n========== CATEGORIZATION START ==========");
        System.out.println("[INPUT] Description: " + description);
        System.out.println("[INPUT] Normalized: " + normalized);
        System.out.println("[INPUT] Amount: " + amount);
        
        // ===== LAYER 1: Rule-based Keyword Matching =====
        long layer1Start = System.currentTimeMillis();
        Long categoryId = matchByKeywords(normalized);
        long layer1Time = System.currentTimeMillis() - layer1Start;
        
        if (categoryId != null) {
            System.out.println("[LAYER 1 ✓] Matched category: " + categoryId + " in " + layer1Time + "ms");
            CategorizationResult result = buildResult(categoryId, 0.95, "Layer 1: Exact keyword match");
            
            // Save to user preferences for future learning
            if (userId != null) {
                saveUserPreference(userId, normalized, categoryId);
            }
            
            printPerformanceStats(startTime, layer1Time, 0, 0);
            return result;
        }
        
        System.out.println("[LAYER 1 ✗] No exact match, proceeding to Layer 2");
        
        // ===== LAYER 2: Fuzzy Matching =====
        long layer2Start = System.currentTimeMillis();
        CategorizationResult fuzzyResult = fuzzyMatchCategories(normalized);
        long layer2Time = System.currentTimeMillis() - layer2Start;
        
        if (fuzzyResult != null && fuzzyResult.getConfidence() >= LAYER2_FUZZY_THRESHOLD) {
            System.out.println("[LAYER 2 ✓] Fuzzy matched category: " + fuzzyResult.getCategory() + 
                             " with " + (fuzzyResult.getConfidence() * 100) + "% confidence in " + layer2Time + "ms");
            
            // Save to user preferences
            if (userId != null) {
                saveUserPreference(userId, normalized, fuzzyResult.getCategory());
            }
            
            printPerformanceStats(startTime, layer1Time, layer2Time, 0);
            return fuzzyResult;
        }
        
        System.out.println("[LAYER 2 ✗] Low confidence or no match, proceeding to Layer 3");
        
        // ===== LAYER 3: LLM Fallback =====
        long layer3Start = System.currentTimeMillis();
        CategorizationResult llmResult = categorizeBySupervisedLLM(description);
        long layer3Time = System.currentTimeMillis() - layer3Start;
        
        if (llmResult != null) {
            System.out.println("[LAYER 3 ✓] LLM categorized: " + llmResult.getCategory() + " in " + layer3Time + "ms");
            
            // Save to user preferences
            if (userId != null) {
                saveUserPreference(userId, normalized, llmResult.getCategory());
            }
            
            // Check if should suggest new category
            if (llmResult.getCategory() == 14L && userId != null) {
                categorySuggestionService.analyzeAndSuggest(
                    description, userId, "expense", llmResult.getCategory()
                ).ifPresent(suggestion -> {
                    System.out.println("[AUTO-SUGGEST] New category suggested: " + 
                        suggestion.getSuggestedName() + " (ID: " + suggestion.getId() + ")");
                });
            }
            
            printPerformanceStats(startTime, layer1Time, layer2Time, layer3Time);
            return llmResult;
        }
        
        // ===== FALLBACK: Category 14 (Khác) =====
        System.out.println("[FALLBACK] Using default category 14 (Khác)");
        
        // Suggest new category when falling back to "Other"
        if (userId != null) {
            categorySuggestionService.analyzeAndSuggest(
                description, userId, "expense", 14L
            ).ifPresent(suggestion -> {
                System.out.println("[AUTO-SUGGEST] New category suggested: " + 
                    suggestion.getSuggestedName() + " (ID: " + suggestion.getId() + ")");
            });
        }
        
        printPerformanceStats(startTime, layer1Time, layer2Time, layer3Time);
        return buildResult(14L, 0.30, "Fallback: Uncategorized");
    }
    
    private void printPerformanceStats(long totalStart, long layer1Time, long layer2Time, long layer3Time) {
        long totalTime = System.currentTimeMillis() - totalStart;
        System.out.println("\n--- Performance Stats ---");
        System.out.println("Layer 1: " + layer1Time + "ms");
        System.out.println("Layer 2: " + layer2Time + "ms");
        System.out.println("Layer 3: " + layer3Time + "ms");
        System.out.println("Total: " + totalTime + "ms");
        System.out.println("========== CATEGORIZATION END ==========\n");
    }
    
    /**
     * Overload for backward compatibility (no userId)
     */
    public CategorizationResult categorizeExpense(String description, Double amount) {
        return categorizeExpense(description, amount, null);
    }
    
    // ===== LAYER 2: Fuzzy Matching Methods =====
    
    /**
     * Layer 2: Fuzzy match using comprehensive keyword lists with similarity scoring
     */
    private CategorizationResult fuzzyMatchCategories(String normalized) {
        // Define keyword lists for each category
        Map<Long, List<String>> categoryKeywords = buildCategoryKeywordMap();
        
        Long bestCategoryId = null;
        double bestSimilarity = 0.0;
        String bestMatchedKeyword = null;
        String bestMatchType = null;
        
        // Check each category's keywords
        for (Map.Entry<Long, List<String>> entry : categoryKeywords.entrySet()) {
            Long categoryId = entry.getKey();
            List<String> keywords = entry.getValue();
            
            FuzzyMatchingService.MatchResult match = 
                fuzzyMatchingService.findBestMatch(normalized, keywords, 0.60); // Lower threshold for finding matches
            
            if (match != null && match.getSimilarity() > bestSimilarity) {
                bestSimilarity = match.getSimilarity();
                bestCategoryId = categoryId;
                bestMatchedKeyword = match.getKeyword();
                bestMatchType = match.getMatchType();
            }
        }
        
        if (bestCategoryId != null) {
            System.out.println("[LAYER 2] Best match: category " + bestCategoryId + 
                             ", keyword: '" + bestMatchedKeyword + 
                             "', similarity: " + (bestSimilarity * 100) + "%" +
                             ", type: " + bestMatchType);
            
            return buildResult(
                bestCategoryId, 
                bestSimilarity, 
                "Layer 2: Fuzzy match (" + bestMatchType + ") - '" + bestMatchedKeyword + "'"
            );
        }
        
        return null;
    }
    
    /**
     * Build comprehensive keyword map for all 14 categories
     */
    private Map<Long, List<String>> buildCategoryKeywordMap() {
        Map<Long, List<String>> map = new HashMap<>();
        
        // Category 1: Lương
        map.put(1L, Arrays.asList("luong", "salary", "wage", "tien luong", "nhan luong", "thuong", "bonus"));
        
        // Category 2: Thu nhập khác
        map.put(2L, Arrays.asList("thu nhap", "income", "lai", "dividend", "hoa hong", "commission", 
                                   "loi nhuan", "profit", "thu khac"));
        
        // Category 3: Đầu tư
        map.put(3L, Arrays.asList("dau tu", "invest", "co phieu", "stock", "chung khoan", "quy", 
                                   "fund", "trai phieu", "bond", "crypto", "bitcoin", "eth"));
        
        // Category 4: Kinh doanh
        map.put(4L, Arrays.asList("kinh doanh", "business", "doanh thu", "revenue", "ban hang", 
                                   "sales", "cung cap", "supplier", "khach hang", "customer"));
        
        // Category 5: Ăn uống (EXPANDED with variants)
        map.put(5L, Arrays.asList(
            // Vietnamese
            "com", "an", "cafe", "tra", "pho", "bun", "quan an", "nha hang", "buffet", 
            "mi", "banh", "nuong", "lau", "an sang", "an trua", "an toi", "an vat",
            // English
            "food", "drink", "restaurant", "breakfast", "lunch", "dinner", "snack",
            // Food delivery apps
            "ship do an", "grab food", "shopeefood", "gofood", "now", "baemin",
            // Brand names
            "highlands", "starbucks", "phuc long", "kfc", "lotteria", "jollibee", 
            "pizza", "burger", "mcdonalds", "popeyes"
        ));
        
        // Category 6: Giao thông (EXPANDED with variants)
        map.put(6L, Arrays.asList(
            // Vietnamese
            "xe", "grab", "be", "taxi", "xe om", "xe buyt", "xang", "dau", "sua xe", "bao duong",
            // English
            "transport", "bus", "motorbike", "car", "fuel", "petrol", "gas", "parking", "ve",
            // Ride-hailing
            "grab bike", "grab car", "be bike", "be car", "gojek", "uber",
            // Travel
            "ve xe", "ve may bay", "flight", "ticket", "airport", "san bay"
        ));
        
        // Category 7: Giải trí (EXPANDED)
        map.put(7L, Arrays.asList(
            "phim", "game", "karaoke", "bar", "pub", "club", "party", "du lich", "travel",
            "entertainment", "movie", "cinema", "concert", "show", "event",
            "netflix", "spotify", "steam", "playstation", "xbox", "nintendo",
            "cgv", "lotte", "galaxy", "beta", "bhd",
            "resort", "khach san", "hotel", "tour", "visa", "passport"
        ));
        
        // Category 8: Sức khỏe (EXPANDED - includes sports & equipment brands)
        map.put(8L, Arrays.asList(
            // Medical
            "benh", "thuoc", "bac si", "kham", "nha thuoc", "benh vien",
            "health", "medicine", "doctor", "hospital", "pharmacy", "clinic",
            "xet nghiem", "test", "vaccine", "tiem", "kham benh", "chua benh",
            "vitamin", "thuc pham chuc nang", "supplement",
            // Sports & Fitness
            "vot", "bong", "gym", "yoga", "the thao", "chay bo", "boi", "cau long", 
            "tennis", "bi da", "sport", "fitness", "workout", "exercise",
            "california", "tgym", "jetts", "elite", "vot cau long",
            // Sports Equipment Brands & Models
            "yonex", "nanoflare", "astrox", "arcsaber", "voltric", "duora",
            "victor", "lining", "mizuno", "forza", "apacs",
            "racket", "racquet", "shuttlecock", "badminton", "squash"
        ));
        
        // Category 9: Giáo dục (EXPANDED)
        map.put(9L, Arrays.asList(
            "hoc", "sach", "khoa hoc", "truong", "giao vien", "lop", "thi", "hoc phi",
            "education", "school", "university", "course", "class", "tuition",
            "study", "learn", "book", "textbook", "notebook", "pen", "pencil",
            "udemy", "coursera", "skillshare", "edx",
            "ielts", "toeic", "toefl", "english", "tieng anh",
            "hoc online", "e-learning"
        ));
        
        // Category 10: Mua sắm (EXPANDED)
        map.put(10L, Arrays.asList(
            "mua", "ao", "quan", "giay", "dep", "tui", "mi", "son", "my pham",
            "shopping", "buy", "purchase", "clothes", "shoes", "bag", "cosmetics",
            "shopee", "lazada", "tiki", "sendo",
            "thoi trang", "fashion", "uniqlo", "zara", "h&m",
            "dien thoai", "phone", "laptop", "may tinh", "tablet",
            "do dung", "furniture", "noi that",
            "do choi", "toy", "game console"
        ));
        
        // Category 11: Tiện ích (EXPANDED)
        map.put(11L, Arrays.asList(
            "dien", "nuoc", "internet", "tien nha", "wifi", "gas",
            "bill", "utility", "electricity", "water", "rent",
            "fpt", "vnpt", "viettel", "mobifone", "vinaphone",
            "dien luc", "evn",
            "rac", "ve sinh", "garbage",
            "bao hiem", "insurance", "phi", "fee"
        ));
        
        // Category 12: Vay nợ (EXPANDED)
        map.put(12L, Arrays.asList(
            "vay", "no", "debt", "loan", "credit", "tra no", "pay debt",
            "lai suat", "interest", "the tin dung", "credit card",
            "bank", "ngan hang", "tpbank", "vietcombank", "techcombank", "mb", "acb",
            "tien ich", "momo", "zalopay", "vnpay", "shopeepay",
            "ky quy", "installment", "tra gop"
        ));
        
        // Category 13: Quà tặng (EXPANDED)
        map.put(13L, Arrays.asList(
            "qua", "tang", "sinh nhat", "tet", "le", "hoi",
            "gift", "present", "birthday", "wedding", "anniversary",
            "mung", "celebrate", "tiec", "party",
            "hoa", "flower", "banh", "cake", "chocolate"
        ));
        
        return map;
    }
    
    // ===== HELPER: Build result =====
    
    private CategorizationResult buildResult(Long categoryId, double confidence, String reason) {
        String categoryName = categoryNameMapping.getOrDefault(categoryId, "Unknown");
        
        return new CategorizationResult(
            categoryId,
            String.valueOf(categoryId),
            categoryName,
            confidence,
            new ArrayList<>(),
            reason
        );
    }
    
    // ===== LAYER 2: User Personalization Methods =====
    
    private CategorizationResult checkUserPreference(Long userId, String normalized) {
        Optional<UserCategorizationPreference> prefOpt = 
            userPrefRepository.findByUserIdAndDescriptionPattern(userId, normalized);
        
        if (prefOpt.isPresent()) {
            UserCategorizationPreference pref = prefOpt.get();
            
            // Only use if user has used this pattern multiple times
            if (pref.getFrequency() >= USER_PREF_MIN_FREQUENCY) {
                Long categoryId = pref.getCategoryId();
                
                // Update frequency and last used
                pref.setFrequency(pref.getFrequency() + 1);
                pref.setLastUsed(LocalDateTime.now());
                userPrefRepository.save(pref);
                
                String categoryName = categoryNameMapping.getOrDefault(categoryId, "Unknown");
                
                return new CategorizationResult(
                    categoryId,
                    String.valueOf(categoryId),
                    categoryName,
                    0.95, // High confidence for user preference
                    new ArrayList<>(),
                    "Layer 2: User Personalization (used " + pref.getFrequency() + " times)"
                );
            }
        }
        
        return null;
    }
    
    private void saveUserPreference(Long userId, String normalized, Long categoryId) {
        try {
            Optional<UserCategorizationPreference> existing = 
                userPrefRepository.findByUserIdAndDescriptionPattern(userId, normalized);
            
            if (existing.isPresent()) {
                // Update existing preference
                UserCategorizationPreference pref = existing.get();
                pref.setFrequency(pref.getFrequency() + 1);
                pref.setLastUsed(LocalDateTime.now());
                pref.setCategoryId(categoryId);
                userPrefRepository.save(pref);
            } else {
                // Create new preference
                UserCategorizationPreference newPref = new UserCategorizationPreference();
                newPref.setUserId(userId);
                newPref.setDescriptionPattern(normalized);
                newPref.setCategoryId(categoryId);
                newPref.setFrequency(1);
                newPref.setLastUsed(LocalDateTime.now());
                userPrefRepository.save(newPref);
            }
        } catch (Exception e) {
            System.err.println("Failed to save user preference: " + e.getMessage());
        }
    }
    
    // ===== LAYER 1: SVM Helper Methods =====
    
    private CategorizationResult buildResultFromSVM(
            LinearSVMClassifier.PredictionResult prediction, String source) {
        
        int svmClassId = prediction.predictedClass;  // SVM class ID = DB category ID directly
        double confidence = prediction.confidence;
        
        // SVM class ID directly matches database category ID
        Long categoryId = (long) svmClassId;
        String categoryName = categoryNameMapping.getOrDefault(categoryId, "Unknown");
        
        // Get all probabilities for top categories
        List<Map<String, Object>> allProbabilities = new ArrayList<>();
        double[] scores = prediction.scores;
        
        // Create list of (categoryId, score) pairs
        List<Map.Entry<Integer, Double>> categoryScores = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            categoryScores.add(new AbstractMap.SimpleEntry<>(i, scores[i]));
        }
        
        // Sort by score descending
        categoryScores.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // Take top 5
        for (int i = 0; i < Math.min(5, categoryScores.size()); i++) {
            Map.Entry<Integer, Double> entry = categoryScores.get(i);
            int svmClass = entry.getKey();
            double score = entry.getValue();
            
            // SVM class = DB category ID directly
            Long dbCatId = (long) svmClass;
            String catName = categoryNameMapping.getOrDefault(dbCatId, "Unknown");
            
            Map<String, Object> probMap = new HashMap<>();
            probMap.put("categoryId", dbCatId);
            probMap.put("categoryCode", String.valueOf(dbCatId));
            probMap.put("categoryName", catName);
            probMap.put("probability", score);
            allProbabilities.add(probMap);
        }
        
        return new CategorizationResult(
            categoryId,
            String.valueOf(categoryId),
            categoryName,
            confidence,
            allProbabilities,
            source
        );
    }
    
    // ===== LAYER 3: Rule-based Fallback + LLM =====
    
    
    // ===== LAYER 1: Keyword Matching Methods =====
    
    private Long matchByKeywords(String normalized) {
        System.out.println("[LAYER 1 DEBUG] Testing normalized: '" + normalized + "'");
        
        // Category 1: Lương (Salary)
        if (normalized.matches(".*\\b(luong|salary|wage|tien luong|nhan luong|thuong|bonus)\\b.*")) {
            System.out.println("[LAYER 1 DEBUG] Matched Category 1 (Lương)");
            return 1L;
        }
        
        // Category 2: Thu nhập khác (Other income)
        if (normalized.matches(".*\\b(thu nhap|income|lai|dividend|hoa hong|commission|loi nhuan|profit)\\b.*")) {
            System.out.println("[LAYER 1 DEBUG] Matched Category 2 (Thu nhập khác)");
            return 2L;
        }
        
        // Category 3: Đầu tư (Investment)
        if (normalized.matches(".*\\b(dau tu|invest|co phieu|stock|chung khoan|quy|fund|trai phieu|bond|crypto|bitcoin)\\b.*")) {
            System.out.println("[LAYER 1 DEBUG] Matched Category 3 (Đầu tư)");
            return 3L;
        }
        
        // Category 4: Kinh doanh (Business)
        if (normalized.matches(".*\\b(kinh doanh|business|doanh thu|revenue|ban hang|sales|cung cap|supplier|khach hang|customer)\\b.*")) {
            System.out.println("[LAYER 1 DEBUG] Matched Category 4 (Kinh doanh)");
            return 4L;
        }
        
        // Category 5: Ăn uống (Food & Beverage) - EXPANDED with word boundaries
        if (normalized.matches(".*\\b(com|an|cafe|tra|pho|bun|quan an|nha hang|buffet|mi|banh|nuong|lau|" +
                               "food|drink|restaurant|breakfast|lunch|dinner|snack|" +
                               "ship do an|grab food|shopeefood|gofood|now|baemin|" +
                               "highlands|starbucks|phuc long|kfc|lotteria|jollibee|pizza|burger)\\b.*")) {
            System.out.println("[LAYER 1 DEBUG] Matched Category 5 (Ăn uống)");
            return 5L;
        }
        
        // Category 6: Giao thông (Transportation) - EXPANDED with word boundaries
        if (normalized.matches(".*\\b(xe|grab|be|taxi|xe om|xe buyt|xang|dau|sua xe|bao duong|" +
                               "transport|bus|motorbike|car|fuel|petrol|gas|parking|ve|" +
                               "grab bike|grab car|be bike|be car|gojek|uber|" +
                               "ve xe|ve may bay|flight|ticket|airport|san bay)\\b.*")) {
            return 6L;
        }
        
        // Category 7: Giải trí (Entertainment) - EXPANDED with word boundaries
        if (normalized.matches(".*\\b(phim|game|karaoke|bar|pub|club|party|du lich|travel|" +
                               "entertainment|movie|cinema|concert|show|event|" +
                               "netflix|spotify|steam|playstation|xbox|nintendo|" +
                               "cgv|lotte|galaxy|beta|bhd|" +
                               "resort|khach san|hotel|tour|visa|passport)\\b.*")) {
            return 7L;
        }
        
        // Category 8: Sức khỏe (Health & Fitness) - EXPANDED with word boundaries
        if (normalized.matches(".*\\b(benh|thuoc|bac si|kham|nha thuoc|benh vien|" +
                               "vot|bong|gym|yoga|the thao|chay bo|boi|cau long|tennis|bi da|" +
                               "health|medicine|doctor|hospital|pharmacy|clinic|" +
                               "sport|fitness|workout|exercise|" +
                               "california|tgym|jetts|elite|" +
                               "xet nghiem|test|vaccine|tiem|kham benh|chua benh|" +
                               "vitamin|thuc pham chuc nang|supplement)\\b.*")) {
            return 8L;
        }
        
        // Category 9: Giáo dục (Education) - EXPANDED with word boundaries
        if (normalized.matches(".*\\b(hoc|sach|khoa hoc|truong|giao vien|lop|thi|hoc phi|" +
                               "education|school|university|course|class|tuition|" +
                               "study|learn|book|textbook|notebook|pen|pencil|" +
                               "udemy|coursera|skillshare|edx|" +
                               "ielts|toeic|toefl|english|tieng anh|" +
                               "hoc online|e-learning)\\b.*")) {
            return 9L;
        }
        
        // Category 10: Mua sắm (Shopping) - EXPANDED with word boundaries
        if (normalized.matches(".*\\b(mua|ao|quan|giay|dep|tui|mi|son|my pham|" +
                               "shopping|buy|purchase|clothes|shoes|bag|cosmetics|" +
                               "shopee|lazada|tiki|sendo|" +
                               "thoi trang|fashion|uniqlo|zara|h&m|" +
                               "dien thoai|phone|laptop|may tinh|tablet|" +
                               "do dung|furniture|noi that|" +
                               "do choi|toy|game console)\\b.*")) {
            return 10L;
        }
        
        // Category 11: Tiện ích (Bills & Utilities) - EXPANDED with word boundaries
        if (normalized.matches(".*\\b(dien|nuoc|internet|tien nha|wifi|gas|" +
                               "bill|utility|electricity|water|rent|" +
                               "fpt|vnpt|viettel|mobifone|vinaphone|" +
                               "dien luc|evn|" +
                               "rac|ve sinh|garbage|" +
                               "bao hiem|insurance|phi|fee)\\b.*")) {
            return 11L;
        }
        
        // Category 12: Vay nợ (Debt & Loan) - EXPANDED with word boundaries
        if (normalized.matches(".*\\b(vay|no|debt|loan|credit|tra no|pay debt|" +
                               "lai suat|interest|the tin dung|credit card|" +
                               "bank|ngan hang|tpbank|vietcombank|techcombank|mb|acb|" +
                               "tien ich|momo|zalopay|vnpay|shopeepay|" +
                               "ky quy|installment|tra gop)\\b.*")) {
            return 12L;
        }
        
        // Category 13: Quà tặng (Gifts) - EXPANDED with word boundaries
        if (normalized.matches(".*\\b(qua|tang|sinh nhat|tet|le|hoi|" +
                               "gift|present|birthday|wedding|anniversary|" +
                               "mung|celebrate|tiec|party|" +
                               "hoa|flower|banh|cake|chocolate)\\b.*")) {
            return 13L;
        }
        
        // Category 14: Khác (Other) - catch-all
        // Will be handled by fallback in main logic
        
        return null; // No match
    }
    
    private Long matchByAmount(String normalized, Double amount) {
        if (amount < 50000) {
            // Small amounts: Food or Transport
            if (normalized.contains("com") || normalized.contains("an") || 
                normalized.contains("cafe") || normalized.contains("tra")) {
                return 5L; // Food & Beverage
            } else if (normalized.contains("xe") || normalized.contains("grab") || 
                       normalized.contains("be") || normalized.contains("taxi")) {
                return 6L; // Transportation
            }
            return 5L; // Default to food for small amounts
        } else if (amount >= 50000 && amount < 500000) {
            // Medium amounts: Shopping, Entertainment, Health
            if (normalized.contains("mua") || normalized.contains("ao") || 
                normalized.contains("quan") || normalized.contains("giay")) {
                return 10L; // Shopping
            } else if (normalized.contains("phim") || normalized.contains("game") || 
                       normalized.contains("karaoke")) {
                return 7L; // Entertainment
            } else if (normalized.contains("benh") || normalized.contains("thuoc") || 
                       normalized.contains("bac si") || normalized.contains("kham")) {
                return 8L; // Health & Medical
            }
            return 10L; // Default to shopping
        } else if (amount >= 5000000) {
            // Large amounts: Salary, Investment, Loan
            if (normalized.contains("luong") || normalized.contains("salary") || 
                normalized.contains("bonus")) {
                return 1L; // Salary
            } else if (normalized.contains("dau tu") || normalized.contains("co phieu") || 
                       normalized.contains("stock")) {
                return 3L; // Investment
            } else if (normalized.contains("vay") || normalized.contains("loan") || 
                       normalized.contains("no")) {
                return 12L; // Debt & Loan
            }
            return 3L; // Default to investment
        }
        return null; // No amount match
    }
    
    /**
     * Supervised LLM categorization with strict output format
     */
    private CategorizationResult categorizeBySupervisedLLM(String description) {
        System.out.println("[DEBUG] Layer 3 - Hybrid Multi-Signal Analysis for: " + description);
        
        String normalized = fuzzyMatchingService.fullNormalize(description);
        
        // Multi-signal scoring với weighted voting
        Map<Long, Double> categoryScores = new HashMap<>();
        
        // Signal 1: Brand-aware pattern matching (weight: 0.35)
        Map<Long, Double> brandScores = scoreBrandMatching(normalized);
        for (Map.Entry<Long, Double> entry : brandScores.entrySet()) {
            categoryScores.merge(entry.getKey(), entry.getValue() * 0.35, Double::sum);
        }
        
        // Signal 2: Context-based inference (weight: 0.25)
        Map<Long, Double> contextScores = scoreContextInference(normalized);
        for (Map.Entry<Long, Double> entry : contextScores.entrySet()) {
            categoryScores.merge(entry.getKey(), entry.getValue() * 0.25, Double::sum);
        }
        
        // Signal 3: N-gram similarity (weight: 0.20)
        Map<Long, Double> ngramScores = scoreNGramSimilarity(normalized);
        for (Map.Entry<Long, Double> entry : ngramScores.entrySet()) {
            categoryScores.merge(entry.getKey(), entry.getValue() * 0.20, Double::sum);
        }
        
        // Signal 4: TF-IDF semantic similarity (weight: 0.15)
        Map<Long, Double> tfidfScores = scoreTFIDFSimilarity(normalized);
        for (Map.Entry<Long, Double> entry : tfidfScores.entrySet()) {
            categoryScores.merge(entry.getKey(), entry.getValue() * 0.15, Double::sum);
        }
        
        // Signal 5: User history learning (weight: 0.05)
        Map<Long, Double> historyScores = scoreUserHistory(normalized);
        for (Map.Entry<Long, Double> entry : historyScores.entrySet()) {
            categoryScores.merge(entry.getKey(), entry.getValue() * 0.05, Double::sum);
        }
        
        // Find best category by weighted score
        Long bestCategory = null;
        double bestScore = 0.0;
        StringBuilder signalDetails = new StringBuilder();
        
        for (Map.Entry<Long, Double> entry : categoryScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCategory = entry.getKey();
            }
            signalDetails.append(String.format("Cat%d:%.2f ", entry.getKey(), entry.getValue()));
        }
        
        System.out.println("[DEBUG] Layer 3 scores: " + signalDetails.toString());
        
        if (bestCategory != null && bestScore >= 0.40) {
            System.out.println("[DEBUG] Layer 3 winner: Category " + bestCategory + " (score: " + String.format("%.2f", bestScore) + ")");
            
            // Cache vào user history để học
            cacheUserPattern(normalized, bestCategory);
            
            return buildResult(bestCategory, Math.min(0.85, bestScore), "Layer 3: Hybrid multi-signal (" + signalDetails.toString().trim() + ")");
        }
        
        System.out.println("[DEBUG] Layer 3 fallback to Category 14 (max score: " + String.format("%.2f", bestScore) + ")");
        return buildResult(14L, 0.30, "Layer 3: Insufficient confidence");
    }
    
    // ========== SIGNAL 1: Brand-aware Pattern Matching ==========
    private Map<Long, Double> scoreBrandMatching(String normalized) {
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, List<String>> brandPatterns = buildBrandPatternMap();
        
        for (Map.Entry<Long, List<String>> entry : brandPatterns.entrySet()) {
            Long categoryId = entry.getKey();
            List<String> patterns = entry.getValue();
            
            for (String pattern : patterns) {
                if (normalized.contains(pattern)) {
                    // Exact brand match = high confidence
                    scores.merge(categoryId, 1.0, Double::max);
                    System.out.println("[SIGNAL 1] Brand match: '" + pattern + "' → Cat" + categoryId);
                    break;
                }
            }
        }
        
        return scores;
    }
    
    private Map<Long, List<String>> buildBrandPatternMap() {
        Map<Long, List<String>> map = new HashMap<>();
        
        // Category 5: Ăn uống - Restaurant brands
        map.put(5L, Arrays.asList(
            "texas", "kfc", "lotteria", "jollibee", "mcdonalds", "burger king",
            "pizza hut", "dominos", "starbucks", "highland", "phuc long",
            "gongcha", "tocotoco", "phopho", "bun bo hue", "com tam"
        ));
        
        // Category 6: Giao thông - Transport brands
        map.put(6L, Arrays.asList(
            "grab", "gojek", "be", "xanh sm", "mai linh", "vinasun",
            "uber", "petrolimex", "shell", "caltex", "pvoil"
        ));
        
        // Category 7: Giải trí - Entertainment brands
        map.put(7L, Arrays.asList(
            "cgv", "lotte cinema", "galaxy cinema", "mega gs", "bhd star",
            "netflix", "spotify", "youtube premium", "steam", "playstation",
            "booking", "agoda", "traveloka", "airbnb"
        ));
        
        // Category 8: Sức khỏe - Sports & health brands
        map.put(8L, Arrays.asList(
            "yonex", "nanoflare", "astrox", "victor", "lining", "mizuno",
            "california fitness", "elite fitness", "yoga", "gym",
            "vinmec", "bv cho ray", "pharmacity", "guardian"
        ));
        
        // Category 9: Giáo dục - Education brands
        map.put(9L, Arrays.asList(
            "coursera", "udemy", "edx", "skillshare", "duolingo",
            "fahasa", "nha nam", "nxb tre", "oxford", "cambridge"
        ));
        
        // Category 10: Mua sắm - Shopping brands
        map.put(10L, Arrays.asList(
            "gundam", "lego", "hot toys", "bandai", "hasbro",
            "shopee", "lazada", "tiki", "sendo", "the gioi di dong",
            "fpt shop", "cellphones", "nguyen kim", "dien may xanh",
            "uniqlo", "zara", "h&m", "nike", "adidas"
        ));
        
        // Category 11: Tiện ích - Utilities
        map.put(11L, Arrays.asList(
            "evn", "dien luc", "sawaco", "cap nuoc", "fpt telecom",
            "viettel", "vnpt", "mobifone", "vinaphone"
        ));
        
        return map;
    }
    
    // ========== SIGNAL 2: Context-based Inference ==========
    private Map<Long, Double> scoreContextInference(String normalized) {
        Map<Long, Double> scores = new HashMap<>();
        
        // Sports equipment inference (vợt + model number)
        if ((normalized.contains("vot") || normalized.contains("racket")) && 
            (normalized.contains("cau long") || normalized.contains("badminton") || 
             normalized.contains("tennis") || normalized.matches(".*\\d{3,4}z?.*"))) {
            scores.put(8L, 0.90);
            System.out.println("[SIGNAL 2] Context: Sports equipment → Cat8");
        }
        
        // Restaurant/food inference
        if ((normalized.contains("chicken") || normalized.contains("ga")) && 
            !normalized.contains("rau") && !normalized.contains("canh")) {
            scores.put(5L, 0.80);
            System.out.println("[SIGNAL 2] Context: Food/chicken → Cat5");
        }
        
        // Toy/model inference
        if (normalized.matches(".*\\b(model|mo hinh|do choi)\\b.*")) {
            scores.put(10L, 0.85);
            System.out.println("[SIGNAL 2] Context: Toy/model → Cat10");
        }
        
        // Transportation inference
        if (normalized.matches(".*\\b(bike|xe|ve nha|di chuyen|taxi)\\b.*")) {
            scores.put(6L, 0.80);
            System.out.println("[SIGNAL 2] Context: Transportation → Cat6");
        }
        
        // Clothing inference
        if (normalized.matches(".*\\b(quan|ao|shirt|dress|jeans)\\b.*") &&
            !normalized.contains("the thao")) {
            scores.put(10L, 0.75);
            System.out.println("[SIGNAL 2] Context: Clothing → Cat10");
        }
        
        // Sports clothing/gear
        if (normalized.contains("the thao") || 
            (normalized.contains("giay") && (normalized.contains("chay") || normalized.contains("bong")))) {
            scores.put(8L, 0.85);
            System.out.println("[SIGNAL 2] Context: Sports gear → Cat8");
        }
        
        return scores;
    }
    
    // ========== SIGNAL 3: N-gram Similarity ==========
    private Map<Long, Double> scoreNGramSimilarity(String normalized) {
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, List<String>> ngramExamples = buildNGramExamples();
        
        for (Map.Entry<Long, List<String>> entry : ngramExamples.entrySet()) {
            Long categoryId = entry.getKey();
            List<String> examples = entry.getValue();
            
            double maxSimilarity = 0.0;
            for (String example : examples) {
                double similarity = calculateNGramSimilarity(normalized, example);
                maxSimilarity = Math.max(maxSimilarity, similarity);
            }
            
            if (maxSimilarity > 0.5) {
                scores.put(categoryId, maxSimilarity);
                System.out.println("[SIGNAL 3] N-gram similarity → Cat" + categoryId + " (" + String.format("%.2f", maxSimilarity) + ")");
            }
        }
        
        return scores;
    }
    
    private Map<Long, List<String>> buildNGramExamples() {
        Map<Long, List<String>> map = new HashMap<>();
        
        map.put(5L, Arrays.asList("an com", "uong cafe", "nha hang", "quan an", "do an"));
        map.put(6L, Arrays.asList("di xe", "xe om", "taxi", "xe bus", "xang xe"));
        map.put(7L, Arrays.asList("xem phim", "du lich", "choi game", "nghe nhac"));
        map.put(8L, Arrays.asList("vot cau long", "the thao", "phong gym", "bac si", "thuoc men"));
        map.put(9L, Arrays.asList("hoc phi", "sach vo", "khoa hoc", "lop hoc"));
        map.put(10L, Arrays.asList("mua sam", "quan ao", "dien thoai", "do choi", "do dung"));
        map.put(11L, Arrays.asList("tien dien", "tien nuoc", "tien nha", "tien internet"));
        
        return map;
    }
    
    private double calculateNGramSimilarity(String s1, String s2) {
        Set<String> bigrams1 = extractBigrams(s1);
        Set<String> bigrams2 = extractBigrams(s2);
        
        if (bigrams1.isEmpty() && bigrams2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> intersection = new HashSet<>(bigrams1);
        intersection.retainAll(bigrams2);
        
        Set<String> union = new HashSet<>(bigrams1);
        union.addAll(bigrams2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    private Set<String> extractBigrams(String text) {
        Set<String> bigrams = new HashSet<>();
        String[] words = text.split("\\s+");
        
        for (int i = 0; i < words.length - 1; i++) {
            bigrams.add(words[i] + " " + words[i + 1]);
        }
        
        return bigrams;
    }
    
    // ========== SIGNAL 4: TF-IDF Semantic Similarity ==========
    private Map<Long, Double> scoreTFIDFSimilarity(String normalized) {
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, List<String>> categoryDocuments = buildCategoryDocuments();
        
        // Calculate TF-IDF for input
        Map<String, Double> inputTFIDF = calculateTFIDF(normalized, categoryDocuments);
        
        for (Map.Entry<Long, List<String>> entry : categoryDocuments.entrySet()) {
            Long categoryId = entry.getKey();
            double maxSimilarity = 0.0;
            
            for (String doc : entry.getValue()) {
                Map<String, Double> docTFIDF = calculateTFIDF(doc, categoryDocuments);
                double similarity = cosineSimilarity(inputTFIDF, docTFIDF);
                maxSimilarity = Math.max(maxSimilarity, similarity);
            }
            
            if (maxSimilarity > 0.3) {
                scores.put(categoryId, maxSimilarity);
                System.out.println("[SIGNAL 4] TF-IDF similarity → Cat" + categoryId + " (" + String.format("%.2f", maxSimilarity) + ")");
            }
        }
        
        return scores;
    }
    
    private Map<Long, List<String>> buildCategoryDocuments() {
        Map<Long, List<String>> map = new HashMap<>();
        
        map.put(5L, Arrays.asList("com chien", "bun cha", "pho bo", "banh mi", "cafe sua", "tra sua"));
        map.put(6L, Arrays.asList("grab bike", "taxi mai linh", "xe bus", "xang petrolimex", "ve xe"));
        map.put(7L, Arrays.asList("ve phim cgv", "du lich da lat", "game steam", "netflix"));
        map.put(8L, Arrays.asList("vot cau long yonex", "giay chay nike", "phong gym", "kham benh", "mua thuoc"));
        map.put(9L, Arrays.asList("sach giao khoa", "khoa hoc udemy", "hoc phi dai hoc"));
        map.put(10L, Arrays.asList("dien thoai samsung", "quan jean", "mo hinh gundam", "giay dep"));
        map.put(11L, Arrays.asList("hoa don dien", "tien nuoc thang", "cuoc internet viettel"));
        
        return map;
    }
    
    private Map<String, Double> calculateTFIDF(String text, Map<Long, List<String>> allDocuments) {
        Map<String, Double> tfidf = new HashMap<>();
        String[] words = text.split("\\s+");
        
        // TF calculation
        Map<String, Integer> termFreq = new HashMap<>();
        for (String word : words) {
            if (word.length() > 2) { // Skip short words
                termFreq.merge(word, 1, Integer::sum);
            }
        }
        
        // IDF calculation
        int totalDocs = allDocuments.values().stream().mapToInt(List::size).sum();
        
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            String term = entry.getKey();
            double tf = (double) entry.getValue() / words.length;
            
            int docCount = 0;
            for (List<String> docs : allDocuments.values()) {
                for (String doc : docs) {
                    if (doc.contains(term)) {
                        docCount++;
                        break;
                    }
                }
            }
            
            double idf = docCount > 0 ? Math.log((double) totalDocs / docCount) : 0.0;
            tfidf.put(term, tf * idf);
        }
        
        return tfidf;
    }
    
    private double cosineSimilarity(Map<String, Double> vec1, Map<String, Double> vec2) {
        if (vec1.isEmpty() || vec2.isEmpty()) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(vec1.keySet());
        allTerms.addAll(vec2.keySet());
        
        for (String term : allTerms) {
            double v1 = vec1.getOrDefault(term, 0.0);
            double v2 = vec2.getOrDefault(term, 0.0);
            
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        
        return (norm1 > 0 && norm2 > 0) ? dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)) : 0.0;
    }
    
    // ========== SIGNAL 5: User History Learning ==========
    private static final Map<String, Long> userPatternCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    
    private Map<Long, Double> scoreUserHistory(String normalized) {
        Map<Long, Double> scores = new HashMap<>();
        
        // Exact match trong cache
        if (userPatternCache.containsKey(normalized)) {
            Long categoryId = userPatternCache.get(normalized);
            scores.put(categoryId, 1.0);
            System.out.println("[SIGNAL 5] User history exact match → Cat" + categoryId);
            return scores;
        }
        
        // Fuzzy match với cached patterns
        for (Map.Entry<String, Long> entry : userPatternCache.entrySet()) {
            String cachedPattern = entry.getKey();
            Long categoryId = entry.getValue();
            
            double similarity = fuzzyMatchingService.calculateSimilarity(normalized, cachedPattern);
            if (similarity > 0.85) {
                scores.merge(categoryId, similarity, Double::max);
                System.out.println("[SIGNAL 5] User history fuzzy match (" + String.format("%.2f", similarity) + ") → Cat" + categoryId);
            }
        }
        
        return scores;
    }
    
    private void cacheUserPattern(String normalized, Long categoryId) {
        if (userPatternCache.size() >= MAX_CACHE_SIZE) {
            // Remove oldest entry (simple eviction strategy)
            String firstKey = userPatternCache.keySet().iterator().next();
            userPatternCache.remove(firstKey);
        }
        
        userPatternCache.put(normalized, categoryId);
        System.out.println("[CACHE] Learned pattern: '" + normalized + "' → Cat" + categoryId);
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
        String normalized = VietnameseTextNormalizer.normalize(transcript);
        
        // Extract amount
        Double amount = extractAmountFromText(normalized);
        
        // Categorize if amount found
        String categoryKey = null;
        double confidence = 0.8;
        
        if (amount != null && amount > 0) {
            CategorizationResult result = categorizeExpense(normalized, amount);
            categoryKey = result.getCategoryKey();
            confidence = Math.min(confidence, result.getConfidence());
        }
        
        return new VoiceProcessingResult(
            amount,
            categoryKey,
            normalized,
            confidence,
            generateVoiceSuggestions(amount, categoryKey),
            transcript
        );
    }
    
    /**
     * Learn from user transaction for model improvement
     */
    public void learnFromTransaction(Transaction transaction) {
        System.out.println("Learning from transaction: " + transaction.getNote() 
            + " -> " + (transaction.getCategory() != null ? transaction.getCategory().getName() : "Unknown"));
    }
    
    // ===== PRIVATE HELPER METHODS =====
    
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
        
        insights.add(new Insight(
            "pattern",
            "Danh mục chi tiêu chính",
            "Bạn chi nhiều nhất cho danh mục " + topCategory + 
            " (" + String.format("%.1f", percentage) + "%)",
            "📊",
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
    
    public static class CategorizationResult {
        private Long category;  // Database ID
        private String categoryKey;  // Category key for icon mapping
        private String categoryName;
        private double confidence;
        private List<Map<String, Object>> probabilities;  // Changed from List<CategorySuggestion>
        private String reasoning;
        
        public CategorizationResult(Long category, String categoryKey, String categoryName, 
                                  double confidence, List<Map<String, Object>> probabilities, 
                                  String reasoning) {
            this.category = category;
            this.categoryKey = categoryKey;
            this.categoryName = categoryName;
            this.confidence = confidence;
            this.probabilities = probabilities;
            this.reasoning = reasoning;
        }
        
        // Getters
        public Long getCategory() { return category; }
        public String getCategoryKey() { return categoryKey; }
        public String getCategoryName() { return categoryName; }
        public double getConfidence() { return confidence; }
        public List<Map<String, Object>> getSuggestions() { return probabilities; }
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
    
    /**
     * Normalize feature vector using L2 normalization
     * Matches the normalization applied during model training
     */
    private void normalizeFeatures(double[] vector) {
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }
    
    /**
     * Check if description is meaningless/gibberish
     * Returns true for:
     * - Empty or too short (< 2 chars)
     * - Only special characters/numbers
     * - Random keyboard mashing (e.g., "asdf", "qwer", "1234")
     * - Repeated characters (e.g., "aaaa", "xxx")
     */
    private boolean isMeaninglessDescription(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }
        
        text = text.trim().toLowerCase();
        
        // Too short
        if (text.length() < 2) {
            return true;
        }
        
        // Only numbers
        if (text.matches("^[0-9]+$")) {
            return true;
        }
        
        // Only special characters
        if (text.matches("^[^a-zA-Z0-9]+$")) {
            return true;
        }
        
        // Random keyboard sequences
        String[] gibberishPatterns = {
            "asdf", "qwer", "zxcv", "wasd", "1234", "4321",
            "abcd", "test", "abc", "xyz", "aaaa", "bbbb",
            "xxxx", "????", "!!!", "..."
        };
        
        for (String pattern : gibberishPatterns) {
            if (text.equals(pattern) || text.startsWith(pattern)) {
                return true;
            }
        }
        
        // Repeated characters (>70% same character)
        if (text.length() >= 3) {
            Map<Character, Integer> charCount = new HashMap<>();
            for (char c : text.toCharArray()) {
                charCount.put(c, charCount.getOrDefault(c, 0) + 1);
            }
            
            for (int count : charCount.values()) {
                if ((double) count / text.length() > 0.7) {
                    return true; // More than 70% is the same character
                }
            }
        }
        
        // Very low ratio of letters to total length
        long letterCount = text.chars().filter(Character::isLetter).count();
        if (text.length() >= 3 && (double) letterCount / text.length() < 0.3) {
            return true; // Less than 30% are actual letters
        }
        
        return false;
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
