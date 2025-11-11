import json
import random
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
import pickle
import logging
from pathlib import Path

# Setup logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class VietnameseDatasetExpander:
    def __init__(self):
        self.categories = {
            'ăn uống': {
                'keywords': ['ăn', 'uống', 'cà phê', 'trà', 'bún', 'phở', 'cơm', 'bánh', 'nước', 'bia', 'rượu', 'nhà hàng', 'quán', 'buffet', 'lẩu', 'nướng', 'chè', 'kem', 'bánh mì', 'xôi'],
                'places': ['Starbucks', 'Highlands', 'KFC', 'McDonald', 'Lotteria', 'Pizza Hut', 'Domino', 'quán cơm', 'quán bún', 'quán phở', 'nhà hàng', 'food court', 'canteen', 'cafeteria'],
                'amounts': [15000, 25000, 35000, 45000, 55000, 65000, 85000, 120000, 150000, 200000, 250000, 300000, 400000, 500000]
            },
            'di chuyển': {
                'keywords': ['xe', 'taxi', 'grab', 'be', 'gojek', 'uber', 'xe buýt', 'tàu', 'máy bay', 'xăng', 'dầu', 'bảo hiểm xe', 'sửa xe', 'rửa xe', 'gửi xe', 'vé số'],
                'places': ['Grab', 'Be', 'Gojek', 'Uber', 'Vinasun', 'Mai Linh', 'bến xe', 'ga tàu', 'sân bay', 'cửa hàng xăng', 'garage', 'trạm rửa xe'],
                'amounts': [20000, 35000, 50000, 75000, 100000, 150000, 200000, 300000, 500000, 800000, 1000000, 1500000]
            },
            'mua sắm': {
                'keywords': ['mua', 'shopping', 'quần áo', 'giày', 'túi', 'điện thoại', 'laptop', 'tivi', 'tủ lạnh', 'máy giặt', 'nồi cơm', 'chăn', 'gối', 'bàn', 'ghế'],
                'places': ['Vincom', 'Aeon', 'Big C', 'Lotte', 'Saigon Centre', 'Landmark', 'Diamond Plaza', 'Shopee', 'Lazada', 'Tiki', 'FPT Shop', 'Thế Giới Di Động'],
                'amounts': [50000, 100000, 200000, 300000, 500000, 800000, 1000000, 1500000, 2000000, 3000000, 5000000, 10000000]
            },
            'giải trí': {
                'keywords': ['xem phim', 'cinema', 'game', 'karaoke', 'bowling', 'billiards', 'massage', 'spa', 'gym', 'yoga', 'bơi lội', 'tennis', 'golf', 'du lịch', 'khách sạn'],
                'places': ['CGV', 'Lotte Cinema', 'Galaxy Cinema', 'BHD', 'karaoke Arirang', 'karaoke Adora', 'California Fitness', 'Elite Fitness', 'Yoga Plus'],
                'amounts': [100000, 150000, 200000, 300000, 500000, 800000, 1000000, 1500000, 2000000, 3000000, 5000000]
            },
            'sức khỏe': {
                'keywords': ['bệnh viện', 'phòng khám', 'bác sĩ', 'thuốc', 'vitamin', 'khám', 'xét nghiệm', 'chữa răng', 'nhổ răng', 'cấy ghép', 'phẫu thuật', 'tiêm chích'],
                'places': ['bệnh viện Chợ Rẫy', 'bệnh viện 115', 'Vinmec', 'FV Hospital', 'phòng khám đa khoa', 'nha khoa Kim', 'nhà thuốc Long Châu', 'Guardian', 'Pharmacity'],
                'amounts': [50000, 100000, 200000, 300000, 500000, 800000, 1000000, 2000000, 3000000, 5000000, 10000000]
            },
            'giáo dục': {
                'keywords': ['học', 'trường', 'khóa học', 'sách', 'vở', 'bút', 'đào tạo', 'kỹ năng', 'ngoại ngữ', 'tin học', 'lái xe', 'bằng cấp', 'chứng chỉ'],
                'places': ['trường đại học', 'trung tâm ngoại ngữ', 'trung tâm tin học', 'trung tâm lái xe', 'thư viện', 'nhà sách Fahasa', 'nhà sách Phương Nam'],
                'amounts': [100000, 200000, 500000, 1000000, 2000000, 3000000, 5000000, 8000000, 10000000, 15000000]
            },
            'đầu tư': {
                'keywords': ['chứng khoán', 'cổ phiếu', 'trái phiếu', 'vàng', 'bất động sản', 'đất', 'nhà', 'căn hộ', 'tiết kiệm', 'gửi ngân hàng', 'quỹ mở', 'bitcoin'],
                'places': ['Vietcombank', 'BIDV', 'Techcombank', 'MB Bank', 'VPBank', 'SSI', 'HSC', 'VPS', 'FPTS', 'công ty chứng khoán'],
                'amounts': [1000000, 2000000, 5000000, 10000000, 20000000, 50000000, 100000000, 200000000, 500000000]
            },
            'khác': {
                'keywords': ['điện', 'nước', 'internet', 'điện thoại', 'thuế', 'từ thiện', 'quà', 'sinh nhật', 'cưới', 'ma chay', 'lễ tết'],
                'places': ['EVN', 'VNPT', 'Viettel', 'Mobifone', 'Vinaphone', 'FPT Telecom', 'cơ quan thuế', 'ủy ban mặt trận', 'chùa', 'nhà thờ'],
                'amounts': [50000, 100000, 200000, 300000, 500000, 1000000, 2000000, 3000000, 5000000]
            }
        }
    
    def create_expanded_dataset(self, target_size=15000):
        """Create expanded Vietnamese dataset"""
        logger.info(f"Creating expanded dataset with {target_size:,} samples...")
        
        dataset = []
        samples_per_category = target_size // len(self.categories)
        
        for category, data in self.categories.items():
            logger.info(f"Generating {samples_per_category} samples for '{category}'...")
            
            for _ in range(samples_per_category):
                keyword = random.choice(data['keywords'])
                place = random.choice(data['places'])
                amount = random.choice(data['amounts'])
                
                # Generate natural Vietnamese transaction text
                templates = [
                    f"Mua {keyword} tại {place} {amount:,} VND",
                    f"Chi tiêu {keyword} ở {place} {amount:,} đồng",
                    f"Thanh toán {keyword} {place} {amount:,}đ",
                    f"{place} - {keyword} {amount:,} VND",
                    f"Tại {place} mua {keyword} giá {amount:,} VND"
                ]
                
                text = random.choice(templates)
                
                dataset.append({
                    'text': text,
                    'category': category,
                    'amount': amount,
                    'place': place,
                    'keyword': keyword,
                    'generated': True,
                    'timestamp': datetime.now().isoformat()
                })
        
        # Shuffle dataset
        random.shuffle(dataset)
        
        # Save to file
        output_file = "expanded_vietnamese_transactions.json"
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(dataset, f, ensure_ascii=False, indent=2)
        
        logger.info(f"Expanded dataset saved to {output_file}")
        logger.info(f"Total samples: {len(dataset):,}")
        
        return dataset
    
    def analyze_dataset_quality(self, dataset):
        """Analyze dataset quality"""
        logger.info("Analyzing dataset quality...")
        
        df = pd.DataFrame(dataset)
        
        # Category distribution
        category_counts = df['category'].value_counts()
        balance_score = (category_counts.min() / category_counts.max()) * 100
        
        # Text diversity
        unique_texts = df['text'].nunique()
        diversity_score = (unique_texts / len(df)) * 100
        
        # Overall quality score
        quality_score = (balance_score + diversity_score) / 2
        
        report = {
            'total_samples': len(df),
            'unique_texts': unique_texts,
            'categories': df['category'].nunique(),
            'balance_score': balance_score,
            'diversity_score': diversity_score,
            'quality_score': quality_score,
            'category_distribution': category_counts.to_dict()
        }
        
        logger.info(f"Quality Analysis:")
        logger.info(f"  Total samples: {report['total_samples']:,}")
        logger.info(f"  Unique texts: {report['unique_texts']:,}")
        logger.info(f"  Balance score: {balance_score:.1f}/100")
        logger.info(f"  Diversity score: {diversity_score:.1f}/100")
        logger.info(f"  Overall quality: {quality_score:.1f}/100")
        
        return report

def main():
    """Main function"""
    logger.info("Vietnamese Dataset Expander")
    logger.info("=" * 50)
    
    expander = VietnameseDatasetExpander()
    
    # Create expanded dataset
    dataset = expander.create_expanded_dataset(target_size=15000)
    
    # Analyze quality
    quality_report = expander.analyze_dataset_quality(dataset)
    
    logger.info("Dataset expansion completed!")

if __name__ == "__main__":
    main()