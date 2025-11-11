#!/usr/bin/env python3
"""
Vietnamese PhoBERT NLP Pipeline
Advanced Vietnamese text processing with PhoBERT for financial transaction analysis
"""

import os
import json
import torch
import logging
from typing import List, Dict, Any, Optional, Tuple
from transformers import AutoTokenizer, AutoModel
from sentence_transformers import SentenceTransformer
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
import pandas as pd
from datetime import datetime
import pickle
import glob

# Setup logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class VietnamesePhoBERTProcessor:
 """
 Advanced Vietnamese NLP processor using PhoBERT for financial transaction analysis
 """
 
 def __init__(self, model_name: str = "vinai/phobert-base", cache_dir: str = "./models"):
 """
 Initialize PhoBERT processor
 
 Args:
 model_name: PhoBERT model name from HuggingFace
 cache_dir: Directory to cache models
 """
 self.model_name = model_name
 self.cache_dir = cache_dir
 self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
 
 # Create cache directory
 os.makedirs(cache_dir, exist_ok=True)
 
 logger.info(f" Initializing Vietnamese PhoBERT processor...")
 logger.info(f" Device: {self.device}")
 logger.info(f"📦 Model: {model_name}")
 
 # Load PhoBERT components
 self.tokenizer = None
 self.model = None
 self.sentence_transformer = None
 self.embeddings_cache = {}
 
 # Vietnamese financial categories with synonyms
 self.vietnamese_categories = {
 'food': {
 'keywords': ['ăn', 'uống', 'phở', 'cơm', 'bánh', 'chè', 'cafe', 'quán', 'nhà hàng', 'buffet', 'đồ ăn', 'thức ăn'],
 'synonyms': ['ẩm thực', 'đồ uống', 'thực phẩm', 'bữa ăn', 'món ăn']
 },
 'transport': {
 'keywords': ['xe', 'taxi', 'grab', 'bus', 'xăng', 'dầu', 'vé', 'tàu', 'máy bay', 'di chuyển', 'đi lại'],
 'synonyms': ['giao thông', 'vận chuyển', 'phương tiện', 'di chuyen']
 },
 'shopping': {
 'keywords': ['mua', 'sắm', 'siêu thị', 'chợ', 'shop', 'store', 'áo', 'quần', 'giày', 'túi', 'đồ dùng'],
 'synonyms': ['mua sắm', 'shopping', 'đồ dùng', 'sản phẩm']
 },
 'entertainment': {
 'keywords': ['phim', 'karaoke', 'game', 'vui chơi', 'giải trí', 'thể thao', 'gym', 'spa', 'massage'],
 'synonyms': ['giải trí', 'vui chơi', 'thư giãn', 'thể dục']
 },
 'utilities': {
 'keywords': ['điện', 'nước', 'internet', 'điện thoại', 'gas', 'viettel', 'fpt', 'vnpt', 'evn'],
 'synonyms': ['tiện ích', 'dịch vụ', 'công cộng', 'hóa đơn']
 },
 'healthcare': {
 'keywords': ['bệnh viện', 'khám', 'thuốc', 'bác sĩ', 'răng', 'mắt', 'tim', 'xét nghiệm', 'vaccine'],
 'synonyms': ['y tế', 'sức khỏe', 'chăm sóc', 'điều trị']
 },
 'education': {
 'keywords': ['học', 'trường', 'sách', 'khóa học', 'tiếng anh', 'đại học', 'học phí', 'giáo dục'],
 'synonyms': ['giáo dục', 'đào tạo', 'học tập', 'kiến thức']
 },
 'income': {
 'keywords': ['lương', 'thưởng', 'thu nhập', 'tiền lương', 'salary', 'bonus', 'làm thêm', 'bán hàng'],
 'synonyms': ['thu nhập', 'tiền bạc', 'kiếm tiền', 'công việc']
 }
 }
 
 # Load models
 self._load_models()
 
 def _load_models(self):
 """Load PhoBERT models"""
 try:
 logger.info("📥 Loading PhoBERT tokenizer...")
 self.tokenizer = AutoTokenizer.from_pretrained(
 self.model_name, 
 cache_dir=self.cache_dir,
 use_fast=False
 )
 
 logger.info("📥 Loading PhoBERT model...")
 self.model = AutoModel.from_pretrained(
 self.model_name,
 cache_dir=self.cache_dir
 ).to(self.device)
 
 logger.info("📥 Loading sentence transformer...")
 # Use multilingual sentence transformer for Vietnamese
 self.sentence_transformer = SentenceTransformer(
 'distiluse-base-multilingual-cased',
 cache_folder=self.cache_dir
 )
 
 logger.info(" All models loaded successfully!")
 
 except Exception as e:
 logger.error(f" Error loading models: {e}")
 raise
 
 def get_embeddings(self, text: str) -> np.ndarray:
 """
 Get PhoBERT embeddings for Vietnamese text
 
 Args:
 text: Vietnamese text to embed
 
 Returns:
 numpy array of embeddings
 """
 if text in self.embeddings_cache:
 return self.embeddings_cache[text]
 
 try:
 # Tokenize text
 inputs = self.tokenizer(
 text, 
 return_tensors="pt", 
 padding=True, 
 truncation=True, 
 max_length=256
 ).to(self.device)
 
 # Get embeddings from PhoBERT
 with torch.no_grad():
 outputs = self.model(**inputs)
 # Use CLS token embedding
 embeddings = outputs.last_hidden_state[:, 0, :].cpu().numpy()
 
 # Cache the result
 self.embeddings_cache[text] = embeddings[0]
 
 return embeddings[0]
 
 except Exception as e:
 logger.error(f" Error getting embeddings for text: {text[:50]}... - {e}")
 # Fallback to sentence transformer
 return self.sentence_transformer.encode([text])[0]
 
 def classify_transaction(self, description: str) -> Dict[str, Any]:
 """
 Classify Vietnamese transaction description using PhoBERT
 
 Args:
 description: Vietnamese transaction description
 
 Returns:
 Dictionary with classification results
 """
 description_lower = description.lower()
 
 # Get embeddings for the description
 desc_embedding = self.get_embeddings(description)
 
 # Calculate similarity with category keywords
 category_scores = {}
 
 for category, data in self.vietnamese_categories.items():
 all_keywords = data['keywords'] + data['synonyms']
 
 # Simple keyword matching (fast)
 keyword_matches = sum(1 for keyword in all_keywords if keyword in description_lower)
 keyword_score = keyword_matches / len(all_keywords)
 
 # Semantic similarity using embeddings (more accurate)
 semantic_scores = []
 for keyword in all_keywords[:5]: # Limit to top 5 keywords for performance
 keyword_embedding = self.get_embeddings(keyword)
 similarity = cosine_similarity([desc_embedding], [keyword_embedding])[0][0]
 semantic_scores.append(similarity)
 
 semantic_score = np.mean(semantic_scores) if semantic_scores else 0
 
 # Combined score
 combined_score = (keyword_score * 0.3) + (semantic_score * 0.7)
 category_scores[category] = {
 'score': combined_score,
 'keyword_score': keyword_score,
 'semantic_score': semantic_score,
 'matches': keyword_matches
 }
 
 # Find best category
 best_category = max(category_scores.keys(), key=lambda k: category_scores[k]['score'])
 confidence = category_scores[best_category]['score']
 
 return {
 'predicted_category': best_category,
 'confidence': confidence,
 'all_scores': category_scores,
 'method': 'phobert_hybrid'
 }
 
 def extract_financial_entities(self, description: str) -> Dict[str, Any]:
 """
 Extract financial entities from Vietnamese text
 
 Args:
 description: Transaction description
 
 Returns:
 Extracted entities
 """
 entities = {
 'amounts': [],
 'merchants': [],
 'locations': [],
 'payment_methods': [],
 'dates': []
 }
 
 # Extract amounts (Vietnamese currency patterns)
 import re
 
 # Amount patterns
 amount_patterns = [
 r'(\d{1,3}(?:\.\d{3})*(?:\,\d+)?)\s*(?:k|K|đ|vnd|VND)', # 100k, 1.000đ
 r'(\d+(?:\.\d+)?)\s*(?:nghìn|triệu|tỷ)', # 100 nghìn
 ]
 
 for pattern in amount_patterns:
 matches = re.findall(pattern, description, re.IGNORECASE)
 entities['amounts'].extend(matches)
 
 # Extract merchants (proper nouns, shop names)
 merchant_patterns = [
 r'(?:quán|shop|cửa hàng|siêu thị)\s+([A-ZÀ-Ỹ][a-zà-ỹ\s]+)',
 r'([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*)\s+(?:store|shop)',
 ]
 
 for pattern in merchant_patterns:
 matches = re.findall(pattern, description)
 entities['merchants'].extend(matches)
 
 # Extract locations
 vietnam_locations = [
 'Hà Nội', 'TP.HCM', 'Đà Nẵng', 'Hải Phòng', 'Cần Thơ', 
 'Huế', 'Nha Trang', 'Đà Lạt', 'Vũng Tàu', 'Quy Nhon',
 'Quận 1', 'Quận 3', 'Quận 7', 'Hà Đông', 'Cầu Giấy'
 ]
 
 for location in vietnam_locations:
 if location.lower() in description.lower():
 entities['locations'].append(location)
 
 # Extract payment methods
 payment_methods = ['grab', 'momo', 'zalopay', 'vnpay', 'viettel pay', 'cash', 'thẻ']
 for method in payment_methods:
 if method.lower() in description.lower():
 entities['payment_methods'].append(method)
 
 return entities
 
 def process_transaction_batch(self, transactions: List[Dict]) -> List[Dict]:
 """
 Process a batch of transactions
 
 Args:
 transactions: List of transaction dictionaries
 
 Returns:
 Processed transactions with classifications
 """
 logger.info(f"🔄 Processing batch of {len(transactions)} transactions...")
 
 processed = []
 for i, transaction in enumerate(transactions):
 if i % 1000 == 0:
 logger.info(f" Processed {i}/{len(transactions)} transactions")
 
 description = transaction.get('description', '')
 
 # Classify transaction
 classification = self.classify_transaction(description)
 
 # Extract entities
 entities = self.extract_financial_entities(description)
 
 # Add to transaction
 processed_transaction = transaction.copy()
 processed_transaction.update({
 'ai_category': classification['predicted_category'],
 'ai_confidence': classification['confidence'],
 'ai_scores': classification['all_scores'],
 'extracted_entities': entities,
 'processed_timestamp': datetime.now().isoformat(),
 'processor_version': '1.0'
 })
 
 processed.append(processed_transaction)
 
 logger.info(f" Batch processing completed!")
 return processed
 
 def save_embeddings_cache(self, filepath: str):
 """Save embeddings cache to disk"""
 with open(filepath, 'wb') as f:
 pickle.dump(self.embeddings_cache, f)
 logger.info(f" Embeddings cache saved: {filepath}")
 
 def load_embeddings_cache(self, filepath: str):
 """Load embeddings cache from disk"""
 if os.path.exists(filepath):
 with open(filepath, 'rb') as f:
 self.embeddings_cache = pickle.load(f)
 logger.info(f" Embeddings cache loaded: {filepath} ({len(self.embeddings_cache)} entries)")

class DatasetProcessor:
 """
 Process the massive 50GB Vietnamese dataset with PhoBERT
 """
 
 def __init__(self, data_dir: str = "./", output_dir: str = "./processed"):
 self.data_dir = data_dir
 self.output_dir = output_dir
 self.processor = VietnamesePhoBERTProcessor()
 
 # Create output directory
 os.makedirs(output_dir, exist_ok=True)
 
 # Load embeddings cache if exists
 cache_file = os.path.join(output_dir, "embeddings_cache.pkl")
 self.processor.load_embeddings_cache(cache_file)
 
 def process_controlled_dataset(self, batch_size: int = 1000):
 """
 Process the controlled 50GB dataset
 
 Args:
 batch_size: Number of transactions to process at once
 """
 logger.info(" Starting controlled dataset processing...")
 
 # Find all controlled dataset files
 pattern = os.path.join(self.data_dir, "transactions_controlled_chunk_*.json")
 chunk_files = sorted(glob.glob(pattern))
 
 logger.info(f" Found {len(chunk_files)} chunk files to process")
 
 total_processed = 0
 
 for i, chunk_file in enumerate(chunk_files):
 logger.info(f" Processing chunk {i+1}/{len(chunk_files)}: {os.path.basename(chunk_file)}")
 
 # Load chunk
 with open(chunk_file, 'r', encoding='utf-8') as f:
 transactions = json.load(f)
 
 # Process in batches
 processed_transactions = []
 for j in range(0, len(transactions), batch_size):
 batch = transactions[j:j + batch_size]
 processed_batch = self.processor.process_transaction_batch(batch)
 processed_transactions.extend(processed_batch)
 
 # Save processed chunk
 output_file = os.path.join(
 self.output_dir, 
 f"processed_transactions_chunk_{i:04d}.json"
 )
 
 with open(output_file, 'w', encoding='utf-8') as f:
 json.dump(processed_transactions, f, ensure_ascii=False, indent=1)
 
 total_processed += len(processed_transactions)
 
 logger.info(f" Saved processed chunk: {output_file}")
 logger.info(f" Total processed so far: {total_processed:,} transactions")
 
 # Save cache periodically
 if i % 10 == 0:
 cache_file = os.path.join(self.output_dir, "embeddings_cache.pkl")
 self.processor.save_embeddings_cache(cache_file)
 
 # Final cache save
 cache_file = os.path.join(self.output_dir, "embeddings_cache.pkl")
 self.processor.save_embeddings_cache(cache_file)
 
 logger.info(f" Dataset processing completed!")
 logger.info(f" Total transactions processed: {total_processed:,}")
 
 return total_processed

if __name__ == "__main__":
 # Test the processor with sample data
 logger.info(" Testing Vietnamese PhoBERT Processor...")
 
 # Initialize processor
 processor = VietnamesePhoBERTProcessor()
 
 # Test transactions
 test_transactions = [
 {"description": "Quán phở Hùng - Phở bò tái 75k", "amount": 75000},
 {"description": "Grab từ Hà Nội đi Hà Đông 120k", "amount": 120000},
 {"description": "Vinmart+ Cầu Giấy - Mua sắm 350k", "amount": 350000},
 {"description": "Lương tháng 12 công ty ABC 15000k", "amount": 15000000},
 {"description": "Tiền điện EVN HANOI 450k", "amount": 450000}
 ]
 
 # Test classification
 for transaction in test_transactions:
 result = processor.classify_transaction(transaction['description'])
 entities = processor.extract_financial_entities(transaction['description'])
 
 print(f"\n Transaction: {transaction['description']}")
 print(f"🏷 Category: {result['predicted_category']} (confidence: {result['confidence']:.3f})")
 print(f" Entities: {entities}")
 
 logger.info(" Test completed successfully!")
 
 # Ask user if they want to process the full dataset
 response = input("\n🤔 Do you want to process the full 50GB dataset? This will take several hours (y/n): ")
 
 if response.lower() in ['y', 'yes']:
 dataset_processor = DatasetProcessor()
 dataset_processor.process_controlled_dataset()
 else:
 logger.info("⏭ Full dataset processing skipped")