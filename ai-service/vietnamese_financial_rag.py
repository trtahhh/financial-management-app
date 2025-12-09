#!/usr/bin/env python3
"""
Vietnamese Financial RAG System with ChromaDB
Creates vector database for Retrieval-Augmented Generation
"""

import json
import logging
from pathlib import Path
from typing import Dict, List, Any, Optional
import chromadb
from chromadb.config import Settings
import numpy as np
from sentence_transformers import SentenceTransformer
import pickle
import gzip
from datetime import datetime
import uuid

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class VietnameseFinancialRAG:
 def __init__(self, db_path: str = "./chroma_db"):
 """Initialize Vietnamese Financial RAG system"""
 
 self.db_path = Path(db_path)
 self.db_path.mkdir(exist_ok=True)
 
 # Initialize ChromaDB
 logger.info(" Initializing ChromaDB...")
 self.client = chromadb.PersistentClient(path=str(self.db_path))
 
 # Collection names
 self.transactions_collection_name = "vietnamese_transactions"
 self.knowledge_collection_name = "financial_knowledge"
 
 # Initialize embedding model (lightweight for Vietnamese)
 logger.info(" Loading Vietnamese-compatible embedding model...")
 try:
 # Use multilingual model that supports Vietnamese
 self.embedding_model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')
 logger.info(" Multilingual embedding model loaded successfully")
 except Exception as e:
 logger.error(f" Error loading embedding model: {e}")
 # Fallback to a smaller model
 self.embedding_model = SentenceTransformer('all-MiniLM-L6-v2')
 logger.info(" Fallback embedding model loaded")
 
 # Load classifier for enhanced context
 try:
 self.classifier = self._load_classifier()
 logger.info(" Vietnamese classifier loaded for enhanced context")
 except Exception as e:
 logger.warning(f" Classifier not available: {e}")
 self.classifier = None
 
 def _load_classifier(self):
 """Load the trained Vietnamese classifier"""
 with open("vietnamese_transaction_classifier.pkl", 'rb') as f:
 model_data = pickle.load(f)
 return model_data
 
 def create_transaction_embeddings(self, sample_size: int = 5000) -> Dict[str, Any]:
 """Create embeddings from Vietnamese transaction sample"""
 
 logger.info(f" Creating embeddings for {sample_size} transactions...")
 
 # Load quality sample
 with open("vietnamese_financial_quality_sample.json", 'r', encoding='utf-8') as f:
 transactions = json.load(f)
 
 # Limit sample size for performance
 if len(transactions) > sample_size:
 transactions = transactions[:sample_size]
 
 logger.info(f" Processing {len(transactions)} transactions...")
 
 # Create or get collection
 try:
 collection = self.client.get_collection(self.transactions_collection_name)
 logger.info(" Using existing transactions collection")
 except:
 collection = self.client.create_collection(
 name=self.transactions_collection_name,
 metadata={"description": "Vietnamese financial transactions with embeddings"}
 )
 logger.info(" Created new transactions collection")
 
 # Prepare documents for embedding
 documents = []
 metadatas = []
 ids = []
 
 for i, transaction in enumerate(transactions):
 # Create rich document text for better RAG
 description = transaction.get('description', '')
 category = transaction.get('category', '')
 region = transaction.get('region', '')
 amount = transaction.get('amount', 0)
 
 # Enhanced document with context
 doc_text = f"{description} [Category: {category}] [Region: {region}] [Amount: {amount:,} VND]"
 
 # Metadata for filtering and context
 metadata = {
 'category': category,
 'region': region,
 'amount': amount,
 'date': transaction.get('date', ''),
 'type': transaction.get('type', 'EXPENSE'),
 'payment_method': transaction.get('payment_method', ''),
 'merchant': transaction.get('merchant', ''),
 'location': transaction.get('location', '')
 }
 
 documents.append(doc_text)
 metadatas.append(metadata)
 ids.append(f"tx_{i}_{uuid.uuid4().hex[:8]}")
 
 # Generate embeddings in batches
 batch_size = 100
 total_added = 0
 
 logger.info(" Generating embeddings...")
 
 for i in range(0, len(documents), batch_size):
 batch_docs = documents[i:i+batch_size]
 batch_metas = metadatas[i:i+batch_size]
 batch_ids = ids[i:i+batch_size]
 
 try:
 # Generate embeddings for this batch
 embeddings = self.embedding_model.encode(batch_docs, convert_to_tensor=False)
 
 # Add to ChromaDB
 collection.add(
 documents=batch_docs,
 metadatas=batch_metas,
 embeddings=embeddings.tolist(),
 ids=batch_ids
 )
 
 total_added += len(batch_docs)
 logger.info(f" Added batch {i//batch_size + 1}: {total_added}/{len(documents)} transactions")
 
 except Exception as e:
 logger.error(f" Error processing batch {i//batch_size + 1}: {e}")
 
 results = {
 "total_transactions": len(transactions),
 "embeddings_created": total_added,
 "collection_name": self.transactions_collection_name,
 "embedding_model": str(self.embedding_model),
 "dimensions": len(embeddings[0]) if embeddings.size > 0 else 0
 }
 
 logger.info(f" Transaction embeddings completed: {total_added} embeddings created")
 
 return results
 
 def create_knowledge_base(self) -> Dict[str, Any]:
 """Create Vietnamese financial knowledge base"""
 
 logger.info(" Creating Vietnamese financial knowledge base...")
 
 # Vietnamese financial knowledge
 financial_knowledge = [
 {
 "topic": "Phân loại giao dịch",
 "content": "Các loại giao dịch tài chính phổ biến: Chi tiêu ăn uống (food), Di chuyển giao thông (transport), Mua sắm (shopping), Giải trí (entertainment), Tiện ích sinh hoạt (utilities), Chăm sóc sức khỏe (healthcare), Giáo dục (education), Thu nhập (income).",
 "keywords": ["phân loại", "giao dịch", "chi tiêu", "thu nhập"],
 "category": "classification"
 },
 {
 "topic": "Quản lý ngân sách cá nhân",
 "content": "Nguyên tắc 50/30/20: 50% thu nhập cho nhu cầu thiết yếu, 30% cho mong muốn cá nhân, 20% cho tiết kiệm và đầu tư. Theo dõi chi tiêu hàng ngày để kiểm soát tài chính hiệu quả.",
 "keywords": ["ngân sách", "50/30/20", "tiết kiệm", "chi tiêu"],
 "category": "budgeting"
 },
 {
 "topic": "Tiết kiệm và đầu tư",
 "content": "Các hình thức tiết kiệm phổ biến ở Việt Nam: Gửi tiết kiệm ngân hàng, mua vàng, đầu tư chứng khoán, bất động sản. Nên có quỹ khẩn cấp tương đương 3-6 tháng chi tiêu.",
 "keywords": ["tiết kiệm", "đầu tư", "quỹ khẩn cấp", "chứng khoán"],
 "category": "investment"
 },
 {
 "topic": "Thanh toán điện tử",
 "content": "Các phương thức thanh toán phổ biến: Thẻ ATM, Internet Banking, ví điện tử (MoMo, ZaloPay, VNPay), QR Code. Ưu điểm: tiện lợi, an toàn, theo dõi chi tiêu dễ dàng.",
 "keywords": ["thanh toán điện tử", "ví điện tử", "MoMo", "ZaloPay", "QR"],
 "category": "payment"
 },
 {
 "topic": "Lập kế hoạch tài chính",
 "content": "Các bước lập kế hoạch tài chính: Xác định mục tiêu, đánh giá tình hình hiện tại, lập ngân sách, thực hiện và theo dõi. Nên đặt mục tiêu SMART (Cụ thể, Đo lường được, Khả thi, Liên quan, Có thời hạn).",
 "keywords": ["kế hoạch tài chính", "mục tiêu", "SMART", "ngân sách"],
 "category": "planning"
 },
 {
 "topic": "Kiểm soát chi tiêu",
 "content": "Cách kiểm soát chi tiêu hiệu quả: Ghi chép chi tiêu hàng ngày, phân loại theo mức độ cần thiết, sử dụng phương pháp envelope budgeting, tránh mua sắm bốc đồng.",
 "keywords": ["kiểm soát chi tiêu", "ghi chép", "envelope budgeting"],
 "category": "expense_control"
 },
 {
 "topic": "Tài chính cá nhân Việt Nam",
 "content": "Đặc điểm tài chính cá nhân người Việt: Ưu tiên tiết kiệm, đầu tư vào vàng và bất động sản, sử dụng nhiều tiền mặt, quan tâm đến bảo hiểm gia đình.",
 "keywords": ["tài chính Việt Nam", "tiền mặt", "vàng", "bảo hiểm"],
 "category": "vietnamese_finance"
 },
 {
 "topic": "Mẹo tiết kiệm hàng ngày",
 "content": "Các mẹo tiết kiệm đơn giản: Nấu ăn tại nhà thay vì ăn ngoài, sử dụng phương tiện công cộng, tắt điện khi không sử dụng, mua sắm theo danh sách, tận dụng khuyến mãi.",
 "keywords": ["mẹo tiết kiệm", "nấu ăn", "công cộng", "khuyến mãi"],
 "category": "saving_tips"
 }
 ]
 
 # Create or get knowledge collection
 try:
 knowledge_collection = self.client.get_collection(self.knowledge_collection_name)
 logger.info(" Using existing knowledge collection")
 except:
 knowledge_collection = self.client.create_collection(
 name=self.knowledge_collection_name,
 metadata={"description": "Vietnamese financial knowledge base"}
 )
 logger.info(" Created new knowledge collection")
 
 # Add knowledge to collection
 documents = []
 metadatas = []
 ids = []
 
 for i, knowledge in enumerate(financial_knowledge):
 documents.append(knowledge['content'])
 metadatas.append({
 'topic': knowledge['topic'],
 'category': knowledge['category'],
 'keywords': ', '.join(knowledge['keywords'])
 })
 ids.append(f"kb_{i}_{knowledge['category']}")
 
 # Generate embeddings for knowledge
 logger.info(" Generating knowledge base embeddings...")
 knowledge_embeddings = self.embedding_model.encode(documents, convert_to_tensor=False)
 
 # Add to ChromaDB
 knowledge_collection.add(
 documents=documents,
 metadatas=metadatas,
 embeddings=knowledge_embeddings.tolist(),
 ids=ids
 )
 
 results = {
 "knowledge_items": len(financial_knowledge),
 "collection_name": self.knowledge_collection_name,
 "categories": list(set(k['category'] for k in financial_knowledge))
 }
 
 logger.info(f" Knowledge base created: {len(financial_knowledge)} items")
 
 return results
 
 def query_transactions(self, query: str, n_results: int = 5, category_filter: Optional[str] = None) -> Dict[str, Any]:
 """Query similar transactions using RAG"""
 
 logger.info(f" Querying transactions: '{query}'")
 
 # Get transaction collection
 collection = self.client.get_collection(self.transactions_collection_name)
 
 # Prepare where clause for filtering
 where_clause = {}
 if category_filter:
 where_clause = {"category": category_filter}
 
 # Query similar transactions
 results = collection.query(
 query_texts=[query],
 n_results=n_results,
 where=where_clause if where_clause else None
 )
 
 response = {
 "query": query,
 "results_count": len(results['documents'][0]),
 "transactions": []
 }
 
 # Format results
 for i in range(len(results['documents'][0])):
 transaction = {
 "document": results['documents'][0][i],
 "metadata": results['metadatas'][0][i],
 "similarity": 1 - results['distances'][0][i], # Convert distance to similarity
 "id": results['ids'][0][i]
 }
 response["transactions"].append(transaction)
 
 return response
 
 def query_knowledge(self, query: str, n_results: int = 3) -> Dict[str, Any]:
 """Query financial knowledge base"""
 
 logger.info(f" Querying knowledge: '{query}'")
 
 # Get knowledge collection
 collection = self.client.get_collection(self.knowledge_collection_name)
 
 # Query knowledge base
 results = collection.query(
 query_texts=[query],
 n_results=n_results
 )
 
 response = {
 "query": query,
 "results_count": len(results['documents'][0]),
 "knowledge": []
 }
 
 # Format results
 for i in range(len(results['documents'][0])):
 knowledge = {
 "content": results['documents'][0][i],
 "metadata": results['metadatas'][0][i],
 "relevance": 1 - results['distances'][0][i],
 "id": results['ids'][0][i]
 }
 response["knowledge"].append(knowledge)
 
 return response
 
 def get_financial_advice(self, query: str) -> Dict[str, Any]:
 """Generate comprehensive financial advice using RAG"""
 
 logger.info(f" Generating advice for: '{query}'")
 
 # Query both transactions and knowledge
 similar_transactions = self.query_transactions(query, n_results=3)
 relevant_knowledge = self.query_knowledge(query, n_results=2)
 
 # Generate contextual advice
 advice = {
 "query": query,
 "similar_transactions": similar_transactions,
 "relevant_knowledge": relevant_knowledge,
 "advice_summary": self._generate_advice_summary(query, similar_transactions, relevant_knowledge),
 "timestamp": datetime.now().isoformat()
 }
 
 return advice
 
 def _generate_advice_summary(self, query: str, transactions: Dict, knowledge: Dict) -> str:
 """Generate advice summary based on RAG context"""
 
 # Extract key information
 categories = [t['metadata'].get('category', '') for t in transactions['transactions']]
 amounts = [t['metadata'].get('amount', 0) for t in transactions['transactions']]
 
 # Basic advice generation
 if 'chi tiêu' in query.lower() or 'tiêu tiền' in query.lower():
 avg_amount = sum(amounts) / len(amounts) if amounts else 0
 return f"Dựa trên các giao dịch tương tự, mức chi tiêu trung bình là {avg_amount:,.0f} VND. Hãy so sánh với ngân sách của bạn và cân nhắc mức độ cần thiết."
 
 elif 'tiết kiệm' in query.lower():
 return "Theo nguyên tắc 50/30/20, hãy dành 20% thu nhập cho tiết kiệm. Bắt đầu với việc theo dõi chi tiêu hàng ngày và tìm các khoản không cần thiết để cắt giảm."
 
 elif 'đầu tư' in query.lower():
 return "Trước khi đầu tư, hãy có quỹ khẩn cấp 3-6 tháng chi tiêu. Với người Việt Nam, có thể bắt đầu với gửi tiết kiệm, sau đó tìm hiểu về chứng khoán và các kênh đầu tư khác."
 
 else:
 return "Để quản lý tài chính tốt, hãy ghi chép chi tiêu, lập ngân sách, và đặt mục tiêu tài chính cụ thể. Sử dụng các ứng dụng theo dõi chi tiêu để dễ dàng kiểm soát."
 
 def get_database_stats(self) -> Dict[str, Any]:
 """Get RAG database statistics"""
 
 stats = {
 "collections": [],
 "total_embeddings": 0,
 "database_path": str(self.db_path)
 }
 
 try:
 # Transaction collection stats
 tx_collection = self.client.get_collection(self.transactions_collection_name)
 tx_count = tx_collection.count()
 
 stats["collections"].append({
 "name": self.transactions_collection_name,
 "count": tx_count,
 "type": "transactions"
 })
 stats["total_embeddings"] += tx_count
 
 except Exception as e:
 logger.warning(f"Could not get transaction collection stats: {e}")
 
 try:
 # Knowledge collection stats
 kb_collection = self.client.get_collection(self.knowledge_collection_name)
 kb_count = kb_collection.count()
 
 stats["collections"].append({
 "name": self.knowledge_collection_name,
 "count": kb_count,
 "type": "knowledge"
 })
 stats["total_embeddings"] += kb_count
 
 except Exception as e:
 logger.warning(f"Could not get knowledge collection stats: {e}")
 
 return stats

def main():
 """Main RAG setup interface"""
 
 print(" Vietnamese Financial RAG System Setup")
 print("=" * 50)
 
 # Initialize RAG system
 rag = VietnameseFinancialRAG()
 
 # Create transaction embeddings
 tx_results = rag.create_transaction_embeddings(sample_size=2000)
 print(f" Transaction embeddings: {tx_results['embeddings_created']} created")
 
 # Create knowledge base
 kb_results = rag.create_knowledge_base()
 print(f" Knowledge base: {kb_results['knowledge_items']} items created")
 
 # Test queries
 print(f"\n Testing RAG system...")
 
 test_queries = [
 "Chi tiêu ăn uống hàng ngày",
 "Cách tiết kiệm tiền hiệu quả",
 "Đầu tư chứng khoán cho người mới bắt đầu",
 "Quản lý ngân sách gia đình"
 ]
 
 for query in test_queries:
 advice = rag.get_financial_advice(query)
 print(f"❓ '{query}'")
 print(f" {advice['advice_summary']}")
 print()
 
 # Show database stats
 stats = rag.get_database_stats()
 print(f" RAG Database Stats:")
 print(f" Total embeddings: {stats['total_embeddings']}")
 for collection in stats['collections']:
 print(f" {collection['name']}: {collection['count']} {collection['type']}")
 
 print(f"\n Vietnamese Financial RAG system ready for production!")

if __name__ == "__main__":
 main()