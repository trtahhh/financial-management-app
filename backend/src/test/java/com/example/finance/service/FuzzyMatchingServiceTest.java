package com.example.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for FuzzyMatchingService
 * Layer 2 fuzzy matching for typos, teencode, Vietnamese without accents
 */
@ExtendWith(MockitoExtension.class)
public class FuzzyMatchingServiceTest {

    private FuzzyMatchingService fuzzyMatchingService;

    @BeforeEach
    public void setUp() {
        fuzzyMatchingService = new FuzzyMatchingService();
    }

    @Test
    public void testRemoveVietnameseAccents_WithAccents() {
        // When: Remove accents from Vietnamese text
        String result = fuzzyMatchingService.removeVietnameseAccents("cà phê sữa đá");
        
        // Then: Returns text without accents
        assertEquals("ca phe sua da", result);
    }

    @Test
    public void testRemoveVietnameseAccents_AllAccentTypes() {
        // When: Remove various Vietnamese accent types
        String result = fuzzyMatchingService.removeVietnameseAccents("àáảãạ ăằắẳẵặ âầấẩẫậ èéẻẽẹ êềếểễệ");
        
        // Then: All accents removed (ă→a has 2 chars in map, â→a has 2 chars, ê→e has 2 chars)
        assertEquals("aaaaa aaaaaa aaaaaa eeeee eeeeee", result);
    }

    @Test
    public void testRemoveVietnameseAccents_Uppercase() {
        // When: Remove accents from uppercase text
        String result = fuzzyMatchingService.removeVietnameseAccents("VIỆT NAM");
        
        // Then: Returns uppercase without accents
        assertEquals("VIET NAM", result);
    }

    @Test
    public void testRemoveVietnameseAccents_NullInput() {
        // When: Remove accents from null
        String result = fuzzyMatchingService.removeVietnameseAccents(null);
        
        // Then: Returns null
        assertNull(result);
    }

    @Test
    public void testRemoveVietnameseAccents_EmptyString() {
        // When: Remove accents from empty string
        String result = fuzzyMatchingService.removeVietnameseAccents("");
        
        // Then: Returns empty string
        assertEquals("", result);
    }

    @Test
    public void testNormalizeTeencode_CommonShortcuts() {
        // When: Normalize teencode
        String result = fuzzyMatchingService.normalizeTeencode("k bik j vs mk");
        
        // Then: Returns normalized text
        assertTrue(result.contains("khong"));
        assertTrue(result.contains("biet"));
        assertTrue(result.contains("gi"));
        assertTrue(result.contains("voi"));
        assertTrue(result.contains("minh"));
    }

    @Test
    public void testNormalizeTeencode_Numbers() {
        // When: Normalize numbers as words
        String result = fuzzyMatchingService.normalizeTeencode("0 1 2 3 4 5 6 7 8 9");
        
        // Then: Numbers converted to words
        assertTrue(result.contains("khong"));
        assertTrue(result.contains("mot"));
        assertTrue(result.contains("hai"));
        assertTrue(result.contains("ba"));
    }

    @Test
    public void testNormalizeTeencode_NullInput() {
        // When: Normalize null
        String result = fuzzyMatchingService.normalizeTeencode(null);
        
        // Then: Returns null
        assertNull(result);
    }

    @Test
    public void testNormalizeTeencode_EmptyString() {
        // When: Normalize empty string
        String result = fuzzyMatchingService.normalizeTeencode("");
        
        // Then: Returns empty string
        assertEquals("", result);
    }

    @Test
    public void testCalculateLevenshteinDistance_SameStrings() {
        // When: Calculate distance between same strings
        int distance = fuzzyMatchingService.calculateLevenshteinDistance("hello", "hello");
        
        // Then: Distance is 0
        assertEquals(0, distance);
    }

    @Test
    public void testCalculateLevenshteinDistance_OneCharDifference() {
        // When: Calculate distance with one char different
        int distance = fuzzyMatchingService.calculateLevenshteinDistance("hello", "hallo");
        
        // Then: Distance is 1
        assertEquals(1, distance);
    }

    @Test
    public void testCalculateLevenshteinDistance_Insertion() {
        // When: Calculate distance with insertion
        int distance = fuzzyMatchingService.calculateLevenshteinDistance("hello", "helllo");
        
        // Then: Distance is 1 (one insertion)
        assertEquals(1, distance);
    }

    @Test
    public void testCalculateLevenshteinDistance_Deletion() {
        // When: Calculate distance with deletion
        int distance = fuzzyMatchingService.calculateLevenshteinDistance("hello", "helo");
        
        // Then: Distance is 1 (one deletion)
        assertEquals(1, distance);
    }

    @Test
    public void testCalculateLevenshteinDistance_CompletelyDifferent() {
        // When: Calculate distance between different strings
        int distance = fuzzyMatchingService.calculateLevenshteinDistance("cat", "dog");
        
        // Then: Distance is 3 (all chars different)
        assertEquals(3, distance);
    }

    @Test
    public void testCalculateLevenshteinDistance_NullInput() {
        // When: Calculate distance with null
        int distance = fuzzyMatchingService.calculateLevenshteinDistance(null, "hello");
        
        // Then: Returns max value
        assertEquals(Integer.MAX_VALUE, distance);
    }

    @Test
    public void testCalculateSimilarity_IdenticalStrings() {
        // When: Calculate similarity for identical strings
        double similarity = fuzzyMatchingService.calculateSimilarity("hello", "hello");
        
        // Then: Similarity is 1.0
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    public void testCalculateSimilarity_SimilarStrings() {
        // When: Calculate similarity for similar strings
        double similarity = fuzzyMatchingService.calculateSimilarity("hello", "hallo");
        
        // Then: Similarity is high but not 1.0
        assertTrue(similarity > 0.7);
        assertTrue(similarity < 1.0);
    }

    @Test
    public void testCalculateSimilarity_DifferentStrings() {
        // When: Calculate similarity for different strings
        double similarity = fuzzyMatchingService.calculateSimilarity("cat", "dog");
        
        // Then: Similarity is low
        assertTrue(similarity < 0.5);
    }

    @Test
    public void testCalculateSimilarity_NullInput() {
        // When: Calculate similarity with null
        double similarity = fuzzyMatchingService.calculateSimilarity(null, "hello");
        
        // Then: Similarity is 0
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    public void testFullNormalize_CompleteNormalization() {
        // When: Full normalize Vietnamese text with teencode
        String result = fuzzyMatchingService.fullNormalize("Mua  CÀ  PHÊ  k bik  j");
        
        // Then: Returns fully normalized text (lowercase, no accents, teencode normalized, spaces trimmed)
        assertEquals("mua ca phe khong biet gi", result);
    }

    @Test
    public void testFullNormalize_ExtraSpaces() {
        // When: Normalize text with extra spaces
        String result = fuzzyMatchingService.fullNormalize("  hello   world  ");
        
        // Then: Extra spaces removed
        assertEquals("hello world", result);
    }

    @Test
    public void testFullNormalize_NullInput() {
        // When: Normalize null
        String result = fuzzyMatchingService.fullNormalize(null);
        
        // Then: Returns empty string
        assertEquals("", result);
    }

    @Test
    public void testFullNormalize_EmptyString() {
        // When: Normalize empty string
        String result = fuzzyMatchingService.fullNormalize("");
        
        // Then: Returns empty string
        assertEquals("", result);
    }

    @Test
    public void testFindBestMatch_ExactMatch() {
        // Given: Keywords list
        List<String> keywords = Arrays.asList("cà phê", "trà sữa", "nước ngọt");
        
        // When: Find best match for exact keyword
        FuzzyMatchingService.MatchResult result = fuzzyMatchingService.findBestMatch("cà phê", keywords, 0.7);
        
        // Then: Returns exact match
        assertNotNull(result);
        assertEquals("cà phê", result.getKeyword());
        assertEquals(1.0, result.getSimilarity(), 0.001);
        assertEquals("exact", result.getMatchType());
    }

    @Test
    public void testFindBestMatch_FuzzyMatch() {
        // Given: Keywords list
        List<String> keywords = Arrays.asList("cà phê", "trà sữa", "nước ngọt");
        
        // When: Find best match for typo (missing accent)
        FuzzyMatchingService.MatchResult result = fuzzyMatchingService.findBestMatch("ca phe", keywords, 0.7);
        
        // Then: Returns fuzzy match
        assertNotNull(result);
        assertEquals("cà phê", result.getKeyword());
        assertTrue(result.getSimilarity() >= 0.7);
    }

    @Test
    public void testFindBestMatch_BelowThreshold() {
        // Given: Keywords list
        List<String> keywords = Arrays.asList("cà phê", "trà sữa", "nước ngọt");
        
        // When: Find best match with high threshold and different input
        FuzzyMatchingService.MatchResult result = fuzzyMatchingService.findBestMatch("pizza", keywords, 0.9);
        
        // Then: Returns null (below threshold)
        assertNull(result);
    }

    @Test
    public void testFindBestMatch_NullInput() {
        // Given: Keywords list
        List<String> keywords = Arrays.asList("cà phê", "trà sữa");
        
        // When: Find match with null input
        FuzzyMatchingService.MatchResult result = fuzzyMatchingService.findBestMatch(null, keywords, 0.7);
        
        // Then: Returns null
        assertNull(result);
    }

    @Test
    public void testFindBestMatch_EmptyKeywords() {
        // When: Find match with empty keywords
        FuzzyMatchingService.MatchResult result = fuzzyMatchingService.findBestMatch("test", new ArrayList<>(), 0.7);
        
        // Then: Returns null
        assertNull(result);
    }

    @Test
    public void testFindBestMatch_WithTeencode() {
        // Given: Keywords list
        List<String> keywords = Arrays.asList("không biết", "không có", "không được");
        
        // When: Find match with teencode input
        FuzzyMatchingService.MatchResult result = fuzzyMatchingService.findBestMatch("k bik", keywords, 0.6);
        
        // Then: Returns fuzzy match (teencode normalized to "khong biet")
        assertNotNull(result);
        assertEquals("không biết", result.getKeyword());
    }

    @Test
    public void testMatchResult_Getters() {
        // Given: Create match result
        FuzzyMatchingService.MatchResult result = new FuzzyMatchingService.MatchResult("test", 0.85, "fuzzy");
        
        // Then: Getters work correctly
        assertEquals("test", result.getKeyword());
        assertEquals(0.85, result.getSimilarity(), 0.001);
        assertEquals("fuzzy", result.getMatchType());
    }

    @Test
    public void testFinancialContextNormalization() {
        // When: Normalize financial terms with teencode
        String result1 = fuzzyMatchingService.fullNormalize("Mua com");
        String result2 = fuzzyMatchingService.fullNormalize("Mua cơm");
        String result3 = fuzzyMatchingService.fullNormalize("mua  com");
        
        // Then: All variations normalize to same result
        assertEquals("mua com", result1);
        assertEquals("mua com", result2);
        assertEquals("mua com", result3);
    }
}
