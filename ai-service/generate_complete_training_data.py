#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Improved training data generator for 14 categories.

Goals of refactor:
1. Reduce duplication & overfitting (remove brute-force upper/title variants)
2. Increase linguistic coverage via rule‑based template expansion
3. Provide diacritic + case normalized variants (Vietnamese no-accent forms)
4. Ensure uniqueness & balanced sampling
5. Allow configurable sample counts & train/test split

Usage:
  python generate_complete_training_data.py --samples 180 --split 0.85 --seed 42

Output:
  vietnamese_transactions_14categories.json  (full shuffled dataset)
  vietnamese_transactions_14categories_train.json
  vietnamese_transactions_14categories_test.json
"""

import json
import random
import argparse
from typing import Dict, List, Set, Tuple

# --- Core lexical resources -------------------------------------------------

# Base synonyms per category (concise; semantic coverage > raw count)
CATEGORY_SYNONYMS: Dict[str, List[str]] = {
    "Lương": ["lương", "mức lương", "salary", "thu nhập", "tiền công", "payroll", "wage"],
    "Thu nhập khác": ["thu nhập thêm", "extra income", "bonus", "hoa hồng", "incentive", "tip", "freelance", "thưởng"],
    "Đầu tư": ["đầu tư", "chứng khoán", "cổ phiếu", "crypto", "bitcoin", "quỹ", "lãi tiết kiệm", "vàng", "bond", "tín phiếu"],
    "Kinh doanh": ["kinh doanh", "doanh thu", "bán hàng", "revenue", "profit", "thu cho thuê", "passive income", "lợi nhuận"],
    "Ăn uống": ["phở", "bún", "cơm", "bánh mì", "ăn sáng", "ăn trưa", "ăn tối", "đồ ăn", "nhà hàng", "quán ăn", "cafe", "trà sữa", "fast food", "buffet", "delivery", "gọi đồ ăn"],
    "Giao thông": ["xăng xe", "grab", "taxi", "be", "gojek", "vé máy bay", "vé xe bus", "bảo dưỡng xe", "parking", "sửa xe", "rửa xe", "thay nhớt"],
    "Giải trí": ["xem phim", "cgv", "karaoke", "game", "netflix", "spotify", "concert", "party", "giải trí", "bi-a", "bowling"],
    "Sức khỏe": ["khám bệnh", "mua thuốc", "bảo hiểm", "gym", "yoga", "xét nghiệm", "dental", "massage", "spa", "nha khoa", "bệnh viện"],
    "Giáo dục": ["học phí", "mua sách", "khóa học", "training", "chứng chỉ", "gia sư", "stationery", "học tập", "tiếng anh", "ielts"],
    "Mua sắm": ["mua áo", "mua quần", "giày", "shopping", "điện thoại", "laptop", "đồ điện tử", "thời trang", "online", "shopee", "lazada"],
    "Tiện ích": ["tiền điện", "tiền nước", "internet", "wifi", "thuê nhà", "rent", "gas", "phí dịch vụ", "phí quản lý"],
    "Vay nợ": ["trả nợ", "vay ngân hàng", "credit", "loan", "trả góp", "interest", "mortgage", "debt", "thẻ tín dụng"],
    "Quà tặng": ["quà sinh nhật", "từ thiện", "donation", "lì xì", "gift", "charity", "ủng hộ", "tặng quà", "mừng cưới"],
    "Khác": ["chi phí khác", "service fee", "phí giao dịch", "withdrawal fee", "subscription", "membership", "admin fee", "misc"]
}

# Action / context verbs to enrich descriptions
VERBS = ["nhận", "trả", "mua", "đóng", "thanh toán", "chi", "đầu tư", "nộp", "sử dụng", "gia hạn"]
TIME_MODIFIERS = ["tháng này", "tháng trước", "hôm nay", "tuần này", "quý này"]
MONTHS = [str(m) for m in range(1, 13)]

# Mapping category metadata
CATEGORY_ID = {
    "Lương": 1, "Thu nhập khác": 2, "Đầu tư": 3, "Kinh doanh": 4,
    "Ăn uống": 5, "Giao thông": 6, "Giải trí": 7, "Sức khỏe": 8,
    "Giáo dục": 9, "Mua sắm": 10, "Tiện ích": 11, "Vay nợ": 12,
    "Quà tặng": 13, "Khác": 14
}

TYPE_MAP = {k: ("income" if v <= 4 else "expense") for k, v in CATEGORY_ID.items()}

# --- Normalization utilities -------------------------------------------------
_VIETNAMESE_DIACRITIC_MAP = {
    # Simple mapping; not exhaustive but covers common characters in synonyms
    "à":"a","á":"a","ả":"a","ã":"a","ạ":"a","ă":"a","ằ":"a","ắ":"a","ẳ":"a","ẵ":"a","ặ":"a","â":"a","ầ":"a","ấ":"a","ẩ":"a","ẫ":"a","ậ":"a",
    "è":"e","é":"e","ẻ":"e","ẽ":"e","ẹ":"e","ê":"e","ề":"e","ế":"e","ể":"e","ễ":"e","ệ":"e",
    "ì":"i","í":"i","ỉ":"i","ĩ":"i","ị":"i",
    "ò":"o","ó":"o","ỏ":"o","õ":"o","ọ":"o","ô":"o","ồ":"o","ố":"o","ổ":"o","ỗ":"o","ộ":"o","ơ":"o","ờ":"o","ớ":"o","ở":"o","ỡ":"o","ợ":"o",
    "ù":"u","ú":"u","ủ":"u","ũ":"u","ụ":"u","ư":"u","ừ":"u","ứ":"u","ử":"u","ữ":"u","ự":"u",
    "ỳ":"y","ý":"y","ỷ":"y","ỹ":"y","ỵ":"y",
    "đ":"d"
}

def strip_diacritics(text: str) -> str:
    return "".join(_VIETNAMESE_DIACRITIC_MAP.get(ch, ch) for ch in text.lower())

def unique(seq: List[str]) -> List[str]:
    seen: Set[str] = set()
    out = []
    for item in seq:
        key = item.strip()
        if key not in seen:
            seen.add(key)
            out.append(item)
    return out

# --- Variation generation ----------------------------------------------------

def generate_variations(base: str) -> List[str]:
    """Generate linguistic variations without naive upper/title duplication."""
    variants = {base.strip()}
    # Add diacritic-free version (for models trained on accent-insensitive corpora)
    variants.add(strip_diacritics(base))
    # Simple punctuation removal variant
    variants.add(base.replace("-", " ").replace(",", " "))
    return list(variants)

def build_descriptions(category: str, synonyms: List[str], samples_target: int, rng: random.Random) -> List[str]:
    pool: List[str] = []
    # Template expansions
    for syn in synonyms:
        syn_variants = generate_variations(syn)
        for var in syn_variants:
            # Base forms
            pool.append(var)
            # Verb + object
            for verb in rng.sample(VERBS, k=min(3, len(VERBS))):
                pool.append(f"{verb} {var}")
            # Time modifiers
            for tm in rng.sample(TIME_MODIFIERS, k=2):
                pool.append(f"{var} {tm}")
            # Month-specific (only for some financial contexts)
            if category in ("Lương", "Thu nhập khác", "Đầu tư", "Kinh doanh", "Tiện ích"):
                month = rng.choice(MONTHS)
                pool.append(f"{var} tháng {month}")
    # Deduplicate
    pool = unique(pool)
    # If pool smaller than requested, allow slight recombination
    if len(pool) < samples_target:
        extra_needed = samples_target - len(pool)
        for _ in range(extra_needed):
            base_choice = rng.choice(synonyms)
            verb = rng.choice(VERBS)
            tm = rng.choice(TIME_MODIFIERS)
            pool.append(f"{verb} {base_choice} {tm}")
        pool = unique(pool)
    # Sample down to target
    rng.shuffle(pool)
    return pool[:samples_target]

# --- Dataset assembly --------------------------------------------------------

def generate_training_dataset(samples_per_category: int = 180, seed: int = 42) -> List[Dict]:
    rng = random.Random(seed)
    dataset: List[Dict] = []
    for category, syns in CATEGORY_SYNONYMS.items():
        descriptions = build_descriptions(category, syns, samples_per_category, rng)
        for desc in descriptions:
            dataset.append({
                "description": desc,
                "category": category,
                "category_id": CATEGORY_ID[category],
                "type": TYPE_MAP[category]
            })
    rng.shuffle(dataset)
    return dataset

def train_test_split(dataset: List[Dict], split_ratio: float, seed: int) -> Tuple[List[Dict], List[Dict]]:
    rng = random.Random(seed)
    data = list(dataset)
    rng.shuffle(data)
    split_index = int(len(data) * split_ratio)
    return data[:split_index], data[split_index:]

def main():
    parser = argparse.ArgumentParser(description="Generate balanced, normalized training data")
    parser.add_argument("--samples", type=int, default=180, help="Samples per category (default 180)")
    parser.add_argument("--split", type=float, default=0.8, help="Train split ratio (default 0.8)")
    parser.add_argument("--seed", type=int, default=42, help="Random seed")
    args = parser.parse_args()

    print("=" * 78)
    print("GENERATING TRAINING DATA (Deduplicated / Template-Based)")
    print("=" * 78)
    print(f"-> Samples/category: {args.samples}\n-> Train split: {args.split}\n-> Seed: {args.seed}")

    dataset = generate_training_dataset(samples_per_category=args.samples, seed=args.seed)
    train, test = train_test_split(dataset, args.split, args.seed)

    print(f"\n✅ Total samples: {len(dataset):,}")
    print(f"   • Train: {len(train):,} | Test: {len(test):,}")

    # Category distribution
    print("\n📈 Distribution:")
    dist = {}
    for row in dataset:
        dist[row['category']] = dist.get(row['category'], 0) + 1
    for cat in sorted(dist.keys(), key=lambda c: CATEGORY_ID[c]):
        print(f"   {cat:<15}: {dist[cat]:4d}")

    # Persist
    with open("vietnamese_transactions_14categories.json", "w", encoding="utf-8") as f:
        json.dump(dataset, f, ensure_ascii=False, indent=2)
    with open("vietnamese_transactions_14categories_train.json", "w", encoding="utf-8") as f:
        json.dump(train, f, ensure_ascii=False, indent=2)
    with open("vietnamese_transactions_14categories_test.json", "w", encoding="utf-8") as f:
        json.dump(test, f, ensure_ascii=False, indent=2)

    print("\n💾 Saved:")
    print("   - vietnamese_transactions_14categories.json")
    print("   - vietnamese_transactions_14categories_train.json")
    print("   - vietnamese_transactions_14categories_test.json")

    print("\n📋 Sample (first 8):")
    for i, sample in enumerate(dataset[:8], 1):
        print(f"   {i}. {sample['description']} -> {sample['category']} ({sample['type']})")

    print("\n✨ Done. Consider further augmentation with contextual numeric amounts or POS tagging.")
    print("=" * 78)

if __name__ == "__main__":
    main()
