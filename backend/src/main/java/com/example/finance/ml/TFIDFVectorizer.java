package com.example.finance.ml;

import java.io.Serializable;
import java.util.*;

public class TFIDFVectorizer implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Map<String, Integer> vocabulary;
    private Map<String, Double> idfScores;
    private int maxFeatures = 1000;
    
    public TFIDFVectorizer() {
        this.vocabulary = new HashMap<>();
        this.idfScores = new HashMap<>();
    }
    
    public TFIDFVectorizer(int maxFeatures) {
        this();
        this.maxFeatures = maxFeatures;
    }
    
    public void fit(List<String> documents) {
        Map<String, Integer> documentFrequency = new HashMap<>();
        Map<String, Integer> termFrequency = new HashMap<>();
        
        for (String doc : documents) {
            List<String> tokens = VietnameseTextNormalizer.tokenize(doc);
            Set<String> uniqueTokens = new HashSet<>(tokens);
            
            for (String token : tokens) {
                termFrequency.put(token, termFrequency.getOrDefault(token, 0) + 1);
            }
            
            for (String token : uniqueTokens) {
                documentFrequency.put(token, documentFrequency.getOrDefault(token, 0) + 1);
            }
        }
        
        List<Map.Entry<String, Integer>> sortedTerms = new ArrayList<>(termFrequency.entrySet());
        sortedTerms.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        int vocabSize = Math.min(maxFeatures, sortedTerms.size());
        for (int i = 0; i < vocabSize; i++) {
            String term = sortedTerms.get(i).getKey();
            vocabulary.put(term, i);
        }
        
        int numDocuments = documents.size();
        for (String term : vocabulary.keySet()) {
            int df = documentFrequency.getOrDefault(term, 0);
            double idf = Math.log((double) numDocuments / (df + 1));
            idfScores.put(term, idf);
        }
    }
    
    public double[] transform(String document) {
        double[] vector = new double[vocabulary.size()];
        Arrays.fill(vector, 0.0);
        
        List<String> tokens = VietnameseTextNormalizer.tokenize(document);
        Map<String, Integer> termCounts = new HashMap<>();
        
        for (String token : tokens) {
            termCounts.put(token, termCounts.getOrDefault(token, 0) + 1);
        }
        
        double norm = 0.0;
        for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
            String term = entry.getKey();
            if (vocabulary.containsKey(term)) {
                int index = vocabulary.get(term);
                double tf = entry.getValue() / (double) tokens.size();
                double idf = idfScores.getOrDefault(term, 0.0);
                double tfidf = tf * idf;
                vector[index] = tfidf;
                norm += tfidf * tfidf;
            }
        }
        
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
        
        return vector;
    }
    
    public double[][] fitTransform(List<String> documents) {
        fit(documents);
        double[][] matrix = new double[documents.size()][];
        for (int i = 0; i < documents.size(); i++) {
            matrix[i] = transform(documents.get(i));
        }
        return matrix;
    }
    
    public int getVocabularySize() {
        return vocabulary.size();
    }
    
    public Map<String, Integer> getVocabulary() {
        return new HashMap<>(vocabulary);
    }
}
