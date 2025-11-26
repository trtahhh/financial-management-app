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

# Import Ultra Enhanced Planning Service (9/10 libraries working!)
try:
    from ultra_enhanced_planning_service import UltraEnhancedPlanningService
    ultra_service = UltraEnhancedPlanningService()
    logger = logging.getLogger(__name__)
    logger.info("🚀 Ultra Planning Service loaded with 9/10 advanced libraries!")
    ULTRA_AVAILABLE = True
except Exception as e:
    print(f"Ultra Planning failed, falling back to Enhanced: {e}")
    ultra_service = None
    ULTRA_AVAILABLE = False

# Fallback to Enhanced Planning Service
try:
    from enhanced_planning_service import EnhancedPlanningService
    planning_service = EnhancedPlanningService()
    logger = logging.getLogger(__name__)
    logger.info("Enhanced Planning Service loaded successfully!")
    PLANNING_AVAILABLE = True
except Exception as e:
    print(f"Enhanced Planning failed, falling back to Smart Planning: {e}")
    try:
        from smart_planning_service import SmartPlanningService
        planning_service = SmartPlanningService()
        logger = logging.getLogger(__name__)
        logger.info("Smart Planning Service loaded successfully")
        PLANNING_AVAILABLE = True
    except Exception as e2:
        print(f"Error loading Planning Service: {e2}")
        planning_service = None
        PLANNING_AVAILABLE = False

app = FastAPI(
    title="Ultra Vietnamese Financial AI Service with 9 Advanced ML Libraries",
    description="Complete AI with XGBoost, LightGBM, Prophet, SHAP, Optuna, SMOTE, VADER, TextBlob, Word2Vec",
    version="3.0.0"
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
    prediction_next_month: float
    recommendation: str
    severity: str
    confidence_score: float
    seasonality_pattern: Optional[str] = None

class SavingsRecommendationModel(BaseModel):
    title: str
    description: str
    potential_savings: float
    difficulty: str
    timeframe: str
    action_steps: List[str]
    priority_score: float
    category_impact: Dict[str, float]
    personalized_tips: List[str]

class GoalPlanModel(BaseModel):
    goal_name: str
    target_amount: float
    current_amount: float
    monthly_required: float
    deadline: str
    feasibility: str
    feasibility_score: float
    recommendations: List[str]
    risk_factors: List[str]
    milestones: List[Dict]
    alternative_strategies: List[str]

class FinancialPlanResponse(BaseModel):
    monthly_income: float
    total_spending: float
    savings_rate: float
    spending_insights: List[SpendingInsightModel]
    savings_recommendations: List[SavingsRecommendationModel]
    goal_plans: List[GoalPlanModel]
    overall_score: float
    next_actions: List[str]
    financial_health_metrics: Dict
    risk_assessment: Dict
    success: bool = True
    timestamp: str

class SystemStatsResponse(BaseModel):
    ai_available: bool
    planning_available: bool
    ultra_available: bool
    classifier_available: bool
    knowledge_base_items: int
    classifier_accuracy: Optional[float] = None
    supported_categories: List[str]
    features_available: List[str]
    ml_libraries: Dict[str, bool]
    version: str = "3.0.0"

# Health check endpoint
@app.get("/health")
async def health_check():
    """Health check with AI system status"""
    return {
        "status": "healthy",
        "service": "ultra-vietnamese-financial-ai-planning",
        "ai_available": AI_AVAILABLE,
        "planning_available": PLANNING_AVAILABLE,
        "ultra_available": ULTRA_AVAILABLE,
        "ml_libraries": {
            "xgboost": ULTRA_AVAILABLE,
            "lightgbm": ULTRA_AVAILABLE,
            "prophet": ULTRA_AVAILABLE,
            "shap": ULTRA_AVAILABLE,
            "optuna": ULTRA_AVAILABLE,
            "smote": ULTRA_AVAILABLE,
            "vader": ULTRA_AVAILABLE,
            "textblob": ULTRA_AVAILABLE,
            "word2vec": ULTRA_AVAILABLE
        },
        "version": "3.0.0",
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
    
    if ULTRA_AVAILABLE:
        features_available.extend([
            "Ensemble Predictions (XGBoost + LightGBM)",
            "Auto Hyperparameter Tuning (Optuna)",
            "Time Series Forecasting (Prophet)",
            "AI Explainability (SHAP)",
            "Imbalanced Data Handling (SMOTE)",
            "Sentiment Analysis (TextBlob + VADER)",
            "Word Embeddings (Word2Vec)",
            "User Clustering (KMeans)"
        ])
    
    if not AI_AVAILABLE:
        return SystemStatsResponse(
            ai_available=False,
            planning_available=PLANNING_AVAILABLE,
            ultra_available=ULTRA_AVAILABLE,
            classifier_available=False,
            knowledge_base_items=0,
            supported_categories=[],
            features_available=features_available,
            ml_libraries={
                "xgboost": ULTRA_AVAILABLE,
                "lightgbm": ULTRA_AVAILABLE,
                "prophet": ULTRA_AVAILABLE,
                "shap": ULTRA_AVAILABLE,
                "optuna": ULTRA_AVAILABLE,
                "smote": ULTRA_AVAILABLE,
                "vader": ULTRA_AVAILABLE,
                "textblob": ULTRA_AVAILABLE,
                "word2vec": ULTRA_AVAILABLE
            }
        )
    
    stats = vietnamese_ai.get_system_stats()
    return SystemStatsResponse(
        ai_available=True,
        planning_available=PLANNING_AVAILABLE,
        ultra_available=ULTRA_AVAILABLE,
        classifier_available=stats.get('classifier_available', False),
        knowledge_base_items=stats.get('knowledge_base_items', 0),
        classifier_accuracy=stats.get('classifier_accuracy'),
        supported_categories=stats.get('supported_categories', []),
        features_available=features_available,
        ml_libraries={
            "xgboost": ULTRA_AVAILABLE,
            "lightgbm": ULTRA_AVAILABLE,
            "prophet": ULTRA_AVAILABLE,
            "shap": ULTRA_AVAILABLE,
            "optuna": ULTRA_AVAILABLE,
            "smote": ULTRA_AVAILABLE,
            "vader": ULTRA_AVAILABLE,
            "textblob": ULTRA_AVAILABLE,
            "word2vec": ULTRA_AVAILABLE
        }
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
            financial_health_metrics=plan.financial_health_metrics,
            risk_assessment=plan.risk_assessment,
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

# Explain AI recommendation endpoint
@app.post("/planning/explain-recommendation")
async def explain_recommendation(
    recommendation: SavingsRecommendationModel,
    spending_insights: List[SpendingInsightModel],
    income: float
):
    """Explain why AI made a specific recommendation using SHAP"""
    if not PLANNING_AVAILABLE:
        raise HTTPException(status_code=503, detail="Planning service not available")
    
    try:
        from enhanced_planning_service import SavingsRecommendation, SpendingInsight
        
        # Convert models to dataclasses
        rec = SavingsRecommendation(**recommendation.dict())
        insights = [SpendingInsight(**insight.dict()) for insight in spending_insights]
        
        # Get explanation
        explanation = planning_service.explain_recommendation(rec, insights, income)
        
        return {
            "success": True,
            "explanation": explanation,
            "timestamp": datetime.now().isoformat()
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Explanation error: {str(e)}")

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
        app,  # Direct app object instead of string
        host="0.0.0.0", 
        port=8001, 
        reload=False,  # Disable reload to avoid import issues
        log_level="info"
    )


# ========================================
# ULTRA ENHANCED ENDPOINTS (9 ML Libraries)
# ========================================

class UltraInsightRequest(BaseModel):
    """Request for ultra insights with all 9 libraries"""
    transactions: List[Dict[str, Any]]
    monthly_income: float
    user_preferences: Optional[Dict[str, Any]] = {}
    enable_shap: bool = True
    enable_prophet: bool = True
    enable_optuna: bool = False  # Slow, only on demand

class UltraInsightResponse(BaseModel):
    """Response with ultra ML insights"""
    ensemble_predictions: Dict[str, Any]
    prophet_forecasts: Dict[str, Any]
    sentiment_analysis: Dict[str, Any]
    shap_explanations: Dict[str, Any]
    user_cluster: int
    similar_categories: List[Dict[str, str]]
    optimization_suggestions: List[str]
    confidence_scores: Dict[str, float]
    timestamp: str

class OptunaOptimizeRequest(BaseModel):
    """Request for Optuna hyperparameter optimization"""
    transactions: List[Dict[str, Any]]
    n_trials: int = 20
    optimization_metric: str = "accuracy"

class SentimentRequest(BaseModel):
    """Request for sentiment analysis"""
    texts: List[str]

class Word2VecRequest(BaseModel):
    """Request for Word2Vec similarity"""
    word1: str
    word2: str
    transactions: List[Dict[str, Any]]  # For training

@app.post("/ultra/generate-insights", response_model=UltraInsightResponse)
async def generate_ultra_insights(request: UltraInsightRequest):
    """
    🚀 ULTRA ENDPOINT - Generate comprehensive insights using all 9 ML libraries
    
    Features:
    - Ensemble predictions (XGBoost + LightGBM)
    - Prophet time series forecasting
    - Sentiment analysis (TextBlob + VADER)
    - SHAP explanations
    - Word2Vec semantic similarity
    - User clustering
    - SMOTE for imbalanced data
    """
    if not ULTRA_AVAILABLE:
        raise HTTPException(
            status_code=503, 
            detail="Ultra service not available. Using enhanced service instead."
        )
    
    try:
        # Generate ultra insights
        insights = ultra_service.generate_ultra_insights(
            transactions=request.transactions,
            income=request.monthly_income,
            user_preferences=request.user_preferences
        )
        
        return UltraInsightResponse(
            ensemble_predictions=insights.get('ensemble_predictions', {}),
            prophet_forecasts=insights.get('prophet_forecasts', {}),
            sentiment_analysis=insights.get('sentiment_analysis', {}),
            shap_explanations=insights.get('shap_explanations', {}) if request.enable_shap else {},
            user_cluster=insights.get('user_cluster', 0),
            similar_categories=insights.get('similar_categories', []),
            optimization_suggestions=insights.get('optimization_suggestions', []),
            confidence_scores=insights.get('confidence_scores', {}),
            timestamp=datetime.now().isoformat()
        )
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Ultra insights error: {str(e)}")


@app.post("/ultra/optimize-hyperparameters")
async def optimize_hyperparameters(request: OptunaOptimizeRequest):
    """
    🎯 Optuna Hyperparameter Tuning
    
    Auto-tune XGBoost/LightGBM parameters for best performance.
    Warning: Can take 1-2 minutes for 20 trials.
    """
    if not ULTRA_AVAILABLE:
        raise HTTPException(status_code=503, detail="Ultra service not available")
    
    try:
        # Prepare data from transactions
        X_train, y_train = ultra_service._prepare_features_from_transactions(
            request.transactions
        )
        
        # Run Optuna optimization
        best_params = ultra_service.optimize_hyperparameters(
            X_train, 
            y_train,
            n_trials=request.n_trials
        )
        
        return {
            "success": True,
            "best_params": best_params,
            "n_trials": request.n_trials,
            "optimization_metric": request.optimization_metric,
            "message": "Hyperparameters optimized successfully",
            "timestamp": datetime.now().isoformat()
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Optuna optimization error: {str(e)}")


@app.post("/ultra/sentiment-analysis")
async def analyze_sentiment_batch(request: SentimentRequest):
    """
    😊 Dual Sentiment Analysis (TextBlob + VADER)
    
    Analyzes sentiment of transaction descriptions.
    Returns polarity score (-1 to +1) and compound score.
    """
    if not ULTRA_AVAILABLE:
        raise HTTPException(status_code=503, detail="Ultra service not available")
    
    try:
        results = []
        for text in request.texts:
            sentiment_score, label = ultra_service.analyze_sentiment(text)
            results.append({
                "text": text,
                "sentiment_score": sentiment_score,
                "label": label,
                "category": "positive" if sentiment_score > 0.2 else "negative" if sentiment_score < -0.2 else "neutral"
            })
        
        return {
            "success": True,
            "total_analyzed": len(results),
            "results": results,
            "average_sentiment": sum(r['sentiment_score'] for r in results) / len(results) if results else 0,
            "timestamp": datetime.now().isoformat()
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Sentiment analysis error: {str(e)}")


@app.post("/ultra/word-similarity")
async def get_word_similarity(request: Word2VecRequest):
    """
    🔤 Word2Vec Semantic Similarity
    
    Train Word2Vec on transaction descriptions and find similarity between words.
    Useful for category suggestions and typo detection.
    """
    if not ULTRA_AVAILABLE:
        raise HTTPException(status_code=503, detail="Ultra service not available")
    
    try:
        # Train Word2Vec if transactions provided
        if request.transactions:
            ultra_service.train_word_embeddings(request.transactions)
        
        # Get similarity
        similarity = ultra_service.get_word_similarity(request.word1, request.word2)
        
        return {
            "success": True,
            "word1": request.word1,
            "word2": request.word2,
            "similarity": similarity,
            "interpretation": "Very similar" if similarity > 0.7 else "Similar" if similarity > 0.4 else "Different",
            "trained_on": len(request.transactions) if request.transactions else 0,
            "timestamp": datetime.now().isoformat()
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Word similarity error: {str(e)}")


@app.post("/ultra/prophet-forecast")
async def forecast_with_prophet(
    category: str,
    transactions: List[Dict[str, Any]],
    periods_ahead: int = 3
):
    """
    📈 Prophet Time Series Forecasting
    
    Forecast future spending for a specific category using Facebook Prophet.
    Detects weekly/monthly seasonality and trends.
    """
    if not ULTRA_AVAILABLE:
        raise HTTPException(status_code=503, detail="Ultra service not available")
    
    try:
        # Filter transactions by category
        category_txns = [t for t in transactions if t.get('category') == category]
        
        if len(category_txns) < 7:
            return {
                "success": False,
                "error": "Need at least 7 transactions for Prophet forecasting",
                "category": category,
                "transactions_found": len(category_txns)
            }
        
        # Use Prophet from enhanced_planning_service
        forecast_result = planning_service._forecast_with_prophet(category, category_txns)
        
        return {
            "success": True,
            "category": category,
            "forecast": forecast_result,
            "transactions_analyzed": len(category_txns),
            "periods_ahead": periods_ahead,
            "timestamp": datetime.now().isoformat()
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prophet forecast error: {str(e)}")


@app.post("/ultra/shap-explain")
async def explain_with_shap(
    model_type: str,
    features: List[Dict[str, float]],
    feature_names: List[str]
):
    """
    🔍 SHAP AI Explainability
    
    Explain why the AI made specific predictions using SHAP values.
    Shows feature importance and contribution to predictions.
    """
    if not ULTRA_AVAILABLE:
        raise HTTPException(status_code=503, detail="Ultra service not available")
    
    try:
        # Convert features to numpy array
        import numpy as np
        X = np.array([list(f.values()) for f in features])
        
        # Get SHAP explanations
        if model_type == "xgboost":
            model = ultra_service.xgb_model
        elif model_type == "lightgbm":
            model = ultra_service.lgb_model
        else:
            raise ValueError(f"Unknown model type: {model_type}")
        
        shap_values = ultra_service.explain_with_shap(model, X, feature_names)
        
        return {
            "success": True,
            "model_type": model_type,
            "feature_importance": shap_values,
            "total_features": len(feature_names),
            "timestamp": datetime.now().isoformat()
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"SHAP explanation error: {str(e)}")


@app.post("/ultra/cluster-users")
async def cluster_users(
    user_features: List[Dict[str, float]],
    n_clusters: int = 3
):
    """
    👥 User Clustering with KMeans
    
    Group users into clusters based on spending patterns.
    Useful for personalized recommendations.
    """
    if not ULTRA_AVAILABLE:
        raise HTTPException(status_code=503, detail="Ultra service not available")
    
    try:
        import numpy as np
        X = np.array([list(f.values()) for f in user_features])
        
        cluster_labels = ultra_service.cluster_users(X, n_clusters)
        
        return {
            "success": True,
            "total_users": len(user_features),
            "n_clusters": n_clusters,
            "cluster_labels": cluster_labels.tolist(),
            "cluster_distribution": {
                str(i): int((cluster_labels == i).sum()) 
                for i in range(n_clusters)
            },
            "timestamp": datetime.now().isoformat()
        }
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Clustering error: {str(e)}")