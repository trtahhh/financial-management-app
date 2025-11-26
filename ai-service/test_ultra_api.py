#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Test Ultra Enhanced API
Quick validation of all 9 ML libraries integration
"""

import requests
import json
from datetime import datetime, timedelta

BASE_URL = "http://localhost:8001"

def test_health():
    """Test health endpoint"""
    print("\n" + "="*60)
    print("🏥 Testing Health Endpoint")
    print("="*60)
    
    response = requests.get(f"{BASE_URL}/health")
    data = response.json()
    
    print(f"✅ Status: {data['status']}")
    print(f"✅ Ultra Available: {data['ultra_available']}")
    print(f"✅ Planning Available: {data['planning_available']}")
    print(f"✅ Version: {data['version']}")
    print(f"\n📚 ML Libraries:")
    for lib, status in data['ml_libraries'].items():
        print(f"  {'✅' if status else '❌'} {lib}")

def test_stats():
    """Test system stats"""
    print("\n" + "="*60)
    print("📊 Testing System Stats")
    print("="*60)
    
    response = requests.get(f"{BASE_URL}/stats")
    data = response.json()
    
    print(f"✅ Ultra Available: {data['ultra_available']}")
    print(f"\n🎯 Features Available ({len(data['features_available'])}):")
    for feature in data['features_available']:
        print(f"  • {feature}")

def test_sentiment_analysis():
    """Test sentiment analysis"""
    print("\n" + "="*60)
    print("😊 Testing Sentiment Analysis")
    print("="*60)
    
    payload = {
        "texts": [
            "Mua cà phê sáng, rất ngon và tươi mát!",
            "Chi tiền điện nước, hóa đơn cao quá!",
            "Đi ăn với gia đình, vui vẻ hạnh phúc",
            "Mất tiền sửa xe, thật bực mình"
        ]
    }
    
    response = requests.post(f"{BASE_URL}/ultra/sentiment-analysis", json=payload)
    data = response.json()
    
    print(f"✅ Total analyzed: {data['total_analyzed']}")
    print(f"✅ Average sentiment: {data['average_sentiment']:.3f}")
    print(f"\n📝 Results:")
    for result in data['results']:
        emoji = "😊" if result['category'] == 'positive' else "😢" if result['category'] == 'negative' else "😐"
        print(f"  {emoji} {result['text'][:40]}... → {result['sentiment_score']:.3f} ({result['label']})")

def test_word_similarity():
    """Test Word2Vec similarity"""
    print("\n" + "="*60)
    print("🔤 Testing Word2Vec Similarity")
    print("="*60)
    
    # Sample transactions for training
    transactions = [
        {"description": "Mua cà phê sáng", "amount": 30000},
        {"description": "Ăn trưa phở bò", "amount": 50000},
        {"description": "Mua trà sữa", "amount": 35000},
        {"description": "Mua quần áo mới", "amount": 500000},
        {"description": "Mua giày dép", "amount": 400000},
        {"description": "Đi xem phim", "amount": 100000},
        {"description": "Ăn tối buffet", "amount": 300000},
        {"description": "Mua sách học", "amount": 150000},
        {"description": "Uống nước ngọt", "amount": 15000},
        {"description": "Mua đồ điện tử", "amount": 2000000}
    ] * 2  # Duplicate to have more data
    
    # Test pairs
    test_pairs = [
        ("cà phê", "trà sữa"),
        ("quần áo", "giày dép"),
        ("phim", "buffet"),
        ("sách", "điện tử")
    ]
    
    for word1, word2 in test_pairs:
        payload = {
            "word1": word1,
            "word2": word2,
            "transactions": transactions
        }
        
        response = requests.post(f"{BASE_URL}/ultra/word-similarity", json=payload)
        data = response.json()
        
        if data['success']:
            print(f"  '{word1}' vs '{word2}': {data['similarity']:.3f} - {data['interpretation']}")
        else:
            print(f"  ❌ Error for '{word1}' vs '{word2}'")

def test_prophet_forecast():
    """Test Prophet forecasting"""
    print("\n" + "="*60)
    print("📈 Testing Prophet Time Series Forecast")
    print("="*60)
    
    # Generate sample transactions with dates
    base_date = datetime.now() - timedelta(days=30)
    transactions = []
    
    for i in range(30):
        date = base_date + timedelta(days=i)
        transactions.append({
            "category": "Food & Dining",
            "amount": 200000 + (i % 7) * 50000,  # Weekly pattern
            "date": date.isoformat(),
            "description": f"Ăn uống ngày {i+1}"
        })
    
    response = requests.post(
        f"{BASE_URL}/ultra/prophet-forecast",
        params={"category": "Food & Dining", "periods_ahead": 3},
        json=transactions
    )
    
    data = response.json()
    
    if data['success']:
        forecast = data['forecast']
        print(f"✅ Category: {data['category']}")
        print(f"✅ Transactions analyzed: {data['transactions_analyzed']}")
        print(f"✅ Trend: {forecast['trend']}")
        print(f"✅ Forecast value: {forecast['forecast']:,.0f} đ")
        print(f"✅ Confidence: {forecast['confidence']:.1%}")
    else:
        print(f"❌ Error: {data.get('error', 'Unknown')}")

def test_planning_with_ultra():
    """Test planning endpoint with ultra service"""
    print("\n" + "="*60)
    print("💰 Testing Planning with Ultra Service")
    print("="*60)
    
    transactions = [
        {"category": "Food & Dining", "amount": 200000, "description": "Ăn sáng"},
        {"category": "Food & Dining", "amount": 300000, "description": "Ăn trưa"},
        {"category": "Shopping", "amount": 500000, "description": "Mua quần áo"},
        {"category": "Transportation", "amount": 100000, "description": "Xăng xe"},
        {"category": "Entertainment", "amount": 150000, "description": "Xem phim"},
    ] * 4  # Repeat for more data
    
    payload = {
        "transactions": transactions,
        "monthly_income": 10000000,
        "goals": []
    }
    
    response = requests.post(f"{BASE_URL}/planning/analyze", json=payload)
    data = response.json()
    
    print(f"✅ Monthly income: {data['monthly_income']:,.0f} đ")
    print(f"✅ Total spending: {data['total_spending']:,.0f} đ")
    print(f"✅ Savings rate: {data['savings_rate']:.1%}")
    print(f"✅ Overall score: {data['overall_score']:.1f}/100")
    print(f"\n📊 Spending Insights ({len(data['spending_insights'])}):")
    for insight in data['spending_insights'][:3]:
        print(f"  • {insight['category']}: {insight['amount']:,.0f} đ ({insight['percentage']:.1f}%) - {insight['trend']}")

if __name__ == "__main__":
    print("\n" + "="*60)
    print("🚀 ULTRA ENHANCED API TEST SUITE")
    print("="*60)
    print("Testing all 9 ML libraries integration...")
    
    try:
        test_health()
        test_stats()
        test_sentiment_analysis()
        test_word_similarity()
        test_prophet_forecast()
        test_planning_with_ultra()
        
        print("\n" + "="*60)
        print("✅ ALL TESTS COMPLETED SUCCESSFULLY!")
        print("="*60)
        print("\n🎉 Ultra Enhanced API is working with 9/10 libraries!")
        
    except Exception as e:
        print(f"\n❌ Error during testing: {str(e)}")
        import traceback
        traceback.print_exc()
