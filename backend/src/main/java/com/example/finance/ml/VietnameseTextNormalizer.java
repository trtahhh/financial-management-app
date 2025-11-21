package com.example.finance.ml;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VietnameseTextNormalizer {
    
    private static final String[] STOPWORDS = {
        "của", "và", "có", "được", "trong", "cho", "từ", "với", "này", "đã",
        "để", "một", "các", "những", "là", "thì", "không", "còn", "như", "người"
    };
    
    public static String normalize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        text = text.toLowerCase().trim();
        text = removeDiacritics(text);
        text = removeSpecialCharacters(text);
        text = removeStopwords(text);
        text = normalizeWhitespace(text);
        
        return text;
    }
    
    public static List<String> tokenize(String text) {
        String normalized = normalize(text);
        return Arrays.stream(normalized.split("\\s+"))
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());
    }
    
    private static String removeDiacritics(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        
        normalized = normalized.replace('đ', 'd').replace('Đ', 'd');
        
        return normalized;
    }
    
    private static String removeSpecialCharacters(String text) {
        return text.replaceAll("[^a-z0-9\\s]", " ");
    }
    
    private static String removeStopwords(String text) {
        List<String> stopwordList = Arrays.asList(STOPWORDS);
        return Arrays.stream(text.split("\\s+"))
                .filter(word -> !stopwordList.contains(word))
                .collect(Collectors.joining(" "));
    }
    
    private static String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}
