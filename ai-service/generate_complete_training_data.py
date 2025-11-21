#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generate complete training dataset for all 14 categories
Extends existing training data with missing categories
"""

import json
import random
from datetime import datetime

# Training data for all 14 categories
TRAINING_DATA = {
    # INCOME CATEGORIES (1-4)
    "Lương": [
        "lương tháng 11", "salary november", "luong thang nay", "tien luong",
        "nhan luong", "luong chinh thuc", "monthly salary", "thu nhap chinh",
        "luong co ban", "luong net", "luong gross", "payroll",
        "luong thang 10", "luong thang 12", "luong part time", "luong full time",
        "luong 13", "luong thuong", "luong cung", "tien cong",
        "salary payment", "wage", "thu nhap on dinh", "luong hang thang"
    ],
    
    "Thu nhập khác": [
        "thuong cuoi nam", "bonus performance", "thuong tet", "hoa hong ban hang",
        "thuong kpi", "thuong du an", "commission", "tip khach hang",
        "thu nhap tu freelance", "thu nhap phu", "extra income", "side income",
        "thuong dac biet", "tien thuong", "bonus thang", "incentive",
        "referral bonus", "thuong gioi thieu", "tien lai phat sinh", "thu nhap ngoai",
        "thuong nang suat", "tien them gio", "overtime pay", "tien tang ca"
    ],
    
    "Đầu tư": [
        "mua co phieu vnindex", "dau tu vang", "bitcoin", "ethereum crypto",
        "quy dau tu", "trai phieu chinh phu", "bat dong san", "fpt stock",
        "gui tiet kiem ngan hang", "trading forex", "dau tu chung khoan",
        "mua vang sjc", "dau tu bds", "crypto trading", "stock market",
        "quy mo", "co phieu", "chung khoan", "bond", "fund",
        "dau tu tai chinh", "investment", "portfolio", "lai suat tiet kiem"
    ],
    
    "Kinh doanh": [
        "doanh thu ban hang", "thu nhap tu kinh doanh", "loi nhuan shop",
        "ban hang online", "doanh thu thang", "thu nhap tu shop", "business income",
        "revenue", "sales income", "profit", "thu tien khach",
        "doanh thu cua hang", "kinh doanh online", "ban do handmade",
        "thu nhap tu cho thue", "rental income", "passive income",
        "thu tu kinh doanh", "loi nhuan rong", "gross profit", "net income",
        "doanh thu thuan", "thu nhap doanh nghiep", "business revenue"
    ],
    
    # EXPENSE CATEGORIES (5-14)
    "Ăn uống": [
        "com tam", "pho bo", "bun cha", "mua pho", "an sang",
        "an trua", "an toi", "mua com", "buffet", "nha hang",
        "quan an", "mua do an", "breakfast", "lunch", "dinner",
        "food delivery", "grab food", "shopeefood", "goi do an",
        "cafe", "tra sua", "thuc an nhanh", "fast food", "kfc",
        "lotteria", "com ga", "bun bo", "banh mi", "che"
    ],
    
    "Giao thông": [
        "grab bike", "go taxi", "be car", "xang xe", "petrol",
        "ve xe bus", "ve tau", "ve may bay", "flight ticket",
        "sua xe", "thay nhot", "rua xe", "bao duong xe",
        "dau nhot", "lop xe", "parking", "tien gui xe",
        "toll fee", "phi duong bo", "ve xe lua", "train ticket",
        "ve tau cao toc", "taxi", "uber", "vehicle maintenance",
        "car service", "bike repair", "repair shop", "phu tung xe"
    ],
    
    "Giải trí": [
        "ve phim cgv", "karaoke", "game pubg", "netflix thang nay",
        "spotify premium", "ve xem show ca nhac", "bi da", "bowling",
        "concert", "rap", "cinema", "movie ticket", "youtube premium",
        "steam game", "playstation", "nintendo switch", "xbox",
        "khu vui choi", "dam sen park", "theme park", "zoo",
        "tham quan", "museum", "exhibition", "su kien am nhac",
        "nhac song", "festival", "party", "club", "bar"
    ],
    
    "Sức khỏe": [
        "kham benh vien bach mai", "mua thuoc cam cum", "nha khoa lam rang",
        "gym monthly fee", "yoga class", "xet nghiem mau", "mat xa bam huyet",
        "mua vitamin", "kham mat", "kham tai mui hong", "kham da khoa",
        "x quang", "sieu am", "test covid", "vaccine", "tiem phong",
        "mua thuoc", "pharmacy", "drug store", "medical check",
        "health insurance", "dental care", "eye check", "wellness",
        "massage", "spa health", "physiotherapy", "phuc hoi chuc nang"
    ],
    
    "Giáo dục": [
        "hoc phi tieng anh", "mua sach giao khoa", "khoa hoc online udemy",
        "coursera subscription", "hoc lap trinh", "ielts lop 7.0",
        "mua vo bai tap", "hoc vien ke toan", "sach tham khao",
        "hoc phi dai hoc", "tuition fee", "toeic course", "toefl",
        "hoc piano", "hoc guitar", "lop hoc them", "gia su",
        "bootcamp", "training course", "certification", "skill course",
        "edx course", "khan academy", "sach giao trinh", "textbook",
        "stationery", "van phong pham hoc tap", "do dung hoc tap"
    ],
    
    "Mua sắm": [
        "ao thun", "quan jeans", "giay sneaker", "tui xach",
        "mua giay dep", "mua quan ao", "shopping mall", "clothes",
        "thoi trang", "fashion", "do dien tu", "electronics",
        "dien thoai", "laptop", "tai nghe", "headphone",
        "do noi that", "furniture", "ban ghe", "tu lanh",
        "may giat", "appliance", "home decor", "trang tri nha",
        "mua sam online", "shopee", "lazada", "tiki", "sendo"
    ],
    
    "Tiện ích": [
        "tien dien thang 11", "tien nuoc", "internet fpt", "dien thoai viettel",
        "tien rac", "phi quan ly", "cap truyen hinh", "tien nha thang nay",
        "electricity bill", "water bill", "wifi bill", "phone bill",
        "rent", "tien thue nha", "gas bill", "tien gas",
        "management fee", "phi dich vu", "utilities", "cleaning service",
        "tien giup viec", "maid service", "bao tri may lanh",
        "sua chua nha", "maintenance", "home service", "plumber"
    ],
    
    "Vay nợ": [
        "tra no the tin dung", "vay ngan hang", "tra gop dien thoai",
        "credit card payment", "vay mua nha", "tra no ban be",
        "lai suat vay", "home credit", "fe credit", "vay tieu dung",
        "consumer loan", "personal loan", "installment", "tra gop",
        "paying debt", "loan repayment", "mortgage payment", "car loan",
        "vay mua xe", "student loan", "vay hoc sinh", "tien lai",
        "interest payment", "bank loan", "credit debt", "outstanding balance",
        "no the", "tra no", "debt payment", "repayment"
    ],
    
    "Quà tặng": [
        "qua tang sinh nhat", "tu thien mien trung", "tien mung cuoi",
        "donation charity", "ung ho thien tai", "qua tet", "tien li xi",
        "tro giup nguoi ngheo", "mua qua tang khach hang", "donation nha tho",
        "qua tang dip le", "mua hoa tang", "birthday gift", "wedding gift",
        "christmas present", "valentine gift", "anniversary gift",
        "tu thien", "charity", "donation", "giving", "helping",
        "ung ho", "ho tro", "support", "contribute", "quen",
        "tang qua", "present", "souvenir", "gift card", "voucher"
    ],
    
    "Khác": [
        "thanh toan", "chi phi", "payment", "expense", "misc",
        "miscellaneous", "khac", "other", "various", "different",
        "phi dich vu", "service fee", "transaction fee", "phi giao dich",
        "phi chuyen khoan", "transfer fee", "withdrawal fee", "rut tien",
        "phi duy tri", "annual fee", "phi thuong nien", "membership",
        "hoi vien", "subscription", "goi cuoc", "package fee",
        "admin fee", "phi hanh chinh", "processing fee", "handling fee"
    ]
}

def generate_training_dataset(samples_per_category=300):
    """Generate training dataset with specified number of samples per category"""
    
    dataset = []
    category_id_map = {
        "Lương": 1, "Thu nhập khác": 2, "Đầu tư": 3, "Kinh doanh": 4,
        "Ăn uống": 5, "Giao thông": 6, "Giải trí": 7, "Sức khỏe": 8,
        "Giáo dục": 9, "Mua sắm": 10, "Tiện ích": 11, "Vay nợ": 12,
        "Quà tặng": 13, "Khác": 14
    }
    
    type_map = {
        "Lương": "income", "Thu nhập khác": "income", "Đầu tư": "income", "Kinh doanh": "income",
        "Ăn uống": "expense", "Giao thông": "expense", "Giải trí": "expense", "Sức khỏe": "expense",
        "Giáo dục": "expense", "Mua sắm": "expense", "Tiện ích": "expense", "Vay nợ": "expense",
        "Quà tặng": "expense", "Khác": "expense"
    }
    
    for category_name, templates in TRAINING_DATA.items():
        category_id = category_id_map[category_name]
        trans_type = type_map[category_name]
        
        # Generate samples by repeating and varying templates
        for i in range(samples_per_category):
            # Pick random template
            template = random.choice(templates)
            
            # Add variations
            variations = [
                template,
                template.upper(),
                template.lower(),
                template.title(),
                f"chi {template}",
                f"thanh toan {template}",
                f"mua {template}",
                f"{template} thang nay",
                f"{template} hom nay"
            ]
            
            description = random.choice(variations)
            
            dataset.append({
                "description": description,
                "category": category_name,
                "category_id": category_id,
                "type": trans_type
            })
    
    # Shuffle dataset
    random.shuffle(dataset)
    
    return dataset

def main():
    print("=" * 70)
    print("GENERATE COMPLETE TRAINING DATA FOR 14 CATEGORIES")
    print("=" * 70)
    
    # Generate dataset
    print("\n📊 Generating training dataset...")
    dataset = generate_training_dataset(samples_per_category=300)
    
    print(f"\n✅ Generated {len(dataset):,} samples")
    print(f"   - Samples per category: ~300")
    print(f"   - Total categories: 14")
    
    # Statistics
    print("\n📈 Dataset Statistics:")
    category_counts = {}
    for item in dataset:
        cat = item['category']
        category_counts[cat] = category_counts.get(cat, 0) + 1
    
    for cat, count in sorted(category_counts.items(), key=lambda x: x[1], reverse=True):
        print(f"   {cat}: {count:,} samples")
    
    # Save dataset
    output_file = "vietnamese_transactions_14categories.json"
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(dataset, f, ensure_ascii=False, indent=2)
    
    print(f"\n💾 Dataset saved to: {output_file}")
    print(f"   File size: {len(json.dumps(dataset, ensure_ascii=False)) / 1024:.1f} KB")
    
    # Sample data
    print("\n📋 Sample data (first 5):")
    for i, sample in enumerate(dataset[:5], 1):
        print(f"   {i}. '{sample['description']}' -> {sample['category']} (ID: {sample['category_id']})")
    
    print("\n" + "=" * 70)
    print("✨ Training dataset generation complete!")
    print("=" * 70)

if __name__ == "__main__":
    main()
