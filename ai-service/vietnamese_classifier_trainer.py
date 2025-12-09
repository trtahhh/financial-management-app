#!/usr/bin/env python3
"""
Vietnamese Transaction Classifier Trainer
Trains ML model for Vietnamese financial transaction categorization
"""

import json
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
from sklearn.preprocessing import LabelEncoder
import pickle
import logging
from pathlib import Path
import re
from typing import Dict, List, Tuple, Any
from underthesea import word_tokenize
import matplotlib.pyplot as plt
import seaborn as sns

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class VietnameseTransactionClassifier:
 def __init__(self):
 self.vectorizer = None
 self.classifier = None
 self.label_encoder = None
 self.model_info = {}
 
 def preprocess_vietnamese_text(self, text: str) -> str:
 """Preprocess Vietnamese transaction description"""
 
 # Convert to lowercase
 text = text.lower().strip()
 
 # Remove extra whitespace
 text = re.sub(r'\s+', ' ', text)
 
 # Remove special characters but keep Vietnamese diacritics
 text = re.sub(r'[^\w\sàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ-]', ' ', text)
 
 # Tokenize Vietnamese text
 try:
 tokens = word_tokenize(text)
 text = ' '.join(tokens)
 except Exception as e:
 logger.warning(f"Tokenization failed for text: {text[:50]}... Error: {e}")
 # Fallback to simple processing
 pass
 
 return text
 
 def load_training_data(self, sample_file: str = "vietnamese_financial_quality_sample.json") -> Tuple[List[str], List[str]]:
 """Load and prepare training data"""
 
 logger.info(f" Loading training data from {sample_file}...")
 
 with open(sample_file, 'r', encoding='utf-8') as f:
 transactions = json.load(f)
 
 logger.info(f" Loaded {len(transactions):,} transactions")
 
 # Extract features and labels
 descriptions = []
 categories = []
 
 for transaction in transactions:
 description = transaction.get('description', '')
 category = transaction.get('category', 'unknown')
 
 # Skip invalid entries
 if not description or not category or category == 'unknown':
 continue
 
 # Preprocess description
 processed_desc = self.preprocess_vietnamese_text(description)
 
 if processed_desc.strip(): # Only add non-empty descriptions
 descriptions.append(processed_desc)
 categories.append(category)
 
 logger.info(f" Prepared {len(descriptions):,} valid training samples")
 
 # Category distribution
 category_counts = pd.Series(categories).value_counts()
 logger.info(" Category distribution:")
 for cat, count in category_counts.items():
 logger.info(f" {cat}: {count:,} ({count/len(categories)*100:.1f}%)")
 
 return descriptions, categories
 
 def train_model(self, descriptions: List[str], categories: List[str], test_size: float = 0.2) -> Dict[str, Any]:
 """Train the Vietnamese transaction classifier"""
 
 logger.info(" Starting model training...")
 
 # Split data
 X_train, X_test, y_train, y_test = train_test_split(
 descriptions, categories, test_size=test_size, random_state=42, stratify=categories
 )
 
 logger.info(f" Training set: {len(X_train):,} samples")
 logger.info(f" Test set: {len(X_test):,} samples")
 
 # Initialize label encoder
 self.label_encoder = LabelEncoder()
 y_train_encoded = self.label_encoder.fit_transform(y_train)
 y_test_encoded = self.label_encoder.transform(y_test)
 
 # Initialize and train TF-IDF vectorizer with Vietnamese-optimized parameters
 self.vectorizer = TfidfVectorizer(
 max_features=10000,
 ngram_range=(1, 3), # Unigrams, bigrams, and trigrams
 min_df=2, # Ignore terms that appear in fewer than 2 documents
 max_df=0.8, # Ignore terms that appear in more than 80% of documents
 lowercase=True,
 stop_words=None, # No built-in stop words for Vietnamese
 analyzer='word',
 token_pattern=r'\b\w+\b'
 )
 
 logger.info("🔤 Fitting TF-IDF vectorizer...")
 X_train_vectorized = self.vectorizer.fit_transform(X_train)
 X_test_vectorized = self.vectorizer.transform(X_test)
 
 logger.info(f" Feature matrix shape: {X_train_vectorized.shape}")
 
 # Initialize and train Random Forest classifier
 self.classifier = RandomForestClassifier(
 n_estimators=200, # More trees for better performance
 max_depth=20, # Prevent overfitting
 min_samples_split=5,
 min_samples_leaf=2,
 max_features='sqrt',
 random_state=42,
 n_jobs=-1 # Use all available cores
 )
 
 logger.info("🌳 Training Random Forest classifier...")
 self.classifier.fit(X_train_vectorized, y_train_encoded)
 
 # Make predictions
 logger.info(" Making predictions on test set...")
 y_pred = self.classifier.predict(X_test_vectorized)
 
 # Calculate metrics
 accuracy = accuracy_score(y_test_encoded, y_pred)
 
 # Decode predictions for detailed report
 y_test_decoded = self.label_encoder.inverse_transform(y_test_encoded)
 y_pred_decoded = self.label_encoder.inverse_transform(y_pred)
 
 # Classification report
 class_report = classification_report(y_test_decoded, y_pred_decoded, output_dict=True)
 
 # Store model information
 self.model_info = {
 'accuracy': accuracy,
 'n_features': X_train_vectorized.shape[1],
 'n_categories': len(self.label_encoder.classes_),
 'categories': list(self.label_encoder.classes_),
 'training_samples': len(X_train),
 'test_samples': len(X_test),
 'classification_report': class_report
 }
 
 logger.info(f" Training completed!")
 logger.info(f" Accuracy: {accuracy:.3f} ({accuracy*100:.1f}%)")
 logger.info(f" Features: {X_train_vectorized.shape[1]:,}")
 logger.info(f"🏷 Categories: {len(self.label_encoder.classes_)}")
 
 # Print detailed classification report
 logger.info("\n Detailed Classification Report:")
 print(classification_report(y_test_decoded, y_pred_decoded))
 
 return self.model_info
 
 def save_model(self, model_path: str = "vietnamese_transaction_classifier.pkl") -> None:
 """Save the trained model and components"""
 
 logger.info(f" Saving model to {model_path}...")
 
 model_data = {
 'vectorizer': self.vectorizer,
 'classifier': self.classifier,
 'label_encoder': self.label_encoder,
 'model_info': self.model_info
 }
 
 with open(model_path, 'wb') as f:
 pickle.dump(model_data, f)
 
 logger.info(f" Model saved successfully!")
 
 def load_model(self, model_path: str = "vietnamese_transaction_classifier.pkl") -> None:
 """Load a saved model"""
 
 logger.info(f" Loading model from {model_path}...")
 
 with open(model_path, 'rb') as f:
 model_data = pickle.load(f)
 
 self.vectorizer = model_data['vectorizer']
 self.classifier = model_data['classifier']
 self.label_encoder = model_data['label_encoder']
 self.model_info = model_data['model_info']
 
 logger.info(f" Model loaded successfully!")
 logger.info(f" Model accuracy: {self.model_info.get('accuracy', 0):.3f}")
 logger.info(f"🏷 Categories: {', '.join(self.model_info.get('categories', []))}")
 
 def predict(self, description: str) -> Tuple[str, float]:
 """Predict category for a single transaction description"""
 
 if not self.vectorizer or not self.classifier or not self.label_encoder:
 raise ValueError("Model not trained or loaded. Please train or load a model first.")
 
 # Preprocess description
 processed_desc = self.preprocess_vietnamese_text(description)
 
 # Vectorize
 desc_vectorized = self.vectorizer.transform([processed_desc])
 
 # Predict
 prediction_encoded = self.classifier.predict(desc_vectorized)[0]
 prediction_proba = self.classifier.predict_proba(desc_vectorized)[0]
 
 # Decode prediction
 predicted_category = self.label_encoder.inverse_transform([prediction_encoded])[0]
 confidence = prediction_proba[prediction_encoded]
 
 return predicted_category, confidence
 
 def batch_predict(self, descriptions: List[str]) -> List[Tuple[str, float]]:
 """Predict categories for multiple descriptions"""
 
 results = []
 for desc in descriptions:
 try:
 category, confidence = self.predict(desc)
 results.append((category, confidence))
 except Exception as e:
 logger.error(f"Error predicting for '{desc[:50]}...': {e}")
 results.append(('unknown', 0.0))
 
 return results
 
 def test_with_examples(self) -> None:
 """Test the model with some Vietnamese examples"""
 
 logger.info(" Testing model with Vietnamese examples...")
 
 test_examples = [
 "Mua bánh mì thịt nướng tại quán Hương 25k",
 "Grab xe từ Hà Nội đến Nội Bài 150k", 
 "Siêu thị BigC mua thực phẩm 280k",
 "Xem phim CGV Vincom Avatar 120k",
 "Tiền điện tháng 3 EVN Hanoi 450k",
 "Khám răng tại phòng khám Việt Pháp 200k",
 "Học phí trường Đại học Bách Khoa 2500k",
 "Lương tháng 3 công ty ABC 15000k"
 ]
 
 for example in test_examples:
 category, confidence = self.predict(example)
 logger.info(f" '{example}' -> {category} ({confidence:.3f})")

def main():
 """Main training interface"""
 
 print(" Vietnamese Transaction Classifier Trainer")
 print("=" * 50)
 
 # Initialize classifier
 classifier = VietnameseTransactionClassifier()
 
 # Load training data
 try:
 descriptions, categories = classifier.load_training_data()
 except FileNotFoundError:
 logger.error(" Quality sample file not found. Please run data_quality_validator.py first.")
 return
 
 # Train model
 model_info = classifier.train_model(descriptions, categories)
 
 # Save model
 classifier.save_model()
 
 # Test with examples
 classifier.test_with_examples()
 
 # Final summary
 print(f"\n Training Summary:")
 print(f" Accuracy: {model_info['accuracy']:.3f} ({model_info['accuracy']*100:.1f}%)")
 print(f" Features: {model_info['n_features']:,}")
 print(f" Categories: {model_info['n_categories']}")
 print(f" Training samples: {model_info['training_samples']:,}")
 print(f" Test samples: {model_info['test_samples']:,}")
 
 if model_info['accuracy'] >= 0.85:
 print(f"\n EXCELLENT: Model ready for production!")
 elif model_info['accuracy'] >= 0.75:
 print(f"\n GOOD: Model ready for testing!")
 else:
 print(f"\n NEEDS IMPROVEMENT: Consider more training data or feature engineering")

if __name__ == "__main__":
 main()