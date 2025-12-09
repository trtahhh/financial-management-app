package com.example.finance.service;

import com.example.finance.ml.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Standalone test for enhanced ML features (Layer 2.5)
 * Tests the new enhanced feature extraction and confidence calibration
 */
public class EnhancedMLIntegrationTest {

    public static void main(String[] args) {
        System.out.println("=== Enhanced ML Features Test ===\n");
        
        try {
            // Load models
            System.out.println("Loading ML models...");
            TFIDFVectorizer vectorizer = ModelSerializer.loadTFIDFVectorizerFromResources("/ml-models/tfidf_vectorizer.bin");
            LinearSVMClassifier svm = ModelSerializer.loadSVMClassifierFromResources("/ml-models/svm_model.bin");
            System.out.println("✓ Models loaded successfully!\n");
            
            // Create services (without Spring dependency injection)
            ConfidenceCalibrationService calibrator = new ConfidenceCalibrationService();
            
            // Test cases with amount and temporal context
            Object[][] testCases = {
                {"Phở bò Hà Nội", 50000.0, 5L, "Ăn uống"},
                {"Grab về nhà", 35000.0, 6L, "Giao thông"},
                {"Mua áo mới Zara", 500000.0, 10L, "Mua sắm"},
                {"Cafe sáng", 25000.0, 5L, "Ăn uống"},
                {"Tiền điện tháng 11", 450000.0, 11L, "Tiện ích"},
                {"Lương tháng 11", 15000000.0, 1L, "Lương"},
                {"Học phí khóa học", 2000000.0, 9L, "Giáo dục"}
            };
            
            int passed = 0;
            int failed = 0;
            
            System.out.println("Running categorization tests...\n");
            
            for (Object[] testCase : testCases) {
                String description = (String) testCase[0];
                double amount = (double) testCase[1];
                long expectedCategory = (long) testCase[2];
                String expectedName = (String) testCase[3];
                
                // Normalize
                String normalized = VietnameseTextNormalizer.normalize(description);
                
                // Extract TF-IDF features
                double[] tfidfVector = vectorizer.transform(normalized);
                
                // Predict with SVM
                LinearSVMClassifier.PredictionResult prediction = svm.predictWithConfidence(tfidfVector);
                int predictedClass = prediction.predictedClass;
                double rawConfidence = prediction.confidence;
                
                // Category name mapping
                Map<Long, String> categoryNames = new HashMap<>();
                categoryNames.put(1L, "Lương");
                categoryNames.put(2L, "Thu nhập khác");
                categoryNames.put(3L, "Đầu tư");
                categoryNames.put(4L, "Kinh doanh");
                categoryNames.put(5L, "Ăn uống");
                categoryNames.put(6L, "Giao thông");
                categoryNames.put(7L, "Giải trí");
                categoryNames.put(8L, "Sức khỏe");
                categoryNames.put(9L, "Giáo dục");
                categoryNames.put(10L, "Mua sắm");
                categoryNames.put(11L, "Tiện ích");
                categoryNames.put(12L, "Vay nợ");
                categoryNames.put(13L, "Quà tặng");
                categoryNames.put(14L, "Khác");
                
                // Calibrate confidence
                ConfidenceCalibrationService.CalibratedPrediction calibrated = 
                    calibrator.calibrate((long)predictedClass, prediction.scores, categoryNames);
                
                boolean isCorrect = (predictedClass == expectedCategory);
                String status = isCorrect ? "✓ PASS" : "✗ FAIL";
                
                if (isCorrect) passed++;
                else failed++;
                
                System.out.println(status + " | " + description);
                System.out.println("    Amount: " + String.format("%,.0f VND", amount));
                System.out.println("    Predicted: Cat " + predictedClass + 
                                 " (Raw: " + String.format("%.2f%%", rawConfidence * 100) +
                                 ", Calibrated: " + String.format("%.2f%%", calibrated.getConfidence() * 100) + ")");
                System.out.println("    Expected: Cat " + expectedCategory + " (" + expectedName + ")");
                
                if (calibrated.isRequiresHumanReview()) {
                    System.out.println("    ⚠ Human review needed: " + calibrated.getExplanation());
                }
                
                if (!calibrated.getAlternativeSuggestions().isEmpty()) {
                    System.out.print("    Alternatives: ");
                    for (int i = 0; i < Math.min(2, calibrated.getAlternativeSuggestions().size()); i++) {
                        ConfidenceCalibrationService.CategoryScore alt = calibrated.getAlternativeSuggestions().get(i);
                        System.out.print("Cat" + alt.categoryId + "(" +
                                       String.format("%.0f%%", alt.score * 100) + ")");
                        if (i < 1 && i < calibrated.getAlternativeSuggestions().size() - 1) {
                            System.out.print(", ");
                        }
                    }
                    System.out.println();
                }
                
                System.out.println();
            }
            
            // Summary
            int total = passed + failed;
            double accuracy = (double) passed / total * 100;
            
            System.out.println("=== Test Summary ===");
            System.out.println("Total: " + total);
            System.out.println("Passed: " + passed);
            System.out.println("Failed: " + failed);
            System.out.println("Accuracy: " + String.format("%.2f%%", accuracy));
            
            if (failed == 0) {
                System.out.println("\n✓ All tests passed!");
            } else {
                System.out.println("\n✗ Some tests failed. Review predictions above.");
            }
            
        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
