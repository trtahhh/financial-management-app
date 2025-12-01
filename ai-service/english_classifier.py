"""
English Transaction Classifier
Provides classification for English-language financial transactions
Designed to work alongside Vietnamese classifier for multilingual support
"""

import re
from typing import Dict, Any, List

class EnglishClassifier:
    """
    English-language financial transaction classifier
    Supports 13 categories with comprehensive English keywords
    """
    
    def __init__(self):
        """Initialize English classifier with keyword sets"""
        
        # English financial categories with keywords and patterns
        self.english_categories = {
            'food': {
                'keywords': [
                    # Food & drink words
                    'food', 'eat', 'drink', 'restaurant', 'cafe', 'coffee', 'lunch', 'dinner', 'breakfast',
                    'meal', 'snack', 'beverage', 'beer', 'wine', 'juice', 'tea', 'milk', 'bread', 'pizza',
                    'burger', 'sandwich', 'salad', 'soup', 'dessert', 'ice cream', 'cake', 'bakery',
                    # Chains & brands
                    'mcdonald', 'mcdonalds', 'kfc', 'subway', 'burger king', 'wendy', 'taco bell',
                    'starbucks', 'dunkin', 'costa', 'pret', 'chipotle', 'panera',
                    'domino', 'pizza hut', 'papa john', 'little caesars',
                    # Delivery services - Food
                    'uber eats', 'ubereats', 'doordash', 'grubhub', 'postmates', 'deliveroo',
                    'just eat', 'food panda', 'grabfood', 'grab food', 'food delivery',
                    'amazon fresh', 'whole foods', 'amazon grocery',
                    'dashmart', 'dash mart', 'doordash grocery',
                    'walmart grocery', 'walmart+', 'walmart plus',
                    # Restaurant types
                    'buffet', 'diner', 'bistro', 'grill', 'steakhouse', 'sushi', 'ramen', 'noodle'
                ],
                'patterns': [
                    r'\brestaurant\b', r'\bcafe\b', r'\beat\s+\w+', r'\bfood\s+\w+',
                    r'\blunch\b', r'\bdinner\b', r'\bbreakfast\b', r'\bdelivery\b'
                ]
            },
            'transport': {
                'keywords': [
                    # Transport types
                    'transport', 'taxi', 'uber', 'lyft', 'cab', 'bus', 'train', 'subway', 'metro',
                    'flight', 'airline', 'airport', 'parking', 'toll', 'gas', 'fuel', 'petrol', 'diesel',
                    'car', 'vehicle', 'auto', 'bike', 'scooter', 'motorcycle', 'ride', 'drive',
                    # Services
                    'uber', 'lyft', 'grab', 'bolt', 'ola', 'didi',
                    'uber ride', 'lyft ride', 'lyft', 'grab ride', 'grab car', 'grab bike',
                    'rideshare', 'ride share', 'taxi service', 'car service',
                    # Airlines
                    'delta', 'united', 'american airlines', 'southwest', 'jetblue', 'spirit',
                    'british airways', 'lufthansa', 'emirates', 'qatar',
                    # Fuel companies
                    'shell', 'exxon', 'bp', 'chevron', 'mobil', 'texaco', 'gulf'
                ],
                'patterns': [
                    r'\buber\b', r'\blyft\b', r'\btaxi\b', r'\bflight\b', r'\bgas\s+station\b',
                    r'\bfrom\s+\w+\s+to\s+\w+\b'
                ]
            },
            'shopping': {
                'keywords': [
                    # Shopping verbs
                    'buy', 'bought', 'purchase', 'shop', 'shopping', 'order', 'ordered',
                    # Venues
                    'store', 'mall', 'market', 'supermarket', 'grocery', 'retail', 'outlet',
                    # Items
                    'clothes', 'clothing', 'shirt', 'pants', 'dress', 'shoes', 'bag', 'purse',
                    'cosmetics', 'makeup', 'perfume', 'skincare', 'beauty',
                    # Chains
                    'walmart', 'target', 'costco', 'amazon', 'ebay', 'aliexpress',
                    'amazon shopping', 'amazon.com', 'amazon order',
                    'walmart shopping', 'walmart.com', 'walmart delivery',
                    'best buy', 'home depot', 'lowes', 'ikea', 'wayfair',
                    'zara', 'h&m', 'gap', 'old navy', 'forever 21', 'uniqlo',
                    'whole foods', 'trader joe', 'kroger', 'safeway', 'publix',
                    # Express/Delivery
                    'amazon prime', 'instacart', 'shipt', 'postmates delivery',
                    'grab mart', 'grab express', 'delivery service'
                ],
                'patterns': [
                    r'\bbuy\s+\w+\b', r'\bbought\s+\w+\b', r'\bshopping\b',
                    r'\border\s+\w+\b', r'\bpurchase\b'
                ]
            },
            'entertainment': {
                'keywords': [
                    # Activities
                    'entertainment', 'fun', 'movie', 'cinema', 'film', 'theater', 'concert',
                    'game', 'gaming', 'play', 'sport', 'sports', 'gym', 'fitness', 'yoga',
                    'music', 'show', 'performance', 'event', 'ticket', 'karaoke',
                    'bowling', 'pool', 'billiards', 'arcade', 'museum', 'zoo', 'park',
                    'spa', 'massage', 'sauna', 'swim', 'pool',
                    # Services & Streaming
                    'netflix', 'spotify', 'hulu', 'disney', 'youtube', 'twitch', 'steam',
                    'playstation', 'xbox', 'nintendo', 'subscription', 'premium', 'streaming',
                    'apple music', 'amazon prime', 'prime video', 'amazon prime video',
                    'hbo', 'paramount',
                    # Venues
                    'amc', 'regal', 'cinemark', 'imax', 'planetarium'
                ],
                'patterns': [
                    r'\bmovie\b', r'\bcinema\b', r'\bticket\b', r'\bconcert\b',
                    r'\bgym\b', r'\bfitness\b', r'\bsubscription\b', r'\bpremium\b',
                    r'\bstreaming\b', r'\bmembership\b'
                ]
            },
            'health': {
                'keywords': [
                    # Medical
                    'health', 'medical', 'doctor', 'hospital', 'clinic', 'pharmacy', 'medicine',
                    'prescription', 'drug', 'pill', 'treatment', 'checkup', 'exam', 'test',
                    'dentist', 'dental', 'teeth', 'eye', 'vision', 'glasses', 'contact',
                    'surgery', 'operation', 'emergency', 'urgent care',
                    # Pharmacies
                    'cvs', 'walgreens', 'rite aid', 'walmart pharmacy', 'target pharmacy',
                    # Items
                    'vitamin', 'supplement', 'aspirin', 'tylenol', 'advil', 'bandage',
                    'first aid', 'thermometer'
                ],
                'patterns': [
                    r'\bdoctor\b', r'\bhospital\b', r'\bpharmacy\b', r'\bmedicine\b',
                    r'\bdental\b', r'\bcheckup\b'
                ]
            },
            'education': {
                'keywords': [
                    # Education
                    'education', 'school', 'college', 'university', 'course', 'class', 'lesson',
                    'tuition', 'fee', 'book', 'textbook', 'notebook', 'pen', 'pencil', 'stationery',
                    'learning', 'study', 'training', 'workshop', 'seminar', 'certification',
                    'purchase book', 'buy book', 'textbook purchase', 'study material',
                    # Levels
                    'kindergarten', 'elementary', 'high school', 'undergraduate', 'graduate',
                    'phd', 'master', 'bachelor',
                    # Languages & skills
                    'english', 'spanish', 'french', 'chinese', 'language', 'ielts', 'toefl',
                    'coding', 'programming', 'computer', 'math', 'science',
                    # Platforms
                    'coursera', 'udemy', 'skillshare', 'linkedin learning', 'khan academy'
                ],
                'patterns': [
                    r'\btuition\b', r'\bschool\s+fee\b', r'\bcourse\b', r'\bclass\b',
                    r'\beducation\b'
                ]
            },
            'utilities': {
                'keywords': [
                    # Utilities
                    'utility', 'utilities', 'electric', 'electricity', 'power', 'water',
                    'gas', 'heating', 'cooling', 'internet', 'wifi', 'broadband', 'phone',
                    'mobile', 'cell', 'cable', 'tv', 'television', 'satellite',
                    'trash', 'garbage', 'waste', 'recycling', 'sewage',
                    # Providers
                    'verizon', 'at&t', 'tmobile', 'sprint', 'comcast', 'xfinity',
                    'spectrum', 'cox', 'centurylink', 'frontier',
                    # Bills
                    'bill', 'payment', 'monthly', 'subscription', 'service fee'
                ],
                'patterns': [
                    r'\belectric\s+bill\b', r'\bwater\s+bill\b', r'\binternet\s+bill\b',
                    r'\bphone\s+bill\b', r'\bmonthly\s+payment\b'
                ]
            },
            'income': {
                'keywords': [
                    # Income
                    'income', 'salary', 'wage', 'paycheck', 'payment', 'earnings', 'revenue',
                    'bonus', 'commission', 'tip', 'gratuity', 'reward', 'prize', 'refund',
                    'cashback', 'rebate', 'reimbursement', 'dividend', 'interest',
                    'freelance', 'gig', 'consulting', 'contract',
                    # Sources
                    'employer', 'client', 'customer', 'return', 'deposit'
                ],
                'patterns': [
                    r'\bsalary\b', r'\bpaycheck\b', r'\bincome\b', r'\bearnings\b',
                    r'\bbonus\b', r'\brefund\b'
                ]
            },
            'investment': {
                'keywords': [
                    # Investment
                    'investment', 'invest', 'stock', 'stocks', 'share', 'shares', 'equity',
                    'bond', 'bonds', 'fund', 'mutual fund', 'etf', 'index fund',
                    'crypto', 'cryptocurrency', 'bitcoin', 'ethereum', 'blockchain',
                    'forex', 'trading', 'broker', 'brokerage', 'portfolio',
                    'real estate', 'property', 'asset', 'gold', 'silver', 'commodity',
                    # Platforms
                    'robinhood', 'etrade', 'fidelity', 'schwab', 'vanguard', 'coinbase',
                    'binance', 'kraken'
                ],
                'patterns': [
                    r'\binvest\b', r'\bstock\b', r'\bcrypto\b', r'\bbitcoin\b',
                    r'\btrading\b'
                ]
            },
            'insurance': {
                'keywords': [
                    # Insurance
                    'insurance', 'premium', 'policy', 'coverage', 'claim', 'deductible',
                    'health insurance', 'life insurance', 'auto insurance', 'car insurance',
                    'home insurance', 'renters insurance', 'travel insurance',
                    'dental insurance', 'vision insurance',
                    # Providers
                    'state farm', 'geico', 'progressive', 'allstate', 'liberty mutual',
                    'nationwide', 'farmers', 'usaa', 'aetna', 'blue cross', 'cigna',
                    'humana', 'kaiser'
                ],
                'patterns': [
                    r'\binsurance\b', r'\bpremium\b', r'\bpolicy\b', r'\bcoverage\b'
                ]
            },
            'family': {
                'keywords': [
                    # Family
                    'family', 'child', 'children', 'kid', 'kids', 'baby', 'son', 'daughter',
                    'parent', 'mom', 'dad', 'mother', 'father', 'spouse', 'wife', 'husband',
                    'brother', 'sister', 'grandparent', 'grandma', 'grandpa',
                    'gift', 'present', 'birthday', 'anniversary', 'wedding', 'celebration',
                    'party', 'graduation', 'baby shower', 'allowance', 'pocket money',
                    # Activities
                    'daycare', 'childcare', 'nanny', 'babysitter', 'toys', 'diapers'
                ],
                'patterns': [
                    r'\bgift\s+for\b', r'\bbirthday\b', r'\banniversary\b', r'\bwedding\b',
                    r'\bfamily\b'
                ]
            },
            'charity': {
                'keywords': [
                    # Charity
                    'charity', 'donation', 'donate', 'contribution', 'support', 'help',
                    'fundraiser', 'nonprofit', 'foundation', 'cause', 'relief',
                    'humanitarian', 'volunteer', 'sponsorship', 'pledge',
                    # Organizations
                    'red cross', 'unicef', 'salvation army', 'goodwill', 'habitat',
                    'church', 'mosque', 'temple', 'synagogue', 'religious'
                ],
                'patterns': [
                    r'\bdonation\b', r'\bcharity\b', r'\bfundraiser\b', r'\bcontribution\b'
                ]
            },
            'other': {
                'keywords': ['other', 'misc', 'miscellaneous', 'unknown'],
                'patterns': []
            }
        }
    
    def preprocess_text(self, text: str) -> str:
        """Preprocess English text"""
        # Convert to lowercase
        text = text.lower().strip()
        
        # Remove extra whitespace
        text = ' '.join(text.split())
        
        return text
    
    def extract_features(self, description: str) -> Dict[str, Any]:
        """Extract features from transaction description"""
        description_lower = description.lower()
        features = {}
        
        for category, config in self.english_categories.items():
            keyword_matches = sum(1 for keyword in config['keywords'] 
                                 if keyword in description_lower)
            pattern_matches = sum(1 for pattern in config['patterns'] 
                                 if re.search(pattern, description_lower, re.IGNORECASE))
            
            features[f'{category}_keywords'] = keyword_matches
            features[f'{category}_patterns'] = pattern_matches
            features[f'{category}_total'] = keyword_matches + pattern_matches
        
        return features
    
    def classify_transaction(self, description: str) -> Dict[str, Any]:
        """Classify English transaction with brand-specific service detection"""
        processed_text = self.preprocess_text(description)
        description_lower = description.lower()
        
        # SMART BRAND SERVICE DETECTION - Override generic keywords
        brand_service_rules = {
            'food': ['uber eats', 'ubereats', 'doordash', 'grubhub', 'postmates', 'deliveroo', 
                    'food delivery', 'grabfood', 'grab food', 'food panda',
                    'amazon fresh', 'amazon grocery',
                    'dashmart', 'dash mart', 'doordash grocery',
                    'walmart grocery', 'walmart+', 'walmart plus', 'walmart food'],
            'shopping': ['instacart', 'shipt', 'grab mart', 'grab express', 
                        'delivery service', 'express delivery',
                        'amazon shopping', 'amazon.com', 'amazon order',
                        'walmart shopping', 'walmart.com', 'walmart delivery', 'walmart order'],
            'transport': ['uber ride', 'uber', 'lyft ride', 'lyft', 'grab ride', 'grab car', 'grab bike', 
                         'rideshare', 'taxi service', 'car service'],
            'entertainment': ['prime video', 'amazon prime video', 'amazon video', 'video subscription']
        }
        
        # Check if specific service mentioned - boost that category
        service_boost = None
        for category, service_keywords in brand_service_rules.items():
            if any(keyword in description_lower for keyword in service_keywords):
                # For 'Amazon Prime', prioritize entertainment if 'video' or 'subscription' mentioned
                if 'amazon prime' in description_lower and category == 'entertainment':
                    if 'video' in description_lower or 'subscription' in description_lower:
                        service_boost = category
                        break
                # For other cases, first match wins
                if service_boost is None:
                    service_boost = category
        
        features = self.extract_features(description)
        
        category_scores = {}
        for category in self.english_categories.keys():
            keyword_score = features.get(f'{category}_keywords', 0)
            pattern_score = features.get(f'{category}_patterns', 0)
            
            # Same scoring as Vietnamese: 60% base + 5% per additional keyword
            if keyword_score > 0:
                keyword_norm = min(0.60 + (keyword_score - 1) * 0.05, 0.95)
            else:
                keyword_norm = 0.0
            
            if pattern_score > 0:
                pattern_norm = min(0.50 + (pattern_score - 1) * 0.05, 0.90)
            else:
                pattern_norm = 0.0
            
            combined_score = (keyword_norm * 0.7) + (pattern_norm * 0.3)
            
            # Apply service-specific boost
            if service_boost == category:
                combined_score = max(combined_score, 0.85)
            
            category_scores[category] = {
                'score': combined_score,
                'keyword_matches': keyword_score,
                'pattern_matches': pattern_score
            }
        
        best_category = max(category_scores.keys(), 
                           key=lambda k: category_scores[k]['score'])
        confidence = category_scores[best_category]['score']
        
        if confidence < 0.1:
            best_category = 'other'
            confidence = 0.5
        
        # Map to capitalized English names
        category_mapping = {
            'food': 'Food & Dining',
            'transport': 'Transportation',
            'shopping': 'Shopping',
            'entertainment': 'Entertainment',
            'health': 'Healthcare',
            'education': 'Education',
            'utilities': 'Utilities',
            'income': 'Income',
            'investment': 'Investment',
            'insurance': 'Insurance',
            'family': 'Family & Personal',
            'charity': 'Charity & Donations',
            'other': 'Other'
        }
        
        english_category = category_mapping.get(best_category, 'Other')
        
        # Create all_probabilities dict
        all_probabilities = {category_mapping.get(cat, cat): scores['score']
                            for cat, scores in category_scores.items()}
        
        return {
            'predicted_category': english_category,
            'confidence': confidence,
            'description': description,
            'processed_description': processed_text,
            'all_probabilities': all_probabilities,
            'all_scores': category_scores,
            'features': features,
            'method': 'english_classifier',
            'language': 'en',
            'success': True
        }
    
    def get_system_stats(self) -> Dict[str, Any]:
        """Get English classifier statistics"""
        total_keywords = sum(len(config['keywords']) 
                            for config in self.english_categories.values())
        total_patterns = sum(len(config['patterns']) 
                            for config in self.english_categories.values())
        
        return {
            'classifier': 'English',
            'language': 'en',
            'categories': len(self.english_categories),
            'total_keywords': total_keywords,
            'total_patterns': total_patterns,
            'avg_keywords_per_category': total_keywords / len(self.english_categories),
            'method': 'keyword_pattern_matching',
            'scoring': '60% base + 5% per additional match'
        }
