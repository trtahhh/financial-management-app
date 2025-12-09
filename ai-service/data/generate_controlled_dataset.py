#!/usr/bin/env python3
"""
Controlled Vietnamese Financial Dataset Generator with 50GB Limit
Generates exactly 50GB of high-quality Vietnamese financial transaction data
"""

import json
import random
import uuid
from datetime import datetime, timedelta
from typing import Dict, List, Any
import os
import logging

# Setup logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class ControlledDatasetGenerator:
    def __init__(self, target_size_gb: float = 50.0):
        """
        Initialize generator with target size limit
        
        Args:
            target_size_gb: Target dataset size in GB (default: 50GB)
        """
        self.target_size_bytes = int(target_size_gb * 1024 * 1024 * 1024)  # Convert GB to bytes
        self.current_size_bytes = 0
        self.chunk_size = 25000  # Smaller chunks for better control
        self.chunk_counter = 0
        
        # Estimate bytes per transaction (based on previous generation)
        self.avg_bytes_per_transaction = 400  # Approximate
        
        # Calculate target number of transactions
        self.target_transactions = int(self.target_size_bytes / self.avg_bytes_per_transaction)
        
        logger.info(f"Target: {target_size_gb}GB = {self.target_size_bytes:,} bytes")
        logger.info(f"Estimated transactions needed: {self.target_transactions:,}")
        
        # Regional distribution percentages
        self.regional_distribution = {
            'northern': 0.30,    # 30% - Hà Nội, Hải Phòng, etc.
            'southern': 0.35,    # 35% - TP.HCM, Đồng Nai, etc.
            'central': 0.20,     # 20% - Đà Nẵng, Huế, etc.
            'chains': 0.15       # 15% - Chuỗi cửa hàng toàn quốc
        }
        
        # Vietnamese transaction patterns by region
        self.transaction_patterns = {
            'northern': {
                'food': [
                    'Quán phở {merchant} - Phở bò tái {amount}k',
                    'Chè {merchant} - Chè đậu xanh {amount}k',
                    'Bánh mì {merchant} - Bánh mì pate {amount}k',
                    'Quán bún {merchant} - Bún bò Huế {amount}k',
                    'Cơm {merchant} - Cơm rang dưa bò {amount}k'
                ],
                'transport': [
                    'Xe bus {amount}k - Tuyến {route}',
                    'Grab xe ôm từ {location1} đến {location2} {amount}k',
                    'Xăng A92 {amount}k - Cửa hàng {merchant}',
                    'Vé tàu {route} {amount}k',
                    'Taxi {merchant} {amount}k'
                ],
                'shopping': [
                    'Mua sắm tại {merchant} - {item} {amount}k',
                    'Siêu thị {merchant} - Thực phẩm {amount}k',
                    'Chợ {location} - Rau củ quả {amount}k',
                    'Cửa hàng {merchant} - {item} {amount}k'
                ],
                'entertainment': [
                    'Xem phim CGV {location} - {movie} {amount}k',
                    'Karaoke {merchant} {amount}k',
                    'Cafe {merchant} - Cappuccino {amount}k',
                    'Game center {merchant} {amount}k'
                ],
                'utilities': [
                    'Tiền điện tháng {month} {amount}k',
                    'Tiền nước {amount}k - EVN HANOI',
                    'Internet FPT {amount}k/tháng',
                    'Điện thoại Viettel {amount}k'
                ],
                'healthcare': [
                    'Khám bệnh {hospital} {amount}k',
                    'Mua thuốc {pharmacy} {amount}k',
                    'Tiêm vaccine {amount}k',
                    'Răng hàm mặt {amount}k'
                ],
                'education': [
                    'Học phí {school} {amount}k',
                    'Sách giáo khoa {amount}k',
                    'Khóa học tiếng Anh {amount}k',
                    'Học lái xe {amount}k'
                ],
                'income': [
                    'Lương tháng {month} {amount}k',
                    'Thưởng cuối năm {amount}k',
                    'Tiền làm thêm {amount}k',
                    'Bán hàng online {amount}k'
                ]
            },
            'southern': {
                'food': [
                    'Quán cơm {merchant} - Cơm tấm sườn {amount}k',
                    'Bánh xèo {merchant} {amount}k',
                    'Hủ tiếu {merchant} - Hủ tiếu nam vang {amount}k',
                    'Chè cung đình {merchant} {amount}k',
                    'Bánh cuốn {merchant} {amount}k'
                ],
                'transport': [
                    'Xe buýt {amount}k - Tuyến {route}',
                    'Grab bike từ {location1} đi {location2} {amount}k',
                    'Xăng RON95 {amount}k - {merchant}',
                    'Vé xe khách {route} {amount}k',
                    'Mai Linh taxi {amount}k'
                ],
                'shopping': [
                    'Mua hàng {merchant} - {item} {amount}k',
                    'Co.opmart {location} - Thực phẩm {amount}k',
                    'Chợ Bến Thành - {item} {amount}k',
                    'Saigon Co.op {amount}k'
                ],
                'entertainment': [
                    'Rạp phim Lotte {location} - {movie} {amount}k',
                    'Karaoke Arirang {amount}k',
                    'Coffee Bean {location} {amount}k',
                    'Bowling Superbowl {amount}k'
                ],
                'utilities': [
                    'Tiền điện EVNHCMC {amount}k',
                    'Tiền nước Sawaco {amount}k',
                    'Internet VNPT {amount}k',
                    'VinaPhone {amount}k'
                ],
                'healthcare': [
                    'Bệnh viện {hospital} {amount}k',
                    'Nhà thuốc {pharmacy} {amount}k',
                    'Khám răng {amount}k',
                    'Xét nghiệm {amount}k'
                ],
                'education': [
                    'Học phí trường {school} {amount}k',
                    'SGK lớp {grade} {amount}k',
                    'Tiếng Anh ILA {amount}k',
                    'Học bơi {amount}k'
                ],
                'income': [
                    'Lương công ty {amount}k',
                    'Tiền thưởng {amount}k',
                    'Part-time {amount}k',
                    'Bán online Shopee {amount}k'
                ]
            },
            'central': {
                'food': [
                    'Quán bún {merchant} - Bún bò Huế {amount}k',
                    'Mì Quảng {merchant} {amount}k',
                    'Cao lầu Hội An {amount}k',
                    'Chè Huế {merchant} {amount}k',
                    'Bánh khoái {amount}k'
                ],
                'transport': [
                    'Xe buýt Đà Nẵng {amount}k',
                    'Grab từ {location1} về {location2} {amount}k',
                    'Xăng Petrolimex {amount}k',
                    'Vé máy bay {route} {amount}k',
                    'Taxi Tiên Sa {amount}k'
                ],
                'shopping': [
                    'BigC {location} - Mua sắm {amount}k',
                    'Chợ Hàn - {item} {amount}k',
                    'Lotte Mart {amount}k',
                    'Cửa hàng {merchant} {amount}k'
                ],
                'entertainment': [
                    'CGV Vincom {location} - {movie} {amount}k',
                    'Karaoke Platinum {amount}k',
                    'Highlands Coffee {amount}k',
                    'Bar {merchant} {amount}k'
                ],
                'utilities': [
                    'Tiền điện PC Đà Nẵng {amount}k',
                    'Nước sạch Đà Nẵng {amount}k',
                    'Cáp quang FPT {amount}k',
                    'MobiFone {amount}k'
                ],
                'healthcare': [
                    'Bệnh viện C Đà Nẵng {amount}k',
                    'Phòng khám {doctor} {amount}k',
                    'Nhà thuốc Long Châu {amount}k',
                    'Spa {merchant} {amount}k'
                ],
                'education': [
                    'Đại học {university} {amount}k',
                    'Trung tâm ngoại ngữ {amount}k',
                    'Học lái xe B2 {amount}k',
                    'Khóa học kỹ năng {amount}k'
                ],
                'income': [
                    'Lương resort {amount}k',
                    'Tips tour guide {amount}k',
                    'Làm thêm khách sạn {amount}k',
                    'Bán đồ lưu niệm {amount}k'
                ]
            },
            'chains': {
                'food': [
                    'KFC {location} - Combo gà {amount}k',
                    'McDonald\'s {location} {amount}k',
                    'Pizza Hut {location} {amount}k',
                    'Lotteria {location} {amount}k',
                    'Jollibee {location} {amount}k'
                ],
                'transport': [
                    'Grab {location1} -> {location2} {amount}k',
                    'Be {location} {amount}k',
                    'Xăng Shell {amount}k',
                    'Vietjet Air {route} {amount}k',
                    'Vietnam Airlines {amount}k'
                ],
                'shopping': [
                    'Vinmart+ {location} {amount}k',
                    'Circle K {location} {amount}k',
                    'GS25 {location} {amount}k',
                    'B\'s Mart {location} {amount}k',
                    'Ministop {location} {amount}k'
                ],
                'entertainment': [
                    'CGV Cinemas {location} - {movie} {amount}k',
                    'Lotte Cinema {location} {amount}k',
                    'Starbucks {location} {amount}k',
                    'The Coffee House {location} {amount}k',
                    'Highlands Coffee {location} {amount}k'
                ],
                'utilities': [
                    'Viettel Pay {amount}k',
                    'MoMo nạp tiền {amount}k',
                    'ZaloPay thanh toán {amount}k',
                    'VNPay {amount}k'
                ],
                'healthcare': [
                    'Pharmacity {location} {amount}k',
                    'Long Châu {location} {amount}k',
                    'Medicare {location} {amount}k',
                    'Phòng khám Đa khoa {amount}k'
                ],
                'education': [
                    'ILA English {location} {amount}k',
                    'Apollo English {amount}k',
                    'ACET {amount}k',
                    'Apax Leaders {amount}k'
                ],
                'income': [
                    'Giao hàng Shopee {amount}k',
                    'Grab driver {amount}k',
                    'Freelance {amount}k',
                    'Affiliate marketing {amount}k'
                ]
            }
        }
        
        # Merchants by region
        self.merchants = {
            'northern': ['Thành', 'Mai', 'Hùng', 'Linh', 'Đức', 'Nga', 'Minh', 'Trang'],
            'southern': ['Tâm', 'Phước', 'Hương', 'Thảo', 'Khang', 'Loan', 'Tuấn', 'Hạnh'],
            'central': ['Dũng', 'Lan', 'Hải', 'Nhung', 'Bình', 'Thu', 'Nam', 'Vy'],
            'chains': ['Store', 'Shop', 'Branch', 'Outlet', 'Center']
        }
        
        # Locations by region
        self.locations = {
            'northern': ['Hà Nội', 'Hải Phòng', 'Nam Định', 'Thái Bình', 'Hưng Yên', 'Hà Nam', 'Ninh Bình'],
            'southern': ['TP.HCM', 'Biên Hòa', 'Vũng Tàu', 'Cần Thơ', 'Long Xuyên', 'Rạch Giá', 'Cà Mau'],
            'central': ['Đà Nẵng', 'Huế', 'Hội An', 'Quy Nhon', 'Nha Trang', 'Đà Lạt', 'Phan Thiết'],
            'chains': ['Quận 1', 'Quận 3', 'Quận 7', 'Hà Đông', 'Cầu Giấy', 'Thanh Xuân', 'Liên Chiểu']
        }
        
        # Amount ranges by category (in thousands VND)
        self.amount_ranges = {
            'food': (15, 200),
            'transport': (8, 500),
            'shopping': (50, 2000),
            'entertainment': (100, 800),
            'utilities': (200, 1500),
            'healthcare': (100, 3000),
            'education': (500, 5000),
            'income': (5000, 50000)
        }

    def get_current_size_mb(self):
        """Get current dataset size in MB"""
        return round(self.current_size_bytes / (1024 * 1024), 2)
    
    def get_current_size_gb(self):
        """Get current dataset size in GB"""
        return round(self.current_size_bytes / (1024 * 1024 * 1024), 3)
    
    def should_continue_generation(self):
        """Check if we should continue generating more data"""
        return self.current_size_bytes < self.target_size_bytes
    
    def calculate_remaining_transactions(self):
        """Calculate how many more transactions we can generate"""
        remaining_bytes = self.target_size_bytes - self.current_size_bytes
        return max(0, int(remaining_bytes / self.avg_bytes_per_transaction))

    def generate_transaction(self, region: str, category: str) -> Dict[str, Any]:
        """Generate a single Vietnamese transaction"""
        
        # Get random pattern for the category and region
        patterns = self.transaction_patterns[region][category]
        pattern = random.choice(patterns)
        
        # Get amount range for category
        min_amount, max_amount = self.amount_ranges[category]
        amount = random.randint(min_amount, max_amount)
        
        # Generate transaction data
        merchant = random.choice(self.merchants[region])
        location1 = random.choice(self.locations[region])
        location2 = random.choice(self.locations[region])
        
        # Format the description
        description = pattern.format(
            merchant=merchant,
            amount=amount,
            location=location1,
            location1=location1,
            location2=location2,
            route=f"{location1}-{location2}",
            month=random.randint(1, 12),
            item=random.choice(['Áo', 'Quần', 'Giày', 'Túi', 'Đồng hồ', 'Kính']),
            movie=random.choice(['Spider-Man', 'Avatar', 'Fast & Furious', 'Avengers']),
            hospital=random.choice(['Bệnh viện Việt Đức', 'BV Bạch Mai', 'BV Chợ Rẫy']),
            pharmacy=random.choice(['Pharmacity', 'Long Châu', 'Medicare']),
            school=random.choice(['THPT', 'Đại học', 'THCS']),
            grade=random.randint(6, 12),
            university=random.choice(['Đại học Bách Khoa', 'Đại học Kinh Tế', 'Đại học Sư Phạm']),
            doctor=random.choice(['BS Nguyễn Văn A', 'BS Trần Thị B', 'BS Lê Văn C'])
        )
        
        # Generate transaction
        transaction_date = datetime.now() - timedelta(days=random.randint(1, 365))
        
        transaction = {
            'id': str(uuid.uuid4()),
            'amount': amount * 1000,  # Convert to VND
            'description': description,
            'category': category,
            'region': region,
            'date': transaction_date.strftime('%Y-%m-%d'),
            'time': transaction_date.strftime('%H:%M:%S'),
            'type': 'EXPENSE' if category != 'income' else 'INCOME',
            'merchant': merchant,
            'location': location1,
            'payment_method': random.choice(['CASH', 'CARD', 'TRANSFER', 'EWALLET']),
            'currency': 'VND',
            'tags': [category, region, 'vietnamese'],
            'metadata': {
                'generated_by': 'controlled_dataset_generator',
                'language': 'vietnamese',
                'dialect': region,
                'timestamp': datetime.now().isoformat()
            }
        }
        
        return transaction

    def save_chunk(self, chunk: List[Dict], chunk_num: int) -> int:
        """Save a chunk of transactions and return file size in bytes"""
        filename = f"transactions_controlled_chunk_{chunk_num:03d}.json"
        filepath = os.path.join(os.getcwd(), filename)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(chunk, f, ensure_ascii=False, indent=1)
        
        # Get file size
        file_size = os.path.getsize(filepath)
        size_mb = round(file_size / (1024 * 1024), 1)
        
        logger.info(f"✅ Saved {filename}: {len(chunk):,} transactions ({size_mb}MB)")
        return file_size

    def generate_controlled_dataset(self):
        """Generate dataset with exact 50GB limit"""
        
        logger.info("🚀 Starting controlled dataset generation...")
        logger.info(f"Target: 50GB = {self.target_size_bytes:,} bytes")
        
        chunk = []
        total_transactions = 0
        
        while self.should_continue_generation():
            remaining_transactions = self.calculate_remaining_transactions()
            
            if remaining_transactions <= 0:
                break
                
            # Don't generate more than we need
            transactions_to_generate = min(self.chunk_size, remaining_transactions)
            
            # Generate transactions for this chunk
            for i in range(transactions_to_generate):
                # Select region based on distribution
                region = random.choices(
                    list(self.regional_distribution.keys()),
                    weights=list(self.regional_distribution.values())
                )[0]
                
                # Select random category
                categories = list(self.transaction_patterns[region].keys())
                category = random.choice(categories)
                
                # Generate transaction
                transaction = self.generate_transaction(region, category)
                chunk.append(transaction)
                
                # Save chunk when full
                if len(chunk) >= self.chunk_size:
                    file_size = self.save_chunk(chunk, self.chunk_counter)
                    self.current_size_bytes += file_size
                    self.chunk_counter += 1
                    total_transactions += len(chunk)
                    
                    # Update average bytes per transaction for better estimates
                    if total_transactions > 0:
                        self.avg_bytes_per_transaction = self.current_size_bytes / total_transactions
                    
                    # Log progress
                    current_gb = self.get_current_size_gb()
                    progress = (self.current_size_bytes / self.target_size_bytes) * 100
                    
                    logger.info(f"📊 Progress: {current_gb:.3f}GB / 50GB ({progress:.1f}%)")
                    logger.info(f"📈 Transactions: {total_transactions:,}")
                    logger.info(f"📏 Avg bytes/transaction: {self.avg_bytes_per_transaction:.0f}")
                    
                    chunk = []
                    
                    # Check if we've reached the limit
                    if not self.should_continue_generation():
                        break
        
        # Save remaining transactions in chunk
        if chunk:
            file_size = self.save_chunk(chunk, self.chunk_counter)
            self.current_size_bytes += file_size
            total_transactions += len(chunk)
        
        # Final statistics
        final_gb = self.get_current_size_gb()
        logger.info("\n" + "="*60)
        logger.info("🎉 CONTROLLED DATASET GENERATION COMPLETED!")
        logger.info(f"📊 Final Size: {final_gb:.3f}GB (Target: 50.0GB)")
        logger.info(f"📈 Total Transactions: {total_transactions:,}")
        logger.info(f"📁 Total Chunks: {self.chunk_counter + 1}")
        logger.info(f"📏 Avg bytes per transaction: {self.avg_bytes_per_transaction:.0f}")
        logger.info(f"🎯 Size Accuracy: {(final_gb/50)*100:.2f}%")
        logger.info("="*60)
        
        return {
            'total_transactions': total_transactions,
            'final_size_gb': final_gb,
            'total_chunks': self.chunk_counter + 1,
            'avg_bytes_per_transaction': self.avg_bytes_per_transaction
        }

if __name__ == "__main__":
    # Generate exactly 50GB dataset
    generator = ControlledDatasetGenerator(target_size_gb=50.0)
    
    try:
        results = generator.generate_controlled_dataset()
        print(f"\n✅ Successfully generated {results['final_size_gb']:.3f}GB dataset!")
        print(f"📈 {results['total_transactions']:,} transactions in {results['total_chunks']} chunks")
        
    except KeyboardInterrupt:
        print(f"\n⏹️ Generation stopped by user")
        print(f"📊 Current size: {generator.get_current_size_gb():.3f}GB")
    except Exception as e:
        print(f"\n❌ Error: {e}")