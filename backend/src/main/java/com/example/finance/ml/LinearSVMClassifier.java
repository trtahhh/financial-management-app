package com.example.finance.ml;

import java.io.Serializable;
import java.util.*;

public class LinearSVMClassifier implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private double[][] weights;
    private double[] bias;
    private int[] classes;
    private double C = 1.0;
    private int maxIterations = 1000;
    
    public LinearSVMClassifier() {
    }
    
    public LinearSVMClassifier(double C, int maxIterations) {
        this.C = C;
        this.maxIterations = maxIterations;
    }
    
    public void train(double[][] X, int[] y) {
        Set<Integer> uniqueClasses = new HashSet<>();
        for (int label : y) {
            uniqueClasses.add(label);
        }
        classes = uniqueClasses.stream().mapToInt(Integer::intValue).sorted().toArray();
        
        int numClasses = classes.length;
        int numFeatures = X[0].length;
        
        weights = new double[numClasses][numFeatures];
        bias = new double[numClasses];
        
        for (int c = 0; c < numClasses; c++) {
            int[] binaryLabels = new int[y.length];
            for (int i = 0; i < y.length; i++) {
                binaryLabels[i] = (y[i] == classes[c]) ? 1 : -1;
            }
            
            BinarySVMResult result = trainBinarySVM(X, binaryLabels, numFeatures);
            weights[c] = result.weights;
            bias[c] = result.bias;
        }
    }
    
    private BinarySVMResult trainBinarySVM(double[][] X, int[] y, int numFeatures) {
        int n = X.length;
        
        double[] w = new double[numFeatures];
        double b = 0.0;
        
        Arrays.fill(w, 0.0);
        
        double initialLearningRate = 0.001;
        double lambda = 1.0 / C;
        
        int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }
        
        Random random = new Random(42);
        
        for (int epoch = 0; epoch < maxIterations; epoch++) {
            shuffleArray(indices, random);
            
            double learningRate = initialLearningRate / (1.0 + epoch * 0.001);
            
            for (int idx : indices) {
                double prediction = dotProduct(w, X[idx]) + b;
                double margin = y[idx] * prediction;
                
                if (margin < 1) {
                    for (int j = 0; j < numFeatures; j++) {
                        w[j] = w[j] * (1 - learningRate * lambda) + learningRate * y[idx] * X[idx][j];
                    }
                    b = b + learningRate * y[idx];
                } else {
                    for (int j = 0; j < numFeatures; j++) {
                        w[j] = w[j] * (1 - learningRate * lambda);
                    }
                }
            }
        }
        
        return new BinarySVMResult(w, b);
    }
    
    private void shuffleArray(int[] array, Random random) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
    
    private static class BinarySVMResult {
        double[] weights;
        double bias;
        
        BinarySVMResult(double[] weights, double bias) {
            this.weights = weights;
            this.bias = bias;
        }
    }
    
    public int predict(double[] x) {
        double[] scores = new double[classes.length];
        
        for (int c = 0; c < classes.length; c++) {
            scores[c] = dotProduct(weights[c], x) + bias[c];
        }
        
        int maxIndex = 0;
        double maxScore = scores[0];
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxIndex = i;
            }
        }
        
        return classes[maxIndex];
    }
    
    public PredictionResult predictWithConfidence(double[] x) {
        double[] scores = new double[classes.length];
        
        for (int c = 0; c < classes.length; c++) {
            scores[c] = dotProduct(weights[c], x) + bias[c];
        }
        
        int maxIndex = 0;
        double maxScore = scores[0];
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > maxScore) {
                maxScore = scores[i];
                maxIndex = i;
            }
        }
        
        double expSum = 0.0;
        for (double score : scores) {
            expSum += Math.exp(score);
        }
        double confidence = Math.exp(maxScore) / expSum;
        
        return new PredictionResult(classes[maxIndex], confidence, scores);
    }
    
    public double evaluateAccuracy(double[][] X, int[] y) {
        int correct = 0;
        for (int i = 0; i < X.length; i++) {
            if (predict(X[i]) == y[i]) {
                correct++;
            }
        }
        return (double) correct / X.length;
    }
    
    private double dotProduct(double[] a, double[] b) {
        double result = 0.0;
        for (int i = 0; i < a.length; i++) {
            result += a[i] * b[i];
        }
        return result;
    }
    
    public static class PredictionResult implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public final int predictedClass;
        public final double confidence;
        public final double[] scores;
        
        public PredictionResult(int predictedClass, double confidence, double[] scores) {
            this.predictedClass = predictedClass;
            this.confidence = confidence;
            this.scores = scores;
        }
    }
}
