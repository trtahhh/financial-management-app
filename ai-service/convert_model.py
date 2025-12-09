import pickle
import logging

try:
    from sklearn.ensemble import RandomForestClassifier
    from sklearn.feature_extraction.text import TfidfVectorizer
    sklearn_available = True
except ImportError:
    sklearn_available = False
    print("Warning: sklearn not available")

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def extract_pipeline_components():
    """Extract components from pipeline for compatibility with old code"""
    
    if not sklearn_available:
        logger.error("Sklearn not available, cannot extract pipeline components")
        return False
    
    try:
        # Load enhanced pipeline
        with open('enhanced_vietnamese_classifier.pkl', 'rb') as f:
            pipeline = pickle.load(f)
        
        logger.info("Loaded enhanced pipeline")
        
        # Extract components
        vectorizer = pipeline.named_steps['tfidf']
        classifier = pipeline.named_steps['classifier']
        
        logger.info(f"Vectorizer features: {vectorizer.max_features}")
        logger.info(f"Classifier trees: {classifier.n_estimators}")
        
        # Create compatible classifier object
        compatible_classifier = {
            'vectorizer': vectorizer,
            'classifier': classifier,
            'categories': classifier.classes_.tolist()
        }
        
        # Save compatible versions
        with open('vietnamese_transaction_classifier.pkl', 'wb') as f:
            pickle.dump(compatible_classifier, f)
        
        with open('tfidf_vectorizer.pkl', 'wb') as f:
            pickle.dump(vectorizer, f)
        
        logger.info("Saved compatible classifier format")
        logger.info(f"Categories: {compatible_classifier['categories']}")
        
        return True
        
    except FileNotFoundError:
        logger.error("Enhanced classifier file not found")
        return False
    except Exception as e:
        logger.error(f"Failed to extract components: {e}")
        return False

if __name__ == "__main__":
    success = extract_pipeline_components()
    if success:
        print("Enhanced model converted to compatible format!")
    else:
        print("Conversion failed!")