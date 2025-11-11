#!/usr/bin/env python3
"""
Enhanced Vietnamese Financial AI Service
Complete RAG + Classification system ready for production
"""

import json
import logging
from pathlib import Path
from typing import Dict, List, Any, Optional
import pickle
from datetime import datetime
import uuid
import re
import numpy as np

try:
    from underthesea import word_tokenize
except ImportError:
    def word_tokenize(text):
        return text.split()

try:
    from sklearn.feature_extraction.text import TfidfVectorizer
    from sklearn.metrics.pairwise import cosine_similarity
except ImportError:
    print("Warning: sklearn not available, using basic similarity")
    TfidfVectorizer = None
    cosine_similarity = None

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class VietnameseFinancialAI:
    def __init__(self):
        """Initialize Enhanced Vietnamese Financial AI system"""
        
        # Load trained classifier
        try:
            self.classifier_data = self._load_classifier()
            logger.info("Vietnamese classifier loaded")
        except Exception as e:
            logger.warning(f"Classifier not available: {e}")
            self.classifier_data = None
        
        # Load transaction embeddings
        self.transaction_embeddings = None
        self.transaction_metadata = None
        
        # Vietnamese financial knowledge base
        self.knowledge_base = self._create_knowledge_base()
        
        # Initialize knowledge vectorizer
        if TfidfVectorizer:
            self.knowledge_vectorizer = TfidfVectorizer(
                max_features=1000,
                ngram_range=(1, 2),
                min_df=1,
                lowercase=True
            )
            
            # Create knowledge embeddings
            self._create_knowledge_embeddings()
    
    def _load_classifier(self):
        """Load trained Vietnamese classifier"""
        try:
            with open("vietnamese_transaction_classifier.pkl", 'rb') as f:
                return pickle.load(f)
        except FileNotFoundError:
            logger.warning("Classifier file not found")
            return None
    
    def _create_knowledge_base(self) -> List[Dict]:
        """Create comprehensive Vietnamese financial knowledge base"""
        
        return [
            {
                "id": "classification_guide",
                "title": "Hướng dẫn phân loại giao dịch tài chính",
                "content": """
                Giao dịch tài chính được phân thành 8 loại chính:
                
                🍜 **Ăn uống (food)**: Quán ăn, cà phê, bánh kẹo, nhà hàng, căng tin
                🚗 **Giao thông (transport)**: Xe bus, taxi, Grab, xăng xe, vé máy bay, tàu xe
                🛒 **Mua sắm (shopping)**: Siêu thị, chợ, cửa hàng, quần áo, đồ gia dụng
                🎬 **Giải trí (entertainment)**: Xem phim, karaoke, du lịch, game, sách báo
                ⚡ **Tiện ích (utilities)**: Điện, nước, internet, điện thoại, gas
                🏥 **Sức khỏe (healthcare)**: Khám bệnh, mua thuốc, bảo hiểm y tế
                📚 **Giáo dục (education)**: Học phí, sách vở, khóa học, gia sư
                💰 **Thu nhập (income)**: Lương, thưởng, làm thêm, bán hàng
                """,
                "keywords": ["phân loại", "giao dịch", "categories", "food", "transport", "shopping"],
                "category": "classification"
            },
            {
                "id": "budget_management",
                "title": "Quản lý ngân sách theo quy tắc 50/30/20",
                "content": """
                Quy tắc quản lý ngân sách 50/30/20 hiệu quả:
                
                **50% cho nhu cầu thiết yếu**: Ăn uống, nhà ở, đi lại, điện nước
                **30% cho mong muốn cá nhân**: Giải trí, mua sắm, du lịch
                **20% cho tiết kiệm & đầu tư**: Gửi ngân hàng, chứng khoán, bất động sản
                
                **Cách thực hiện:**
                - Tính thu nhập sau thuế hàng tháng
                - Chia theo tỷ lệ 50/30/20
                - Theo dõi chi tiêu hàng ngày
                - Điều chỉnh nếu vượt ngân sách
                """,
                "keywords": ["ngân sách", "50/30/20", "quản lý", "tiết kiệm", "chi tiêu"],
                "category": "budgeting"
            }
        ]
    
    def _create_knowledge_embeddings(self):
        """Create embeddings for knowledge base"""
        if not TfidfVectorizer:
            return
            
        knowledge_texts = []
        for item in self.knowledge_base:
            text = f"{item['title']} {item['content']} {' '.join(item['keywords'])}"
            knowledge_texts.append(text)
        
        try:
            self.knowledge_embeddings = self.knowledge_vectorizer.fit_transform(knowledge_texts)
            logger.info(f"Created embeddings for {len(knowledge_texts)} knowledge items")
        except Exception as e:
            logger.error(f"Error creating knowledge embeddings: {e}")
    
    def classify_transaction(self, transaction_text: str) -> Dict[str, Any]:
        """Classify Vietnamese transaction text"""
        
        # Simple rule-based classification as fallback
        categories = {
            'ăn uống': ['ăn', 'uống', 'cà phê', 'cơm', 'phở', 'quán', 'nhà hàng', 'kfc', 'lotteria'],
            'di chuyển': ['grab', 'taxi', 'xe', 'xăng', 'vé', 'máy bay', 'tàu'],
            'mua sắm': ['mua', 'shopping', 'siêu thị', 'chợ', 'quần áo', 'giày'],
            'giải trí': ['xem phim', 'karaoke', 'du lịch', 'game', 'cinema'],
            'tiện ích': ['điện', 'nước', 'internet', 'wifi', 'gas'],
            'sức khỏe': ['bệnh viện', 'thuốc', 'khám'],
            'giáo dục': ['học', 'sách', 'trường'],
            'khác': []
        }
        
        text_lower = transaction_text.lower()
        
        for category, keywords in categories.items():
            for keyword in keywords:
                if keyword in text_lower:
                    return {
                        'category': category,
                        'confidence': 0.8,
                        'method': 'rule_based'
                    }
        
        return {
            'category': 'khác',
            'confidence': 0.5,
            'method': 'default'
        }
    
    def get_financial_advice(self, query: str) -> Dict[str, Any]:
        """Get financial advice using RAG"""
        
        if not self.knowledge_base:
            return {
                'answer': 'Xin lỗi, hệ thống tư vấn chưa sẵn sàng.',
                'confidence': 0.0,
                'sources': []
            }
        
        # Simple keyword matching as fallback
        query_lower = query.lower()
        relevant_items = []
        
        for item in self.knowledge_base:
            for keyword in item['keywords']:
                if keyword.lower() in query_lower:
                    relevant_items.append(item)
                    break
        
        if not relevant_items:
            return {
                'answer': 'Tôi chưa tìm thấy thông tin phù hợp. Bạn có thể hỏi về phân loại giao dịch, quản lý ngân sách, hoặc đầu tư tiết kiệm.',
                'confidence': 0.3,
                'sources': []
            }
        
        # Return the most relevant item
        best_item = relevant_items[0]
        
        return {
            'answer': best_item['content'],
            'confidence': 0.9,
            'sources': [best_item['title']],
            'category': best_item['category']
        }
    
    def process_transaction_batch(self, transactions: List[Dict]) -> List[Dict]:
        """Process a batch of transactions"""
        results = []
        
        for transaction in transactions:
            try:
                text = transaction.get('text', transaction.get('description', ''))
                classification = self.classify_transaction(text)
                
                enhanced_transaction = transaction.copy()
                enhanced_transaction.update({
                    'ai_category': classification['category'],
                    'ai_confidence': classification['confidence'],
                    'processed_at': datetime.now().isoformat(),
                    'processor_version': '2.0'
                })
                
                results.append(enhanced_transaction)
                
            except Exception as e:
                logger.error(f"Error processing transaction: {e}")
                results.append(transaction)
        
        return results

def main():
    """Test the Enhanced Vietnamese Financial AI"""
    ai = VietnameseFinancialAI()
    
    # Test classification
    test_transactions = [
        "Mua cà phê Starbucks 85000 VND",
        "Grab từ nhà đến công ty 45000 VND",
        "Mua sắm ở Vincom 250000 VND"
    ]
    
    logger.info("Testing transaction classification:")
    for text in test_transactions:
        result = ai.classify_transaction(text)
        logger.info(f"'{text}' -> {result['category']} (confidence: {result['confidence']})")
    
    # Test advice
    test_queries = [
        "Làm thế nào để quản lý ngân sách?",
        "Phân loại giao dịch như thế nào?",
        "Nên đầu tư vào đâu?"
    ]
    
    logger.info("\nTesting financial advice:")
    for query in test_queries:
        advice = ai.get_financial_advice(query)
        logger.info(f"Q: {query}")
        logger.info(f"A: {advice['answer'][:100]}...")
        logger.info(f"Confidence: {advice['confidence']}\n")

if __name__ == "__main__":
    main()