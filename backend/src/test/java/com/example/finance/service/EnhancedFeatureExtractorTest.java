package com.example.finance.service;

import com.example.finance.entity.Transaction;
import com.example.finance.entity.Category;
import com.example.finance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Test for EnhancedFeatureExtractor - ML feature extraction service
 */
@ExtendWith(MockitoExtension.class)
public class EnhancedFeatureExtractorTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private EnhancedFeatureExtractor featureExtractor;

    @BeforeEach
    void setUp() {
        // Setup is handled by @Mock and @InjectMocks
    }

    @Test
    void testExtractEnhancedFeatures_AmountFeatures() {
        // Given: Small amount (cafe/street food range)
        BigDecimal amount = new BigDecimal("15000");
        LocalDateTime timestamp = LocalDateTime.now();
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            "Cafe Highlands", amount, null, timestamp
        );
        
        // Then: Should have amount-based features
        assertTrue(features.containsKey("log_amount"));
        assertTrue(features.containsKey("is_micro"));
        assertEquals(1.0, features.get("is_micro")); // < 20K
        assertEquals(0.0, features.get("is_small")); // Not in 20K-100K range
    }

    @Test
    void testExtractEnhancedFeatures_TemporalFeatures_Morning() {
        // Given: Morning time (breakfast)
        LocalDateTime morningTime = LocalDateTime.of(2024, 1, 15, 8, 30);
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            "Pho bo", new BigDecimal("50000"), null, morningTime
        );
        
        // Then: Should detect morning pattern
        assertEquals(1.0, features.get("is_morning"));
        assertEquals(0.0, features.get("is_evening"));
    }

    @Test
    void testExtractEnhancedFeatures_TemporalFeatures_Weekend() {
        // Given: Saturday (weekend)
        LocalDateTime saturday = LocalDateTime.of(2024, 1, 13, 14, 0); // Saturday
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            "Shopping", new BigDecimal("200000"), null, saturday
        );
        
        // Then: Should detect weekend
        assertEquals(1.0, features.get("is_weekend"));
    }

    @Test
    void testExtractEnhancedFeatures_MonthPatterns() {
        // Given: Start of month (salary day)
        LocalDateTime monthStart = LocalDateTime.of(2024, 1, 3, 10, 0);
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            "Salary", new BigDecimal("10000000"), null, monthStart
        );
        
        // Then: Should detect month start pattern
        assertEquals(1.0, features.get("is_month_start"));
        assertEquals(0.0, features.get("is_month_end"));
    }

    @Test
    void testExtractEnhancedFeatures_TextFeatures_WithBrand() {
        // Given: Description with brand name
        String description = "Grab bike to district 1";
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            description, new BigDecimal("25000"), null, LocalDateTime.now()
        );
        
        // Then: Should detect brand name
        assertEquals(1.0, features.get("has_brand_name"));
        assertTrue(features.get("desc_length") > 0);
        assertTrue(features.get("word_count") >= 5);
    }

    @Test
    void testExtractEnhancedFeatures_TextFeatures_WithDigits() {
        // Given: Description with digits
        String description = "Invoice #12345";
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            description, new BigDecimal("100000"), null, LocalDateTime.now()
        );
        
        // Then: Should detect digits
        assertEquals(1.0, features.get("has_digits"));
        assertTrue(features.get("digit_ratio") > 0);
    }

    @Test
    void testExtractEnhancedFeatures_UserContext_NoHistory() {
        // Given: User with no transaction history
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(anyLong(), any()))
            .thenReturn(Collections.emptyList());
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            "Coffee", new BigDecimal("30000"), 1L, LocalDateTime.now()
        );
        
        // Then: Should indicate no history
        assertEquals(0.0, features.get("user_has_history"));
    }

    @Test
    void testExtractEnhancedFeatures_UserContext_WithHistory() {
        // Given: User with transaction history
        Category foodCategory = new Category();
        foodCategory.setId(5L);
        foodCategory.setName("Ăn uống");
        
        Transaction tx1 = new Transaction();
        tx1.setCategory(foodCategory);
        tx1.setNote("Pho");
        
        Transaction tx2 = new Transaction();
        tx2.setCategory(foodCategory);
        tx2.setNote("Com tam");
        
        when(transactionRepository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(anyLong(), any()))
            .thenReturn(Arrays.asList(tx1, tx2));
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            "Banh mi", new BigDecimal("20000"), 1L, LocalDateTime.now()
        );
        
        // Then: Should have user history features
        assertEquals(1.0, features.get("user_has_history"));
        assertTrue(features.containsKey("user_top_category_1"));
    }

    @Test
    void testExtractEnhancedFeatures_RoundNumbers() {
        // Given: Round amount (10K)
        BigDecimal roundAmount = new BigDecimal("50000");
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            "Payment", roundAmount, null, LocalDateTime.now()
        );
        
        // Then: Should detect round number
        assertEquals(1.0, features.get("is_round_10k"));
    }

    @Test
    void testExtractEnhancedFeatures_LargeAmount() {
        // Given: Large amount (rent/tuition range)
        BigDecimal largeAmount = new BigDecimal("1000000");
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            "Rent payment", largeAmount, null, LocalDateTime.now()
        );
        
        // Then: Should categorize as large
        assertEquals(1.0, features.get("is_large"));
        assertEquals(0.0, features.get("is_very_large"));
    }

    @Test
    void testExtractEnhancedFeatures_WithLocation() {
        // Given: Description with location marker
        String description = "Cafe in quan 1";
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            description, new BigDecimal("35000"), null, LocalDateTime.now()
        );
        
        // Then: Should detect location
        assertEquals(1.0, features.get("has_location"));
    }

    @Test
    void testExtractEnhancedFeatures_NullInputs() {
        // When: All optional inputs are null
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            null, null, null, null
        );
        
        // Then: Should return empty or minimal features without errors
        assertNotNull(features);
    }

    @Test
    void testToFeatureArray_ConversionSuccess() {
        // Given: Feature map and feature names
        Map<String, Double> features = new HashMap<>();
        features.put("feature1", 1.0);
        features.put("feature2", 0.5);
        features.put("feature3", 0.0);
        
        List<String> featureNames = Arrays.asList("feature1", "feature2", "feature3", "feature4");
        
        // When
        double[] array = featureExtractor.toFeatureArray(features, featureNames);
        
        // Then
        assertEquals(4, array.length);
        assertEquals(1.0, array[0]);
        assertEquals(0.5, array[1]);
        assertEquals(0.0, array[2]);
        assertEquals(0.0, array[3]); // Missing feature defaults to 0
    }

    @Test
    void testExtractEnhancedFeatures_EveningTime() {
        // Given: Evening time (dinner/entertainment)
        LocalDateTime eveningTime = LocalDateTime.of(2024, 1, 15, 19, 30);
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            "Dinner", new BigDecimal("150000"), null, eveningTime
        );
        
        // Then: Should detect evening pattern
        assertEquals(1.0, features.get("is_evening"));
        assertEquals(0.0, features.get("is_morning"));
    }

    @Test
    void testExtractEnhancedFeatures_FridayPattern() {
        // Given: Friday (payday pattern)
        LocalDateTime friday = LocalDateTime.of(2024, 1, 12, 15, 0); // Friday
        
        // When
        Map<String, Double> features = featureExtractor.extractEnhancedFeatures(
            "Shopping", new BigDecimal("300000"), null, friday
        );
        
        // Then: Should detect Friday
        assertEquals(1.0, features.get("is_friday"));
    }
}
