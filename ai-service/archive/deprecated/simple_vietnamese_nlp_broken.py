#!/usr/bin/env python3
"""
Simplified Vietnamese NLP Pipeline
Using underthesea and pyvi for Vietnamese transaction analysis without transformers compatibility issues
"""

import os
import json
import logging
from typing import List, Dict, Any, Optional, Tuple
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.metrics.pairwise import cosine_similarity
import pandas as pd
from datetime import datetime
import pickle
import glob
import re

# Vietnamese NLP tools
try:
 import underthesea
 UNDERTHESEA_AVAILABLE = True
except ImportError:
 UNDERTHESEA_AVAILABLE = False

try:
 from pyvi import ViTokenizer
 PYVI_AVAILABLE = True
except ImportError:
 PYVI_AVAILABLE = False

# Setup logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class SimpleVietnameseNLPProcessor:
    """
    Simplified Vietnamese NLP processor for financial transaction analysis
    Uses underthesea and pyvi instead of PhoBERT to avoid compatibility issues
    """

    def __init__(self, cache_dir: str = "./models"):
        """
        Initialize Vietnamese NLP processor

        Args:
        cache_dir: Directory to cache models
        """
        self.cache_dir = cache_dir

        # Create cache directory
        os.makedirs(cache_dir, exist_ok=True)

        logger.info(f" Initializing Simple Vietnamese NLP processor...")
        logger.info(f"📦 Underthesea available: {UNDERTHESEA_AVAILABLE}")
            logger.info(f"📦 PyVi available: {PYVI_AVAILABLE}")

        # Vietnamese financial categories with enhanced keywords and patterns
        self.vietnamese_categories = {
                'food': {
        'keywords': [
        # Main food keywords
                        'ăn', 'uống', 'phở', 'cơm', 'bánh', 'chè', 'cafe', 'quán', 'nhà hàng', 'buffet',
                        'đồ ăn', 'thức ăn', 'bún', 'miến', 'hủ tiếu', 'cháo', 'xôi', 'nem', 'gỏi',
        # Chain restaurants
                        'kfc', 'mcdonald', 'lotteria', 'jollibee', 'pizza hut', 'domino',
        # Vietnamese food terms
                        'tái', 'nạm', 'gầu', 'sườn', 'chả', 'thịt nướng', 'bánh xèo', 'bánh cuốn',
        # Drinks
                        'trà', 'cà phê', 'nước', 'bia', 'rượu', 'sinh tố', 'nước ngọt'
                ],
        'patterns': [
                r'quán\s+\w+', r'phở\s+\w+', r'cơm\s+\w+', r'bánh\s+\w+',
                r'\w+\s*k(?:\s|$)', r'combo\s+\w+', r'set\s+\w+'
        ]
        },
        'transport': {
        'keywords': [
        # Transportation
                'xe', 'taxi', 'grab', 'bus', 'xe buýt', 'xe ôm', 'xăng', 'dầu', 'vé', 'tàu',
                'máy bay', 'di chuyển', 'đi lại', 'be', 'gojek', 'uber',
        # Fuel and maintenance
                'petrolimex', 'shell', 'caltex', 'pvoil', 'xăng ron', 'a92', 'a95', 'dầu diesel',
        # Airlines and transport companies
                'vietnam airlines', 'vietjet', 'bamboo airways', 'mai linh', 'vinasun', 'tiên sa',
        # Locations and routes
                'từ', 'đến', 'đi', 'về', 'tuyến', 'chuyến'
        ],
        'patterns': [
            r'grab\s+\w+', r'từ\s+\w+\s+đến\s+\w+', r'tuyến\s+\w+',
            r'xăng\s+\w+', r'vé\s+\w+', r'\w+k\s*(?:từ|đến)'
        ]
        },
        'shopping': {
        'keywords': [
        # Shopping general
                'mua', 'sắm', 'siêu thị', 'chợ', 'shop', 'store', 'cửa hàng', 'trung tâm thương mại',
        # Clothing and accessories
                'áo', 'quần', 'giày', 'dép', 'túi', 'ví', 'đồng hồ', 'kính', 'mũ', 'thắt lưng',
        # Supermarkets and stores
                'vinmart', 'coopmart', 'lotte mart', 'big c', 'metro', 'aeon', 'saigon coop',
                'circle k', 'gs25', 'ministop', 'family mart', 'b\'s mart',
        # Electronics
                'điện thoại', 'laptop', 'iphone', 'samsung', 'oppo', 'vivo', 'xiaomi',
        # General items
                'đồ dùng', 'sản phẩm', 'hàng hóa', 'mỹ phẩm', 'nước hoa'
        ],
        'patterns': [
            r'mua\s+\w+', r'siêu thị\s+\w+', r'shop\s+\w+',
            r'vinmart\+?', r'circle\s*k', r'gs\d+'
        ]
        },
        'entertainment': {
        'keywords': [
        # Entertainment venues
                'phim', 'rạp', 'cinema', 'karaoke', 'game', 'vui chơi', 'giải trí', 'thể thao',
                'gym', 'spa', 'massage', 'bar', 'club', 'pub', 'disco',
        # Cinemas
                'cgv', 'lotte cinema', 'galaxy', 'beta', 'cinestar', 'bhd',
        # Sports and fitness
                'bóng đá', 'tennis', 'cầu lông', 'bơi lội', 'yoga', 'aerobic', 'zumba',
        # Entertainment activities
                'bowling', 'billiards', 'bi-a', 'game center', 'timezone', 'quantum'
        ],
        'patterns': [
            r'xem\s+phim', r'cgv\s+\w+', r'karaoke\s+\w+',
            r'gym\s+\w+', r'spa\s+\w+', r'game\s+\w+'
        ]
        },
        'utilities': {
        'keywords': [
        # Basic utilities
                'điện', 'nước', 'internet', 'điện thoại', 'gas', 'điện lực', 'nước sạch',
        # Utility companies
                'evn', 'vnpt', 'fpt', 'viettel', 'mobifone', 'vinaphone', 'vietnamobile',
                'sawaco', 'hwaco', 'capewaco', 'petrovietnam gas',
        # Services
                'cáp quang', 'wifi', 'adsl', 'fiber', '3g', '4g', '5g', 'truyền hình',
        # Bills
                'hóa đơn', 'tiền', 'phí', 'cước'
        ],
        'patterns': [
            r'tiền\s+điện', r'tiền\s+nước', r'internet\s+\w+',
            r'evn\s*\w*', r'fpt\s*\w*', r'viettel\s*\w*'
        ]
        },
        'healthcare': {
        'keywords': [
        # Medical facilities
                'bệnh viện', 'phòng khám', 'khám', 'chữa', 'điều trị', 'thuốc', 'y tế',
                'bác sĩ', 'thầy thuốc', 'nha khoa', 'răng', 'mắt', 'tim', 'gan', 'thận',
        # Medical procedures
                'xét nghiệm', 'siêu âm', 'x quang', 'mri', 'ct scan', 'nội soi',
                'tiêm', 'vaccine', 'vắc xin', 'phòng ngừa', 'khám định kỳ',
        # Pharmacies and medical stores
                'pharmacity', 'long châu', 'medicare', 'phòng thuốc', 'nhà thuốc',
        # Specialties
                'tai mũi họng', 'da liễu', 'thần kinh', 'cơ xương khớp', 'phụ khoa'
        ],
        'patterns': [
            r'bệnh viện\s+\w+', r'phòng khám\s+\w+', r'khám\s+\w+',
            r'mua thuốc', r'pharmacity', r'long châu'
        ]
        },
        'education': {
        'keywords': [
        # Educational institutions
                'học', 'trường', 'sách', 'khóa học', 'lớp học', 'giáo dục', 'đào tạo',
                'đại học', 'cao đẳng', 'trung học', 'tiểu học', 'mầm non',
        # Subjects and skills
                'tiếng anh', 'tiếng nhật', 'tiếng trung', 'tin học', 'kế toán', 'marketing',
                'lái xe', 'nấu ăn', 'may vá', 'cắt tóc', 'nail', 'makeup',
        # Education companies
                'ila', 'apollo', 'acet', 'apax', 'smartkids', 'ames', 'yola',
        # Materials and fees
                'học phí', 'sách giáo khoa', 'vở', 'bút', 'cặp sách', 'đồng phục'
        ],
        'patterns': [
            r'học\s+\w+', r'trường\s+\w+', r'khóa học\s+\w+',
            r'tiếng\s+\w+', r'học phí', r'sách\s+\w+'
        ]
        },
        'income': {
        'keywords': [
        # Salary and wages
                'lương', 'tiền lương', 'thưởng', 'thu nhập', 'salary', 'wage', 'bonus',
                'tiền công', 'công việc', 'làm việc', 'làm thêm', 'part time', 'full time',
        # Business income
                'bán hàng', 'kinh doanh', 'buôn bán', 'doanh thu', 'lợi nhuận', 'hoa hồng',
                'commission', 'affiliate', 'freelance', 'tự do',
        # Other income sources
                'đầu tư', 'cổ tức', 'lãi suất', 'cho thuê', 'bất động sản',
                'giao hàng', 'shipper', 'grab driver', 'uber', 'be driver'
        ],
        'patterns': [
            r'lương\s+tháng', r'thưởng\s+\w+', r'tiền\s+\w+',
            r'bán\s+\w+', r'thu\s+nhập', r'làm\s+thêm'
        ]
        }
        }

        # Initialize TF-IDF vectorizer for text similarity
        self.vectorizer = TfidfVectorizer(
        max_features=5000,
        stop_words=None, # No built-in Vietnamese stopwords
        ngram_range=(1, 3),
        analyzer='word'
        )

        self.classifier = MultinomialNB()
        self.is_trained = False

    def preprocess_text(self, text: str) -> str:
        """
        Preprocess Vietnamese text

        Args:
        text: Raw Vietnamese text

        Returns:
        Preprocessed text
        """
        # Convert to lowercase
        text = text.lower()

        # Tokenize using PyVi if available
        if PYVI_AVAILABLE:
        try:
        text = ViTokenizer.tokenize(text)
        except:
        pass # Fall back to original text if tokenization fails

        # Remove extra whitespace
        text = ' '.join(text.split())

        return text

    def extract_features(self, description: str) -> Dict[str, Any]:
        """
        Extract features from Vietnamese transaction description

        Args:
        description: Transaction description

        Returns:
        Feature dictionary
        """
        description_lower = description.lower()
        features = {}

        # Keyword matching features
        for category, data in self.vietnamese_categories.items():
        keyword_matches = 0
        pattern_matches = 0

        # Count keyword matches
        for keyword in data['keywords']:
        if keyword in description_lower:
        keyword_matches += 1

        # Count pattern matches
        for pattern in data.get('patterns', []):
        matches = len(re.findall(pattern, description_lower))
        pattern_matches += matches

        features[f'{category}_keywords'] = keyword_matches
        features[f'{category}_patterns'] = pattern_matches
        features[f'{category}_total'] = keyword_matches + pattern_matches

        # Text length features
        features['text_length'] = len(description)
        features['word_count'] = len(description.split())

        # Number detection
        numbers = re.findall(r'\d+', description)
        features['number_count'] = len(numbers)
        features['has_large_number'] = any(int(num) > 1000 for num in numbers if num.isdigit())

        return features

    def classify_transaction(self, description: str) -> Dict[str, Any]:
        """
        Classify Vietnamese transaction description

        Args:
        description: Vietnamese transaction description

        Returns:
        Dictionary with classification results
        """
        # Preprocess text
        processed_text = self.preprocess_text(description)

        # Extract features
        features = self.extract_features(description)

        # Calculate category scores
        category_scores = {}

        for category in self.vietnamese_categories.keys():
        # Get total matches for this category
        total_score = features.get(f'{category}_total', 0)
        keyword_score = features.get(f'{category}_keywords', 0)
        pattern_score = features.get(f'{category}_patterns', 0)

        # Normalize by category keyword count
        max_keywords = len(self.vietnamese_categories[category]['keywords'])
        max_patterns = len(self.vietnamese_categories[category].get('patterns', []))

        # Calculate normalized scores
        keyword_norm = keyword_score / max_keywords if max_keywords > 0 else 0
        pattern_norm = pattern_score / max_patterns if max_patterns > 0 else 0

        # Combined score with weights
        combined_score = (keyword_norm * 0.7) + (pattern_norm * 0.3)

        category_scores[category] = {
        'score': combined_score,
        'keyword_matches': keyword_score,
        'pattern_matches': pattern_score,
        'keyword_norm': keyword_norm,
        'pattern_norm': pattern_norm
        }

        # Find best category
        best_category = max(category_scores.keys(), key=lambda k: category_scores[k]['score'])
        confidence = category_scores[best_category]['score']

        # If confidence is too low, classify as 'other'
        if confidence < 0.1:
        best_category = 'other'
        confidence = 0.5

        return {
        'predicted_category': best_category,
        'confidence': confidence,
        'all_scores': category_scores,
        'features': features,
        'method': 'vietnamese_nlp_simple'
        }

    def extract_financial_entities(self, description: str) -> Dict[str, Any]:
        """
        Extract financial entities from Vietnamese text

        Args:
        description: Transaction description

        Returns:
        Extracted entities
        """
        entities = {
        'amounts': [],
        'merchants': [],
        'locations': [],
        'payment_methods': [],
        'times': []
        }

        # Extract amounts (Vietnamese currency patterns)
        amount_patterns = [
            r'(\d{1,3}(?:\.\d{3})*(?:\,\d+)?)\s*(?:k|K|đ|vnd|VND|nghìn)',
            r'(\d+(?:\.\d+)?)\s*(?:triệu|tỷ)',
            r'(\d+)\s*(?:k|K)(?:\s|$|[^\w])',
        ]

        for pattern in amount_patterns:
        matches = re.findall(pattern, description, re.IGNORECASE)
        entities['amounts'].extend(matches)

        # Extract merchants and shops
        merchant_patterns = [
            r'(?:quán|shop|cửa hàng|siêu thị|chợ)\s+([A-ZÀ-Ỹ][a-zà-ỹ\s]*)',
            r'([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*)\s+(?:store|shop|mart)',
            r'(vinmart|circle\s*k|gs\d+|pharmacity|long châu|cgv|lotte)',
        ]

        for pattern in merchant_patterns:
        matches = re.findall(pattern, description, re.IGNORECASE)
        entities['merchants'].extend(matches)

        # Extract Vietnamese locations
        vietnam_locations = [
            'hà nội', 'tp.hcm', 'sài gòn', 'đà nẵng', 'hải phòng', 'cần thơ',
            'huế', 'nha trang', 'đà lạt', 'vũng tàu', 'quy nhon', 'buôn ma thuột',
            'quận 1', 'quận 2', 'quận 3', 'quận 4', 'quận 5', 'quận 6', 'quận 7',
            'quận 8', 'quận 9', 'quận 10', 'quận 11', 'quận 12', 'thủ đức',
            'ba đình', 'hoàn kiếm', 'đống đa', 'hai bà trưng', 'hoàng mai',
            'long biên', 'tây hồ', 'cầu giấy', 'thanh xuân', 'hà đông',
            'liên chiểu', 'hải châu', 'sơn trà', 'ngũ hành sơn', 'cẩm lệ'
        ]

        for location in vietnam_locations:
        if location in description.lower():
        entities['locations'].append(location)

        # Extract payment methods
        payment_methods = [
            'grab', 'momo', 'zalopay', 'vnpay', 'viettel pay', 'airpay',
            'cash', 'tiền mặt', 'thẻ', 'card', 'visa', 'mastercard',
            'chuyển khoản', 'banking', 'atm'
        ]

        for method in payment_methods:
        if method in description.lower():
        entities['payment_methods'].append(method)

        # Extract time references
        time_patterns = [
            r'tháng\s+(\d{1,2})',
            r'(\d{1,2})/(\d{1,2})',
            r'(hôm nay|hôm qua|tuần này|tháng này)',
        ]

        for pattern in time_patterns:
        matches = re.findall(pattern, description, re.IGNORECASE)
        entities['times'].extend(matches)

        return entities

    def train_classifier(self, transactions: List[Dict]):
        """
        Train the classifier on transaction data

        Args:
        transactions: List of labeled transactions
        """
        logger.info(f" Training classifier on {len(transactions)} transactions...")

        # Prepare training data
        texts = []
        labels = []

        for transaction in transactions:
        text = self.preprocess_text(transaction.get('description', ''))
        texts.append(text)
        labels.append(transaction.get('category', 'other'))

        # Fit vectorizer and classifier
        X = self.vectorizer.fit_transform(texts)
        self.classifier.fit(X, labels)
        self.is_trained = True

        logger.info(" Classifier training completed!")

    def process_transaction_batch(self, transactions: List[Dict]) -> List[Dict]:
        """
        Process a batch of transactions

        Args:
        transactions: List of transaction dictionaries

        Returns:
        Processed transactions with classifications
        """
        logger.info(f"🔄 Processing batch of {len(transactions)} transactions...")

        processed = []
        for i, transaction in enumerate(transactions):
        if i % 1000 == 0 and i > 0:
        logger.info(f" Processed {i}/{len(transactions)} transactions")

        description = transaction.get('description', '')

        # Classify transaction
        classification = self.classify_transaction(description)

        # Extract entities
        entities = self.extract_financial_entities(description)

        # Add to transaction
        processed_transaction = transaction.copy()
        processed_transaction.update({
        'ai_category': classification['predicted_category'],
        'ai_confidence': classification['confidence'],
        'ai_scores': classification['all_scores'],
        'extracted_entities': entities,
        'processed_timestamp': datetime.now().isoformat(),
        'processor_version': 'simple_nlp_v1.0'
        })

        processed.append(processed_transaction)

        logger.info(f" Batch processing completed!")
        return processed

    def test_processor():
        """Test the Vietnamese NLP processor"""
        logger.info(" Testing Vietnamese NLP Processor...")

        # Initialize processor
        processor = SimpleVietnameseNLPProcessor()

        # Test transactions
        test_transactions = [
        {"description": "Quán phở Hùng - Phở bò tái 75k", "amount": 75000},
            {"description": "Grab từ Hà Nội đi Hà Đông 120k", "amount": 120000},
                {"description": "Vinmart+ Cầu Giấy - Mua sắm thực phẩm 350k", "amount": 350000},
                    {"description": "Lương tháng 12 công ty ABC 15000k", "amount": 15000000},
                        {"description": "Tiền điện EVN HANOI tháng 11 - 450k", "amount": 450000},
                            {"description": "CGV Vincom Bà Triệu - Xem phim Avatar 180k", "amount": 180000},
                                {"description": "Pharmacity Nguyễn Trãi - Mua thuốc cảm 85k", "amount": 85000},
                                    {"description": "ILA English Thanh Xuân - Học phí tháng 12", "amount": 2500000},
                                        {"description": "Karaoke Nice Time - Ca hát với bạn bè 320k", "amount": 320000},
                                            {"description": "Circle K Láng Hạ - Mua nước và snack 45k", "amount": 45000}
                                            ]

        # Test classification
        print("\n" + "="*80)
        print(" VIETNAMESE TRANSACTION CLASSIFICATION RESULTS")
        print("="*80)

        for transaction in test_transactions:
        result = processor.classify_transaction(transaction['description'])
        entities = processor.extract_financial_entities(transaction['description'])

                                        print(f"\n Transaction: {transaction['description']}")
                                            print(f"🏷 Category: {result['predicted_category']} (confidence: {result['confidence']:.3f})")
                                                print(f" Amount: {transaction['amount']:,} VND")
                                                    print(f" Entities: {entities}")

        # Show top 3 category scores
        sorted_scores = sorted(result['all_scores'].items(),
        key=lambda x: x[1]['score'], reverse=True)[:3]
        print(f" Top scores:")
        for cat, score_info in sorted_scores:
                                                        print(f" {cat}: {score_info['score']:.3f} (kw:{score_info['keyword_matches']}, pat:{score_info['pattern_matches']})")

        print("\n" + "="*80)
        logger.info(" Testing completed successfully!")

        if __name__ == "__main__":
        test_processor()