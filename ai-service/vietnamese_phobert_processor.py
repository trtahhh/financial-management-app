"""
Vietnamese PhoBERT Processor
High-performance Vietnamese text processing using PhoBERT
"""

import torch
import logging
import numpy as np
from typing import List, Dict, Any, Optional
from transformers import AutoTokenizer, AutoModel
import pickle
import os
from datetime import datetime

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class VietnamesePhoBERTProcessor:
    """
    Advanced Vietnamese text processor using PhoBERT
    Provides embeddings, similarity, and classification capabilities
    """
    
    def __init__(self, model_name: str = "vinai/phobert-base", cache_dir: str = "./models"):
        """
        Initialize PhoBERT processor
        
        Args:
            model_name: PhoBERT model name from HuggingFace
            cache_dir: Directory to cache models
        """
        self.model_name = model_name
        self.cache_dir = cache_dir
        self.tokenizer = None
        self.model = None
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        
        # Caches
        self.embeddings_cache = {}
        self.similarity_cache = {}
        
        logger.info(f"🚀 Initializing PhoBERT Processor with {model_name}")
        logger.info(f"💾 Cache directory: {cache_dir}")
        logger.info(f"🖥️  Device: {self.device}")
        
        # Create cache directory
        os.makedirs(cache_dir, exist_ok=True)
        
        # Load models
        self._load_models()
    
    def _load_models(self):
        """Load PhoBERT models"""
        try:
            logger.info("📥 Loading PhoBERT tokenizer...")
            self.tokenizer = AutoTokenizer.from_pretrained(
                self.model_name,
                cache_dir=self.cache_dir,
                local_files_only=False
            )
            
            logger.info("📥 Loading PhoBERT model...")
            self.model = AutoModel.from_pretrained(
                self.model_name,
                cache_dir=self.cache_dir,
                local_files_only=False
            )
            
            # Move to device
            self.model.to(self.device)
            self.model.eval()  # Set to evaluation mode
            
            logger.info("✅ PhoBERT models loaded successfully!")
            
        except Exception as e:
            logger.error(f"❌ Error loading PhoBERT models: {e}")
            raise e
    
    def get_embeddings(self, text: str) -> np.ndarray:
        """
        Get PhoBERT embeddings for Vietnamese text
        
        Args:
            text: Vietnamese text to embed
            
        Returns:
            numpy array of embeddings
        """
        # Check cache first
        if text in self.embeddings_cache:
            return self.embeddings_cache[text]
        
        try:
            # Tokenize text
            inputs = self.tokenizer(
                text,
                return_tensors="pt",
                truncation=True,
                padding=True,
                max_length=256
            )
            
            # Move to device
            inputs = {k: v.to(self.device) for k, v in inputs.items()}
            
            # Get embeddings
            with torch.no_grad():
                outputs = self.model(**inputs)
                
            # Use CLS token embedding (first token)
            embeddings = outputs.last_hidden_state[:, 0, :].cpu().numpy()
            
            # Cache result
            self.embeddings_cache[text] = embeddings[0]
            
            return embeddings[0]
            
        except Exception as e:
            logger.error(f"Error getting embeddings for text: {e}")
            # Return zero vector as fallback
            return np.zeros(768)  # PhoBERT dimension
    
    def calculate_similarity(self, text1: str, text2: str) -> float:
        """
        Calculate cosine similarity between two Vietnamese texts
        
        Args:
            text1: First text
            text2: Second text
            
        Returns:
            Similarity score between 0 and 1
        """
        cache_key = f"{text1}|||{text2}"
        if cache_key in self.similarity_cache:
            return self.similarity_cache[cache_key]
        
        try:
            # Get embeddings
            emb1 = self.get_embeddings(text1)
            emb2 = self.get_embeddings(text2)
            
            # Calculate cosine similarity
            dot_product = np.dot(emb1, emb2)
            norm1 = np.linalg.norm(emb1)
            norm2 = np.linalg.norm(emb2)
            
            if norm1 == 0 or norm2 == 0:
                similarity = 0.0
            else:
                similarity = dot_product / (norm1 * norm2)
            
            # Normalize to 0-1 range
            similarity = (similarity + 1) / 2
            
            # Cache result
            self.similarity_cache[cache_key] = similarity
            
            return float(similarity)
            
        except Exception as e:
            logger.error(f"Error calculating similarity: {e}")
            return 0.0
    
    def process_batch(self, texts: List[str]) -> List[np.ndarray]:
        """
        Process multiple texts in batch for better performance
        
        Args:
            texts: List of Vietnamese texts
            
        Returns:
            List of embedding arrays
        """
        embeddings = []
        
        try:
            for text in texts:
                embedding = self.get_embeddings(text)
                embeddings.append(embedding)
            
            logger.info(f"✅ Processed {len(texts)} texts successfully")
            return embeddings
            
        except Exception as e:
            logger.error(f"Error processing batch: {e}")
            return [np.zeros(768) for _ in texts]
    
    def find_most_similar(self, query: str, candidates: List[str], top_k: int = 5) -> List[Dict[str, Any]]:
        """
        Find most similar texts to query from candidates
        
        Args:
            query: Query text
            candidates: List of candidate texts
            top_k: Number of top results to return
            
        Returns:
            List of similarity results with scores
        """
        results = []
        
        try:
            for candidate in candidates:
                similarity = self.calculate_similarity(query, candidate)
                results.append({
                    'text': candidate,
                    'similarity': similarity
                })
            
            # Sort by similarity score
            results.sort(key=lambda x: x['similarity'], reverse=True)
            
            return results[:top_k]
            
        except Exception as e:
            logger.error(f"Error finding similar texts: {e}")
            return []
    
    def save_cache(self, cache_file: str = "phobert_cache.pkl"):
        """Save embedding cache to disk"""
        try:
            cache_path = os.path.join(self.cache_dir, cache_file)
            cache_data = {
                'embeddings_cache': self.embeddings_cache,
                'similarity_cache': self.similarity_cache,
                'timestamp': datetime.now().isoformat()
            }
            
            with open(cache_path, 'wb') as f:
                pickle.dump(cache_data, f)
            
            logger.info(f"💾 Cache saved to {cache_path}")
            
        except Exception as e:
            logger.error(f"Error saving cache: {e}")
    
    def load_cache(self, cache_file: str = "phobert_cache.pkl"):
        """Load embedding cache from disk"""
        try:
            cache_path = os.path.join(self.cache_dir, cache_file)
            
            if os.path.exists(cache_path):
                with open(cache_path, 'rb') as f:
                    cache_data = pickle.load(f)
                
                self.embeddings_cache = cache_data.get('embeddings_cache', {})
                self.similarity_cache = cache_data.get('similarity_cache', {})
                
                logger.info(f"📥 Cache loaded from {cache_path}")
                logger.info(f"🔢 Loaded {len(self.embeddings_cache)} embeddings and {len(self.similarity_cache)} similarities")
            
        except Exception as e:
            logger.error(f"Error loading cache: {e}")
    
    def get_stats(self) -> Dict[str, Any]:
        """Get processor statistics"""
        return {
            'model_name': self.model_name,
            'device': str(self.device),
            'embeddings_cached': len(self.embeddings_cache),
            'similarities_cached': len(self.similarity_cache),
            'cache_dir': self.cache_dir,
            'model_loaded': self.model is not None and self.tokenizer is not None
        }

# Test function
def test_phobert_processor():
    """Test PhoBERT processor functionality"""
    try:
        logger.info("🧪 Testing PhoBERT Processor...")
        
        processor = VietnamesePhoBERTProcessor()
        
        # Test texts
        test_texts = [
            "Tôi muốn tiết kiệm tiền để mua nhà",
            "Chi tiêu hàng tháng của tôi rất cao",
            "Đầu tư chứng khoán có rủi ro không?",
            "Làm sao để quản lý ngân sách hiệu quả?"
        ]
        
        # Test embeddings
        logger.info("Testing embeddings...")
        embeddings = processor.process_batch(test_texts)
        logger.info(f"Generated {len(embeddings)} embeddings")
        
        # Test similarity
        logger.info("Testing similarity...")
        similarity = processor.calculate_similarity(test_texts[0], test_texts[1])
        logger.info(f"Similarity score: {similarity:.3f}")
        
        # Test search
        logger.info("Testing similarity search...")
        results = processor.find_most_similar(
            "tiết kiệm tiền", 
            test_texts, 
            top_k=3
        )
        
        for i, result in enumerate(results):
            logger.info(f"Result {i+1}: {result['similarity']:.3f} - {result['text'][:50]}...")
        
        # Get stats
        stats = processor.get_stats()
        logger.info(f"📊 Processor stats: {stats}")
        
        logger.info("✅ PhoBERT Processor test completed successfully!")
        
    except Exception as e:
        logger.error(f"❌ Test failed: {e}")

if __name__ == "__main__":
    test_phobert_processor()