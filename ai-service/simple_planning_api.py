"""
Simple Planning API Service
Working version for integration testing
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
import uvicorn
from datetime import datetime
import random

app = FastAPI(
    title="Simple Planning API",
    description="Working Planning API for integration testing", 
    version="1.0.0"
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class PlanningRequest(BaseModel):
    transactions: List[Dict[str, Any]]
    monthly_income: float
    goals: Optional[List[Dict[str, Any]]] = []
    user_id: Optional[int] = None

class SpendingInsight(BaseModel):
    category: str
    amount: float
    percentage: float
    trend: str
    recommendation: str
    severity: str

class SavingsRecommendation(BaseModel):
    title: str
    description: str
    potential_savings: float
    difficulty: str
    timeframe: str
    action_steps: List[str]

class GoalPlan(BaseModel):
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
    spending_insights: List[SpendingInsight]
    savings_recommendations: List[SavingsRecommendation] 
    goal_plans: List[GoalPlan]
    overall_score: float
    next_actions: List[str]
    success: bool = True
    timestamp: str

@app.get("/")
async def root():
    return {"message": "Simple Planning API is running", "status": "ok"}

@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "service": "simple-planning-api",
        "timestamp": datetime.now().isoformat(),
        "features": ["Planning Analysis", "Spending Insights", "Savings Recommendations"]
    }

@app.post("/planning/analyze", response_model=FinancialPlanResponse)
async def analyze_financial_plan(request: PlanningRequest):
    """Generate comprehensive financial plan"""
    try:
        # Calculate basic stats
        total_spending = sum(t.get('amount', 0) for t in request.transactions if t.get('type') == 'EXPENSE')
        savings_rate = ((request.monthly_income - total_spending) / request.monthly_income * 100) if request.monthly_income > 0 else 0
        
        # Generate spending insights
        spending_insights = generate_spending_insights(request.transactions, request.monthly_income)
        
        # Generate savings recommendations
        savings_recommendations = generate_savings_recommendations(request.transactions, request.monthly_income)
        
        # Generate goal plans
        goal_plans = generate_goal_plans(request.goals, request.monthly_income, total_spending)
        
        # Generate next actions
        next_actions = generate_next_actions(savings_rate, len(request.goals))
        
        # Calculate overall score
        overall_score = calculate_financial_score(savings_rate, len(spending_insights), len(request.goals))
        
        response = FinancialPlanResponse(
            monthly_income=request.monthly_income,
            total_spending=total_spending,
            savings_rate=max(0, savings_rate),
            spending_insights=spending_insights,
            savings_recommendations=savings_recommendations,
            goal_plans=goal_plans,
            overall_score=overall_score,
            next_actions=next_actions,
            timestamp=datetime.now().isoformat()
        )
        
        return response
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Planning analysis error: {str(e)}")

def generate_spending_insights(transactions: List[Dict], monthly_income: float) -> List[SpendingInsight]:
    """Generate AI-powered spending insights"""
    
    # Group transactions by category
    category_totals = {}
    total_spending = 0
    
    for transaction in transactions:
        if transaction.get('type') == 'EXPENSE':
            category = transaction.get('category', 'Khác')
            amount = transaction.get('amount', 0)
            category_totals[category] = category_totals.get(category, 0) + amount
            total_spending += amount
    
    insights = []
    
    for category, amount in category_totals.items():
        percentage = (amount / total_spending * 100) if total_spending > 0 else 0
        
        # Determine severity and trend
        if percentage > 30:
            severity = "high"
            trend = "Tăng cao"
            recommendation = f"Chi tiêu {category.lower()} chiếm {percentage:.1f}% ngân sách. Cần giảm bớt để cải thiện tài chính."
        elif percentage > 15:
            severity = "medium" 
            trend = "Ổn định"
            recommendation = f"Chi tiêu {category.lower()} ở mức trung bình. Có thể tối ưu hóa để tiết kiệm thêm."
        else:
            severity = "low"
            trend = "Hợp lý"
            recommendation = f"Chi tiêu {category.lower()} ở mức hợp lý. Duy trì thói quen tốt này."
        
        insights.append(SpendingInsight(
            category=category,
            amount=amount,
            percentage=percentage,
            trend=trend,
            recommendation=recommendation,
            severity=severity
        ))
    
    # Sort by amount (descending)
    insights.sort(key=lambda x: x.amount, reverse=True)
    
    return insights[:5]  # Top 5 categories

def generate_savings_recommendations(transactions: List[Dict], monthly_income: float) -> List[SavingsRecommendation]:
    """Generate AI savings recommendations"""
    
    recommendations = []
    
    # Calculate food spending
    food_spending = sum(t.get('amount', 0) for t in transactions 
                      if t.get('type') == 'EXPENSE' and 'ăn' in t.get('category', '').lower())
    
    if food_spending > monthly_income * 0.2:  # More than 20% on food
        recommendations.append(SavingsRecommendation(
            title="Tối ưu chi phí ăn uống",
            description="Chi phí ăn uống của bạn cao hơn mức khuyến nghị. Có thể tiết kiệm bằng cách nấu ăn tại nhà nhiều hơn.",
            potential_savings=food_spending * 0.3,
            difficulty="Dễ",
            timeframe="1-2 tuần",
            action_steps=[
                "Lập kế hoạch menu hàng tuần",
                "Mua sắm theo danh sách định sẵn", 
                "Nấu ăn tại nhà ít nhất 5 bữa/tuần",
                "Đem cơm trưa đi làm thay vì ăn ngoài"
            ]
        ))
    
    # Calculate entertainment spending
    entertainment_spending = sum(t.get('amount', 0) for t in transactions
                               if t.get('type') == 'EXPENSE' and any(keyword in t.get('category', '').lower() 
                                                                   for keyword in ['giải trí', 'mua sắm']))
    
    if entertainment_spending > monthly_income * 0.15:
        recommendations.append(SavingsRecommendation(
            title="Kiểm soát chi tiêu giải trí", 
            description="Hạn chế các khoản chi không cần thiết cho giải trí và mua sắm để tăng khả năng tiết kiệm.",
            potential_savings=entertainment_spending * 0.4,
            difficulty="Trung bình",
            timeframe="1 tháng",
            action_steps=[
                "Đặt ngân sách cố định cho giải trí mỗi tháng",
                "Tìm các hoạt động miễn phí thay thế",
                "Áp dụng quy tắc 24h trước khi mua đồ không cần thiết",
                "Sử dụng ứng dụng theo dõi chi tiêu"
            ]
        ))
    
    # Transportation recommendation
    transport_spending = sum(t.get('amount', 0) for t in transactions
                           if t.get('type') == 'EXPENSE' and 'di chuyển' in t.get('category', '').lower())
    
    if transport_spending > monthly_income * 0.1:
        recommendations.append(SavingsRecommendation(
            title="Tối ưu chi phí di chuyển",
            description="Sử dụng phương tiện công cộng hoặc xe đạp để giảm chi phí di chuyển hàng ngày.",
            potential_savings=transport_spending * 0.25,
            difficulty="Dễ", 
            timeframe="2-3 tuần",
            action_steps=[
                "Sử dụng xe buýt/tàu điện thay vì taxi",
                "Đi xe đạp cho quãng đường ngắn",
                "Chia sẻ xe với đồng nghiệp",
                "Lên kế hoạch di chuyển hiệu quả"
            ]
        ))
    
    return recommendations

def generate_goal_plans(goals: List[Dict], monthly_income: float, total_spending: float) -> List[GoalPlan]:
    """Generate goal achievement plans"""
    
    available_savings = max(0, monthly_income - total_spending)
    goal_plans = []
    
    for goal in goals:
        target_amount = goal.get('target_amount', 0)
        current_amount = goal.get('current_amount', 0) 
        remaining_amount = target_amount - current_amount
        
        # Assume 1 year timeline if not specified
        months_to_goal = 12
        monthly_required = remaining_amount / months_to_goal
        
        # Determine feasibility
        if monthly_required <= available_savings * 0.3:
            feasibility = "Khả thi"
            recommendations = [
                "Mục tiêu hoàn toàn khả thi với thu nhập hiện tại",
                "Thiết lập tự động chuyển tiền tiết kiệm",
                "Tìm tài khoản tiết kiệm lãi suất cao"
            ]
        elif monthly_required <= available_savings * 0.6:
            feasibility = "Khó khăn"
            recommendations = [
                "Cần cắt giảm một số chi phí không cần thiết",
                "Xem xét tăng thu nhập từ công việc phụ",
                "Gia hạn thời gian thực hiện mục tiêu"
            ]
        else:
            feasibility = "Không khả thi"
            recommendations = [
                "Cần tăng thu nhập đáng kể hoặc giảm chi phí",
                "Xem xét chia nhỏ mục tiêu thành các giai đoạn",
                "Tìm nguồn thu nhập thụ động"
            ]
        
        goal_plans.append(GoalPlan(
            goal_name=goal.get('name', 'Mục tiêu chưa đặt tên'),
            target_amount=target_amount,
            current_amount=current_amount,
            monthly_required=monthly_required,
            deadline=goal.get('deadline', '2025-12-31'),
            feasibility=feasibility,
            recommendations=recommendations
        ))
    
    return goal_plans

def generate_next_actions(savings_rate: float, num_goals: int) -> List[str]:
    """Generate actionable next steps"""
    
    actions = []
    
    if savings_rate < 10:
        actions.append("🎯 Ưu tiên tăng tỷ lệ tiết kiệm lên ít nhất 10% thu nhập")
        actions.append("📊 Phân tích chi tiết các khoản chi tiêu để tìm cơ hội tiết kiệm")
    elif savings_rate < 20:
        actions.append("💪 Tăng tỷ lệ tiết kiệm lên 20% để có nền tảng tài chính vững chắc")
        
    if num_goals == 0:
        actions.append("🎯 Đặt ít nhất 2-3 mục tiêu tài chính cụ thể")
    elif num_goals > 5:
        actions.append("🎯 Ưu tiên 3-5 mục tiêu quan trọng nhất để tập trung nguồn lực")
        
    actions.append("📱 Sử dụng ứng dụng để theo dõi chi tiêu hàng ngày")
    actions.append("💰 Thiết lập tài khoản tiết kiệm tự động")
    actions.append("📚 Học thêm về đầu tư để tăng tài sản trong dài hạn")
    
    return actions

def calculate_financial_score(savings_rate: float, num_insights: int, num_goals: int) -> float:
    """Calculate overall financial health score out of 100"""
    
    # Savings rate score (0-40 points)
    savings_score = min(40, savings_rate * 2)  # 20% savings rate = 40 points
    
    # Financial awareness score (0-30 points)  
    awareness_score = min(30, num_insights * 6)  # Up to 5 insights = 30 points
    
    # Goal setting score (0-30 points)
    goal_score = min(30, num_goals * 10)  # Up to 3 goals = 30 points
    
    total_score = savings_score + awareness_score + goal_score
    
    return round(total_score, 1)

if __name__ == "__main__":
    uvicorn.run(
        app,
        host="127.0.0.1", 
        port=8002,
        reload=False,
        log_level="info"
    )