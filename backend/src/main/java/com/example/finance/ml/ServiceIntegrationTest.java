package com.example.finance.ml;

public class ServiceIntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("=== SVM Model Integration Test (Standalone) ===\n");
        
        try {
            // Load models directly
            System.out.println("Loading models...");
            VietnameseTextNormalizer normalizer = new VietnameseTextNormalizer();
            TFIDFVectorizer vectorizer = ModelSerializer.loadTFIDFVectorizerFromResources("/ml-models/tfidf_vectorizer.bin");
            LinearSVMClassifier svm = ModelSerializer.loadSVMClassifierFromResources("/ml-models/svm_model.bin");
            System.out.println("✓ Models loaded successfully!\n");
            
            // Category mapping
            String[] categoryNames = {
                null, "Lương", "Thu nhập khác", "Đầu tư", "Kinh doanh",
                "Ăn uống", "Giao thông", "Giải trí", "Sức khỏe", "Giáo dục",
                "Mua sắm", "Tiện ích"
            };
            
            // Test cases
            String[][] testCases = {
                {"mua pho ga", "5", "Ăn uống"},
                {"grab bike ve nha", "6", "Giao thông"},
                {"hoc phi dai hoc", "9", "Giáo dục"},
                {"mua quan ao", "10", "Mua sắm"},
                {"tien dien thang 11", "11", "Tiện ích"},
                {"luong thang 11", "1", "Lương"},
                {"co phieu", "3", "Đầu tư"},
                {"ban hang online", "4", "Kinh doanh"},
                {"kichi kichi", "5", "Ăn uống"},
                {"xem phim cgv", "7", "Giải trí"}
            };
            
            int passed = 0;
            int failed = 0;
            
            for (String[] testCase : testCases) {
                String description = testCase[0];
                int expectedCategory = Integer.parseInt(testCase[1]);
                String expectedName = testCase[2];
                
                // Predict
                String normalized = normalizer.normalize(description);
                double[] features = vectorizer.transform(normalized);
                LinearSVMClassifier.PredictionResult result = svm.predictWithConfidence(features);
                
                boolean isCorrect = (result.predictedClass == expectedCategory);
                
                if (isCorrect) {
                    passed++;
                    System.out.println("✓ PASS: \"" + description + "\"");
                    System.out.println("  → Category: " + categoryNames[result.predictedClass] + 
                                     " (ID: " + result.predictedClass + ")");
                    System.out.println("  → Confidence: " + 
                                     String.format("%.2f%%", result.confidence * 100));
                } else {
                    failed++;
                    System.out.println("✗ FAIL: \"" + description + "\"");
                    System.out.println("  Expected: " + expectedName + " (ID: " + expectedCategory + ")");
                    System.out.println("  Got: " + categoryNames[result.predictedClass] + 
                                     " (ID: " + result.predictedClass + ")");
                    System.out.println("  Confidence: " + 
                                     String.format("%.2f%%", result.confidence * 100));
                }
                System.out.println();
            }
            
            System.out.println("=== Test Summary ===");
            System.out.println("Total: " + testCases.length + " tests");
            System.out.println("Passed: " + passed + " ✓");
            System.out.println("Failed: " + failed + " ✗");
            System.out.println("Accuracy: " + String.format("%.2f%%", 
                (passed * 100.0 / testCases.length)));
            
            if (failed == 0) {
                System.out.println("\n🎉 All tests passed! SVM integration successful!");
            } else {
                System.out.println("\n⚠ Some tests failed. Please review.");
            }
            
        } catch (Exception e) {
            System.err.println("✗ Test failed with exception:");
            e.printStackTrace();
        }
    }
}
