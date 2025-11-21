package com.example.finance.ml;

/**
 * Test SVM model directly
 */
public class TestSVMDirect {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== Testing SVM Model Directly ===\n");
            
            // Load models
            System.out.println("1. Loading TF-IDF vectorizer...");
            TFIDFVectorizer vectorizer = ModelSerializer.loadTFIDFVectorizerFromResources("/ml-models/tfidf_vectorizer.bin");
            System.out.println("   ✓ Loaded TF-IDF vectorizer");
            
            System.out.println("\n2. Loading SVM model...");
            LinearSVMClassifier svm = ModelSerializer.loadSVMClassifierFromResources("/ml-models/svm_model.bin");
            System.out.println("   ✓ Loaded SVM classifier");
            
            // Test cases
            String[] tests = {
                "pho bo",
                "grab bike",
                "mua ao",
                "ve phim",
                "luong thang 11"
            };
            
            System.out.println("\n3. Running predictions:\n");
            for (String test : tests) {
                String normalized = VietnameseTextNormalizer.normalize(test);
                double[] features = vectorizer.transform(normalized);
                LinearSVMClassifier.PredictionResult result = svm.predictWithConfidence(features);
                
                System.out.println("Input: " + test);
                System.out.println("  Normalized: " + normalized);
                System.out.println("  Features: " + features.length + " dimensions");
                System.out.println("  Predicted Class: " + result.predictedClass);
                System.out.println("  Confidence: " + String.format("%.4f", result.confidence));
                System.out.println("  All scores length: " + result.scores.length);
                System.out.println();
            }
            
            System.out.println("=== Test Completed Successfully ===");
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
