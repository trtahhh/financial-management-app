#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Retrain SVM model with complete 14-category dataset
Converts to Java-compatible format
"""

import json
import pickle
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.svm import LinearSVC
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, accuracy_score
import re
from datetime import datetime

class VietnameseTextNormalizer:
    """Vietnamese text normalization (matches Java version)"""
    
    @staticmethod
    def normalize(text):
        """Normalize Vietnamese text"""
        text = str(text).lower().strip()
        
        # Remove extra whitespace
        text = re.sub(r'\s+', ' ', text)
        
        # Remove special characters but keep Vietnamese
        text = re.sub(r'[^\w\s]', ' ', text)
        text = re.sub(r'\s+', ' ', text)
        
        return text.strip()

class JavaCompatibleTFIDFVectorizer:
    """TF-IDF Vectorizer compatible with Java implementation"""
    
    def __init__(self, max_features=500):
        self.vectorizer = TfidfVectorizer(
            max_features=max_features,
            ngram_range=(1, 2),  # Unigrams and bigrams
            min_df=2,  # Minimum document frequency
            sublinear_tf=True  # Use log(tf) + 1
        )
        self.vocabulary = None
        self.idf_values = None
    
    def fit(self, texts):
        """Fit vectorizer on texts"""
        self.vectorizer.fit(texts)
        self.vocabulary = self.vectorizer.vocabulary_
        self.idf_values = self.vectorizer.idf_
        return self
    
    def transform(self, texts):
        """Transform texts to TF-IDF vectors"""
        return self.vectorizer.transform(texts).toarray()
    
    def fit_transform(self, texts):
        """Fit and transform"""
        self.fit(texts)
        return self.transform(texts)
    
    def to_java_format(self):
        """Export to Java-compatible format"""
        return {
            'vocabulary': self.vocabulary,
            'idf_values': self.idf_values.tolist(),
            'num_features': len(self.vocabulary)
        }

class JavaCompatibleSVM:
    """Linear SVM compatible with Java implementation"""
    
    def __init__(self, C=1.0, max_iter=1000):
        self.model = LinearSVC(
            C=C,
            max_iter=max_iter,
            random_state=42,
            dual=False  # Use primal optimization for better performance
        )
        self.classes = None
        self.weights = None
        self.bias = None
    
    def fit(self, X, y):
        """Train SVM model"""
        self.model.fit(X, y)
        self.classes = self.model.classes_
        self.weights = self.model.coef_
        self.bias = self.model.intercept_
        return self
    
    def predict(self, X):
        """Predict classes"""
        return self.model.predict(X)
    
    def decision_function(self, X):
        """Get decision function scores"""
        return self.model.decision_function(X)
    
    def to_java_format(self):
        """Export to Java-compatible format"""
        return {
            'classes': self.classes.tolist(),
            'weights': self.weights.tolist(),
            'bias': self.bias.tolist(),
            'num_classes': len(self.classes),
            'num_features': self.weights.shape[1]
        }

def load_dataset(file_path):
    """Load training dataset"""
    print(f"\n📂 Loading dataset: {file_path}")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    print(f"   ✅ Loaded {len(data):,} samples")
    
    # Extract texts and labels
    texts = [item['description'] for item in data]
    labels = [item['category_id'] for item in data]
    
    return texts, labels

def train_model(texts, labels):
    """Train TF-IDF + SVM model"""
    
    print("\n🔄 Preprocessing texts...")
    normalized_texts = [VietnameseTextNormalizer.normalize(text) for text in texts]
    
    print("\n🎯 Training TF-IDF vectorizer...")
    vectorizer = JavaCompatibleTFIDFVectorizer(max_features=500)
    X = vectorizer.fit_transform(normalized_texts)
    
    print(f"   ✅ Vocabulary size: {len(vectorizer.vocabulary)}")
    print(f"   ✅ Feature dimensions: {X.shape[1]}")
    
    print("\n🤖 Training SVM classifier...")
    svm = JavaCompatibleSVM(C=1.0, max_iter=2000)
    svm.fit(X, labels)
    
    print(f"   ✅ Classes: {len(svm.classes)}")
    print(f"   ✅ Model shape: {svm.weights.shape}")
    
    return vectorizer, svm, X, labels

def evaluate_model(vectorizer, svm, X, labels):
    """Evaluate model performance"""
    
    print("\n📊 Evaluating model...")
    
    # Split data for evaluation
    X_train, X_test, y_train, y_test = train_test_split(
        X, labels, test_size=0.2, random_state=42, stratify=labels
    )
    
    # Retrain on train split
    svm_eval = JavaCompatibleSVM(C=1.0, max_iter=2000)
    svm_eval.fit(X_train, y_train)
    
    # Predict on test set
    y_pred = svm_eval.predict(X_test)
    
    # Calculate accuracy
    accuracy = accuracy_score(y_test, y_pred)
    
    print(f"\n✅ Model Accuracy: {accuracy:.4f} ({accuracy*100:.2f}%)")
    
    # Detailed report
    category_names = {
        1: "Lương", 2: "Thu nhập khác", 3: "Đầu tư", 4: "Kinh doanh",
        5: "Ăn uống", 6: "Giao thông", 7: "Giải trí", 8: "Sức khỏe",
        9: "Giáo dục", 10: "Mua sắm", 11: "Tiện ích", 12: "Vay nợ",
        13: "Quà tặng", 14: "Khác"
    }
    
    target_names = [category_names[i] for i in sorted(set(y_test))]
    
    print("\n📈 Classification Report:")
    print(classification_report(y_test, y_pred, target_names=target_names))
    
    return accuracy

def save_java_models(vectorizer, svm, output_dir="../backend/src/main/resources/ml-models"):
    """Save models in Java-compatible format"""
    
    import os
    os.makedirs(output_dir, exist_ok=True)
    
    print(f"\n💾 Saving models to: {output_dir}")
    
    # Save TF-IDF vectorizer (Python pickle for conversion)
    tfidf_path = os.path.join(output_dir, "tfidf_vectorizer.bin")
    with open(tfidf_path, 'wb') as f:
        pickle.dump(vectorizer, f)
    print(f"   ✅ TF-IDF vectorizer: {tfidf_path}")
    
    # Save SVM model (Python pickle for conversion)
    svm_path = os.path.join(output_dir, "svm_model.bin")
    with open(svm_path, 'wb') as f:
        pickle.dump(svm, f)
    print(f"   ✅ SVM model: {svm_path}")
    
    # Save metadata
    metadata = {
        "model_type": "Linear SVM",
        "feature_extractor": "TF-IDF",
        "accuracy": 0.0,  # Will be updated
        "training_samples": 0,  # Will be updated
        "vocabulary_size": len(vectorizer.vocabulary),
        "categories": len(svm.classes),
        "trained_date": datetime.now().strftime("%c"),
        "language": "Vietnamese",
        "version": "2.0"
    }
    
    metadata_path = os.path.join(output_dir, "model_metadata.json")
    with open(metadata_path, 'w', encoding='utf-8') as f:
        json.dump(metadata, f, ensure_ascii=False, indent=2)
    print(f"   ✅ Metadata: {metadata_path}")

def test_predictions(vectorizer, svm):
    """Test model with sample inputs"""
    
    print("\n🧪 Testing predictions...")
    
    test_cases = [
        "pho bo",
        "grab bike",
        "ve phim",
        "gym",
        "hoc phi",
        "tien dien",
        "luong thang 11",
        "mua co phieu",
        "tra no the tin dung",
        "qua tang sinh nhat"
    ]
    
    category_names = {
        1: "Lương", 2: "Thu nhập khác", 3: "Đầu tư", 4: "Kinh doanh",
        5: "Ăn uống", 6: "Giao thông", 7: "Giải trí", 8: "Sức khỏe",
        9: "Giáo dục", 10: "Mua sắm", 11: "Tiện ích", 12: "Vay nợ",
        13: "Quà tặng", 14: "Khác"
    }
    
    for test_text in test_cases:
        normalized = VietnameseTextNormalizer.normalize(test_text)
        features = vectorizer.transform([normalized])
        prediction = svm.predict(features)[0]
        scores = svm.decision_function(features)[0]
        
        # Softmax to get probabilities
        exp_scores = np.exp(scores - np.max(scores))
        probabilities = exp_scores / exp_scores.sum()
        confidence = probabilities.max()
        
        category_name = category_names.get(prediction, "Unknown")
        
        print(f"   '{test_text}' -> {category_name} (ID: {prediction}, conf: {confidence:.2%})")

def main():
    print("=" * 70)
    print("RETRAIN SVM MODEL WITH 14 CATEGORIES")
    print("=" * 70)
    
    # Load dataset
    texts, labels = load_dataset("vietnamese_transactions_14categories.json")
    
    # Train model
    vectorizer, svm, X, labels = train_model(texts, labels)
    
    # Evaluate model
    accuracy = evaluate_model(vectorizer, svm, X, labels)
    
    # Save models
    save_java_models(vectorizer, svm)
    
    # Update metadata with actual values
    import os
    metadata_path = "../backend/src/main/resources/ml-models/model_metadata.json"
    with open(metadata_path, 'r', encoding='utf-8') as f:
        metadata = json.load(f)
    
    metadata['accuracy'] = round(accuracy, 4)
    metadata['training_samples'] = len(texts)
    
    with open(metadata_path, 'w', encoding='utf-8') as f:
        json.dump(metadata, f, ensure_ascii=False, indent=2)
    
    # Test predictions
    test_predictions(vectorizer, svm)
    
    print("\n" + "=" * 70)
    print("✨ Model retraining complete!")
    print(f"   Accuracy: {accuracy:.2%}")
    print(f"   Samples: {len(texts):,}")
    print(f"   Categories: 14")
    print("=" * 70)

if __name__ == "__main__":
    main()
