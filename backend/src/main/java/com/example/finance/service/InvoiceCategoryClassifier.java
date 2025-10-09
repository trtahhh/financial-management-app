package com.example.finance.service;

import com.example.finance.entity.Category;
import com.example.finance.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple heuristic classifier mapping OCR invoice text to an existing Category.
 * Strategy:
 *  - Preprocess: lowercase + strip diacritics
 *  - Keyword dictionary: Map<CategoryName, List<Keyword or weighted phrase>>
 *  - Score = sum(weight) for matched keywords (word boundary or substring) / (normalization factor)
 *  - Select highest score above threshold
 */
@Service
@RequiredArgsConstructor
public class InvoiceCategoryClassifier {

    private final CategoryRepository categoryRepository;

    private volatile Map<String, KeywordProfile> profileCache = new ConcurrentHashMap<>();
    private volatile long lastLoadTs = 0L;
    private static final long RELOAD_INTERVAL_MS = 60_000; // 1 phút

    private static class KeywordProfile {
        Category category;
        List<WeightedKeyword> keywords = new ArrayList<>();
    }
    private record WeightedKeyword(String token, double weight) {}

    public static class Prediction {
        public Long categoryId; public String categoryName; public double confidence;
    }

    public Prediction predict(String rawText) {
        if (rawText == null || rawText.isBlank()) return null;
        ensureProfiles();
        String norm = normalize(rawText);
        double bestScore = 0; KeywordProfile best = null;
        for (KeywordProfile p : profileCache.values()) {
            double score = score(norm, p);
            if (score > bestScore) { bestScore = score; best = p; }
        }
        if (best == null) return null;
        // Confidence: logistic-ish scaling relative to arbitrary scale (cap typical scores around 1.0)
        double conf = Math.min(0.99, bestScore / 8.0);
        if (conf < 0.2) return null; // dưới ngưỡng thì bỏ
        Prediction pred = new Prediction();
        pred.categoryId = best.category.getId();
        pred.categoryName = best.category.getName();
        pred.confidence = conf;
        return pred;
    }

    private double score(String text, KeywordProfile p) {
        double total = 0;
        for (WeightedKeyword wk : p.keywords) {
            if (text.contains(wk.token())) {
                total += wk.weight();
            }
        }
        return total;
    }

    private void ensureProfiles() {
        long now = System.currentTimeMillis();
        if (profileCache.isEmpty() || (now - lastLoadTs) > RELOAD_INTERVAL_MS) {
            synchronized (this) {
                if (profileCache.isEmpty() || (now - lastLoadTs) > RELOAD_INTERVAL_MS) {
                    loadProfiles();
                }
            }
        }
    }

    private void loadProfiles() {
        List<Category> categories = categoryRepository.findAll();
        Map<String, KeywordProfile> map = new HashMap<>();
        for (Category c : categories) {
            KeywordProfile kp = new KeywordProfile();
            kp.category = c;
            kp.keywords = buildKeywordsFor(c.getName(), c.getType());
            map.put(c.getName(), kp);
        }
        this.profileCache = map;
        this.lastLoadTs = System.currentTimeMillis();
    }

    private List<WeightedKeyword> buildKeywordsFor(String name, String type) {
        String normName = normalize(name);
        List<WeightedKeyword> list = new ArrayList<>();
        // Common patterns by category name
        if (normName.contains("an uong") || normName.contains("thuc an") || normName.contains("food")) {
            list.addAll(tokens("an",1.2,"uong",1.0,"thuc an",2.0,"restaurant",1.5,"quan",0.8,"sushi",1.5,"pho",1.2,"bun",1.0,"com",0.8,"pizza",1.2,"ga",0.8));
        } else if (normName.contains("do uong") || normName.contains("coffee") || normName.contains("tra sua") ) {
            list.addAll(tokens("coffee",2.0,"cafe",2.0,"tra sua",1.6,"tea",1.0,"milk tea",1.4,"drink",0.8));
        } else if (normName.contains("di chuyen") || normName.contains("xe") || normName.contains("travel") ) {
            list.addAll(tokens("taxi",2.2,"grab",2.0,"gojek",1.8,"bus",1.0,"xang",1.5,"tram thu phi",1.2,"parking",1.0,"ben xe",1.2));
        } else if (normName.contains("dien") || normName.contains("nuoc") || normName.contains("hoa don") || normName.contains("tien ich") ) {
            list.addAll(tokens("electric",2.0,"dien",2.0,"nuoc",2.0,"water",1.5,"internet",2.0,"wifi",1.5,"hoa don",1.2,"bill",1.2,"gas",1.2));
        } else if (normName.contains("mua sam") || normName.contains("shopping") ) {
            list.addAll(tokens("sieu thi",2.0,"mart",1.6,"supermarket",2.0,"shop",1.0,"clothing",1.4,"giay",1.2,"quan ao",1.5,"lazada",1.5,"tiki",1.5,"shopee",1.5));
        } else if (normName.contains("y te") || normName.contains("suc khoe") || normName.contains("health")) {
            list.addAll(tokens("thuoc",2.0,"nha thuoc",2.0,"pharmacy",2.0,"clinic",1.8,"hospital",2.0,"vien phi",1.6,"kham",1.2));
        } else if (normName.contains("giai tri") || normName.contains("entertainment") ) {
            list.addAll(tokens("cinema",2.0,"rap phim",2.0,"movie",1.5,"game",1.2,"karaoke",2.0,"netflix",1.5,"spotify",1.2));
        } else if (normName.contains("giao duc") || normName.contains("education") || normName.contains("hoc")) {
            list.addAll(tokens("hoc phi",2.2,"hoc",1.5,"tu sach",1.2,"book",1.0,"khoa hoc",1.4,"course",1.4,"university",1.6));
        } else if (normName.contains("luong") || normName.contains("salary") || (type != null && type.equalsIgnoreCase("income"))) {
            list.addAll(tokens("salary",2.5,"luong",2.5,"thuong",1.8,"bonus",1.8,"income",1.5));
        }
        // Fallback: tokens from category name itself (low weight)
        for (String token : normName.split(" ")) {
            if (token.length() > 2) list.add(new WeightedKeyword(token,0.5));
        }
        return list;
    }

    private List<WeightedKeyword> tokens(Object... arr) {
        List<WeightedKeyword> list = new ArrayList<>();
        for (int i=0; i<arr.length; i+=2) {
            String t = normalize(arr[i].toString());
            double w = (double) arr[i+1];
            list.add(new WeightedKeyword(t,w));
        }
        return list;
    }

    private String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
        return n;
    }
}
