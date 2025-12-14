import json
import pandas as pd
import logging

# Setup logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def clean_dataset_categories():
    """Clean dataset to keep only Vietnamese categories"""
    logger.info("Cleaning dataset categories...")
    
    # Vietnamese categories we want to keep
    vietnamese_categories = {
        'ăn uống', 'di chuyển', 'mua sắm', 'giải trí', 
        'sức khỏe', 'giáo dục', 'đầu tư', 'khác'
    }
    
    # English to Vietnamese mapping
    category_mapping = {
        'food': 'ăn uống',
        'transport': 'di chuyển', 
        'shopping': 'mua sắm',
        'entertainment': 'giải trí',
        'healthcare': 'sức khỏe',
        'education': 'giáo dục',
        'income': 'đầu tư',
        'utilities': 'khác'
    }
    
    try:
        # Load expanded dataset
        with open('expanded_vietnamese_transactions.json', 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        logger.info(f"Original dataset: {len(data)} samples")
        
    except FileNotFoundError:
        logger.warning("Expanded dataset not found, creating sample data...")
        data = [
            {'text': 'Mua cà phê Starbucks', 'category': 'food'},
            {'text': 'Grab đi làm', 'category': 'transport'},
            {'text': 'Mua áo ở Zara', 'category': 'shopping'},
            {'text': 'Xem phim CGV', 'category': 'entertainment'},
            {'text': 'Tiền điện tháng 10', 'category': 'utilities'}
        ]
    
    # Clean and map categories
    cleaned_data = []
    stats_before = {}
    stats_after = {}
    
    for item in data:
        category = item['category']
        
        # Count original categories
        stats_before[category] = stats_before.get(category, 0) + 1
        
        # Map or filter categories
        if category in vietnamese_categories:
            cleaned_data.append(item)
            stats_after[category] = stats_after.get(category, 0) + 1
        elif category in category_mapping:
            # Map English to Vietnamese
            item['category'] = category_mapping[category]
            cleaned_data.append(item)
            vietnamese_cat = category_mapping[category]
            stats_after[vietnamese_cat] = stats_after.get(vietnamese_cat, 0) + 1
        else:
            # Skip unknown categories
            logger.warning(f"Skipping unknown category: {category}")
    
    logger.info(f"Cleaned dataset: {len(cleaned_data)} samples")
    logger.info("Category mapping:")
    for eng, viet in category_mapping.items():
        before_count = stats_before.get(eng, 0)
        if before_count > 0:
            logger.info(f"  {eng} ({before_count}) → {viet}")
    
    logger.info("Final Vietnamese categories:")
    for category, count in sorted(stats_after.items()):
        logger.info(f"  {category}: {count:,} samples")
    
    # Save cleaned dataset
    with open('cleaned_vietnamese_transactions.json', 'w', encoding='utf-8') as f:
        json.dump(cleaned_data, f, ensure_ascii=False, indent=2)
    
    logger.info("Cleaned dataset saved: cleaned_vietnamese_transactions.json")
    
    return cleaned_data

if __name__ == "__main__":
    cleaned_data = clean_dataset_categories()
    
    # Quick quality check
    df = pd.DataFrame(cleaned_data)
    logger.info(f"Final stats:")
    logger.info(f"  Total samples: {len(df):,}")
    logger.info(f"  Categories: {df['category'].nunique()}")
    logger.info(f"  Unique texts: {df['text'].nunique():,}")
    logger.info(f"  Uniqueness: {df['text'].nunique()/len(df):.1%}")