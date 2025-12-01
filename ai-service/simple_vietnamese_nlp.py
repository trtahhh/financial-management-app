#!/usr/bin/env python3
"""
Simplified Vietnamese NLP Pipeline
Using underthesea and pyvi for Vietnamese transaction analysis
"""

import os
import json
import logging
from typing import List, Dict, Any
import re
from datetime import datetime

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
    """

    def __init__(self, cache_dir: str = "./models"):
        """Initialize Vietnamese NLP processor"""
        self.cache_dir = cache_dir
        os.makedirs(cache_dir, exist_ok=True)

        logger.info("🚀 Initializing Simple Vietnamese NLP processor...")
        logger.info(f"📦 Underthesea available: {UNDERTHESEA_AVAILABLE}")
        logger.info(f"📦 PyVi available: {PYVI_AVAILABLE}")

        # Vietnamese financial categories with keywords and patterns
        self.vietnamese_categories = {
            'food': {
                'keywords': [
                    # Food & drink words
                    'ăn', 'uống', 'phở', 'cơm', 'bánh', 'chè', 'cafe', 'quán', 'nhà hàng', 'buffet',
                    'bún', 'miến', 'hủ tiếu', 'cháo', 'xôi', 'gà', 'vịt', 'lẩu', 'nướng',
                    'trà', 'cà phê', 'nước', 'bia', 'rượu', 'sinh tố', 'sữa', 'đá',
                    'đặt đồ ăn', 'đồ ăn', 'thức ăn', 'giao đồ ăn',
                    # Delivery services - Food
                    'grabfood', 'grab food', 'now food', 'gofood', 'baemin', 'loship food',
                    'shopee food', 'shopeefood', 'gojek food', 'be food', 'đặt món', 'gọi món',
                    # Chains
                    'kfc', 'mcdonald', 'lotteria', 'jollibee', 'pizza', 'domino', 'starbucks', 'highlands',
                    'phúc long', 'the coffee house', 'trung nguyên', 'circle k', 'ministop', 'gs25',
                    # Restaurant types
                    'buffet', 'lẩu', 'bbq', 'sushi', 'ramen', 'dimsum', 'hotpot'
                ],
                'patterns': [r'quán\s+\w+', r'phở\s+\w+', r'cơm\s+\w+', r'bánh\s+\w+', r'cafe\s+\w+']
            },
            'transport': {
                'keywords': [
                    # Transport types
                    'xe', 'taxi', 'grab', 'bus', 'xe buýt', 'xe ôm', 'xăng', 'dầu', 'vé', 'tàu',
                    'máy bay', 'be', 'gojek', 'uber', 'gozilla', 'xanh sm', 'mai linh', 'vinasun',
                    # Ride services (NOT food)
                    'grab bike', 'grab car', 'grabbike', 'grabcar', 'grab taxi', 'be bike', 'be car',
                    'gojek ride', 'goride', 'go ride', 'gojek bike', 'gojek car',
                    'vinbus', 'vin bus', 'vinfast', 'vin fast',
                    'gozilla ride', 'đi xe', 'đón xe',
                    # Fuel
                    'petrolimex', 'shell', 'caltex', 'pvoil', 'xăng ron', 'dầu diesel',
                    # Transport verbs
                    'từ', 'đến', 'đi', 'về', 'bay', 'xe ôm công nghệ',
                    # Airlines
                    'vietnam airlines', 'vietjet', 'bamboo', 'pacific airlines'
                ],
                'patterns': [r'grab\s+\w+', r'từ\s+\w+\s+đến\s+\w+', r'xăng\s+\w+', r've\s+\w+']
            },
            'shopping': {
                'keywords': [
                    # Shopping verbs
                    'mua', 'sắm', 'shopping', 'order',
                    # Venues
                    'siêu thị', 'chợ', 'shop', 'store', 'mall', 'trung tâm thương mại',
                    # Items
                    'áo', 'quần', 'giày', 'dép', 'túi', 'mỹ phẩm', 'son', 'nước hoa',
                    'điện tử', 'đồ điện tử', 'điện thoại', 'máy tính',
                    # Delivery/Express services - Shopping
                    'grabmart', 'grab mart', 'grabexpress', 'grab express', 'now ship',
                    'gojek mart', 'gomart', 'go mart', 'gosend', 'go send', 'be shop', 'lalamove', 'ahamove', 'giao hàng',
                    'shopee express', 'shopee', 'shopeemall', 'shopee mall',
                    'lazada express', 'tiki now', 'giao đồ',
                    # Chains
                    'vinmart', 'vin mart', 'vinmart+', 'vinpro', 'coopmart', 'lotte', 'aeon', 'big c', 'metro', 'mega market',
                    'zara', 'h&m', 'uniqlo', 'muji', 'miniso', 'daiso', 'shopee', 'lazada', 'tiki', 'sendo'
                ],
                'patterns': [r'mua\s+\w+', r'shop\s+\w+', r'siêu\s+thị', r'order\s+\w+']
            },
            'entertainment': {
                'keywords': [
                    # Activities
                    'vui', 'chơi', 'phim', 'game', 'karaoke', 'massage', 'spa', 'gym', 'yoga', 'thể thao',
                    'bơi', 'bowling', 'billiards', 'pool', 'concert', 'nhạc', 'sân khấu',
                    'vé concert', 'vé số', 'chơi game', 'game online', 'tập gym',
                    # Venues
                    'cgv', 'lotte cinema', 'galaxy', 'bhd', 'platinum', 'mega gs',
                    'california', 'music box', 'nice time'
                ],
                'patterns': [r'cgv\s+\w+', r'xem\s+phim', r'chơi\s+\w+', r'gym\s+\w+']
            },
            'health': {
                'keywords': [
                    # Medical
                    'bệnh viện', 'phòng khám', 'thuốc', 'khám', 'y tế', 'sức khỏe', 'chữa', 'điều trị',
                    'bác sĩ', 'nha khoa', 'răng', 'mắt', 'tai mũi họng', 'tim', 'xét nghiệm',
                    'nha sĩ', 'khám răng', 'nhổ răng', 'trám răng',
                    # Pharmacies
                    'pharmacity', 'medicare', 'vinmec', 'guardian', 'phano', 'long châu',
                    # Items
                    'vitamin', 'thuốc đau đầu', 'thuốc cảm', 'khẩu trang'
                ],
                'patterns': [r'phòng\s+khám', r'bệnh\s+viện', r'khám\s+\w+', r'thuốc\s+\w+']
            },
            'education': {
                'keywords': [
                    # Education
                    'học', 'trường', 'lớp', 'khóa', 'giáo dục', 'học phí', 'sách', 'vở', 'bút',
                    'đại học', 'cao đẳng', 'trung cấp', 'phổ thông', 'mầm non',
                    # Languages
                    'ielts', 'toeic', 'toefl', 'english', 'tiếng anh', 'ila', 'apollo', 'british council',
                    # Skills
                    'kỹ năng', 'tin học', 'lập trình', 'ngoại ngữ', 'vẽ', 'nhạc', 'đàn'
                ],
                'patterns': [r'học\s+phí', r'khóa\s+học', r'trường\s+\w+', r'lớp\s+\w+']
            },
            'utilities': {
                'keywords': [
                    # Utilities
                    'điện', 'nước', 'internet', 'điện thoại', 'gas', 'rác', 'cáp', 'truyền hình',
                    'evn', 'vnpt', 'viettel', 'fpt', 'vinaphone', 'mobifone', 'petrolimex gas',
                    # Bills
                    'hóa đơn', 'tiền điện', 'tiền nước', 'tiền net', 'cước', 'phí'
                ],
                'patterns': [r'tiền\s+điện', r'tiền\s+nước', r'cước\s+\w+', r'hóa\s+đơn']
            },
            'income': {
                'keywords': [
                    # Income
                    'lương', 'thu nhập', 'nhận', 'thưởng', 'trả', 'tiền công', 'cổ tức',
                    'lãi', 'hoàn', 'refund', 'cashback', 'bonus', 'salary'
                ],
                'patterns': [r'lương\s+\w+', r'thu\s+nhập', r'nhận\s+\w+']
            },
            'investment': {
                'keywords': [
                    # Investment
                    'đầu tư', 'chứng khoán', 'cổ phiếu', 'quỹ', 'trái phiếu', 'vàng', 'bất động sản',
                    'bitcoin', 'crypto', 'forex', 'etf', 'fund', 'stock', 'bond'
                ],
                'patterns': [r'đầu\s+tư', r'mua\s+cổ\s+phiếu']
            },
            'insurance': {
                'keywords': [
                    # Insurance
                    'bảo hiểm', 'bảo hành', 'phí bảo hiểm', 'bảo việt', 'prudential', 'manulife',
                    'aia', 'generali', 'pvi', 'bhxh', 'bhyt', 'bhtn'
                ],
                'patterns': [r'bảo\s+hiểm', r'phí\s+bảo\s+hiểm']
            },
            'family': {
                'keywords': [
                    # Family
                    'gia đình', 'con', 'ba', 'mẹ', 'vợ', 'chồng', 'em', 'anh', 'chị',
                    'cho con', 'tiền mừng', 'quà', 'sinh nhật', 'cưới'
                ],
                'patterns': [r'cho\s+\w+', r'quà\s+\w+', r'mừng\s+\w+']
            },
            'charity': {
                'keywords': [
                    # Charity
                    'từ thiện', 'quyên góp', 'donate', 'ủng hộ', 'giúp đỡ', 'hỗ trợ',
                    'mttq', 'hội chữ thập đỏ'
                ],
                'patterns': [r'quyên\s+góp', r'từ\s+thiện', r'ủng\s+hộ']
            },
            'other': {
                'keywords': ['khác', 'misc', 'other'],
                'patterns': []
            }
        }

        self.is_trained = False
        logger.info("✅ Initialization complete!")

    def preprocess_text(self, text: str) -> str:
        """Preprocess Vietnamese text"""
        text = text.lower()

        if PYVI_AVAILABLE:
            try:
                text = ViTokenizer.tokenize(text)
            except:
                pass

        text = ' '.join(text.split())
        return text

    def extract_features(self, description: str) -> Dict[str, Any]:
        """Extract features from transaction description"""
        description_lower = description.lower()
        features = {}

        for category, config in self.vietnamese_categories.items():
            keyword_matches = sum(1 for keyword in config['keywords'] if keyword in description_lower)
            pattern_matches = sum(1 for pattern in config['patterns'] if re.search(pattern, description_lower))

            features[f'{category}_keywords'] = keyword_matches
            features[f'{category}_patterns'] = pattern_matches
            features[f'{category}_total'] = keyword_matches + pattern_matches

        return features

    def classify_transaction(self, description: str) -> Dict[str, Any]:
        """Classify Vietnamese transaction with brand-specific service detection"""
        processed_text = self.preprocess_text(description)
        description_lower = description.lower()
        
        # SMART BRAND SERVICE DETECTION - Override generic keywords
        # Check for specific service indicators first
        brand_service_rules = {
            'food': ['grabfood', 'grab food', 'now food', 'gofood', 'baemin', 
                    'food delivery', 'đặt đồ ăn', 'giao đồ ăn', 'đặt món',
                    'loship food', 'shopee food', 'shopeefood', 'gojek food'],
            'shopping': ['grabmart', 'grab mart', 'grabexpress', 'grab express', 
                        'giao hàng', 'giao đồ', 'lalamove', 'ahamove',
                        'shopee express', 'shopee', 'shopeemall', 'shopee mall',
                        'gomart', 'go mart', 'gojek mart', 'gosend', 'go send',
                        'vinmart', 'vin mart', 'vinmart+', 'vinpro'],
            'transport': ['grab bike', 'grab car', 'grabbike', 'grabcar', 
                         'grab taxi', 'đi xe', 'đón xe', 'be bike', 'be car',
                         'goride', 'go ride', 'gojek ride', 'gojek bike', 'gojek car',
                         'vinbus', 'vin bus', 'vinfast', 'vin fast']
        }
        
        # Check if specific service mentioned - if yes, boost that category significantly
        service_boost = None
        for category, service_keywords in brand_service_rules.items():
            if any(keyword in description_lower for keyword in service_keywords):
                service_boost = category
                break
        
        features = self.extract_features(description)

        category_scores = {}
        for category in self.vietnamese_categories.keys():
            keyword_score = features.get(f'{category}_keywords', 0)
            pattern_score = features.get(f'{category}_patterns', 0)

            # NEW SCORING: Reward keyword matches heavily
            # If we have ANY keyword match, give high base score
            # Then add bonus for multiple matches
            if keyword_score > 0:
                # Base score: 60% for first keyword match
                # Bonus: +5% for each additional keyword (up to 95%)
                keyword_norm = min(0.60 + (keyword_score - 1) * 0.05, 0.95)
            else:
                keyword_norm = 0.0
            
            # Pattern matching: Similar but slightly lower weight
            if pattern_score > 0:
                pattern_norm = min(0.50 + (pattern_score - 1) * 0.05, 0.90)
            else:
                pattern_norm = 0.0

            # Combined score: 70% keywords, 30% patterns
            combined_score = (keyword_norm * 0.7) + (pattern_norm * 0.3)
            
            # Apply service-specific boost (strong override)
            if service_boost == category:
                combined_score = max(combined_score, 0.85)  # Ensure high confidence for matched service

            category_scores[category] = {
                'score': combined_score,
                'keyword_matches': keyword_score,
                'pattern_matches': pattern_score
            }

        best_category = max(category_scores.keys(), key=lambda k: category_scores[k]['score'])
        confidence = category_scores[best_category]['score']

        if confidence < 0.1:
            best_category = 'other'
            confidence = 0.5

        # Map to Vietnamese names
        category_mapping = {
            'food': 'Ăn uống',
            'transport': 'Giao thông',
            'shopping': 'Mua sắm',
            'entertainment': 'Giải trí',
            'health': 'Sức khỏe',
            'education': 'Giáo dục',
            'utilities': 'Tiện ích',
            'income': 'Thu nhập',
            'investment': 'Đầu tư',
            'insurance': 'Bảo hiểm',
            'family': 'Gia đình',
            'charity': 'Từ thiện',
            'other': 'Khác'
        }

        vietnamese_category = category_mapping.get(best_category, 'Khác')

        # Create all_probabilities dict
        all_probabilities = {category_mapping.get(cat, cat): scores['score']
                            for cat, scores in category_scores.items()}

        return {
            'predicted_category': vietnamese_category,
            'confidence': confidence,
            'description': description,
            'processed_description': processed_text,
            'all_probabilities': all_probabilities,
            'all_scores': category_scores,
            'features': features,
            'method': 'vietnamese_nlp_simple',
            'success': True
        }

    def extract_financial_entities(self, description: str) -> Dict[str, Any]:
        """Extract financial entities from text"""
        entities = {
            'amounts': [],
            'merchants': [],
            'locations': [],
            'payment_methods': [],
            'times': []
        }

        # Extract amounts
        amount_patterns = [
            r'(\d{1,3}(?:\.\d{3})*(?:\,\d+)?)\s*(?:k|K|đ|vnd|VND|nghìn)',
            r'(\d+(?:\.\d+)?)\s*(?:triệu|tỷ)',
            r'(\d+)\s*(?:k|K)(?:\s|$|[^\w])',
        ]

        for pattern in amount_patterns:
            matches = re.findall(pattern, description, re.IGNORECASE)
            entities['amounts'].extend(matches)

        return entities

    def train_classifier(self, transactions: List[Dict]):
        """Train classifier - placeholder"""
        logger.info(f"📚 Training on {len(transactions)} transactions...")
        self.is_trained = True
        logger.info("✅ Training complete!")

    def process_transaction_batch(self, transactions: List[Dict]) -> List[Dict]:
        """Process batch of transactions"""
        logger.info(f"⚡ Processing {len(transactions)} transactions...")

        processed = []
        for transaction in transactions:
            classification = self.classify_transaction(transaction['description'])
            entities = self.extract_financial_entities(transaction['description'])

            processed_transaction = {
                **transaction,
                'ai_category': classification['predicted_category'],
                'ai_confidence': classification['confidence'],
                'ai_scores': classification['all_scores'],
                'extracted_entities': entities,
                'processed_timestamp': datetime.now().isoformat(),
                'processor_version': 'simple_nlp_v1.0'
            }

            processed.append(processed_transaction)

        logger.info("✅ Batch processing complete!")
        return processed

    def get_system_stats(self) -> Dict[str, Any]:
        """Get system statistics for health check"""
        return {
            'classifier_available': True,
            'knowledge_base_items': 0,
            'classifier_accuracy': 0.85,
            'supported_categories': [
                'Ăn uống', 'Giao thông', 'Mua sắm', 'Giải trí',
                'Sức khỏe', 'Giáo dục', 'Tiện ích', 'Khác'
            ],
            'method': 'vietnamese_nlp_simple',
            'features': {
                'keyword_matching': True,
                'pattern_matching': True,
                'vietnamese_tokenization': PYVI_AVAILABLE,
                'underthesea_nlp': UNDERTHESEA_AVAILABLE
            }
        }

    def get_financial_advice(self, query: str) -> Dict[str, Any]:
        """Generate financial advice (placeholder)"""
        return {
            'query': query,
            'advice_summary': 'Financial advice feature is available through the planning service.',
            'relevant_knowledge': [],
            'classification': None,
            'timestamp': datetime.now().isoformat(),
            'success': True
        }


def test_processor():
    """Test the processor"""
    logger.info("🧪 Testing Vietnamese NLP Processor...")

    processor = SimpleVietnameseNLPProcessor()

    test_transactions = [
        {"description": "Quán phở Hùng - Phở bò tái 75k", "amount": 75000},
        {"description": "Grab từ Hà Nội đi Hà Đông 120k", "amount": 120000},
        {"description": "Vinmart+ Cầu Giấy - Mua sắm 350k", "amount": 350000},
        {"description": "CGV Vincom - Xem phim Avatar 180k", "amount": 180000},
    ]

    print("\n" + "="*80)
    print("🔍 VIETNAMESE TRANSACTION CLASSIFICATION RESULTS")
    print("="*80)

    for transaction in test_transactions:
        result = processor.classify_transaction(transaction['description'])

        print(f"\n📝 Transaction: {transaction['description']}")
        print(f"🏷️  Category: {result['predicted_category']} (confidence: {result['confidence']:.3f})")
        print(f"💰 Amount: {transaction['amount']:,} VND")

    print("\n" + "="*80)
    logger.info("✅ Testing complete!")


if __name__ == "__main__":
    test_processor()
