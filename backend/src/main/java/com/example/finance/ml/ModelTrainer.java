package com.example.finance.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;

public class ModelTrainer {
    
    private static final double TRAIN_TEST_SPLIT = 0.8;
    private static final Random random = new Random(42);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static class TrainingData {
        public String description;
        public String category;
        public int category_id;
        public String type;
    }
    
    /**
     * Load training dataset from JSON file (14 categories)
     */
    private static Map<Long, List<String>> loadDatasetFromJSON(String filePath) throws IOException {
        System.out.println("📂 Loading training dataset: " + filePath);
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Dataset file not found: " + filePath);
        }
        
        JsonNode rootNode = objectMapper.readTree(file);
        Map<Long, List<String>> dataset = new HashMap<>();
        
        for (JsonNode node : rootNode) {
            TrainingData data = objectMapper.treeToValue(node, TrainingData.class);
            Long categoryId = (long) data.category_id;
            
            dataset.putIfAbsent(categoryId, new ArrayList<>());
            dataset.get(categoryId).add(data.description);
        }
        
        System.out.println("   ✅ Loaded dataset with " + dataset.size() + " categories");
        return dataset;
    }
    
    public static void main(String[] args) {
        System.out.println("Starting Financial Transaction Categorization Model Training (14 Categories)...");
        System.out.println("=================================================================================");
        
        try {
            // Load 14-category dataset from JSON
            String projectRoot = System.getProperty("user.dir");
            String datasetPath = projectRoot + "/../ai-service/vietnamese_transactions_14categories.json";
            Map<Long, List<String>> dataset = loadDatasetFromJSON(datasetPath);
            
            System.out.println("\n1. Dataset Statistics:");
            int totalSamples = 0;
            for (Map.Entry<Long, List<String>> entry : dataset.entrySet()) {
                int count = entry.getValue().size();
                totalSamples += count;
                System.out.printf("   Category %d: %d samples\n", entry.getKey(), count);
            }
            System.out.println("   Total: " + totalSamples + " samples");
            
            System.out.println("\n2. Splitting train/test sets (80/20)...");
            TrainTestData split = splitTrainTest(dataset);
            System.out.printf("   Training: %d samples\n", split.trainTexts.size());
            System.out.printf("   Testing: %d samples\n", split.testTexts.size());
            
            System.out.println("\n3. Training TF-IDF Vectorizer...");
            VietnameseTextNormalizer normalizer = new VietnameseTextNormalizer();
            TFIDFVectorizer vectorizer = new TFIDFVectorizer(2000);
            
            List<String> normalizedTrainTexts = new ArrayList<>();
            for (String text : split.trainTexts) {
                normalizedTrainTexts.add(VietnameseTextNormalizer.normalize(text));
            }
            
            double[][] trainVectors = vectorizer.fitTransform(normalizedTrainTexts);
            System.out.println("   Vocabulary size: " + vectorizer.getVocabularySize());
            System.out.println("   Feature dimensions: " + trainVectors[0].length);
            
            System.out.println("\n3.5. Normalizing feature vectors (L2 norm)...");
            normalizeFeatures(trainVectors);
            System.out.println("   ✅ Features normalized");
            
            System.out.println("\n4. Training SVM Classifier...");
            LinearSVMClassifier svm = new LinearSVMClassifier(10.0, 10000);
            int[] trainLabelsArray = split.trainLabels.stream().mapToInt(Long::intValue).toArray();
            svm.train(trainVectors, trainLabelsArray);
            System.out.println("   Training completed!");
            
            System.out.println("\n5. Evaluating on test set...");
            List<String> normalizedTestTexts = new ArrayList<>();
            for (String text : split.testTexts) {
                normalizedTestTexts.add(VietnameseTextNormalizer.normalize(text));
            }
            double[][] testVectors = new double[normalizedTestTexts.size()][];
            for (int i = 0; i < normalizedTestTexts.size(); i++) {
                testVectors[i] = vectorizer.transform(normalizedTestTexts.get(i));
            }
            normalizeFeatures(testVectors);
            
            double accuracy = evaluateModel(svm, testVectors, split.testLabels);
            System.out.printf("   Test Accuracy: %.2f%%\n", accuracy * 100);
            
            if (accuracy < 0.95) {
                System.out.println("\n   WARNING: Accuracy below 95% threshold!");
                System.out.println("   Consider: increasing dataset size, tuning hyperparameters");
            }
            
            System.out.println("\n6. Detailed Performance Analysis:");
            analyzePerformance(svm, vectorizer, normalizer, dataset);
            
            System.out.println("\n7. Saving models to disk...");
            String modelPath = getResourcesPath() + "/ml-models/";
            
            ModelSerializer.saveTFIDFVectorizer(vectorizer, modelPath + "tfidf_vectorizer.bin");
            System.out.println("   Saved: tfidf_vectorizer.bin");
            
            ModelSerializer.saveSVMClassifier(svm, modelPath + "svm_model.bin");
            System.out.println("   Saved: svm_model.bin");
            
            saveMetadata(modelPath + "model_metadata.json", accuracy, totalSamples, 
                        vectorizer.getVocabularySize());
            System.out.println("   Saved: model_metadata.json");
            
            System.out.println("\n================================================================");
            System.out.println("Training Complete! Models ready for production.");
            System.out.println("================================================================");
            
        } catch (Exception e) {
            System.err.println("Error during training: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static TrainTestData splitTrainTest(Map<Long, List<String>> dataset) {
        List<String> trainTexts = new ArrayList<>();
        List<Long> trainLabels = new ArrayList<>();
        List<String> testTexts = new ArrayList<>();
        List<Long> testLabels = new ArrayList<>();
        
        for (Map.Entry<Long, List<String>> entry : dataset.entrySet()) {
            Long categoryId = entry.getKey();
            List<String> samples = new ArrayList<>(entry.getValue());
            Collections.shuffle(samples, random);
            
            int trainSize = (int) (samples.size() * TRAIN_TEST_SPLIT);
            
            for (int i = 0; i < samples.size(); i++) {
                if (i < trainSize) {
                    trainTexts.add(samples.get(i));
                    trainLabels.add(categoryId);
                } else {
                    testTexts.add(samples.get(i));
                    testLabels.add(categoryId);
                }
            }
        }
        
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < trainTexts.size(); i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, random);
        
        List<String> shuffledTrainTexts = new ArrayList<>();
        List<Long> shuffledTrainLabels = new ArrayList<>();
        for (int idx : indices) {
            shuffledTrainTexts.add(trainTexts.get(idx));
            shuffledTrainLabels.add(trainLabels.get(idx));
        }
        
        return new TrainTestData(shuffledTrainTexts, shuffledTrainLabels, testTexts, testLabels);
    }
    
    private static double evaluateModel(LinearSVMClassifier svm, double[][] testVectors, 
                                       List<Long> testLabels) {
        int correct = 0;
        for (int i = 0; i < testVectors.length; i++) {
            long predicted = svm.predict(testVectors[i]);
            if (predicted == testLabels.get(i)) {
                correct++;
            }
        }
        return (double) correct / testLabels.size();
    }
    
    private static void analyzePerformance(LinearSVMClassifier svm, TFIDFVectorizer vectorizer,
                                          VietnameseTextNormalizer normalizer,
                                          Map<Long, List<String>> dataset) {
        String[] testCases = {
            "mua quan ao",
            "kichi kichi",
            "hamburger",
            "pho ga",
            "grab bike",
            "hoc phi",
            "tien dien",
            "luong thang 11",
            "co phieu",
            "ban hang online"
        };
        
        System.out.println("   Sample Predictions:");
        for (String testCase : testCases) {
            String normalized = VietnameseTextNormalizer.normalize(testCase);
            double[] vector = vectorizer.transform(normalized);
            LinearSVMClassifier.PredictionResult result = svm.predictWithConfidence(vector);
            System.out.printf("   '%s' -> Category %d (%.1f%% confidence)\n", 
                            testCase, result.predictedClass, result.confidence * 100);
        }
    }
    
    private static String getResourcesPath() {
        String projectPath = System.getProperty("user.dir");
        return projectPath + "/src/main/resources";
    }
    
    /**
     * Normalize feature vectors using L2 normalization
     * This helps improve SVM performance and confidence scores
     */
    private static void normalizeFeatures(double[][] vectors) {
        for (double[] vector : vectors) {
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
    }
    
    private static void saveMetadata(String filepath, double accuracy, int totalSamples, 
                                    int vocabularySize) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filepath))) {
            writer.write("{\n");
            writer.write("  \"model_type\": \"Linear SVM\",\n");
            writer.write("  \"feature_extractor\": \"TF-IDF\",\n");
            writer.write("  \"accuracy\": " + String.format("%.4f", accuracy) + ",\n");
            writer.write("  \"training_samples\": " + totalSamples + ",\n");
            writer.write("  \"vocabulary_size\": " + vocabularySize + ",\n");
            writer.write("  \"categories\": 14,\n");
            writer.write("  \"trained_date\": \"" + new Date() + "\",\n");
            writer.write("  \"language\": \"Vietnamese\",\n");
            writer.write("  \"version\": \"2.0\"\n");
            writer.write("}\n");
        }
    }
    
    private static class TrainTestData {
        List<String> trainTexts;
        List<Long> trainLabels;
        List<String> testTexts;
        List<Long> testLabels;
        
        TrainTestData(List<String> trainTexts, List<Long> trainLabels,
                     List<String> testTexts, List<Long> testLabels) {
            this.trainTexts = trainTexts;
            this.trainLabels = trainLabels;
            this.testTexts = testTexts;
            this.testLabels = testLabels;
        }
    }
}
