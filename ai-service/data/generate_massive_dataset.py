"""
MASSIVE Vietnamese Financial Dataset Generator
Generate 500K+ transactions with regional variations, comprehensive knowledge base
Target: ~50GB high-quality training data for maximum accuracy
"""

import json
import random
import itertools
from datetime import datetime, timedelta
import uuid
from typing import List, Dict, Set
import os

# MASSIVE Vietnamese transaction patterns with regional variations
TRANSACTION_PATTERNS = {
    "food_beverage": {
        "northern": [
            "Ăn phở {merchant} {amount}k", "Cà phê {merchant} {amount}k", "Bánh mì {merchant} {amount}k",
            "Chè {merchant} {amount}k", "Bún chả {amount}k", "Nem nướng {amount}k", 
            "Cơm tấm {merchant} {amount}k", "Trà đá vỉa hè {amount}k", "Bánh cuốn {amount}k",
            "Phở gà {merchant} {amount}k", "Bún riêu {amount}k", "Bánh giò {amount}k"
        ],
        "southern": [
            "Hủ tiếu {merchant} {amount}k", "Cà phê sữa đá {merchant} {amount}k", "Bánh tráng nướng {amount}k",
            "Cơm tấm sườn {merchant} {amount}k", "Chè đậu xanh {amount}k", "Bánh xèo {amount}k",
            "Gỏi cuốn {merchant} {amount}k", "Trà chanh {amount}k", "Bánh flan {amount}k",
            "Bún bò Huế {merchant} {amount}k", "Cao lầu {amount}k", "Mì Quảng {amount}k"
        ],
        "central": [
            "Bún bò Huế {merchant} {amount}k", "Cao lầu Hội An {amount}k", "Mì Quảng {amount}k",
            "Bánh khoái {amount}k", "Nem lụi {merchant} {amount}k", "Chè Huế {amount}k"
        ],
        "chains": [
            "Highland Coffee {amount}k", "Starbucks {amount}k", "The Coffee House {amount}k",
            "KFC {amount}k", "Lotteria {amount}k", "McDonald's {amount}k", "Pizza Hut {amount}k",
            "Domino's Pizza {amount}k", "Gong Cha {amount}k", "Tocotoco {amount}k"
        ]
    },
    "transportation": {
        "ride_sharing": [
            "Grab xe ôm {distance}km {amount}k", "Be xe ôm {amount}k", "Gojek {amount}k",
            "Grab Car {distance}km {amount}k", "Be Car {amount}k", "Taxi {company} {amount}k",
            "Grab Bike từ {location1} đến {location2} {amount}k"
        ],
        "fuel": [
            "Xăng Shell {amount}k", "Xăng Petrolimex {amount}k", "Xăng Caltex {amount}k",
            "Đổ xăng A95 {amount}k", "Xăng E5 {amount}k", "Diesel {amount}k"
        ],
        "public_transport": [
            "Vé xe bus {route} {amount}k", "Vé tàu điện ngầm {amount}k", "Xe buýt BRT {amount}k",
            "Vé tàu hỏa {from_city} - {to_city} {amount}k", "Máy bay {airline} {amount}k"
        ],
        "maintenance": [
            "Sửa xe máy {amount}k", "Thay nhớt {brand} {amount}k", "Bảo dưỡng xe {amount}k",
            "Rửa xe {amount}k", "Bơm lốp {amount}k", "Đăng kiểm xe {amount}k"
        ]
    },
    "shopping": {
        "fashion": [
            "Quần áo {brand} {amount}k", "Giày {brand} {amount}k", "Túi xách {amount}k",
            "Đồ lót {amount}k", "Mũ nón {amount}k", "Kính mát {brand} {amount}k",
            "Đồng hồ {brand} {amount}k", "Trang sức {amount}k"
        ],
        "electronics": [
            "iPhone {model} {amount}k", "Samsung Galaxy {model} {amount}k", "Laptop {brand} {amount}k",
            "Tai nghe {brand} {amount}k", "Sạc dự phòng {amount}k", "Ốp lưng điện thoại {amount}k",
            "Tivi {brand} {size}inch {amount}k", "Máy lạnh {brand} {amount}k"
        ],
        "ecommerce": [
            "Shopee {category} {amount}k", "Lazada {category} {amount}k", "Tiki sách {amount}k",
            "Sendo {category} {amount}k", "Fahasa sách {amount}k", "Being đồ gia dụng {amount}k"
        ],
        "supermarket": [
            "Co.opMart {items} {amount}k", "Big C {items} {amount}k", "Lotte Mart {amount}k",
            "Vinmart {items} {amount}k", "Aeon Mall {amount}k", "Saigon Co.op {amount}k"
        ]
    },
    "utilities": {
        "bills": [
            "Tiền điện EVN tháng {month} {amount}k", "Tiền nước Sawaco {amount}k",
            "Internet FPT {speed}Mbps {amount}k", "Internet Viettel {amount}k",
            "Điện thoại Viettel {amount}k", "Điện thoại Vinaphone {amount}k",
            "Truyền hình cáp SCTV {amount}k", "Gas Petrolimex {amount}k"
        ],
        "housing": [
            "Tiền thuê nhà Q{district} {amount}k", "Tiền nhà chung cư {amount}k",
            "Phí quản lý chung cư {amount}k", "Tiền gửi xe {amount}k",
            "Bảo vệ chung cư {amount}k", "Vệ sinh chung cư {amount}k"
        ]
    },
    "healthcare": {
        "medical": [
            "Khám bệnh BV {hospital} {amount}k", "Xét nghiệm {test_type} {amount}k",
            "Siêu âm {amount}k", "Chụp X-quang {amount}k", "MRI {amount}k",
            "Nha khoa {clinic} {amount}k", "Niềng răng {amount}k", "Cắt amidan {amount}k"
        ],
        "pharmacy": [
            "Mua thuốc {pharmacy} {amount}k", "Thuốc cảm cúm {amount}k",
            "Vitamin {brand} {amount}k", "Thuốc đau bụng {amount}k",
            "Kem bôi da {amount}k", "Nước súc miệng {amount}k"
        ],
        "insurance": [
            "BHYT tháng {month} {amount}k", "Bảo hiểm Prudential {amount}k",
            "BHXH đóng góp {amount}k", "Bảo hiểm AIA {amount}k"
        ]
    },
    "entertainment": {
        "media": [
            "Netflix tháng {month} {amount}k", "Spotify Premium {amount}k",
            "YouTube Premium {amount}k", "Apple Music {amount}k", "VTV Cab {amount}k"
        ],
        "gaming": [
            "Nạp Liên Quân {amount}k", "PUBG Mobile {amount}k", "FIFA Online {amount}k",
            "Game Steam {game} {amount}k", "Robux Roblox {amount}k"
        ],
        "activities": [
            "Xem phim CGV {movie} {amount}k", "Karaoke {venue} {amount}k",
            "Bowling {amount}k", "Billiards {amount}k", "Massage {amount}k",
            "Gym {club} {amount}k", "Yoga {amount}k", "Bơi lội {amount}k"
        ]
    },
    "education": {
        "formal": [
            "Học phí đại học {amount}k", "Học phí tiếng Anh {center} {amount}k",
            "Khóa học lập trình {amount}k", "Học lái xe {amount}k",
            "Sách giáo khoa {amount}k", "Đồ dùng học tập {amount}k"
        ],
        "online": [
            "Udemy khóa {course} {amount}k", "Coursera {amount}k",
            "Edumall {amount}k", "Unica {amount}k", "Kyna.vn {amount}k"
        ]
    },
    "income": {
        "salary": [
            "Lương tháng {month} công ty {company}",
            "Thưởng cuối năm", "Thưởng dự án {project}",
            "Lương overtime tháng {month}", "Phụ cấp đi lại"
        ],
        "freelance": [
            "Freelance thiết kế {project}", "Dịch thuật tài liệu",
            "Viết content {topic}", "Chụp ảnh sự kiện {event}",
            "Dạy kèm {subject}", "Lập trình {project}"
        ],
        "business": [
            "Bán hàng online {platform}", "Kinh doanh cafe",
            "Cho thuê phòng trọ", "Bán bánh handmade",
            "Dịch vụ sửa chữa", "Kinh doanh mỹ phẩm"
        ],
        "investment": [
            "Lãi tiền gửi ngân hàng", "Cổ tức {company}",
            "Lãi trái phiếu", "Bán cổ phiếu {stock}",
            "Thu nhập từ cho vay", "Lãi quỹ đầu tư"
        ]
    }
}

# MASSIVE merchant/brand database
MERCHANTS = {
    "food_chains": [
        "Highland Coffee", "Starbucks", "The Coffee House", "Phúc Long", "Trung Nguyên",
        "KFC", "McDonald's", "Lotteria", "Jollibee", "Burger King", "Domino's", "Pizza Hut",
        "Gong Cha", "Tocotoco", "Ding Tea", "Royaltea", "Phindi", "Koi Thé"
    ],
    "retail": [
        "Vinmart", "Co.opMart", "Big C", "Lotte Mart", "Aeon Mall", "Parkson",
        "FPT Shop", "Thế Giới Di Động", "CellphoneS", "Điện Máy Xanh", "Nguyễn Kim",
        "Shopee", "Lazada", "Tiki", "Sendo", "Fahasa", "Being"
    ],
    "transport": [
        "Grab", "Be", "Gojek", "Mai Linh", "Vinasun", "G7", "Uber"
    ],
    "banks": [
        "Vietcombank", "BIDV", "Agribank", "Techcombank", "ACB", "MB Bank", "VP Bank"
    ],
    "hospitals": [
        "Chợ Rẫy", "Bạch Mai", "Việt Đức", "Đại học Y", "Thu Cúc", "Vinmec", "FV"
    ]
}

# Vietnamese locations for realistic transactions  
LOCATIONS = {
    "ho_chi_minh": [
        "Q1", "Q2", "Q3", "Q4", "Q5", "Q7", "Bình Thạnh", "Tân Bình", "Thủ Đức",
        "Gò Vấp", "Phú Nhuận", "Tân Phú", "Bình Tân", "Quận 6", "Quận 8"
    ],
    "hanoi": [
        "Hoàn Kiếm", "Ba Đình", "Đống Đa", "Hai Bà Trưng", "Hoàng Mai", "Long Biên",
        "Tây Hồ", "Thanh Xuân", "Cầu Giấy", "Nam Từ Liêm", "Bắc Từ Liêm"
    ],
    "danang": [
        "Hải Châu", "Thanh Khê", "Sơn Trà", "Ngũ Hành Sơn", "Liên Chiểu", "Cẩm Lệ"
    ]
}

# Amount ranges optimized for Vietnamese economy
AMOUNT_RANGES = {
    "food_beverage": (5, 2000),     # 5k - 2M (street food to luxury dining)
    "transportation": (5, 5000),    # 5k - 5M (bus to flight) 
    "shopping": (10, 100000),       # 10k - 100M (small items to luxury)
    "utilities": (50, 10000),       # 50k - 10M (monthly bills)
    "healthcare": (20, 50000),      # 20k - 50M (medicine to surgery)
    "entertainment": (10, 5000),    # 10k - 5M (small games to luxury entertainment)
    "education": (100, 200000),     # 100k - 200M (books to university)
    "income": (3000, 2000000)       # 3M - 2B (minimum wage to executive salary)
}

class MassiveDatasetGenerator:
    def __init__(self, target_size_gb: float = 45):
        self.target_size_gb = target_size_gb
        self.target_transactions = 500000  # 500K base transactions
        self.generated_descriptions: Set[str] = set()
        
    def generate_description_variants(self, base_pattern: str, **kwargs) -> List[str]:
        """Generate multiple variants of a transaction description"""
        variants = []
        
        # Basic substitution with safe formatting
        try:
            desc = base_pattern.format(**kwargs)
        except KeyError as e:
            # Use only available parameters 
            available_params = {k: v for k, v in kwargs.items() if f'{{{k}}}' in base_pattern}
            desc = base_pattern.format(**available_params) if available_params else base_pattern
        
        variants.append(desc)
        
        # Add time variants
        time_prefixes = ["", "Sáng ", "Trưa ", "Chiều ", "Tối ", "Hôm nay ", "Hôm qua "]
        for prefix in time_prefixes:
            variants.append(f"{prefix}{desc.lower()}")
            
        # Add method variants
        payment_methods = ["", " - Momo", " - ZaloPay", " - ViettelPay", " - Thẻ", " - Tiền mặt", " - Chuyển khoản"]
        for method in payment_methods:
            variants.append(f"{desc}{method}")
            
        return variants
    
    def generate_regional_transactions(self, region: str, num_transactions: int) -> List[Dict]:
        """Generate transactions specific to a region"""
        transactions = []
        
        for category, patterns_dict in TRANSACTION_PATTERNS.items():
            if category == "income":
                continue  # Handle income separately
                
            # Get regional patterns
            if region in patterns_dict:
                regional_patterns = patterns_dict[region]
            else:
                regional_patterns = patterns_dict.get("chains", list(patterns_dict.values())[0])
            
            category_count = num_transactions // len(TRANSACTION_PATTERNS)
            
            for _ in range(category_count):
                pattern = random.choice(regional_patterns)
                
                # Generate parameters
                min_amt, max_amt = AMOUNT_RANGES.get(category, (10, 1000))
                amount = random.randint(min_amt, max_amt)
                
                # Comprehensive parameter set
                kwargs = {
                    'amount': amount,
                    'merchant': random.choice(MERCHANTS.get('food_chains', ['Highland'])),
                    'distance': random.randint(1, 50),
                    'location1': random.choice(LOCATIONS.get('ho_chi_minh', ['Q1'])),
                    'location2': random.choice(LOCATIONS.get('ho_chi_minh', ['Q2'])),
                    'month': random.randint(1, 12),
                    'district': random.randint(1, 12),
                    'company': random.choice(['Shell', 'Petrolimex', 'Caltex']),
                    'brand': random.choice(['Samsung', 'Apple', 'Nike', 'Adidas']),
                    'route': f"Tuyến {random.randint(1, 150)}",
                    'from_city': random.choice(['TP.HCM', 'Hà Nội', 'Đà Nẵng']),
                    'to_city': random.choice(['Nha Trang', 'Hội An', 'Vũng Tàu']),
                    'airline': random.choice(['Vietnam Airlines', 'Jetstar', 'VietJet']),
                    'category': random.choice(['điện tử', 'thời trang', 'gia dụng']),
                    'items': random.choice(['thực phẩm', 'đồ gia dụng', 'rau củ']),
                    'speed': random.choice([30, 50, 100, 200]),
                    'hospital': random.choice(MERCHANTS.get('hospitals', ['BV Chợ Rẫy'])),
                    'test_type': random.choice(['máu', 'nước tiểu', 'tổng quát']),
                    'pharmacy': random.choice(['Pharmacity', 'Long Châu', 'An Khang']),
                    'game': random.choice(['CS:GO', 'PUBG', 'FIFA', 'LOL']),
                    'movie': random.choice(['Avatar', 'Spider-Man', 'Fast & Furious']),
                    'venue': random.choice(['Arirang', 'Newway', 'Family']),
                    'club': random.choice(['California', 'Elite', 'Gym Plus']),
                    'center': random.choice(['ILA', 'Wall Street', 'Apollo']),
                    'course': random.choice(['Python', 'React', 'AI', 'Marketing']),
                    'model': random.choice(['S24', 'iPhone 15', 'Galaxy A55']),
                    'size': random.choice([43, 50, 55, 65]),
                    'clinic': random.choice(['Nha Khoa Paris', 'Răng Hàm Mặt'])
                }
                
                # Generate variants
                variants = self.generate_description_variants(pattern, **kwargs)
                
                for variant in variants:
                    if variant not in self.generated_descriptions:
                        self.generated_descriptions.add(variant)
                        
                        transactions.append({
                            "id": str(uuid.uuid4()),
                            "description": variant,
                            "category": category,
                            "amount": amount * 1000,  # Convert to VND
                            "type": "EXPENSE",
                            "confidence": 1.0,
                            "region": region,
                            "timestamp": self.random_timestamp(),
                            "metadata": {
                                "pattern_source": pattern,
                                "generation_method": "regional_variant"
                            }
                        })
        
        return transactions
    
    def random_timestamp(self) -> str:
        """Generate random timestamp within last 2 years"""
        start_date = datetime.now() - timedelta(days=730)
        random_days = random.randint(0, 730)
        random_date = start_date + timedelta(days=random_days)
        return random_date.isoformat()
    
    def generate_income_transactions(self, num_transactions: int) -> List[Dict]:
        """Generate comprehensive income transactions"""
        transactions = []
        
        income_patterns = TRANSACTION_PATTERNS["income"]
        
        for _ in range(num_transactions):
            category_type = random.choice(list(income_patterns.keys()))
            pattern = random.choice(income_patterns[category_type])
            
            # Income amounts (in thousands VND)
            min_amt, max_amt = AMOUNT_RANGES["income"]
            amount = random.randint(min_amt, max_amt)
            
            kwargs = {
                'month': random.randint(1, 12),
                'company': f"Công ty {random.choice(['ABC', 'XYZ', 'Tech', 'Solutions', 'Digital'])}",
                'project': f"Dự án {random.choice(['Web', 'Mobile', 'AI', 'Blockchain'])}",
                'platform': random.choice(['Shopee', 'Lazada', 'Facebook', 'Zalo']),
                'subject': random.choice(['Toán', 'Anh văn', 'Lập trình', 'Guitar']),
                'event': random.choice(['Cưới hỏi', 'Sinh nhật', 'Công ty', 'Hội nghị']),
                'stock': f"{random.choice(['VIC', 'VCB', 'GAS', 'MSN', 'HPG'])}",
                'topic': random.choice(['Tech', 'Du lịch', 'Ẩm thực', 'Thời trang'])
            }
            
            description = pattern.format(**kwargs)
            
            transactions.append({
                "id": str(uuid.uuid4()),
                "description": description,
                "category": "income",
                "amount": amount * 1000,
                "type": "INCOME",
                "confidence": 1.0,
                "region": "national",
                "timestamp": self.random_timestamp(),
                "metadata": {
                    "income_type": category_type,
                    "generation_method": "income_comprehensive"
                }
            })
        
        return transactions
    
    def generate_massive_dataset(self) -> List[Dict]:
        """Generate massive 500K+ transaction dataset"""
        print(f"🚀 Generating MASSIVE dataset targeting {self.target_size_gb}GB...")
        
        all_transactions = []
        
        # Generate regional transactions
        regions = ["northern", "southern", "central", "chains"]
        transactions_per_region = self.target_transactions // len(regions)
        
        for region in regions:
            print(f"🏢 Generating {transactions_per_region:,} transactions for {region} region...")
            regional_tx = self.generate_regional_transactions(region, transactions_per_region)
            all_transactions.extend(regional_tx)
            print(f"✅ Generated {len(regional_tx):,} {region} transactions")
        
        # Generate income transactions (20% of total)
        income_count = len(all_transactions) // 4
        print(f"💰 Generating {income_count:,} income transactions...")
        income_tx = self.generate_income_transactions(income_count)
        all_transactions.extend(income_tx)
        
        print(f"📊 Total generated: {len(all_transactions):,} transactions")
        print(f"📝 Unique descriptions: {len(self.generated_descriptions):,}")
        
        return all_transactions

def generate_comprehensive_financial_knowledge() -> Dict:
    """Generate comprehensive financial knowledge base"""
    
    return {
        "transaction_keywords": {
            "food_vietnamese": [
                "phở", "bún", "bánh mì", "cơm tấm", "hủ tiếu", "bánh xèo", 
                "gỏi cuốn", "nem", "chè", "bánh flan", "trà sữa", "cà phê"
            ],
            "shopping_vietnamese": [
                "mua", "shopping", "order", "đặt hàng", "thanh toán", "pay",
                "quần áo", "giày dép", "túi xách", "mỹ phẩm", "điện thoại"
            ],
            "transport_vietnamese": [
                "grab", "be", "taxi", "xe ôm", "bus", "xe buýt", "xăng", 
                "đổ xăng", "sửa xe", "rửa xe", "vé tàu", "máy bay"
            ],
            "bills_vietnamese": [
                "tiền điện", "tiền nước", "internet", "điện thoại", "thuê nhà",
                "phí", "cước", "hóa đơn", "bill", "EVN", "FPT", "Viettel"
            ]
        },
        "amount_patterns": {
            "currency_units": ["k", "nghìn", "triệu", "tr", "đ", "vnd", "dong", "vnđ"],
            "number_words": [
                "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín", "mười",
                "mười một", "mười hai", "hai mười", "ba mười", "năm mười", "trăm"
            ],
            "amount_indicators": ["giá", "tổng", "total", "cost", "chi phí", "thanh toán"]
        },
        "merchant_categories": {
            "food_merchants": MERCHANTS["food_chains"],
            "retail_merchants": MERCHANTS["retail"], 
            "transport_merchants": MERCHANTS["transport"],
            "bank_merchants": MERCHANTS["banks"]
        },
        "location_patterns": {
            "ho_chi_minh": LOCATIONS["ho_chi_minh"],
            "hanoi": LOCATIONS["hanoi"], 
            "danang": LOCATIONS["danang"]
        }
    }

def generate_comprehensive_advice_database() -> List[Dict]:
    """Generate comprehensive financial advice database"""
    
    scenarios = [
        {
            "scenario": "tiết kiệm cho người mới bắt đầu",
            "tags": ["tiết_kiệm", "người_mới", "cơ_bản"],
            "advice": "Bắt đầu với việc theo dõi chi tiêu hàng ngày và áp dụng quy tắc 50-30-20: 50% nhu cầu thiết yếu, 30% mong muốn, 20% tiết kiệm.",
            "detailed_tips": [
                "Ghi chép tất cả chi tiêu trong 30 ngày để hiểu rõ pattern",
                "Sử dụng app quản lý tài chính để theo dõi tự động",
                "Đặt mục tiêu tiết kiệm cụ thể: VD 2 triệu/tháng", 
                "Tự động chuyển tiền tiết kiệm ngay khi nhận lương",
                "Cắt giảm chi tiêu không cần thiết như cafe, đồ ăn nhanh"
            ],
            "examples": [
                "Thay vì mua cafe 50k/ngày, pha cafe tại nhà tiết kiệm 1.2tr/tháng",
                "Nấu ăn tại nhà thay vì order đồ ăn tiết kiệm 3-5tr/tháng"
            ]
        },
        {
            "scenario": "quản lý nợ thẻ tín dụng", 
            "tags": ["nợ", "thẻ_tín_dụng", "quản_lý_nợ"],
            "advice": "Ưu tiên trả nợ thẻ tín dụng có lãi suất cao nhất trước, đồng thời duy trì thanh toán tối thiểu cho các thẻ khác.",
            "detailed_tips": [
                "Liệt kê tất cả thẻ tín dụng với số dư và lãi suất",
                "Áp dụng phương pháp 'debt avalanche': trả nợ lãi suất cao trước",
                "Đàm phán với ngân hàng để giảm lãi suất hoặc gia hạn",
                "Cắt bỏ các thẻ không cần thiết để tránh cám dỗ chi tiêu",
                "Chuyển nợ sang thẻ có lãi suất thấp hơn nếu có thể"
            ],
            "examples": [
                "Thẻ A: 5tr - 25%/năm, Thẻ B: 3tr - 20%/năm → Trả thẻ A trước",
                "Chuyển nợ từ thẻ 25%/năm sang thẻ 15%/năm tiết kiệm 500k/năm"
            ]
        }
        # Add more comprehensive scenarios...
    ]
    
    return scenarios

if __name__ == "__main__":
    generator = MassiveDatasetGenerator(target_size_gb=45)
    
    print("🔥 MASSIVE VIETNAMESE FINANCIAL DATASET GENERATOR")
    print("=" * 60)
    print(f"Target: {generator.target_size_gb}GB of high-quality training data")
    print(f"Expected transactions: {generator.target_transactions:,}+")
    print()
    
    # Generate massive transaction dataset
    transactions = generator.generate_massive_dataset()
    
    # Save in chunks for better memory management
    chunk_size = 50000
    chunks = [transactions[i:i + chunk_size] for i in range(0, len(transactions), chunk_size)]
    
    print(f"💾 Saving {len(chunks)} chunks of {chunk_size:,} transactions each...")
    
    for i, chunk in enumerate(chunks):
        filename = f"transactions_massive_chunk_{i+1:03d}.json"
        with open(filename, "w", encoding="utf-8") as f:
            json.dump(chunk, f, ensure_ascii=False, indent=1)
        
        file_size = os.path.getsize(filename) / (1024 * 1024)  # MB
        print(f"✅ Saved {filename}: {len(chunk):,} transactions ({file_size:.1f}MB)")
    
    # Generate comprehensive knowledge base
    print("\n📚 Generating comprehensive financial knowledge...")
    knowledge = generate_comprehensive_financial_knowledge()
    
    with open("financial_knowledge_comprehensive.json", "w", encoding="utf-8") as f:
        json.dump(knowledge, f, ensure_ascii=False, indent=2)
    
    # Generate comprehensive advice database
    print("💡 Generating comprehensive advice database...")
    advice_db = generate_comprehensive_advice_database()
    
    with open("advice_database_comprehensive.json", "w", encoding="utf-8") as f:
        json.dump(advice_db, f, ensure_ascii=False, indent=2)
    
    # Calculate total dataset size
    total_size = sum(os.path.getsize(f"transactions_massive_chunk_{i+1:03d}.json") 
                    for i in range(len(chunks)))
    total_size_gb = total_size / (1024 ** 3)
    
    print("\n🎯 MASSIVE DATASET GENERATION COMPLETE!")
    print("=" * 60)
    print(f"📊 Total transactions: {len(transactions):,}")
    print(f"📝 Unique descriptions: {len(generator.generated_descriptions):,}")
    print(f"💾 Dataset size: {total_size_gb:.2f}GB")
    print(f"📁 Files generated: {len(chunks)} transaction chunks + knowledge bases")
    print(f"🏆 Ready for industrial-grade PhoBERT training!")