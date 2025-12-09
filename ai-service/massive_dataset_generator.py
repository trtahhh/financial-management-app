import json
import random
import logging
from datetime import datetime, timedelta
import pandas as pd
import numpy as np
from pathlib import Path

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class MassiveVietnameseDatasetGenerator:
    def __init__(self):
        self.categories = {
            'ăn uống': {
                'primary_keywords': ['ăn', 'uống', 'cà phê', 'trà', 'bún', 'phở', 'cơm', 'bánh', 'nước', 'bia', 'rượu'],
                'places': ['Starbucks', 'Highlands', 'KFC', 'McDonald', 'Lotteria', 'Pizza Hut', 'quán cơm', 'quán bún', 'quán phở', 'nhà hàng', 'food court', 'canteen', 'Jollibee', 'Domino', 'The Coffee House', 'Phúc Long', 'Cong Caphe', 'quán chè', 'quán kem', 'quán sinh tố'],
                'food_items': ['cà phê', 'trà sữa', 'bánh mì', 'cơm tấm', 'phở bò', 'bún bò', 'bánh cuốn', 'xôi', 'chè', 'kem', 'pizza', 'hamburger', 'gà rán', 'mì tôm', 'bánh tráng nướng', 'nem nướng', 'bánh xèo', 'cháo', 'súp', 'salad'],
                'amounts': list(range(10000, 500000, 5000)),
                'descriptors': ['ngon', 'tươi', 'nóng', 'lạnh', 'size M', 'size L', 'combo', 'set', 'đặc biệt', 'truyền thống']
            },
            'di chuyển': {
                'primary_keywords': ['xe', 'taxi', 'grab', 'be', 'gojek', 'xe buýt', 'tàu', 'máy bay', 'xăng', 'vé'],
                'places': ['Grab', 'Be', 'Gojek', 'Uber', 'Vinasun', 'Mai Linh', 'FastGo', 'bến xe', 'ga tàu', 'sân bay Tân Sơn Nhất', 'sân bay Nội Bài', 'cửa hàng xăng Petrolimex', 'cửa hàng xăng Shell', 'garage oto'],
                'transport_types': ['xe máy', 'ô tô', 'xe buýt', 'tàu hỏa', 'máy bay', 'xe đạp', 'motorbike taxi', 'xe ôm'],
                'amounts': list(range(15000, 2000000, 10000)),
                'routes': ['Quận 1 - Quận 7', 'Hà Nội - TP.HCM', 'Bình Thạnh - Tân Bình', 'sân bay về nhà', 'đi làm']
            },
            'mua sắm': {
                'primary_keywords': ['mua', 'shopping', 'quần áo', 'giày', 'túi', 'điện thoại', 'laptop', 'tivi'],
                'places': ['Vincom', 'Aeon', 'Big C', 'Lotte', 'Saigon Centre', 'Landmark', 'Diamond Plaza', 'Shopee', 'Lazada', 'Tiki', 'FPT Shop', 'Thế Giới Di Động', 'Điện Máy Xanh', 'Nguyen Kim', 'Co.opmart', 'Lotte Mart'],
                'items': ['áo thun', 'quần jean', 'giày thể thao', 'túi xách', 'iPhone', 'Samsung', 'laptop Dell', 'tivi Samsung', 'tủ lạnh', 'máy giặt', 'nồi cơm điện', 'quần áo', 'mỹ phẩm', 'sách', 'đồ chơi'],
                'amounts': list(range(50000, 50000000, 25000)),
                'brands': ['Nike', 'Adidas', 'Uniqlo', 'H&M', 'Zara', 'Apple', 'Samsung', 'Xiaomi', 'Sony']
            },
            'giải trí': {
                'primary_keywords': ['xem phim', 'cinema', 'game', 'karaoke', 'du lịch', 'khách sạn', 'massage'],
                'places': ['CGV', 'Lotte Cinema', 'Galaxy Cinema', 'BHD Star', 'Megastar', 'karaoke Arirang', 'karaoke Adora', 'Family Mart', 'Steam', 'PlayStation Store', 'resort', 'khách sạn'],
                'activities': ['xem phim', 'chơi game', 'karaoke', 'du lịch Đà Lạt', 'du lịch Phú Quốc', 'massage', 'spa', 'bowling', 'billiards', 'gym', 'yoga', 'bơi lội'],
                'amounts': list(range(80000, 5000000, 20000))
            },
            'sức khỏe': {
                'primary_keywords': ['bệnh viện', 'phòng khám', 'thuốc', 'vitamin', 'khám bệnh', 'xét nghiệm'],
                'places': ['bệnh viện Chợ Rẫy', 'bệnh viện 115', 'Vinmec', 'FV Hospital', 'Columbia Asia', 'phòng khám đa khoa', 'nha khoa Kim', 'nhà thuốc Long Châu', 'Guardian', 'Pharmacity', 'Medicare'],
                'services': ['khám tổng quát', 'xét nghiệm máu', 'chụp X-quang', 'siêu âm', 'nha khoa', 'mắt', 'tai mũi họng', 'da liễu', 'tim mạch'],
                'amounts': list(range(100000, 10000000, 50000))
            },
            'giáo dục': {
                'primary_keywords': ['học', 'trường', 'khóa học', 'sách', 'học phí', 'đào tạo'],
                'places': ['trường đại học', 'trung tâm ngoại ngữ ILA', 'trung tâm Anh văn', 'trung tâm tin học', 'trung tâm lái xe', 'thư viện', 'nhà sách Fahasa', 'nhà sách Phương Nam'],
                'courses': ['tiếng Anh', 'tiếng Nhật', 'tin học', 'lái xe', 'yoga', 'piano', 'guitar', 'vẽ', 'nấu ăn', 'IELTS', 'TOEIC'],
                'amounts': list(range(200000, 20000000, 100000))
            },
            'đầu tư': {
                'primary_keywords': ['chứng khoán', 'cổ phiếu', 'vàng', 'bất động sản', 'tiết kiệm', 'bitcoin'],
                'places': ['Vietcombank', 'BIDV', 'Techcombank', 'MB Bank', 'VPBank', 'SSI', 'HSC', 'VPS', 'FPTS', 'Binance', 'sàn vàng'],
                'investments': ['mua cổ phiếu', 'mua vàng SJC', 'gửi tiết kiệm', 'mua bitcoin', 'ETF', 'trái phiếu', 'quỹ mở', 'bảo hiểm nhân thọ'],
                'amounts': list(range(1000000, 1000000000, 500000))
            },
            'khác': {
                'primary_keywords': ['điện', 'nước', 'internet', 'điện thoại', 'thuế', 'từ thiện', 'quà'],
                'places': ['EVN', 'VNPT', 'Viettel', 'Mobifone', 'Vinaphone', 'FPT Telecom', 'cơ quan thuế', 'ủy ban mặt trận', 'chùa', 'nhà thờ'],
                'services': ['tiền điện', 'tiền nước', 'internet cáp quang', 'cước điện thoại', 'thuế thu nhập', 'từ thiện', 'sinh nhật', 'cưới hỏi', 'ma chay'],
                'amounts': list(range(50000, 5000000, 25000))
            }
        }
        
        self.templates = {
            'simple': [
                'Mua {item} {amount} VND',
                'Chi tiêu {item} {amount} VND', 
                'Thanh toán {item} {amount}đ',
                '{item} - {amount} VND',
                'Trả tiền {item} {amount} VND'
            ],
            'with_place': [
                'Mua {item} tại {place} {amount} VND',
                'Chi tiêu {item} tại {place} {amount} VND',
                'Thanh toán {item} ở {place} {amount}đ',
                '{place} - {item} {amount} VND',
                'Tại {place} mua {item} giá {amount} VND',
                '{item} {place} {amount} VND'
            ],
            'detailed': [
                'Mua {item} {descriptor} tại {place} với giá {amount} VND',
                'Chi phí {item} {descriptor} ở {place} là {amount} VND',
                'Thanh toán hóa đơn {item} tại {place} số tiền {amount}đ',
                'Giao dịch mua {item} tại {place} - {amount} VND',
                'Chi tiêu cho {item} {descriptor} ở {place}: {amount} VND'
            ],
            'natural': [
                'Hôm nay mua {item} ở {place} hết {amount} VND',
                'Vừa thanh toán {item} tại {place} {amount}đ', 
                'Chi {amount} VND cho {item} ở {place}',
                'Mới mua {item} {descriptor} giá {amount} VND',
                'Tốn {amount}đ để mua {item} tại {place}'
            ]
        }

    def generate_transaction_variations(self, category, count=1000):
        """Generate diverse transaction variations for a category"""
        cat_data = self.categories[category]
        transactions = []
        
        for i in range(count):
            # Choose template type based on probability
            template_type = np.random.choice(
                ['simple', 'with_place', 'detailed', 'natural'], 
                p=[0.2, 0.4, 0.25, 0.15]
            )
            
            template = random.choice(self.templates[template_type])
            
            # Select components
            if category == 'ăn uống':
                item = random.choice(cat_data['food_items'])
            elif category == 'di chuyển':
                item = random.choice(cat_data['transport_types'])
            elif category == 'mua sắm':
                item = random.choice(cat_data['items'])
            elif category == 'giải trí':
                item = random.choice(cat_data['activities'])
            elif category == 'sức khỏe':
                item = random.choice(cat_data['services'])
            elif category == 'giáo dục':
                item = random.choice(cat_data['courses'])
            elif category == 'đầu tư':
                item = random.choice(cat_data['investments'])
            else:  # khác
                item = random.choice(cat_data['services'])
            
            place = random.choice(cat_data['places'])
            amount = random.choice(cat_data['amounts'])
            
            # Add descriptor if template needs it
            descriptor = cat_data.get('descriptors', [''])[0] if 'descriptors' in cat_data else ''
            
            # Format transaction
            try:
                if '{descriptor}' in template:
                    if descriptor:
                        transaction = template.format(
                            item=item, place=place, amount=amount, descriptor=descriptor
                        )
                    else:
                        transaction = template.replace(' {descriptor}', '').format(
                            item=item, place=place, amount=amount
                        )
                else:
                    transaction = template.format(item=item, place=place, amount=amount)
                
                # Add some random variations
                if random.random() < 0.3:  # 30% chance to add time/date
                    time_phrases = ['hôm nay', 'hôm qua', 'sáng nay', 'chiều nay', 'tối qua']
                    transaction = f"{random.choice(time_phrases)} {transaction}"
                
                if random.random() < 0.2:  # 20% chance to add emotion
                    emotions = ['khá đắt', 'rẻ quá', 'giá tốt', 'khuyến mãi', 'sale off']
                    transaction = f"{transaction} - {random.choice(emotions)}"
                
                transactions.append({
                    'text': transaction,
                    'category': category,
                    'generated': True,
                    'variation_type': template_type,
                    'timestamp': datetime.now().isoformat()
                })
                
            except KeyError:
                # Skip if template formatting fails
                continue
        
        return transactions

    def create_massive_dataset(self, target_size=100000):
        """Create massive Vietnamese financial dataset"""
        logger.info(f"Creating MASSIVE Vietnamese dataset with {target_size:,} samples...")
        
        all_transactions = []
        samples_per_category = target_size // len(self.categories)
        
        for category in self.categories.keys():
            logger.info(f"Generating {samples_per_category:,} samples for '{category}'...")
            
            category_transactions = self.generate_transaction_variations(
                category, samples_per_category
            )
            
            all_transactions.extend(category_transactions)
            logger.info(f"Generated {len(category_transactions):,} samples for '{category}'")
        
        # Shuffle for better distribution
        random.shuffle(all_transactions)
        
        # Save massive dataset
        output_file = f"massive_vietnamese_dataset_{target_size//1000}k.json"
        
        logger.info(f"Saving massive dataset to {output_file}...")
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(all_transactions, f, ensure_ascii=False, indent=2)
        
        # Statistics
        stats = {}
        for item in all_transactions:
            cat = item['category']
            stats[cat] = stats.get(cat, 0) + 1
        
        file_size_mb = Path(output_file).stat().st_size / (1024 * 1024)
        
        logger.info(f"MASSIVE DATASET CREATED!")
        logger.info(f"Total samples: {len(all_transactions):,}")
        logger.info(f"File size: {file_size_mb:.2f} MB")
        logger.info(f"Category distribution:")
        for category, count in sorted(stats.items()):
            logger.info(f"  {category}: {count:,} samples")
        
        return all_transactions, output_file

if __name__ == "__main__":
    generator = MassiveVietnameseDatasetGenerator()
    
    # Create 100K dataset (có thể tăng lên 500K, 1M tùy nhu cầu)
    target_size = 200000  # 200K samples
    
    massive_data, output_file = generator.create_massive_dataset(target_size)
    
    logger.info("MASSIVE Vietnamese dataset generation completed!")
    logger.info(f"Dataset saved: {output_file}")
    logger.info("Ready for industrial-scale ML training!")