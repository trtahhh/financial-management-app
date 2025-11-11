"""
Smart Financial Planning Service API
Testing version without AI dependencies
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
import uvicorn
import logging
from datetime import datetime

# Import Smart Planning Service only
try:
    from smart_planning_service import SmartPlanningService
    planning_service = SmartPlanningService()
    print("Smart Planning Service loaded successfully")
    PLANNING_AVAILABLE = True
except Exception as e:
    print(f"Error loading Planning Service: {e}")
    planning_service = None
    PLANNING_AVAILABLE = False

app = FastAPI(
    title="Smart Financial Planning Service",
    description="AI service for smart financial planning and savings recommendations",
    version="2.0.0"
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://localhost:3000", "http://127.0.0.1:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

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

# Health check endpoint
@app.get("/health")
async def health_check():
    """Health check with planning service status"""
    return {
        "status": "healthy",
        "service": "smart-financial-planning",
        "planning_available": PLANNING_AVAILABLE,
        "version": "2.0.0",
        "timestamp": datetime.now().isoformat()
    }

# Smart Financial Planning endpoint
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

# Quick spending insights endpoint
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

# Savings recommendations endpoint  
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

if __name__ == "__main__":
    uvicorn.run(
        "main_planning:app", 
        host="0.0.0.0", 
        port=8001, 
        reload=True,
        log_level="info"
    )