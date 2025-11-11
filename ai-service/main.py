"""
Enhanced Vietnamese Financial AI Service with Smart Planning
Complete RAG + Classification + Financial Planning system
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
import uvicorn
import logging
from datetime import datetime

# Import our enhanced Vietnamese AI system (temporarily disabled)
AI_AVAILABLE = False
vietnamese_ai = None
print("Vietnamese AI temporarily disabled - focusing on Planning features")

# Import Smart Planning Service
try:
    from smart_planning_service import SmartPlanningService
    planning_service = SmartPlanningService()
    logger = logging.getLogger(__name__)
    logger.info("Smart Planning Service loaded successfully")
    PLANNING_AVAILABLE = True
except Exception as e:
    print(f"Error loading Planning Service: {e}")
    planning_service = None
    PLANNING_AVAILABLE = False

app = FastAPI(
    title="Enhanced Vietnamese Financial AI Service with Smart Planning",
    description="Complete AI service with transaction classification, RAG-based advice, and smart financial planning",
    version="2.1.0"
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://localhost:3000", "http://127.0.0.1:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Enhanced Request/Response models
class TransactionRequest(BaseModel):
    description: str
    amount: Optional[float] = None
    user_id: Optional[int] = None
    date: Optional[str] = None

class ClassificationResponse(BaseModel):
    predicted_category: str
    confidence: float
    description: str
    all_probabilities: Dict[str, float]
    processed_description: str
    success: bool = True

class AdviceRequest(BaseModel):
    query: str
    user_id: Optional[int] = None
    category: Optional[str] = None
    financial_context: Optional[Dict[str, Any]] = {}

class AdviceResponse(BaseModel):
    query: str
    advice_summary: str
    relevant_knowledge: List[Dict[str, Any]]
    classification: Optional[Dict[str, Any]] = None
    timestamp: str
    success: bool = True

class ChatRequest(BaseModel):
    message: str
    user_id: Optional[int] = None
    context: Optional[List[str]] = []

class ChatResponse(BaseModel):
    response: str
    advice_type: str
    confidence: float
    timestamp: str
    success: bool = True

# Smart Planning Request/Response Models
class PlanningRequest(BaseModel):
    transactions: List[Dict[str, Any]]
    monthly_income: float
    goals: Optional[List[Dict[str, Any]]] = []
    user_id: Optional[int] = None

class SpendingInsightModel(BaseModel):
    category: str
    amount: float
    percentage: float
    trend: str
    recommendation: str
    severity: str

class SavingsRecommendationModel(BaseModel):
    title: str
    description: str
    potential_savings: float
    difficulty: str
    timeframe: str
    action_steps: List[str]

class GoalPlanModel(BaseModel):
    goal_name: str
    target_amount: float
    current_amount: float
    monthly_required: float
    deadline: str
    feasibility: str
    recommendations: List[str]

class FinancialPlanResponse(BaseModel):
    monthly_income: float
    total_spending: float
    savings_rate: float
    spending_insights: List[SpendingInsightModel]
    savings_recommendations: List[SavingsRecommendationModel]
    goal_plans: List[GoalPlanModel]
    overall_score: float
    next_actions: List[str]
    success: bool = True
    timestamp: str

class SystemStatsResponse(BaseModel):
    ai_available: bool
    planning_available: bool
    classifier_available: bool
    knowledge_base_items: int
    classifier_accuracy: Optional[float] = None
    supported_categories: List[str]
    features_available: List[str]
    version: str = "2.1.0"

# Health check endpoint
@app.get("/health")
async def health_check():
    """Health check with AI system status"""
    return {
        "status": "healthy",
        "service": "enhanced-vietnamese-financial-ai-planning",
        "ai_available": AI_AVAILABLE,
        "planning_available": PLANNING_AVAILABLE,
        "version": "2.1.0",
        "timestamp": datetime.now().isoformat()
    }

# System statistics endpoint
@app.get("/stats", response_model=SystemStatsResponse)
async def get_system_stats():
    """Get comprehensive AI system statistics"""
    features_available = ["Health Check", "System Stats"]
    
    if AI_AVAILABLE:
        features_available.extend([
            "Transaction Classification",
            "Financial Advice",
            "Chat AI",
            "Knowledge Search",
            "Batch Classification"
        ])
    
    if PLANNING_AVAILABLE:
        features_available.extend([
            "Smart Financial Planning",
            "Spending Analysis", 
            "Savings Recommendations",
            "Goal Planning"
        ])
    
    if not AI_AVAILABLE:
        return SystemStatsResponse(
            ai_available=False,
            planning_available=PLANNING_AVAILABLE,
            classifier_available=False,
            knowledge_base_items=0,
            supported_categories=[],
            features_available=features_available
        )
    
    stats = vietnamese_ai.get_system_stats()
    return SystemStatsResponse(
        ai_available=True,
        planning_available=PLANNING_AVAILABLE,
        classifier_available=stats.get('classifier_available', False),
        knowledge_base_items=stats.get('knowledge_base_items', 0),
        classifier_accuracy=stats.get('classifier_accuracy'),
        supported_categories=stats.get('supported_categories', []),
        features_available=features_available
    )

# Enhanced transaction classification endpoint
@app.post("/classify", response_model=ClassificationResponse)
async def classify_transaction(request: TransactionRequest):
    """Classify Vietnamese transaction description using trained ML model"""
    if not AI_AVAILABLE:
        raise HTTPException(status_code=503, detail="AI system not available")
    
    try:
        result = vietnamese_ai.classify_transaction(request.description)
        
        if 'error' in result:
            raise HTTPException(status_code=500, detail=result['error'])
        
        return ClassificationResponse(**result)
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Classification error: {str(e)}")

# Enhanced financial advice endpoint with RAG
@app.post("/advice", response_model=AdviceResponse)
async def get_financial_advice(request: AdviceRequest):
    """Generate comprehensive financial advice using RAG methodology"""
    if not AI_AVAILABLE:
        raise HTTPException(status_code=503, detail="AI system not available")
    
    try:
        advice = vietnamese_ai.get_financial_advice(request.query)
        return AdviceResponse(**advice)
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Advice generation error: {str(e)}")

# Enhanced chat endpoint for conversational AI
@app.post("/chat", response_model=ChatResponse)
async def chat_with_ai(request: ChatRequest):
    """Conversational AI for Vietnamese financial queries"""
    if not AI_AVAILABLE:
        raise HTTPException(status_code=503, detail="AI system not available")
    
    try:
        # Use the advice system for chat responses
        advice = vietnamese_ai.get_financial_advice(request.message)
        
        # Determine advice type based on query
        advice_type = "general"
        query_lower = request.message.lower()
        
        if any(word in query_lower for word in ['chi tiêu', 'mua sắm']):
            advice_type = "expense_control"
        elif any(word in query_lower for word in ['tiết kiệm', 'tích lũy']):
            advice_type = "saving"
        elif any(word in query_lower for word in ['đầu tư', 'sinh lời']):
            advice_type = "investment"
        elif any(word in query_lower for word in ['ngân sách', 'kế hoạch']):
            advice_type = "planning"
        elif any(word in query_lower for word in ['thanh toán', 'ví điện tử']):
            advice_type = "payment"
        
        # Calculate confidence based on knowledge relevance
        confidence = 0.8 if advice['relevant_knowledge'] else 0.5
        
        return ChatResponse(
            response=advice['advice_summary'],
            advice_type=advice_type,
            confidence=confidence,
            timestamp=advice['timestamp']
        )
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Chat error: {str(e)}")

# NEW: Smart Financial Planning endpoint
@app.post("/planning/analyze", response_model=FinancialPlanResponse)
async def analyze_financial_plan(request: PlanningRequest):
    """Generate comprehensive financial plan with spending analysis and recommendations"""
    if not PLANNING_AVAILABLE:
        raise HTTPException(status_code=503, detail="Planning service not available")
    
    try:
        # Generate comprehensive financial plan
        plan = planning_service.generate_comprehensive_plan(
            transactions=request.transactions,
            income=request.monthly_income,
            goals=request.goals or []
        )
        
        # Convert to response format
        response = FinancialPlanResponse(
            monthly_income=plan.monthly_income,
            total_spending=plan.total_spending,
            savings_rate=plan.savings_rate,
            spending_insights=[
                SpendingInsightModel(**insight.__dict__) 
                for insight in plan.spending_insights
            ],
            savings_recommendations=[
                SavingsRecommendationModel(**rec.__dict__)
                for rec in plan.savings_recommendations
            ],
            goal_plans=[
                GoalPlanModel(**goal.__dict__)
                for goal in plan.goal_plans
            ],
            overall_score=plan.overall_score,
            next_actions=plan.next_actions,
            timestamp=datetime.now().isoformat()
        )
        
        return response
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Planning analysis error: {str(e)}")

# NEW: Quick spending insights endpoint
@app.post("/planning/spending-insights")
async def get_spending_insights(transactions: List[Dict[str, Any]], income: float):
    """Get quick spending pattern insights"""
    if not PLANNING_AVAILABLE:
        raise HTTPException(status_code=503, detail="Planning service not available")
    
    try:
        analysis = planning_service.analyze_spending_pattern(transactions)
        insights = planning_service.generate_spending_insights(analysis, income)
        
        return {
            "total_spending": analysis.get('total_spending', 0),
            "categories": analysis.get('categories', {}),
            "insights": [insight.__dict__ for insight in insights],
            "timestamp": datetime.now().isoformat()
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Spending insights error: {str(e)}")

# NEW: Savings recommendations endpoint  
@app.post("/planning/savings-recommendations")
async def get_savings_recommendations(transactions: List[Dict[str, Any]], income: float):
    """Get personalized savings recommendations"""
    if not PLANNING_AVAILABLE:
        raise HTTPException(status_code=503, detail="Planning service not available")
    
    try:
        analysis = planning_service.analyze_spending_pattern(transactions)
        recommendations = planning_service.generate_savings_recommendations(analysis, income)
        
        return {
            "recommendations": [rec.__dict__ for rec in recommendations],
            "total_potential_savings": sum(rec.potential_savings for rec in recommendations),
            "timestamp": datetime.now().isoformat()
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Savings recommendations error: {str(e)}")

# Batch classification endpoint
@app.post("/classify-batch")
async def classify_batch_transactions(transactions: List[TransactionRequest]):
    """Classify multiple transactions in batch for better performance"""
    if not AI_AVAILABLE:
        raise HTTPException(status_code=503, detail="AI system not available")
    
    try:
        results = []
        for transaction in transactions:
            result = vietnamese_ai.classify_transaction(transaction.description)
            if 'error' not in result:
                results.append({
                    "description": transaction.description,
                    "amount": transaction.amount,
                    **result
                })
            else:
                results.append({
                    "description": transaction.description,
                    "error": result['error']
                })
        
        return {
            "total_processed": len(transactions),
            "successful_classifications": len([r for r in results if 'error' not in r]),
            "results": results
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Batch classification error: {str(e)}")

# Knowledge search endpoint
@app.get("/knowledge/search")
async def search_knowledge(query: str, limit: int = 3):
    """Search Vietnamese financial knowledge base"""
    if not AI_AVAILABLE:
        raise HTTPException(status_code=503, detail="AI system not available")
    
    try:
        results = vietnamese_ai.search_knowledge(query, top_k=limit)
        return {
            "query": query,
            "results_count": len(results),
            "knowledge_items": results
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Knowledge search error: {str(e)}")

if __name__ == "__main__":
    uvicorn.run(
        "main:app", 
        host="0.0.0.0", 
        port=8001, 
        reload=True,
        log_level="info"
    )