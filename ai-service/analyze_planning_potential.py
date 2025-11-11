#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Phân tích khả năng phát triển tính năng gợi ý kế hoạch tài chính và tiết kiệm
"""

import json
import os
from collections import Counter

def analyze_planning_potential():
    """Phân tích dataset để đánh giá khả năng phát triển tính năng planning"""
    
    dataset_file = 'massive_vietnamese_dataset_200k.json'
    
    if not os.path.exists(dataset_file):
        print("❌ Dataset không tìm thấy!")
        return
    
    print("🔍 PHÂN TÍCH DATASET CHO TÍNH NĂNG GỢI Ý FINANCIAL PLANNING")
    print("=" * 70)
    
    # Load dataset
    with open(dataset_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    print(f"📊 Tổng số mẫu: {len(data):,}")
    
    # Phân tích categories
    categories = [item['category'] for item in data]
    cat_counts = Counter(categories)
    
    print(f"\n📂 Các danh mục ({len(cat_counts)}):")
    for cat, count in cat_counts.most_common():
        percentage = (count / len(data)) * 100
        print(f"   {cat}: {count:,} mẫu ({percentage:.1f}%)")
    
    # Phân tích từ khóa planning
    planning_keywords = [
        'kế hoạch', 'tiết kiệm', 'đầu tư', 'lập kế hoạch', 
        'gợi ý', 'nên', 'khuyên', 'tư vấn', 'chiến lược',
        'mục tiêu', 'định hướng', 'phân bổ', 'quản lý chi tiêu'
    ]
    
    planning_samples = []
    savings_samples = []
    investment_samples = []
    budget_samples = []
    
    for item in data:
        text = item['text'].lower()
        category = item['category'].lower()
        
        # Phân loại theo tính năng
        if any(word in text for word in ['kế hoạch', 'lập kế hoạch', 'chiến lược', 'mục tiêu']) or category == 'đầu tư':
            planning_samples.append(item)
        
        if any(word in text for word in ['tiết kiệm', 'gửi tiết kiệm', 'tích lũy', 'dành dụm']):
            savings_samples.append(item)
            
        if 'đầu tư' in category or any(word in text for word in ['đầu tư', 'sinh lời', 'cổ phiếu', 'chứng khoán']):
            investment_samples.append(item)
            
        if any(word in text for word in ['ngân sách', 'phân bổ', 'chi tiêu', 'quản lý chi', 'budget']):
            budget_samples.append(item)
    
    print(f"\n💡 PHÂN TÍCH KHẢ NĂNG PHÁT TRIỂN TÍNH NĂNG:")
    print(f"   📋 Lập kế hoạch tài chính: {len(planning_samples):,} mẫu")
    print(f"   💰 Gợi ý tiết kiệm: {len(savings_samples):,} mẫu") 
    print(f"   📈 Tư vấn đầu tư: {len(investment_samples):,} mẫu")
    print(f"   📊 Quản lý ngân sách: {len(budget_samples):,} mẫu")
    
    # Tính tỷ lệ phủ sóng
    total_planning = len(set([item['text'] for item in planning_samples + savings_samples + investment_samples + budget_samples]))
    coverage = (total_planning / len(data)) * 100
    
    print(f"\n🎯 Tỷ lệ phủ sóng planning: {total_planning:,}/{len(data):,} ({coverage:.1f}%)")
    
    # Sample examples
    print(f"\n📝 VÍ DỤ CÁC MẪU PHÙ HỢP:")
    
    if planning_samples:
        print(f"\n🔵 Lập kế hoạch tài chính:")
        for i, sample in enumerate(planning_samples[:3]):
            print(f"   {i+1}. Text: {sample['text']}")
            print(f"       Category: {sample['category']}")
    
    if savings_samples:
        print(f"\n🟢 Gợi ý tiết kiệm:")
        for i, sample in enumerate(savings_samples[:3]):
            print(f"   {i+1}. Text: {sample['text']}")
            print(f"       Category: {sample['category']}")
    
    if investment_samples:
        print(f"\n🟡 Tư vấn đầu tư:")
        for i, sample in enumerate(investment_samples[:3]):
            print(f"   {i+1}. Text: {sample['text']}")
            print(f"       Category: {sample['category']}")
    
    # Đánh giá khả năng
    print(f"\n" + "=" * 70)
    print(f"🏆 ĐÁNH GIÁ KHẢ NĂNG PHÁT TRIỂN:")
    
    if total_planning >= 10000:
        rating = "XUẤT SẮC ⭐⭐⭐⭐⭐"
    elif total_planning >= 5000:
        rating = "RẤT TỐT ⭐⭐⭐⭐"
    elif total_planning >= 2000:
        rating = "TỐT ⭐⭐⭐"
    elif total_planning >= 1000:
        rating = "KHẢ THI ⭐⭐"
    else:
        rating = "CẦN BỔ SUNG ⭐"
    
    print(f"   Đánh giá: {rating}")
    print(f"   Lý do: Có {total_planning:,} mẫu liên quan đến financial planning")
    
    # Khuyến nghị
    print(f"\n💡 KHUYẾN NGHỊ:")
    if total_planning >= 5000:
        print(f"   ✅ Dataset đủ mạnh để phát triển tính năng gợi ý kế hoạch và tiết kiệm")
        print(f"   ✅ Có thể xây dựng các module:")
        print(f"      - Smart Budget Planner")
        print(f"      - Savings Goal Advisor") 
        print(f"      - Investment Strategy Recommender")
        print(f"      - Expense Optimization Suggestions")
    else:
        print(f"   ⚠️  Dataset cần bổ sung thêm dữ liệu về financial planning")
        print(f"   💡 Có thể bắt đầu với basic recommendations và mở rộng dần")

if __name__ == "__main__":
    analyze_planning_potential()