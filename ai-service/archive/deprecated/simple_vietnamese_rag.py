#!/usr/bin/env python3
"""
Simplified Vietnamese Financial RAG System
Uses TF-IDF for embeddings instead of transformer models
"""

import json
import logging
from pathlib import Path
from typing import Dict, List, Any, Optional
import chromadb
from sklearn.feature_extraction.text import TfidfVectorizer
import numpy as np
import pickle
from datetime import datetime
import uuid
from underthesea import word_tokenize
import re

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class SimpleVietnameseRAG:
 def __init__(self, db_path: str = "./simple_chroma_db"):
 """Initialize Simplified Vietnamese RAG system"""
 
 self.db_path = Path(db_path)
 self.db_path.mkdir(exist_ok=True)
 
 # Initialize ChromaDB
 logger.info(" Initializing ChromaDB...")
 self.client = chromadb.PersistentClient(path=str(self.db_path))
 
 # Collection names
 self.transactions_collection_name = "vietnamese_transactions_simple"
 self.knowledge_collection_name = "financial_knowledge_simple"
 
 # Initialize TF-IDF vectorizer for Vietnamese
 self.vectorizer = TfidfVectorizer(
 max_features=1000,
 ngram_range=(1, 2),
 min_df=1,
 max_df=0.8,
 lowercase=True,
 analyzer='word'
 )
 
 # Load classifier for enhanced context
 try:
 self.classifier = self._load_classifier()
 logger.info(" Vietnamese classifier loaded")
 except Exception as e:
 logger.warning(f" Classifier not available: {e}")
 self.classifier = None
 
 def _load_classifier(self):
 """Load the trained Vietnamese classifier"""
 with open("vietnamese_transaction_classifier.pkl", 'rb') as f:
 model_data = pickle.load(f)
 return model_data
 
 def preprocess_vietnamese_text(self, text: str) -> str:
 """Preprocess Vietnamese text"""
 text = text.lower().strip()
 text = re.sub(r'\s+', ' ', text)
 text = re.sub(r'[^\w\sàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ-]', ' ', text)
 
 try:
 tokens = word_tokenize(text)
 text = ' '.join(tokens)
 except:
 pass
 
 return text
 
 def create_transaction_embeddings(self, sample_size: int = 2000) -> Dict[str, Any]:
 """Create TF-IDF embeddings from Vietnamese transactions"""
 
 logger.info(f" Creating embeddings for {sample_size} transactions...")
 
 # Load quality sample
 with open("vietnamese_financial_quality_sample.json", 'r', encoding='utf-8') as f:
 transactions = json.load(f)
 
 # Limit sample size
 if len(transactions) > sample_size:
 transactions = transactions[:sample_size]
 
 logger.info(f" Processing {len(transactions)} transactions...")
 
 # Create or get collection
 try:
 collection = self.client.get_collection(self.transactions_collection_name)
 logger.info(" Using existing transactions collection")
 # Clear existing data
 collection.delete(where={})
 except:
 collection = self.client.create_collection(
 name=self.transactions_collection_name,
 metadata={"description": "Vietnamese financial transactions with TF-IDF embeddings"}
 )
 logger.info(" Created new transactions collection")
 
 # Prepare documents
 documents = []
 metadatas = []
 ids = []
 
 for i, transaction in enumerate(transactions):
 description = transaction.get('description', '')
 category = transaction.get('category', '')
 region = transaction.get('region', '')
 amount = transaction.get('amount', 0)
 
 # Preprocess description
 processed_desc = self.preprocess_vietnamese_text(description)
 
 # Enhanced document
 doc_text = f"{processed_desc} {category} {region}"
 
 metadata = {
 'category': category,
 'region': region,
 'amount': amount,
 'date': transaction.get('date', ''),
 'type': transaction.get('type', 'EXPENSE'),
 'original_description': description
 }
 
 documents.append(doc_text)
 metadatas.append(metadata)
 ids.append(f"tx_{i}_{uuid.uuid4().hex[:8]}")
 
 # Generate TF-IDF embeddings
 logger.info(" Generating TF-IDF embeddings...")
 tfidf_matrix = self.vectorizer.fit_transform(documents)
 
 # Convert to list of lists for ChromaDB
 embeddings = []
 for i in range(tfidf_matrix.shape[0]):
 row = tfidf_matrix[i].toarray()[0]
 embeddings.append(row.tolist())
 
 # Add to ChromaDB in batches
 batch_size = 100
 total_added = 0
 
 for i in range(0, len(documents), batch_size):
 batch_docs = documents[i:i+batch_size]
 batch_metas = metadatas[i:i+batch_size]
 batch_ids = ids[i:i+batch_size]
 batch_embeddings = embeddings[i:i+batch_size]
 
 try:
 collection.add(
 documents=batch_docs,
 metadatas=batch_metas,
 embeddings=batch_embeddings,
 ids=batch_ids
 )
 
 total_added += len(batch_docs)
 logger.info(f" Added batch {i//batch_size + 1}: {total_added}/{len(documents)} transactions")
 
 except Exception as e:
 logger.error(f" Error processing batch {i//batch_size + 1}: {e}")
 
 # Save vectorizer for later use
 with open("tfidf_vectorizer.pkl", 'wb') as f:
 pickle.dump(self.vectorizer, f)
 
 results = {
 "total_transactions": len(transactions),
 "embeddings_created": total_added,
 "collection_name": self.transactions_collection_name,
 "embedding_type": "TF-IDF",
 "dimensions": len(embeddings[0]) if embeddings else 0
 }
 
 logger.info(f" Transaction embeddings completed: {total_added} embeddings created")
 
 return results
 
 def create_knowledge_base(self) -> Dict[str, Any]:
 """Create Vietnamese financial knowledge base"""
 
 logger.info(" Creating Vietnamese financial knowledge base...")
 
 # Vietnamese financial knowledge
 financial_knowledge = [
 {
 "topic": "Phân loại giao dịch tài chính",
 "content": "Giao dịch tài chính được phân thành 8 loại chính: Chi tiêu ăn uống (food) bao gồm quán ăn, cà phê, bánh kẹo. Di chuyển giao thông (transport) như xe bus, taxi, Grab, xăng xe. Mua sắm (shopping) tại siêu thị, chợ, cửa hàng. Giải trí (entertainment) xem phim, karaoke, du lịch. Tiện ích sinh hoạt (utilities) như điện, nước, internet, điện thoại. Chăm sóc sức khỏe (healthcare) khám bệnh, mua thuốc, bảo hiểm. Giáo dục (education) học phí, sách vở, khóa học. Thu nhập (income) lương, thưởng, làm thêm.",
 "keywords": "phân loại giao dịch chi tiêu ăn uống transport shopping giải trí utilities healthcare education income",
 "category": "classification"
 },
 {
 "topic": "Quản lý ngân sách cá nhân hiệu quả",
 "content": "Nguyên tắc quản lý ngân sách 50/30/20 rất phổ biến: 50% thu nhập dành cho các nhu cầu thiết yếu như ăn uống, nhà ở, đi lại. 30% cho các mong muốn cá nhân như giải trí, mua sắm không cần thiết. 20% cho tiết kiệm và đầu tư tương lai. Theo dõi chi tiêu hàng ngày bằng sổ tay hoặc ứng dụng để kiểm soát tài chính hiệu quả. Lập danh sách mua sắm trước khi đi chợ để tránh chi tiêu bốc đồng.",
 "keywords": "quản lý ngân sách 50/30/20 tiết kiệm đầu tư theo dõi chi tiêu nhu cầu thiết yếu",
 "category": "budgeting"
 },
 {
 "topic": "Tiết kiệm và đầu tư cho người Việt",
 "content": "Các hình thức tiết kiệm phổ biến ở Việt Nam: Gửi tiết kiệm ngân hàng với lãi suất 6-8%/năm, mua vàng SJC để bảo toàn giá trị, đầu tư chứng khoán qua các công ty chứng khoán uy tín, mua bất động sản để cho thuê hoặc chờ tăng giá. Nên có quỹ khẩn cấp tương đương 3-6 tháng chi tiêu để đối phó với tình huống bất ngờ. Đầu tư theo nguyên tắc không bỏ trứng vào một giỏ, đa dạng hóa danh mục.",
 "keywords": "tiết kiệm đầu tư ngân hàng vàng chứng khoán bất động sản quỹ khẩn cấp đa dạng hóa",
 "category": "investment"
 },
 {
 "topic": "Thanh toán điện tử và ví điện tử",
 "content": "Các phương thức thanh toán điện tử phổ biến tại Việt Nam: Thẻ ATM của các ngân hàng lớn như Vietcombank, BIDV, Techcombank. Internet Banking để chuyển khoản và thanh toán hóa đơn. Ví điện tử như MoMo, ZaloPay, VNPay để thanh toán qua QR Code. Ưu điểm: tiện lợi, an toàn, theo dõi chi tiêu dễ dàng, có nhiều ưu đãi cashback. Lưu ý bảo mật: không chia sẻ mã PIN, OTP với người khác.",
 "keywords": "thanh toán điện tử ví điện tử MoMo ZaloPay VNPay QR Code internet banking thẻ ATM",
 "category": "payment"
 },
 {
 "topic": "Lập kế hoạch tài chính dài hạn",
 "content": "Các bước lập kế hoạch tài chính: 1) Xác định mục tiêu tài chính cụ thể như mua nhà, mua xe, con em du học. 2) Đánh giá tình hình tài chính hiện tại gồm thu nhập, chi phí, tài sản, nợ. 3) Lập ngân sách chi tiết theo tháng và năm. 4) Thực hiện kế hoạch và theo dõi tiến độ định kỳ. Nên đặt mục tiêu SMART: Cụ thể (Specific), Đo lường được (Measurable), Khả thi (Achievable), Liên quan (Relevant), Có thời hạn (Time-bound).",
 "keywords": "kế hoạch tài chính mục tiêu SMART ngân sách thu nhập chi phí tài sản nợ",
 "category": "planning"
 },
 {
 "topic": "Kiểm soát chi tiêu và cắt giảm chi phí",
 "content": "Cách kiểm soát chi tiêu hiệu quả: Ghi chép chi tiêu hàng ngày để biết tiền đi đâu. Phân loại chi tiêu theo mức độ cần thiết: Cần thiết (must have), Mong muốn (nice to have), Xa xỉ (luxury). Sử dụng phương pháp envelope budgeting: chia tiền mặt vào các phong bì theo mục đích. Tránh mua sắm bốc đồng bằng cách chờ 24 giờ trước khi mua hàng không cần thiết. Tận dụng các chương trình khuyến mãi, giảm giá để tiết kiệm.",
 "keywords": "kiểm soát chi tiêu ghi chép phân loại envelope budgeting khuyến mãi giảm giá",
 "category": "expense_control"
 },
 {
 "topic": "Đặc điểm tài chính cá nhân người Việt Nam",
 "content": "Người Việt Nam có những đặc điểm tài chính riêng: Ưu tiên tiết kiệm và tích lũy của cải, thích sử dụng tiền mặt hơn thẻ, đầu tư nhiều vào vàng và bất động sản vì tin tưởng giá trị bền vững, quan tâm đến bảo hiểm gia đình và giáo dục con em. Thường gửi tiền về gia đình ở quê, có thói quen mua sắm theo mùa và dịp lễ tết. Ngày càng sử dụng nhiều dịch vụ ngân hàng số và ví điện tử.",
 "keywords": "tài chính Việt Nam tiền mặt vàng bất động sản bảo hiểm gia đình giáo dục ngân hàng số",
 "category": "vietnamese_finance"
 },
 {
 "topic": "Mẹo tiết kiệm tiền hàng ngày thực tế",
 "content": "Các mẹo tiết kiệm đơn giản và hiệu quả: Nấu ăn tại nhà thay vì ăn ngoài để tiết kiệm 50-70% chi phí ăn uống. Sử dụng phương tiện công cộng như xe buýt, xe điện thay vì taxi hoặc Grab. Tắt các thiết bị điện không cần thiết để giảm hóa đơn tiền điện. Mua sắm theo danh sách đã lập sẵn để tránh mua thừa. Tận dụng các chương trình khuyến mãi, mua hàng vào thời điểm giảm giá. Sử dụng kupon, mã giảm giá khi mua sắm online.",
 "keywords": "mẹo tiết kiệm nấu ăn nhà phương tiện công cộng tắt điện danh sách mua sắm khuyến mãi kupon",
 "category": "saving_tips"
 }
 ]
 
 # Create or get knowledge collection
 try:
 knowledge_collection = self.client.get_collection(self.knowledge_collection_name)
 logger.info(" Using existing knowledge collection")
 knowledge_collection.delete(where={})
 except:
 knowledge_collection = self.client.create_collection(
 name=self.knowledge_collection_name,
 metadata={"description": "Vietnamese financial knowledge base with TF-IDF"}
 )
 logger.info(" Created new knowledge collection")
 
 # Prepare knowledge documents
 documents = []
 metadatas = []
 ids = []
 
 for i, knowledge in enumerate(financial_knowledge):
 # Combine content and keywords for better search
 doc_text = f"{knowledge['content']} {knowledge['keywords']}"
 processed_doc = self.preprocess_vietnamese_text(doc_text)
 
 documents.append(processed_doc)
 metadatas.append({
 'topic': knowledge['topic'],
 'category': knowledge['category'],
 'keywords': knowledge['keywords']
 })
 ids.append(f"kb_{i}_{knowledge['category']}")
 
 # Generate TF-IDF embeddings for knowledge (reuse fitted vectorizer)
 logger.info(" Generating knowledge base embeddings...")
 
 # Extend vocabulary if needed
 try:
 knowledge_tfidf = self.vectorizer.transform(documents)
 except:
 # If vocabulary doesn't cover knowledge documents, refit
 all_docs = documents # Use knowledge docs to fit
 knowledge_tfidf = self.vectorizer.fit_transform(all_docs)
 
 # Convert to embeddings
 knowledge_embeddings = []
 for i in range(knowledge_tfidf.shape[0]):
 row = knowledge_tfidf[i].toarray()[0]
 knowledge_embeddings.append(row.tolist())
 
 # Add to ChromaDB
 knowledge_collection.add(
 documents=documents,
 metadatas=metadatas,
 embeddings=knowledge_embeddings,
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
 """Query similar transactions"""
 
 logger.info(f" Querying transactions: '{query}'")
 
 # Load vectorizer
 try:
 with open("tfidf_vectorizer.pkl", 'rb') as f:
 vectorizer = pickle.load(f)
 except:
 logger.error("Vectorizer not found. Please run create_transaction_embeddings first.")
 return {"error": "Vectorizer not available"}
 
 # Get collection
 collection = self.client.get_collection(self.transactions_collection_name)
 
 # Process query
 processed_query = self.preprocess_vietnamese_text(query)
 query_vector = vectorizer.transform([processed_query])
 query_embedding = query_vector.toarray()[0].tolist()
 
 # Prepare where clause
 where_clause = {}
 if category_filter:
 where_clause = {"category": category_filter}
 
 # Query with embedding
 results = collection.query(
 query_embeddings=[query_embedding],
 n_results=n_results,
 where=where_clause if where_clause else None
 )
 
 response = {
 "query": query,
 "results_count": len(results['documents'][0]) if results['documents'] else 0,
 "transactions": []
 }
 
 # Format results
 if results['documents']:
 for i in range(len(results['documents'][0])):
 transaction = {
 "document": results['documents'][0][i],
 "metadata": results['metadatas'][0][i],
 "similarity": 1 - results['distances'][0][i],
 "id": results['ids'][0][i]
 }
 response["transactions"].append(transaction)
 
 return response
 
 def query_knowledge(self, query: str, n_results: int = 3) -> Dict[str, Any]:
 """Query financial knowledge base"""
 
 logger.info(f" Querying knowledge: '{query}'")
 
 # Get collection
 collection = self.client.get_collection(self.knowledge_collection_name)
 
 # Process query
 processed_query = self.preprocess_vietnamese_text(query)
 
 # Use simple text matching for knowledge queries
 results = collection.query(
 query_texts=[processed_query],
 n_results=n_results
 )
 
 response = {
 "query": query,
 "results_count": len(results['documents'][0]) if results['documents'] else 0,
 "knowledge": []
 }
 
 # Format results
 if results['documents']:
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
 """Generate financial advice using RAG"""
 
 logger.info(f" Generating advice for: '{query}'")
 
 # Query both transactions and knowledge
 similar_transactions = self.query_transactions(query, n_results=3)
 relevant_knowledge = self.query_knowledge(query, n_results=2)
 
 # Generate advice
 advice = {
 "query": query,
 "similar_transactions": similar_transactions,
 "relevant_knowledge": relevant_knowledge,
 "advice_summary": self._generate_advice_summary(query, similar_transactions, relevant_knowledge),
 "timestamp": datetime.now().isoformat()
 }
 
 return advice
 
 def _generate_advice_summary(self, query: str, transactions: Dict, knowledge: Dict) -> str:
 """Generate contextual advice summary"""
 
 query_lower = query.lower()
 
 # Extract transaction context
 categories = []
 amounts = []
 
 if transactions.get('transactions'):
 categories = [t['metadata'].get('category', '') for t in transactions['transactions']]
 amounts = [t['metadata'].get('amount', 0) for t in transactions['transactions']]
 
 # Context-aware advice generation
 if any(word in query_lower for word in ['chi tiêu', 'tiêu tiền', 'mua sắm']):
 if amounts:
 avg_amount = sum(amounts) / len(amounts)
 return f"Dựa trên dữ liệu tương tự, mức chi tiêu trung bình cho '{query}' là {avg_amount:,.0f} VND. Hãy so sánh với ngân sách và cân nhắc mức độ cần thiết trước khi chi tiêu."
 else:
 return "Để kiểm soát chi tiêu hiệu quả, hãy lập danh sách mua sắm, so sánh giá, và chỉ mua những gì thực sự cần thiết."
 
 elif any(word in query_lower for word in ['tiết kiệm', 'tích lũy']):
 return "Áp dụng quy tắc 50/30/20: 50% thu nhập cho nhu cầu thiết yếu, 30% cho mong muốn, 20% cho tiết kiệm. Bắt đầu bằng việc theo dõi chi tiêu hàng ngày và tìm các khoản có thể cắt giảm."
 
 elif any(word in query_lower for word in ['đầu tư', 'sinh lời']):
 return "Trước khi đầu tư, hãy có quỹ khẩn cấp 3-6 tháng chi tiêu. Người Việt Nam có thể bắt đầu với gửi tiết kiệm ngân hàng, sau đó tìm hiểu chứng khoán và đa dạng hóa danh mục đầu tư."
 
 elif any(word in query_lower for word in ['ngân sách', 'quản lý', 'kế hoạch']):
 return "Lập ngân sách theo quy tắc SMART: mục tiêu Cụ thể, Đo lường được, Khả thi, Liên quan và Có thời hạn. Ghi chép chi tiêu hàng ngày và đánh giá lại hàng tháng."
 
 else:
 # General advice
 if categories:
 main_category = max(set(categories), key=categories.count) if categories else None
 return f"Dựa trên phân tích, giao dịch liên quan chủ yếu thuộc category '{main_category}'. Hãy theo dõi chi tiêu trong category này và đặt giới hạn phù hợp với thu nhập."
 else:
 return "Để quản lý tài chính hiệu quả, hãy: 1) Ghi chép chi tiêu hàng ngày, 2) Lập ngân sách rõ ràng, 3) Đặt mục tiêu tiết kiệm cụ thể, 4) Đa dạng hóa nguồn thu nhập."

def main():
 """Main RAG setup interface"""
 
 print(" Simple Vietnamese Financial RAG System")
 print("=" * 50)
 
 # Initialize RAG system
 rag = SimpleVietnameseRAG()
 
 # Create transaction embeddings
 print(" Creating transaction embeddings...")
 tx_results = rag.create_transaction_embeddings(sample_size=2000)
 print(f" Transaction embeddings: {tx_results['embeddings_created']} created")
 
 # Create knowledge base
 print(" Creating knowledge base...")
 kb_results = rag.create_knowledge_base()
 print(f" Knowledge base: {kb_results['knowledge_items']} items created")
 
 # Test queries
 print(f"\n Testing RAG system...")
 
 test_queries = [
 "Chi tiêu ăn uống hàng ngày",
 "Cách tiết kiệm tiền hiệu quả", 
 "Đầu tư chứng khoán cho người mới",
 "Quản lý ngân sách gia đình",
 "Mua sắm thông minh"
 ]
 
 for query in test_queries:
 print(f"\n❓ Query: '{query}'")
 advice = rag.get_financial_advice(query)
 print(f" Advice: {advice['advice_summary']}")
 
 # Show similar transactions
 if advice['similar_transactions']['transactions']:
 print(" Similar transactions:")
 for tx in advice['similar_transactions']['transactions'][:2]:
 desc = tx['metadata'].get('original_description', tx['document'])[:60]
 category = tx['metadata'].get('category', '')
 amount = tx['metadata'].get('amount', 0)
 print(f" - {desc}... [{category}] {amount:,} VND")
 
 print(f"\n Simple Vietnamese Financial RAG system is ready!")
 print(f" Total embeddings created: {tx_results['embeddings_created'] + kb_results['knowledge_items']}")
 print(f" System ready for integration with FastAPI!")

if __name__ == "__main__":
 main()