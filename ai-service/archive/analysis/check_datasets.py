import json
import os

# Kiểm tra tất cả datasets
datasets = [
 'vietnamese_financial_quality_sample.json',
 'expanded_vietnamese_transactions.json', 
 'cleaned_vietnamese_transactions.json'
]

print(" DATASET ANALYSIS:")
print("=" * 60)

for dataset in datasets:
    if os.path.exists(dataset):
        try:
            with open(dataset, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            size_mb = os.path.getsize(dataset) / (1024 * 1024)
            categories = set(item['category'] for item in data)
            
            print(f"\n {dataset}")
            print(f" Size: {len(data):,} samples ({size_mb:.2f} MB)")
            print(f" Categories: {len(categories)} -> {sorted(categories)}")
            
            # Category distribution
            cat_counts = {}
            for item in data:
                cat = item['category']
                cat_counts[cat] = cat_counts.get(cat, 0) + 1
            
            print(f" Distribution:")
            for cat, count in sorted(cat_counts.items()):
                print(f"   {cat}: {count:,} samples")
            
        except Exception as e:
            print(f" Error reading {dataset}: {e}")
    else:
        print(f" {dataset} not found")

print("\n" + "=" * 60)