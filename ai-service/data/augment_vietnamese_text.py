"""
Data Augmentation for Vietnamese Financial Text
Advanced techniques to expand dataset diversity and quality
"""

import json
import random
import re
from typing import List, Dict, Set
from dataclasses import dataclass
import itertools

@dataclass
class AugmentationRule:
    """Rule for text augmentation"""
    name: str
    pattern: str
    replacements: List[str]
    probability: float = 0.3

class VietnameseTextAugmenter:
    """Advanced Vietnamese text augmentation for financial transactions"""
    
    def __init__(self):
        self.augmentation_rules = self._load_augmentation_rules()
        self.synonym_dict = self._load_vietnamese_synonyms()
        self.slang_dict = self._load_vietnamese_slang()
        
    def _load_augmentation_rules(self) -> List[AugmentationRule]:
        """Load augmentation rules for Vietnamese financial text"""
        return [
            AugmentationRule(
                "payment_methods",
                r"\b(mua|thanh toán|trả)\b",
                ["mua", "thanh toán", "trả", "chi", "đóng", "nộp"],
                0.4
            ),
            AugmentationRule(
                "time_expressions", 
                r"\b(hôm nay|hôm qua|sáng|trưa|chiều|tối)\b",
                ["hôm nay", "hôm qua", "sáng nay", "trưa nay", "chiều nay", "tối nay", ""],
                0.3
            ),
            AugmentationRule(
                "location_modifiers",
                r"\bở\s+",
                ["ở ", "tại ", "bên ", ""],
                0.2
            ),
            AugmentationRule(
                "amount_formats",
                r"(\d+)k\b",
                lambda m: random.choice([f"{m.group(1)}k", f"{m.group(1)} nghìn", f"{m.group(1)}.000đ"]),
                0.4
            )
        ]
    
    def _load_vietnamese_synonyms(self) -> Dict[str, List[str]]:
        """Load Vietnamese synonym dictionary for financial terms"""
        return {
            "mua": ["mua", "sắm", "tậu", "order", "đặt"],
            "ăn": ["ăn", "dùng bữa", "ăn uống", "nhậu", "buffet"],
            "uống": ["uống", "nhâm nhi", "thưởng thức", "order"],
            "đi": ["đi", "di chuyển", "bay", "lái xe", "xuống"],
            "cà phê": ["cà phê", "coffee", "cafe", "cf", "caphe"],
            "tiền": ["tiền", "cash", "money", "đồng", "bạc"],
            "với": ["với", "cùng", "và", "&"],
            "cho": ["cho", "để", "dành cho", "phục vụ"],
            "của": ["của", "thuộc", "do"],
            "tại": ["tại", "ở", "bên", "trong"],
            "từ": ["từ", "xuất phát từ", "bắt đầu từ"],
            "đến": ["đến", "tới", "về", "đi tới"]
        }
    
    def _load_vietnamese_slang(self) -> Dict[str, List[str]]:
        """Load Vietnamese slang and informal expressions"""
        return {
            "mua": ["tậu", "sắm", "múc", "chốt đơn"],
            "ăn": ["xơi", "húp", "cày", "quẩy"],
            "uống": ["húp", "nhâm nhi", "poppy"], 
            "đi": ["đi lăn", "phăng", "bay"],
            "tiền": ["xu", "đồng", "tiền bạc", "money"],
            "đắt": ["chát", "cháy túi", "mắc", "giá cắt cổ"],
            "rẻ": ["bèo", "hời", "giá bùi"],
            "ngon": ["ngon tuyệt", "xuất sắc", "tuyệt vời", "5 sao"],
            "xấu": ["dở", "tệ", "không ổn", "fail"]
        }
    
    def augment_synonym_replacement(self, text: str) -> List[str]:
        """Replace words with Vietnamese synonyms"""
        variants = []
        words = text.split()
        
        # Generate multiple combinations
        for _ in range(3):  # Generate 3 variants
            new_words = []
            for word in words:
                word_clean = re.sub(r'[^\w]', '', word.lower())
                if word_clean in self.synonym_dict and random.random() < 0.3:
                    replacement = random.choice(self.synonym_dict[word_clean])
                    new_words.append(replacement)
                else:
                    new_words.append(word)
            
            variant = ' '.join(new_words)
            if variant != text:
                variants.append(variant)
        
        return variants
    
    def augment_slang_injection(self, text: str) -> List[str]:
        """Inject Vietnamese slang and informal expressions"""
        variants = []
        
        for base_word, slang_options in self.slang_dict.items():
            if base_word in text.lower():
                for slang in slang_options:
                    if random.random() < 0.2:  # 20% chance to use slang
                        variant = text.replace(base_word, slang)
                        variants.append(variant)
        
        return variants
    
    def augment_regional_variations(self, text: str) -> List[str]:
        """Add regional Vietnamese variations"""
        variants = []
        
        regional_replacements = {
            "northern": {
                "cà phê": "café", "bánh mì": "bánh mỳ", 
                "xe ôm": "xe ôm", "gọi": "gọi điện"
            },
            "southern": {
                "cà phê": "cafe", "bánh mì": "bánh mì", 
                "xe ôm": "xe om", "gọi": "call"
            },
            "central": {
                "cà phê": "cà phê", "bánh mì": "bánh mì Hội An"
            }
        }
        
        for region, replacements in regional_replacements.items():
            variant_text = text
            for original, replacement in replacements.items():
                if original in variant_text.lower():
                    variant_text = variant_text.replace(original, replacement)
            
            if variant_text != text:
                variants.append(variant_text)
        
        return variants
    
    def augment_typos_and_misspellings(self, text: str) -> List[str]:
        """Add realistic Vietnamese typing errors"""
        variants = []
        
        # Common Vietnamese typing mistakes
        typo_mappings = {
            'ă': ['a', 'â'], 'â': ['a', 'ă'], 'ê': ['e'], 'ô': ['o', 'ơ'],
            'ơ': ['o', 'ô'], 'ư': ['u'], 'đ': ['d'], 'q': ['qu'], 'gi': ['g'],
            'ph': ['f'], 'th': ['t'], 'kh': ['k'], 'gh': ['g'], 'ng': ['n']
        }
        
        # Generate typo variants (low probability)
        if random.random() < 0.1:  # 10% chance for typos
            words = text.split()
            typo_text = []
            
            for word in words:
                if random.random() < 0.2:  # 20% chance to modify a word
                    for original, replacements in typo_mappings.items():
                        if original in word:
                            replacement = random.choice(replacements)
                            word = word.replace(original, replacement, 1)
                            break
                typo_text.append(word)
            
            variant = ' '.join(typo_text)
            if variant != text:
                variants.append(variant)
        
        return variants
    
    def augment_formatting_variations(self, text: str) -> List[str]:
        """Add formatting and punctuation variations"""
        variants = []
        
        # Case variations
        variants.extend([
            text.lower(),
            text.upper(), 
            text.title(),
            text.capitalize()
        ])
        
        # Punctuation variations
        punctuation_variants = [
            text + ".",
            text + "!",
            text.replace(" ", "_"),
            text.replace(" ", "-"),
            f"💰 {text}",
            f"{text} 🛒",
            f"✅ {text}"
        ]
        
        variants.extend(punctuation_variants)
        
        return [v for v in variants if v != text]
    
    def augment_transaction(self, transaction: Dict) -> List[Dict]:
        """Augment a single transaction with multiple techniques"""
        original_desc = transaction['description']
        augmented_transactions = []
        
        # Apply all augmentation techniques
        all_variants = []
        all_variants.extend(self.augment_synonym_replacement(original_desc))
        all_variants.extend(self.augment_slang_injection(original_desc))
        all_variants.extend(self.augment_regional_variations(original_desc))
        all_variants.extend(self.augment_typos_and_misspellings(original_desc))
        all_variants.extend(self.augment_formatting_variations(original_desc))
        
        # Remove duplicates and original
        unique_variants = list(set(all_variants))
        unique_variants = [v for v in unique_variants if v != original_desc and len(v.strip()) > 0]
        
        # Create augmented transactions
        for variant in unique_variants[:5]:  # Limit to 5 variants per transaction
            augmented_tx = transaction.copy()
            augmented_tx['description'] = variant
            augmented_tx['metadata'] = transaction.get('metadata', {}).copy()
            augmented_tx['metadata']['augmentation_method'] = 'vietnamese_advanced'
            augmented_tx['metadata']['original_description'] = original_desc
            augmented_transactions.append(augmented_tx)
        
        return augmented_transactions

def augment_massive_dataset(input_files: List[str], output_prefix: str = "augmented"):
    """Augment massive dataset with Vietnamese text variations"""
    
    augmenter = VietnameseTextAugmenter()
    
    for file_path in input_files:
        print(f"🔄 Augmenting {file_path}...")
        
        # Load original data
        with open(file_path, 'r', encoding='utf-8') as f:
            original_transactions = json.load(f)
        
        # Augment transactions
        augmented_data = []
        for i, transaction in enumerate(original_transactions):
            if i % 1000 == 0:
                print(f"   Processed {i:,} transactions...")
            
            # Add original transaction
            augmented_data.append(transaction)
            
            # Add augmented variants (with sampling to control size)
            if random.random() < 0.3:  # 30% chance to augment
                variants = augmenter.augment_transaction(transaction)
                augmented_data.extend(variants)
        
        # Save augmented data
        output_file = f"{output_prefix}_{file_path}"
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(augmented_data, f, ensure_ascii=False, indent=1)
        
        expansion_ratio = len(augmented_data) / len(original_transactions)
        print(f"✅ {output_file}: {len(augmented_data):,} transactions ({expansion_ratio:.1f}x expansion)")

if __name__ == "__main__":
    print("🚀 VIETNAMESE TEXT AUGMENTATION SYSTEM")
    print("=" * 50)
    
    # Example usage - will be applied after massive dataset generation completes
    sample_text = "Mua cà phé Highland Coffee 45k"
    augmenter = VietnameseTextAugmenter()
    
    print(f"Original: {sample_text}")
    print("\n📝 Augmentation Examples:")
    
    variants = []
    variants.extend(augmenter.augment_synonym_replacement(sample_text))
    variants.extend(augmenter.augment_slang_injection(sample_text))
    variants.extend(augmenter.augment_regional_variations(sample_text))
    
    for i, variant in enumerate(variants[:10], 1):
        print(f"{i:2d}. {variant}")
    
    print(f"\n🎯 Ready to augment massive dataset when generation completes!")
    print("   This will expand 500K → 1M+ transactions with Vietnamese variations")