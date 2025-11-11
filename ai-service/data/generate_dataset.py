"""
Vietnamese Transaction Training Dataset
Real-world transaction descriptions for ML training
"""

import json
import random
from typing import List, Dict

# Vietnamese transaction patterns for training data
TRANSACTION_PATTERNS = {
    "food_beverage": [
        "Mua cà phê Highland Coffee {amount}k",
        "Ăn trưa quán phở {amount}",
        "Order đồ ăn Grab Food {amount}k",
        "Mua bánh mì {amount}k",
        "Nhậu với bạn {amount}k",
        "Ăn KFC {amount}k",
        "Lotte Mart mua đồ ăn {amount}k",
        "Cơm văn phòng {amount}k",
        "Trà sữa Gong Cha {amount}k",
        "Buffet lẩu {amount}k",
        "Pizza Hut {amount}k",
        "Bánh tráng nướng {amount}k"
    ],
    "transportation": [
        "Grab xe ôm {amount}k",
        "Taxi đi làm {amount}k", 
        "Xăng xe máy {amount}k",
        "Vé xe bus {amount}k",
        "Gửi xe {amount}k",
        "Sửa xe máy {amount}k",
        "Đổ xăng Shell {amount}k",
        "Grab Car {amount}k",
        "Bảo hiểm xe {amount}k",
        "Rửa xe {amount}k"
    ],
    "shopping": [
        "Mua quần áo {amount}k",
        "Shopee mua đồ {amount}k",
        "Lazada order {amount}k",
        "Mua giày {amount}k", 
        "Siêu thị Co.opMart {amount}k",
        "Mua sách {amount}k",
        "Điện thoại Samsung {amount}k",
        "Laptop Dell {amount}k",
        "Mỹ phẩm {amount}k",
        "Tiki mua sách {amount}k"
    ],
    "utilities": [
        "Tiền điện tháng {month} {amount}k",
        "Tiền nước {amount}k",
        "Internet FPT {amount}k",
        "Điện thoại Viettel {amount}k",
        "Tiền thuê nhà {amount}k",
        "Gas nấu ăn {amount}k",
        "Cáp truyền hình {amount}k"
    ],
    "healthcare": [
        "Khám bệnh {amount}k",
        "Mua thuốc {amount}k",
        "Nha khoa {amount}k",
        "Bảo hiểm y tế {amount}k",
        "Xét nghiệm {amount}k"
    ],
    "entertainment": [
        "Xem phim CGV {amount}k",
        "Karaoke {amount}k",
        "Game online {amount}k",
        "Netflix {amount}k",
        "Spotify {amount}k",
        "Gym {amount}k"
    ],
    "income": [
        "Lương tháng {month}",
        "Thưởng dự án", 
        "Làm thêm",
        "Bán hàng online",
        "Freelance",
        "Lãi ngân hàng"
    ]
}

# Vietnamese merchants and locations
MERCHANTS = [
    "Highland Coffee", "Starbucks", "The Coffee House", "Phúc Long",
    "Lotteria", "KFC", "McDonald's", "Jollibee", "Pizza Hut",
    "Vinmart", "Co.opMart", "Big C", "Lotte Mart", "Aeon Mall",
    "Grab", "Be", "Gojek", "Taxi Mai Linh", "Vinasun",
    "FPT Shop", "Thế Giới Di Động", "CellphoneS", "Điện Máy Xanh",
    "Shopee", "Lazada", "Tiki", "Sendo", "Fahasa"
]

# Amount ranges by category (in thousands VND)
AMOUNT_RANGES = {
    "food_beverage": (15, 500),
    "transportation": (10, 200), 
    "shopping": (50, 5000),
    "utilities": (100, 2000),
    "healthcare": (50, 1000),
    "entertainment": (50, 800),
    "income": (5000, 50000)
}

def generate_transaction_data(num_samples: int = 1000) -> List[Dict]:
    """Generate Vietnamese transaction training data"""
    
    transactions = []
    
    for _ in range(num_samples):
        # Random category
        category = random.choice(list(TRANSACTION_PATTERNS.keys()))
        
        # Random pattern from category
        pattern = random.choice(TRANSACTION_PATTERNS[category])
        
        # Generate amount
        min_amt, max_amt = AMOUNT_RANGES[category]
        amount = random.randint(min_amt, max_amt)
        
        # Generate description
        if category == "income":
            description = pattern
            transaction_type = "INCOME"
            amount = amount * 1000  # Convert to full VND
        else:
            description = pattern.format(
                amount=amount,
                month=random.randint(1, 12)
            )
            transaction_type = "EXPENSE" 
            amount = amount * 1000
        
        # Add merchant randomly
        if random.random() > 0.7:  # 30% chance
            merchant = random.choice(MERCHANTS)
            if merchant not in description:
                description = f"{description} - {merchant}"
        
        transactions.append({
            "description": description,
            "category": category,
            "amount": amount,
            "type": transaction_type,
            "confidence": 1.0  # Perfect labels for training
        })
    
    return transactions

def generate_financial_terms() -> Dict[str, List[str]]:
    """Vietnamese financial terminology for NLU"""
    
    return {
        "income_keywords": [
            "lương", "thưởng", "thu nhập", "tiền lương", "salary",
            "freelance", "làm thêm", "bán hàng", "kinh doanh",
            "lãi", "cổ tức", "đầu tư", "tiền thuê", "hoa hồng"
        ],
        "expense_keywords": [
            "mua", "chi", "trả", "thanh toán", "payment", "pay",
            "order", "grab", "taxi", "ăn", "uống", "shopping",
            "siêu thị", "tiền điện", "tiền nước", "thuê nhà"
        ],
        "categories": {
            "food_beverage": ["đồ ăn", "thức uống", "cà phê", "trà", "bia", "nhậu", "ăn uống", "food", "coffee"],
            "transportation": ["xe", "xăng", "grab", "taxi", "bus", "giao thông", "transport", "fuel"],
            "shopping": ["mua sắm", "quần áo", "giày", "túi", "shopping", "shopee", "lazada", "order"],
            "utilities": ["điện", "nước", "internet", "điện thoại", "thuê nhà", "utilities", "bill"],
            "healthcare": ["bệnh viện", "thuốc", "khám", "y tế", "nha khoa", "health", "medical"],
            "entertainment": ["phim", "karaoke", "game", "giải trí", "netflix", "gym", "entertainment"]
        },
        "amounts": {
            "units": ["k", "nghìn", "triệu", "tr", "đ", "vnd", "dong"],
            "numbers": ["một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín", "mười"]
        }
    }

def generate_advice_templates() -> List[Dict]:
    """Vietnamese financial advice templates for RAG"""
    
    return [
        {
            "scenario": "tiết kiệm hàng tháng",
            "advice": "Để tiết kiệm hiệu quả, bạn nên áp dụng quy tắc 50-30-20: 50% cho nhu cầu thiết yếu, 30% cho giải trí, 20% cho tiết kiệm.",
            "tips": [
                "Lập kế hoạch chi tiêu hàng tháng",
                "Ghi chép mọi khoản chi tiêu", 
                "Tránh mua sắm theo cảm xúc",
                "Tìm kiếm ưu đãi, khuyến mãi"
            ]
        },
        {
            "scenario": "quản lý nợ",
            "advice": "Ưu tiên trả nợ có lãi suất cao trước, sau đó mới đến nợ lãi suất thấp. Tránh vay nợ mới để trả nợ cũ.",
            "tips": [
                "Liệt kê tất cả các khoản nợ",
                "Tính toán khả năng trả nợ hàng tháng", 
                "Thương lượng giảm lãi suất với ngân hàng",
                "Tránh sử dụng thẻ tín dụng không cần thiết"
            ]
        },
        {
            "scenario": "đầu tư cơ bản",
            "advice": "Bắt đầu đầu tư với số tiền nhỏ, đa dạng hóa danh mục, và đầu tư dài hạn. Không đầu tư tiền cần dùng ngay.",
            "tips": [
                "Tìm hiểu kỹ trước khi đầu tư",
                "Bắt đầu với quỹ đầu tư ít rủi ro",
                "Đầu tư định kỳ hàng tháng", 
                "Không panicbán khi thị trường giảm"
            ]
        },
        {
            "scenario": "lập ngân sách",
            "advice": "Ngân sách hiệu quả giúp bạn kiểm soát chi tiêu và đạt mục tiêu tài chính. Hãy realistic và review thường xuyên.",
            "tips": [
                "Tính thu nhập ròng thực tế",
                "Chia thành các hạng mục cụ thể",
                "Để dành 10% cho trường hợp khẩn cấp",
                "Review và điều chỉnh hàng tháng"
            ]
        }
    ]

# Generate and save data
if __name__ == "__main__":
    # Generate training data
    print("🔄 Generating Vietnamese transaction data...")
    transactions = generate_transaction_data(2000)
    
    # Save transaction data  
    with open("transactions_training.json", "w", encoding="utf-8") as f:
        json.dump(transactions, f, ensure_ascii=False, indent=2)
    
    # Generate financial terms
    print("🔄 Generating financial terminology...")
    terms = generate_financial_terms()
    
    with open("financial_terms.json", "w", encoding="utf-8") as f:
        json.dump(terms, f, ensure_ascii=False, indent=2)
    
    # Generate advice templates
    print("🔄 Generating advice templates...")
    advice = generate_advice_templates()
    
    with open("advice_templates.json", "w", encoding="utf-8") as f:
        json.dump(advice, f, ensure_ascii=False, indent=2)
    
    print(f"✅ Generated {len(transactions)} transactions")
    print(f"✅ Generated {len(terms['categories'])} categories")  
    print(f"✅ Generated {len(advice)} advice templates")
    print("🎯 Data ready for PhoBERT training!")