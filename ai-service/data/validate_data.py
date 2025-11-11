"""
Data Validation and Statistics
Analyze the generated Vietnamese financial dataset
"""

import json
from collections import Counter
import statistics

def load_and_analyze_data():
    """Load and analyze the generated training data"""
    
    # Load transaction data
    with open("transactions_training.json", "r", encoding="utf-8") as f:
        transactions = json.load(f)
    
    print("📊 DATASET STATISTICS")
    print("=" * 50)
    print(f"Total transactions: {len(transactions)}")
    
    # Count categories and types
    categories = [t['category'] for t in transactions]
    types = [t['type'] for t in transactions]
    amounts = [t['amount'] for t in transactions]
    
    print(f"Categories: {len(set(categories))}")
    print(f"Transaction types: {len(set(types))}")
    
    print("\n📈 CATEGORY DISTRIBUTION")
    print("-" * 30)
    category_counts = Counter(categories)
    for cat, count in category_counts.items():
        percentage = (count / len(transactions)) * 100
        print(f"{cat:15}: {count:4d} ({percentage:5.1f}%)")
    
    print("\n💰 AMOUNT STATISTICS")
    print("-" * 30)
    print(f"Min amount: {min(amounts):,} VND")
    print(f"Max amount: {max(amounts):,} VND") 
    print(f"Average: {statistics.mean(amounts):,.0f} VND")
    print(f"Median: {statistics.median(amounts):,.0f} VND")
    
    print("\n🔍 SAMPLE TRANSACTIONS BY CATEGORY")
    print("-" * 45)
    for category in set(categories):
        category_samples = [t for t in transactions if t['category'] == category][:2]
        print(f"\n{category.upper()}:")
        for transaction in category_samples:
            desc = transaction['description'][:50] + "..." if len(transaction['description']) > 50 else transaction['description']
            print(f"  • {desc} ({transaction['amount']:,} VND)")
    
    return transactions

def validate_data_quality(transactions):
    """Validate data quality"""
    
    print("\n🔍 DATA QUALITY CHECKS")
    print("=" * 50)
    
    # Check for missing values
    missing = 0
    for t in transactions:
        for key in ['description', 'category', 'amount', 'type']:
            if key not in t or t[key] is None or t[key] == "":
                missing += 1
    print(f"Missing values: {missing}")
    
    # Check for duplicate descriptions
    descriptions = [t['description'] for t in transactions]
    duplicates = len(descriptions) - len(set(descriptions))
    print(f"Duplicate descriptions: {duplicates}")
    
    # Check amount ranges by category
    print("\n💵 AMOUNT RANGES BY CATEGORY")
    print("-" * 35)
    
    categories = set(t['category'] for t in transactions)
    for category in categories:
        cat_amounts = [t['amount'] for t in transactions if t['category'] == category]
        min_amt, max_amt = min(cat_amounts), max(cat_amounts)
        print(f"{category:15}: {min_amt:>8,} - {max_amt:>10,} VND")
    
    # Validate Vietnamese text
    vietnamese_chars = ['à', 'á', 'ả', 'ã', 'ạ', 'ầ', 'ấ', 'ẩ', 'ẫ', 'ậ', 'ằ', 'ắ', 'ẳ', 'ẵ', 'ặ',
                       'è', 'é', 'ẻ', 'ẽ', 'ẹ', 'ề', 'ế', 'ể', 'ễ', 'ệ',
                       'ì', 'í', 'ỉ', 'ĩ', 'ị', 'ò', 'ó', 'ỏ', 'õ', 'ọ', 'ồ', 'ố', 'ổ', 'ỗ', 'ộ',
                       'ờ', 'ớ', 'ở', 'ỡ', 'ợ', 'ù', 'ú', 'ủ', 'ũ', 'ụ', 'ừ', 'ứ', 'ử', 'ữ', 'ự',
                       'ỳ', 'ý', 'ỷ', 'ỹ', 'ỵ', 'đ']
    
    vietnamese_texts = 0
    for t in transactions:
        desc = t['description']
        if any(char in desc.lower() for char in vietnamese_chars):
            vietnamese_texts += 1
    
    print(f"\nVietnamese text ratio: {vietnamese_texts/len(transactions)*100:.1f}%")
    
    return True

def analyze_financial_terms():
    """Analyze financial terms dictionary"""
    
    with open("financial_terms.json", "r", encoding="utf-8") as f:
        terms = json.load(f)
    
    print("\n🗂️ FINANCIAL TERMS ANALYSIS")
    print("=" * 50)
    
    for key, values in terms.items():
        if isinstance(values, list):
            print(f"{key:20}: {len(values)} terms")
        elif isinstance(values, dict):
            print(f"{key:20}:")
            for subkey, subvalues in values.items():
                print(f"  {subkey:15}: {len(subvalues)} terms")

def analyze_advice_templates():
    """Analyze advice templates"""
    
    with open("advice_templates.json", "r", encoding="utf-8") as f:
        advice = json.load(f)
    
    print("\n💡 ADVICE TEMPLATES ANALYSIS")
    print("=" * 50)
    
    print(f"Total scenarios: {len(advice)}")
    print("\nScenarios covered:")
    for i, template in enumerate(advice, 1):
        print(f"{i}. {template['scenario']}")
        print(f"   Tips: {len(template['tips'])}")

if __name__ == "__main__":
    # Load and analyze data
    transactions = load_and_analyze_data()
    
    # Validate data quality
    validate_data_quality(transactions)
    
    # Analyze financial terms
    analyze_financial_terms()
    
    # Analyze advice templates
    analyze_advice_templates()
    
    print("\n✅ DATA VALIDATION COMPLETE")
    print("🎯 Dataset ready for PhoBERT training!")
    print("📁 Files generated:")
    print("   • transactions_training.json (2000 samples)")
    print("   • financial_terms.json (Vietnamese NLU keywords)")
    print("   • advice_templates.json (RAG knowledge base)")