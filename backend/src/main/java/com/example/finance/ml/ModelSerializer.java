package com.example.finance.ml;

import java.io.*;

public class ModelSerializer {
    
    public static void saveTFIDFVectorizer(TFIDFVectorizer vectorizer, String filepath) 
            throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(filepath)))) {
            oos.writeObject(vectorizer);
            oos.flush();
        }
    }
    
    public static TFIDFVectorizer loadTFIDFVectorizer(String filepath) 
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(filepath)))) {
            return (TFIDFVectorizer) ois.readObject();
        }
    }
    
    public static TFIDFVectorizer loadTFIDFVectorizerFromResources(String resourcePath) 
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(
                    ModelSerializer.class.getResourceAsStream(resourcePath)))) {
            return (TFIDFVectorizer) ois.readObject();
        }
    }
    
    public static void saveSVMClassifier(LinearSVMClassifier svm, String filepath) 
            throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(filepath)))) {
            oos.writeObject(svm);
            oos.flush();
        }
    }
    
    public static LinearSVMClassifier loadSVMClassifier(String filepath) 
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(filepath)))) {
            return (LinearSVMClassifier) ois.readObject();
        }
    }
    
    public static LinearSVMClassifier loadSVMClassifierFromResources(String resourcePath) 
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(
                    ModelSerializer.class.getResourceAsStream(resourcePath)))) {
            return (LinearSVMClassifier) ois.readObject();
        }
    }
    
    public static long getFileSize(String filepath) {
        File file = new File(filepath);
        return file.exists() ? file.length() : 0;
    }
    
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
