import json
import pickle
import logging
import numpy as np
import pandas as pd
from datetime import datetime

try:
    from sklearn.feature_extraction.text import TfidfVectorizer
    from sklearn.ensemble import RandomForestClassifier
    from sklearn.model_selection import train_test_split, cross_val_score, GridSearchCV
    from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
    from sklearn.pipeline import Pipeline
    sklearn_available = True
except ImportError:
    sklearn_available = False
    print("Warning: sklearn not available")

try:
    import matplotlib.pyplot as plt
    import seaborn as sns
    viz_available = True
except ImportError:
    viz_available = False
    print("Warning: visualization libraries not available")

try:
    from underthesea import word_tokenize
except ImportError:
    def word_tokenize(text):
        return text.split()

try:
    import joblib
except ImportError:
    joblib = None

# Setup logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class EnhancedVietnameseClassifierTrainer:
    def __init__(self):
        self.model = None
        self.vectorizer = None
        self.label_encoder = None
        self.training_history = []
        
        if not sklearn_available:
            logger.warning("Sklearn not available, some features will be limited")
    
    def load_dataset(self, file_path="expanded_vietnamese_transactions.json"):
        """Load expanded dataset"""
        logger.info(f"Loading dataset from {file_path}...")
        
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            df = pd.DataFrame(data)
            logger.info(f"Loaded {len(df):,} samples with {df['category'].nunique()} categories")
            
            # Dataset statistics
            category_counts = df['category'].value_counts()
            logger.info("Category distribution:")
            for category, count in category_counts.items():
                logger.info(f"  {category}: {count:,} samples")
            
            return df
            
        except FileNotFoundError:
            logger.error(f"Dataset file not found: {file_path}")
            raise
        except Exception as e:
            logger.error(f"Error loading dataset: {e}")
            raise

    def preprocess_vietnamese_text(self, text):
        """Enhanced Vietnamese text preprocessing"""
        import re
        
        # Basic cleaning
        text = str(text).lower().strip()
        text = re.sub(r'\d+', ' NUMBER ', text)  # Replace numbers
        text = re.sub(r'[^\w\s]', ' ', text)  # Remove punctuation
        text = re.sub(r'\s+', ' ', text)  # Normalize whitespace
        
        # Vietnamese tokenization
        try:
            tokens = word_tokenize(text)
            return ' '.join(tokens)
        except:
            return text

    def predict(self, text):
        """Predict category for a single text"""
        if not self.model:
            logger.error("No trained model available")
            return None
            
        processed_text = self.preprocess_vietnamese_text(text)
        
        try:
            prediction = self.model.predict([processed_text])[0]
            
            # Get prediction probability if available
            if hasattr(self.model, 'predict_proba'):
                probabilities = self.model.predict_proba([processed_text])[0]
                confidence = max(probabilities)
            else:
                confidence = 0.8
            
            return {
                'category': prediction,
                'confidence': confidence,
                'processed_text': processed_text
            }
        except Exception as e:
            logger.error(f"Error making prediction: {e}")
            return None

def main():
    """Main training function"""
    logger.info("Enhanced Vietnamese Classifier Trainer")
    logger.info("=" * 50)
    
    trainer = EnhancedVietnameseClassifierTrainer()
    
    logger.info("Enhanced Vietnamese Classifier Trainer ready!")

if __name__ == "__main__":
    main()