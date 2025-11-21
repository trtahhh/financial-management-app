package com.example.finance.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Layer 2: Fuzzy Matching Service
 * Xử lý typo, teencode, tiếng Việt không dấu bằng thuật toán:
 * - Levenshtein Distance (similarity matching)
 * - Vietnamese accent removal & normalization
 * - Teencode mapping
 * - Phonetic matching
 */
@Service
public class FuzzyMatchingService {
    
    // Vietnamese accent mapping
    private static final Map<Character, String> VIETNAMESE_ACCENTS = new HashMap<>();
    static {
        // a
        VIETNAMESE_ACCENTS.put('à', "a"); VIETNAMESE_ACCENTS.put('á', "a"); VIETNAMESE_ACCENTS.put('ả', "a");
        VIETNAMESE_ACCENTS.put('ã', "a"); VIETNAMESE_ACCENTS.put('ạ', "a");
        VIETNAMESE_ACCENTS.put('ă', "a"); VIETNAMESE_ACCENTS.put('ằ', "a"); VIETNAMESE_ACCENTS.put('ắ', "a");
        VIETNAMESE_ACCENTS.put('ẳ', "a"); VIETNAMESE_ACCENTS.put('ẵ', "a"); VIETNAMESE_ACCENTS.put('ặ', "a");
        VIETNAMESE_ACCENTS.put('â', "a"); VIETNAMESE_ACCENTS.put('ầ', "a"); VIETNAMESE_ACCENTS.put('ấ', "a");
        VIETNAMESE_ACCENTS.put('ẩ', "a"); VIETNAMESE_ACCENTS.put('ẫ', "a"); VIETNAMESE_ACCENTS.put('ậ', "a");
        // e
        VIETNAMESE_ACCENTS.put('è', "e"); VIETNAMESE_ACCENTS.put('é', "e"); VIETNAMESE_ACCENTS.put('ẻ', "e");
        VIETNAMESE_ACCENTS.put('ẽ', "e"); VIETNAMESE_ACCENTS.put('ẹ', "e");
        VIETNAMESE_ACCENTS.put('ê', "e"); VIETNAMESE_ACCENTS.put('ề', "e"); VIETNAMESE_ACCENTS.put('ế', "e");
        VIETNAMESE_ACCENTS.put('ể', "e"); VIETNAMESE_ACCENTS.put('ễ', "e"); VIETNAMESE_ACCENTS.put('ệ', "e");
        // i
        VIETNAMESE_ACCENTS.put('ì', "i"); VIETNAMESE_ACCENTS.put('í', "i"); VIETNAMESE_ACCENTS.put('ỉ', "i");
        VIETNAMESE_ACCENTS.put('ĩ', "i"); VIETNAMESE_ACCENTS.put('ị', "i");
        // o
        VIETNAMESE_ACCENTS.put('ò', "o"); VIETNAMESE_ACCENTS.put('ó', "o"); VIETNAMESE_ACCENTS.put('ỏ', "o");
        VIETNAMESE_ACCENTS.put('õ', "o"); VIETNAMESE_ACCENTS.put('ọ', "o");
        VIETNAMESE_ACCENTS.put('ô', "o"); VIETNAMESE_ACCENTS.put('ồ', "o"); VIETNAMESE_ACCENTS.put('ố', "o");
        VIETNAMESE_ACCENTS.put('ổ', "o"); VIETNAMESE_ACCENTS.put('ỗ', "o"); VIETNAMESE_ACCENTS.put('ộ', "o");
        VIETNAMESE_ACCENTS.put('ơ', "o"); VIETNAMESE_ACCENTS.put('ờ', "o"); VIETNAMESE_ACCENTS.put('ớ', "o");
        VIETNAMESE_ACCENTS.put('ở', "o"); VIETNAMESE_ACCENTS.put('ỡ', "o"); VIETNAMESE_ACCENTS.put('ợ', "o");
        // u
        VIETNAMESE_ACCENTS.put('ù', "u"); VIETNAMESE_ACCENTS.put('ú', "u"); VIETNAMESE_ACCENTS.put('ủ', "u");
        VIETNAMESE_ACCENTS.put('ũ', "u"); VIETNAMESE_ACCENTS.put('ụ', "u");
        VIETNAMESE_ACCENTS.put('ư', "u"); VIETNAMESE_ACCENTS.put('ừ', "u"); VIETNAMESE_ACCENTS.put('ứ', "u");
        VIETNAMESE_ACCENTS.put('ử', "u"); VIETNAMESE_ACCENTS.put('ữ', "u"); VIETNAMESE_ACCENTS.put('ự', "u");
        // y
        VIETNAMESE_ACCENTS.put('ỳ', "y"); VIETNAMESE_ACCENTS.put('ý', "y"); VIETNAMESE_ACCENTS.put('ỷ', "y");
        VIETNAMESE_ACCENTS.put('ỹ', "y"); VIETNAMESE_ACCENTS.put('ỵ', "y");
        // d
        VIETNAMESE_ACCENTS.put('đ', "d");
        
        // Uppercase
        VIETNAMESE_ACCENTS.put('À', "A"); VIETNAMESE_ACCENTS.put('Á', "A"); VIETNAMESE_ACCENTS.put('Ả', "A");
        VIETNAMESE_ACCENTS.put('Ã', "A"); VIETNAMESE_ACCENTS.put('Ạ', "A");
        VIETNAMESE_ACCENTS.put('Ă', "A"); VIETNAMESE_ACCENTS.put('Ằ', "A"); VIETNAMESE_ACCENTS.put('Ắ', "A");
        VIETNAMESE_ACCENTS.put('Ẳ', "A"); VIETNAMESE_ACCENTS.put('Ẵ', "A"); VIETNAMESE_ACCENTS.put('Ặ', "A");
        VIETNAMESE_ACCENTS.put('Â', "A"); VIETNAMESE_ACCENTS.put('Ầ', "A"); VIETNAMESE_ACCENTS.put('Ấ', "A");
        VIETNAMESE_ACCENTS.put('Ẩ', "A"); VIETNAMESE_ACCENTS.put('Ẫ', "A"); VIETNAMESE_ACCENTS.put('Ậ', "A");
        VIETNAMESE_ACCENTS.put('È', "E"); VIETNAMESE_ACCENTS.put('É', "E"); VIETNAMESE_ACCENTS.put('Ẻ', "E");
        VIETNAMESE_ACCENTS.put('Ẽ', "E"); VIETNAMESE_ACCENTS.put('Ẹ', "E");
        VIETNAMESE_ACCENTS.put('Ê', "E"); VIETNAMESE_ACCENTS.put('Ề', "E"); VIETNAMESE_ACCENTS.put('Ế', "E");
        VIETNAMESE_ACCENTS.put('Ể', "E"); VIETNAMESE_ACCENTS.put('Ễ', "E"); VIETNAMESE_ACCENTS.put('Ệ', "E");
        VIETNAMESE_ACCENTS.put('Ì', "I"); VIETNAMESE_ACCENTS.put('Í', "I"); VIETNAMESE_ACCENTS.put('Ỉ', "I");
        VIETNAMESE_ACCENTS.put('Ĩ', "I"); VIETNAMESE_ACCENTS.put('Ị', "I");
        VIETNAMESE_ACCENTS.put('Ò', "O"); VIETNAMESE_ACCENTS.put('Ó', "O"); VIETNAMESE_ACCENTS.put('Ỏ', "O");
        VIETNAMESE_ACCENTS.put('Õ', "O"); VIETNAMESE_ACCENTS.put('Ọ', "O");
        VIETNAMESE_ACCENTS.put('Ô', "O"); VIETNAMESE_ACCENTS.put('Ồ', "O"); VIETNAMESE_ACCENTS.put('Ố', "O");
        VIETNAMESE_ACCENTS.put('Ổ', "O"); VIETNAMESE_ACCENTS.put('Ỗ', "O"); VIETNAMESE_ACCENTS.put('Ộ', "O");
        VIETNAMESE_ACCENTS.put('Ơ', "O"); VIETNAMESE_ACCENTS.put('Ờ', "O"); VIETNAMESE_ACCENTS.put('Ớ', "O");
        VIETNAMESE_ACCENTS.put('Ở', "O"); VIETNAMESE_ACCENTS.put('Ỡ', "O"); VIETNAMESE_ACCENTS.put('Ợ', "O");
        VIETNAMESE_ACCENTS.put('Ù', "U"); VIETNAMESE_ACCENTS.put('Ú', "U"); VIETNAMESE_ACCENTS.put('Ủ', "U");
        VIETNAMESE_ACCENTS.put('Ũ', "U"); VIETNAMESE_ACCENTS.put('Ụ', "U");
        VIETNAMESE_ACCENTS.put('Ư', "U"); VIETNAMESE_ACCENTS.put('Ừ', "U"); VIETNAMESE_ACCENTS.put('Ứ', "U");
        VIETNAMESE_ACCENTS.put('Ử', "U"); VIETNAMESE_ACCENTS.put('Ữ', "U"); VIETNAMESE_ACCENTS.put('Ự', "U");
        VIETNAMESE_ACCENTS.put('Ỳ', "Y"); VIETNAMESE_ACCENTS.put('Ý', "Y"); VIETNAMESE_ACCENTS.put('Ỷ', "Y");
        VIETNAMESE_ACCENTS.put('Ỹ', "Y"); VIETNAMESE_ACCENTS.put('Ỵ', "Y");
        VIETNAMESE_ACCENTS.put('Đ', "D");
    }
    
    // Teencode mapping (phổ biến nhất)
    private static final Map<String, String> TEENCODE_MAP = new HashMap<>();
    static {
        // Numbers as words
        TEENCODE_MAP.put("0", "khong");
        TEENCODE_MAP.put("1", "mot");
        TEENCODE_MAP.put("2", "hai");
        TEENCODE_MAP.put("3", "ba");
        TEENCODE_MAP.put("4", "bon");
        TEENCODE_MAP.put("5", "nam");
        TEENCODE_MAP.put("6", "sau");
        TEENCODE_MAP.put("7", "bay");
        TEENCODE_MAP.put("8", "tam");
        TEENCODE_MAP.put("9", "chin");
        
        // Common shortcuts
        TEENCODE_MAP.put("k", "khong");
        TEENCODE_MAP.put("ko", "khong");
        TEENCODE_MAP.put("hok", "khong");
        TEENCODE_MAP.put("hong", "khong");
        TEENCODE_MAP.put("nx", "nua");
        TEENCODE_MAP.put("dc", "duoc");
        TEENCODE_MAP.put("dk", "duoc");
        TEENCODE_MAP.put("vs", "voi");
        TEENCODE_MAP.put("vz", "voi");
        TEENCODE_MAP.put("mk", "minh");
        TEENCODE_MAP.put("mik", "minh");
        TEENCODE_MAP.put("bik", "biet");
        TEENCODE_MAP.put("bit", "biet");
        TEENCODE_MAP.put("biet", "biet");
        TEENCODE_MAP.put("j", "gi");
        TEENCODE_MAP.put("gj", "gi");
        TEENCODE_MAP.put("ntn", "nhu the nao");
        TEENCODE_MAP.put("sao", "sao");
        TEENCODE_MAP.put("r", "roi");
        TEENCODE_MAP.put("bh", "bao gio");
        TEENCODE_MAP.put("trc", "truoc");
        TEENCODE_MAP.put("sau", "sau");
        TEENCODE_MAP.put("vc", "viec");
        TEENCODE_MAP.put("nc", "nuoc");
        TEENCODE_MAP.put("qc", "quang cao");
        TEENCODE_MAP.put("cx", "cung");
        TEENCODE_MAP.put("nch", "nha");
        TEENCODE_MAP.put("ng", "nguoi");
        TEENCODE_MAP.put("ngta", "nguoi ta");
        TEENCODE_MAP.put("ny", "nguoi yeu");
        TEENCODE_MAP.put("tlh", "that la");
        TEENCODE_MAP.put("uk", "uh");
        TEENCODE_MAP.put("uh", "uh");
        TEENCODE_MAP.put("uhm", "uh");
        TEENCODE_MAP.put("xl", "xin loi");
        TEENCODE_MAP.put("tks", "cam on");
        TEENCODE_MAP.put("ty", "cam on");
        TEENCODE_MAP.put("thanks", "cam on");
        TEENCODE_MAP.put("thank", "cam on");
        
        // Specific to financial context
        TEENCODE_MAP.put("tien", "tien");
        TEENCODE_MAP.put("tiin", "tien");
        TEENCODE_MAP.put("tin", "tien");
        TEENCODE_MAP.put("xeng", "xe");
        TEENCODE_MAP.put("xe", "xe");
        TEENCODE_MAP.put("com", "com");
        TEENCODE_MAP.put("an", "an");
        TEENCODE_MAP.put("mua", "mua");
        TEENCODE_MAP.put("ban", "ban");
    }
    
    /**
     * Remove Vietnamese accents
     */
    public String removeVietnameseAccents(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            String replacement = VIETNAMESE_ACCENTS.get(c);
            result.append(replacement != null ? replacement : c);
        }
        
        return result.toString();
    }
    
    /**
     * Normalize teencode to standard words
     */
    public String normalizeTeencode(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String normalized = text.toLowerCase().trim();
        
        // Replace teencode patterns
        for (Map.Entry<String, String> entry : TEENCODE_MAP.entrySet()) {
            // Word boundary replacement to avoid partial matches
            normalized = normalized.replaceAll("\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
        }
        
        return normalized;
    }
    
    /**
     * Calculate Levenshtein distance (edit distance)
     * Đo độ tương đồng giữa 2 chuỗi (số ký tự cần thay đổi)
     */
    public int calculateLevenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Integer.MAX_VALUE;
        }
        
        int len1 = s1.length();
        int len2 = s2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[len1][len2];
    }
    
    /**
     * Calculate similarity score (0.0 - 1.0)
     * 1.0 = hoàn toàn giống nhau
     * 0.0 = hoàn toàn khác nhau
     */
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) {
            return 1.0;
        }
        
        int distance = calculateLevenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }
    
    /**
     * Normalize text: remove accents + teencode + lowercase + trim
     */
    public String fullNormalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Step 1: Lowercase and trim
        String normalized = text.toLowerCase().trim();
        
        // Step 2: Remove Vietnamese accents
        normalized = removeVietnameseAccents(normalized);
        
        // Step 3: Normalize teencode
        normalized = normalizeTeencode(normalized);
        
        // Step 4: Remove extra spaces
        normalized = normalized.replaceAll("\\s+", " ");
        
        return normalized;
    }
    
    /**
     * Find best match from a list of keywords
     * Returns: {keyword, similarity}
     */
    public MatchResult findBestMatch(String input, List<String> keywords, double threshold) {
        if (input == null || input.isEmpty() || keywords == null || keywords.isEmpty()) {
            return null;
        }
        
        String normalizedInput = fullNormalize(input);
        
        String bestMatch = null;
        double bestSimilarity = 0.0;
        String bestMatchType = "fuzzy";
        
        for (String keyword : keywords) {
            String normalizedKeyword = fullNormalize(keyword);
            
            // Exact WHOLE WORD match using word boundaries
            // Pattern: either at start/end of string or surrounded by spaces
            String wordPattern = "(^|\\s)" + java.util.regex.Pattern.quote(normalizedKeyword) + "(\\s|$)";
            if (normalizedInput.matches(".*" + wordPattern + ".*")) {
                return new MatchResult(keyword, 1.0, "exact");
            }
            
            // Fuzzy match using Levenshtein distance
            double similarity = calculateSimilarity(normalizedInput, normalizedKeyword);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestMatch = keyword;
            }
        }
        
        if (bestSimilarity >= threshold) {
            return new MatchResult(bestMatch, bestSimilarity, "fuzzy");
        }
        
        return null;
    }
    
    /**
     * Match result class
     */
    public static class MatchResult {
        private final String keyword;
        private final double similarity;
        private final String matchType; // "exact" or "fuzzy"
        
        public MatchResult(String keyword, double similarity, String matchType) {
            this.keyword = keyword;
            this.similarity = similarity;
            this.matchType = matchType;
        }
        
        public String getKeyword() { return keyword; }
        public double getSimilarity() { return similarity; }
        public String getMatchType() { return matchType; }
    }
}
