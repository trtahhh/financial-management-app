package com.example.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfidenceCalibrationService - Group 6
 * Tests ML confidence calibration and uncertainty estimation
 */
@ExtendWith(MockitoExtension.class)
public class ConfidenceCalibrationServiceTest {

    @InjectMocks
    private ConfidenceCalibrationService calibrationService;

    private Map<Long, String> categoryNames;

    @BeforeEach
    void setUp() {
        categoryNames = new HashMap<>();
        categoryNames.put(1L, "Lương");
        categoryNames.put(2L, "Đầu tư");
        categoryNames.put(3L, "Tiền khác");
        categoryNames.put(4L, "Bán đồ");
        categoryNames.put(5L, "Ăn uống");
        categoryNames.put(6L, "Di chuyển");
        categoryNames.put(7L, "Giải trí");
        categoryNames.put(8L, "Y tế");
        categoryNames.put(9L, "Giáo dục");
        categoryNames.put(10L, "Mua sắm");
        categoryNames.put(11L, "Hóa đơn");
        categoryNames.put(12L, "Vay nợ");
        categoryNames.put(13L, "Quà tặng");
        categoryNames.put(14L, "Khác");
    }

    /**
     * Test calibrate - High Confidence Prediction
     */
    @Test
    void testCalibrate_HighConfidence() {
        // Arrange
        Long predictedCategory = 5L;
        double[] rawScores = new double[14];
        rawScores[4] = 5.0; // Category 5 (index 4) has very high score
        rawScores[6] = 1.0; // Other categories have low scores
        rawScores[9] = 0.5;

        // Act
        ConfidenceCalibrationService.CalibratedPrediction result = 
            calibrationService.calibrate(predictedCategory, rawScores, categoryNames);

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.getPredictedCategory());
        assertTrue(result.getConfidence() > 0.6); // High confidence after calibration
        assertFalse(result.isRequiresHumanReview()); // No review needed for high confidence
        assertNotNull(result.getExplanation());
        assertTrue(result.getAlternativeSuggestions().size() <= 2);
    }

    /**
     * Test calibrate - Low Confidence Prediction
     */
    @Test
    void testCalibrate_LowConfidence() {
        // Arrange
        Long predictedCategory = 5L;
        double[] rawScores = new double[14];
        rawScores[4] = 1.5; // Category 5 (index 4) has moderate score
        rawScores[6] = 1.3; // Close competitor
        rawScores[9] = 1.0; // Another close competitor

        // Act
        ConfidenceCalibrationService.CalibratedPrediction result = 
            calibrationService.calibrate(predictedCategory, rawScores, categoryNames);

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.getPredictedCategory());
        assertTrue(result.getConfidence() < 0.60); // Low confidence after calibration
        assertTrue(result.isRequiresHumanReview()); // Should require human review
        assertTrue(result.getExplanation().contains("không chắc") || 
                   result.getExplanation().contains("kiểm tra lại"));
    }

    /**
     * Test calibrate - Close Competition
     */
    @Test
    void testCalibrate_CloseCompetition() {
        // Arrange
        Long predictedCategory = 5L;
        double[] rawScores = new double[14];
        rawScores[4] = 2.0; // Category 5
        rawScores[6] = 1.9; // Category 7 very close

        // Act
        ConfidenceCalibrationService.CalibratedPrediction result = 
            calibrationService.calibrate(predictedCategory, rawScores, categoryNames);

        // Assert
        assertNotNull(result);
        assertTrue(result.isRequiresHumanReview()); // Close competition requires review
        assertTrue(result.getAlternativeSuggestions().size() >= 1);
        assertTrue(result.getExplanation().contains("Có thể là") || 
                   result.getExplanation().contains("hoặc"));
    }

    /**
     * Test calibrate - Uniform Distribution (High Entropy)
     */
    @Test
    void testCalibrate_HighEntropy() {
        // Arrange
        Long predictedCategory = 5L;
        double[] rawScores = new double[14];
        for (int i = 0; i < 14; i++) {
            rawScores[i] = 1.0; // All categories equally likely (uniform distribution)
        }

        // Act
        ConfidenceCalibrationService.CalibratedPrediction result = 
            calibrationService.calibrate(predictedCategory, rawScores, categoryNames);

        // Assert
        assertNotNull(result);
        assertTrue(result.isRequiresHumanReview()); // High entropy requires review
        assertTrue(result.getUncertaintyScore() > 0.8); // High uncertainty
    }

    /**
     * Test calibrate - Alternative Suggestions
     */
    @Test
    void testCalibrate_AlternativeSuggestions() {
        // Arrange
        Long predictedCategory = 5L;
        double[] rawScores = new double[14];
        rawScores[4] = 3.0; // Category 5
        rawScores[6] = 2.0; // Category 7 second
        rawScores[9] = 1.5; // Category 10 third
        rawScores[13] = 0.5; // Category 14 fourth

        // Act
        ConfidenceCalibrationService.CalibratedPrediction result = 
            calibrationService.calibrate(predictedCategory, rawScores, categoryNames);

        // Assert
        assertNotNull(result);
        List<ConfidenceCalibrationService.CategoryScore> alternatives = 
            result.getAlternativeSuggestions();
        assertTrue(alternatives.size() >= 1);
        assertTrue(alternatives.size() <= 2); // Max 2 alternatives

        // Verify alternatives are sorted by confidence
        if (alternatives.size() >= 2) {
            assertTrue(alternatives.get(0).score >= alternatives.get(1).score);
        }
    }

    /**
     * Test calibrate - Zero Scores
     */
    @Test
    void testCalibrate_ZeroScores() {
        // Arrange
        Long predictedCategory = 5L;
        double[] rawScores = new double[14]; // All zeros

        // Act
        ConfidenceCalibrationService.CalibratedPrediction result = 
            calibrationService.calibrate(predictedCategory, rawScores, categoryNames);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getPredictedCategory());
        assertTrue(result.getConfidence() >= 0.0 && result.getConfidence() <= 1.0, 
            "Confidence should be between 0 and 1, got: " + result.getConfidence());
        // For zero scores, uncertainty is undefined (NaN) which is >= 0.0 returns false
        // So we just check it's a valid number
        assertFalse(Double.isNaN(result.getUncertaintyScore()) || 
                   Double.isInfinite(result.getUncertaintyScore()),
                   "Uncertainty score should not be NaN or Infinite");
    }

    /**
     * Test calibrate - Single High Score
     */
    @Test
    void testCalibrate_SingleHighScore() {
        // Arrange
        Long predictedCategory = 5L;
        double[] rawScores = new double[14];
        rawScores[4] = 10.0; // Very high score for Category 5
        // All others are 0

        // Act
        ConfidenceCalibrationService.CalibratedPrediction result = 
            calibrationService.calibrate(predictedCategory, rawScores, categoryNames);

        // Assert
        assertNotNull(result);
        assertTrue(result.getConfidence() > 0.8); // Very high confidence
        assertFalse(result.isRequiresHumanReview());
        assertTrue(result.getUncertaintyScore() < 0.3); // Low uncertainty
    }

    /**
     * Test calibrate - Negative Scores
     */
    @Test
    void testCalibrate_NegativeScores() {
        // Arrange
        Long predictedCategory = 5L;
        double[] rawScores = new double[14];
        rawScores[4] = 2.0; // Category 5
        rawScores[6] = -1.0; // Negative score (SVM can produce negative values)
        rawScores[9] = -2.0;

        // Act
        ConfidenceCalibrationService.CalibratedPrediction result = 
            calibrationService.calibrate(predictedCategory, rawScores, categoryNames);

        // Assert
        assertNotNull(result);
        assertTrue(result.getConfidence() >= 0.0 && result.getConfidence() <= 1.0);
        // Softmax should handle negative values correctly
    }

    /**
     * Test calibrate - Explanation Format
     */
    @Test
    void testCalibrate_ExplanationFormat() {
        // Arrange
        Long predictedCategory = 5L;
        double[] rawScores = new double[14];
        rawScores[4] = 5.0;

        // Act
        ConfidenceCalibrationService.CalibratedPrediction result = 
            calibrationService.calibrate(predictedCategory, rawScores, categoryNames);

        // Assert
        assertNotNull(result.getExplanation());
        assertFalse(result.getExplanation().isEmpty());
        // Should contain category name
        assertTrue(result.getExplanation().contains("Ăn uống") || 
                   result.getExplanation().contains("**Ăn uống**"));
    }

    /**
     * Test updateTemperature - With Calibration Examples
     */
    @Test
    void testUpdateTemperature_WithExamples() {
        // Arrange
        List<ConfidenceCalibrationService.CalibrationExample> examples = new ArrayList<>();
        
        double[] logits1 = {2.0, 1.0, 0.5, 0.3, 0.2, 0.1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        examples.add(new ConfidenceCalibrationService.CalibrationExample(logits1, 1L));
        
        double[] logits2 = {0.5, 3.0, 1.0, 0.5, 0.3, 0.2, 0.1, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        examples.add(new ConfidenceCalibrationService.CalibrationExample(logits2, 2L));

        // Act (should not throw exception)
        assertDoesNotThrow(() -> {
            calibrationService.updateTemperature(examples);
        });
    }

    /**
     * Test calibrate - CategoryScore Properties
     */
    @Test
    void testCalibrate_CategoryScoreProperties() {
        // Arrange
        Long predictedCategory = 5L;
        double[] rawScores = new double[14];
        rawScores[4] = 3.0;
        rawScores[6] = 2.0;

        // Act
        ConfidenceCalibrationService.CalibratedPrediction result = 
            calibrationService.calibrate(predictedCategory, rawScores, categoryNames);

        // Assert
        List<ConfidenceCalibrationService.CategoryScore> alternatives = 
            result.getAlternativeSuggestions();
        
        if (!alternatives.isEmpty()) {
            ConfidenceCalibrationService.CategoryScore first = alternatives.get(0);
            assertNotNull(first.categoryId);
            assertNotNull(first.categoryName);
            assertTrue(first.score >= 0.0 && first.score <= 1.0);
        }
    }
}
