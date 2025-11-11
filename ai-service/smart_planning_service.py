#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Smart Financial Planning Service
Dịch vụ gợi ý kế hoạch tài chính và tiết kiệm thông minh
"""

import json
import os
from datetime import datetime, timedelta
from collections import defaultdict, Counter
from dataclasses import dataclass, asdict
from typing import List, Dict, Optional, Union
import pickle
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.ensemble import RandomForestClassifier
import numpy as np

@dataclass
class SpendingInsight:
    """Insight về spending pattern"""
    category: str
    amount: float
    percentage: float
    trend: str  # increasing, decreasing, stable
    recommendation: str
    severity: str  # low, medium, high

@dataclass
class SavingsRecommendation:
    """Gợi ý tiết kiệm"""
    title: str
    description: str
    potential_savings: float
    difficulty: str  # easy, medium, hard
    timeframe: str  # immediate, short-term, long-term
    action_steps: List[str]

@dataclass
class GoalPlan:
    """Kế hoạch cho mục tiêu tài chính"""
    goal_name: str
    target_amount: float
    current_amount: float
    monthly_required: float
    deadline: str
    feasibility: str  # feasible, challenging, unrealistic
    recommendations: List[str]

@dataclass
class FinancialPlan:
    """Kế hoạch tài chính tổng thể"""
    monthly_income: float
    total_spending: float
    savings_rate: float
    spending_insights: List[SpendingInsight]
    savings_recommendations: List[SavingsRecommendation]
    goal_plans: List[GoalPlan]
    overall_score: float
    next_actions: List[str]

class SmartPlanningService:
    """Service chính cho Smart Financial Planning"""
    
    def __init__(self):
        self.dataset_path = "massive_vietnamese_dataset_200k.json"
        self.model_path = "enhanced_vietnamese_classifier.pkl"
        self.vectorizer_path = "enhanced_tfidf_vectorizer.pkl"
        
        # Load models và data
        self.knowledge_base = self._load_knowledge_base()
        self.classifier, self.vectorizer = self._load_models()
        
        # Planning templates
        self.savings_templates = self._load_savings_templates()
        self.investment_advice = self._load_investment_advice()
        self.budget_rules = self._load_budget_rules()
    
    def _load_knowledge_base(self) -> Dict:
        """Load knowledge từ dataset"""
        try:
            if os.path.exists(self.dataset_path):
                with open(self.dataset_path, 'r', encoding='utf-8') as f:
                    data = json.load(f)
                
                knowledge = {
                    'spending_patterns': defaultdict(list),
                    'savings_examples': [],
                    'investment_examples': [],
                    'category_insights': defaultdict(dict)
                }
                
                # Phân tích patterns từ dataset
                for item in data:
                    text = item['text'].lower()
                    category = item['category']
                    
                    knowledge['spending_patterns'][category].append(text)
                    
                    if any(word in text for word in ['tiết kiệm', 'gửi tiết kiệm']):
                        knowledge['savings_examples'].append(item)
                    
                    if category == 'đầu tư':
                        knowledge['investment_examples'].append(item)
                
                return knowledge
            
        except Exception as e:
            print(f"Error loading knowledge base: {e}")
        
        return {'spending_patterns': {}, 'savings_examples': [], 'investment_examples': [], 'category_insights': {}}
    
    def _load_models(self):
        """Load trained models"""
        try:
            if os.path.exists(self.model_path) and os.path.exists(self.vectorizer_path):
                with open(self.model_path, 'rb') as f:
                    classifier = pickle.load(f)
                with open(self.vectorizer_path, 'rb') as f:
                    vectorizer = pickle.load(f)
                return classifier, vectorizer
        except Exception as e:
            print(f"Warning: Could not load models: {e}")
        
        return None, None
    
    def _load_savings_templates(self) -> List[Dict]:
        """Load savings advice templates"""
        return [
            {
                "trigger": "high_food_spending",
                "condition": lambda data: data.get('ăn uống', {}).get('percentage', 0) > 30,
                "title": "Tối ưu hóa chi tiêu ăn uống",
                "template": "Chi tiêu ăn uống chiếm {percentage}% tổng chi tiêu. Khuyến nghị giảm xuống 25% bằng cách nấu ăn tại nhà nhiều hơn.",
                "potential_savings": 0.05,
                "difficulty": "easy"
            },
            {
                "trigger": "low_savings_rate", 
                "condition": lambda data: data.get('savings_rate', 0) < 10,
                "title": "Tăng tỷ lệ tiết kiệm",
                "template": "Tỷ lệ tiết kiệm hiện tại chỉ {savings_rate}%. Mục tiêu nên đạt ít nhất 20% thu nhập.",
                "potential_savings": 0.1,
                "difficulty": "medium"
            },
            {
                "trigger": "high_entertainment",
                "condition": lambda data: data.get('giải trí', {}).get('percentage', 0) > 20,
                "title": "Cân bằng chi tiêu giải trí",
                "template": "Chi tiêu giải trí {percentage}% có thể giảm xuống 15% để tăng tiết kiệm.",
                "potential_savings": 0.03,
                "difficulty": "medium"
            }
        ]
    
    def _load_investment_advice(self) -> List[Dict]:
        """Load investment advice templates"""
        return [
            {
                "savings_rate_range": (20, 30),
                "advice": "Với tỷ lệ tiết kiệm tốt, hãy xem xét gửi tiết kiệm có kỳ hạn hoặc trái phiếu chính phủ.",
                "risk_level": "low"
            },
            {
                "savings_rate_range": (30, 50),
                "advice": "Có thể phân bổ một phần vào quỹ tương hỗ cân bằng để tăng sinh lời.",
                "risk_level": "medium"
            },
            {
                "savings_rate_range": (50, 100),
                "advice": "Xem xét đa dạng hóa với cổ phiếu blue-chip và bất động sản.",
                "risk_level": "medium-high"
            }
        ]
    
    def _load_budget_rules(self) -> Dict:
        """Load budget allocation rules"""
        return {
            "50_30_20": {
                "needs": 0.50,      # Chi tiêu cần thiết
                "wants": 0.30,      # Chi tiêu mong muốn  
                "savings": 0.20     # Tiết kiệm
            },
            "category_limits": {
                "ăn uống": 0.25,
                "di chuyển": 0.15,
                "giải trí": 0.15,
                "mua sắm": 0.10,
                "sức khỏe": 0.10,
                "giáo dục": 0.05,
                "khác": 0.10
            }
        }
    
    def analyze_spending_pattern(self, transactions: List[Dict]) -> Dict:
        """Phân tích chi tiêu pattern chi tiết"""
        if not transactions:
            return {}
        
        # Tính toán chi tiêu theo category
        category_data = defaultdict(lambda: {'amount': 0, 'count': 0, 'transactions': []})
        total_spending = 0
        
        for trans in transactions:
            category = trans.get('category', 'khác')
            amount = abs(float(trans.get('amount', 0)))
            
            category_data[category]['amount'] += amount
            category_data[category]['count'] += 1
            category_data[category]['transactions'].append(trans)
            total_spending += amount
        
        # Tính percentages và insights
        analysis = {
            'total_spending': total_spending,
            'categories': {},
            'insights': []
        }
        
        for category, data in category_data.items():
            percentage = (data['amount'] / total_spending) * 100 if total_spending > 0 else 0
            avg_amount = data['amount'] / data['count'] if data['count'] > 0 else 0
            
            analysis['categories'][category] = {
                'amount': data['amount'],
                'percentage': percentage,
                'count': data['count'],
                'average': avg_amount
            }
        
        return analysis
    
    def generate_spending_insights(self, spending_analysis: Dict, income: float) -> List[SpendingInsight]:
        """Tạo insights về spending pattern"""
        insights = []
        
        if not spending_analysis:
            return insights
        
        budget_limits = self.budget_rules['category_limits']
        categories = spending_analysis.get('categories', {})
        
        for category, data in categories.items():
            percentage = data['percentage']
            limit = budget_limits.get(category, 15) * 100  # Convert to percentage
            
            if percentage > limit * 1.5:  # Vượt quá 150% limit
                severity = "high"
                recommendation = f"Cần giảm chi tiêu {category} từ {percentage:.1f}% xuống {limit:.1f}%"
                trend = "concerning"
            elif percentage > limit:  # Vượt limit
                severity = "medium" 
                recommendation = f"Nên giảm chi tiêu {category} xuống mức khuyến nghị {limit:.1f}%"
                trend = "above_average"
            else:
                severity = "low"
                recommendation = f"Chi tiêu {category} ở mức hợp lý"
                trend = "normal"
            
            insights.append(SpendingInsight(
                category=category,
                amount=data['amount'],
                percentage=percentage,
                trend=trend,
                recommendation=recommendation,
                severity=severity
            ))
        
        return insights
    
    def generate_savings_recommendations(self, spending_analysis: Dict, income: float) -> List[SavingsRecommendation]:
        """Tạo gợi ý tiết kiệm"""
        recommendations = []
        
        if not spending_analysis:
            return recommendations
        
        total_spending = spending_analysis['total_spending']
        savings_rate = ((income - total_spending) / income) * 100 if income > 0 else 0
        categories = spending_analysis.get('categories', {})
        
        # Áp dụng savings templates
        for template in self.savings_templates:
            data = {
                'savings_rate': savings_rate,
                **{cat: {'percentage': data['percentage']} for cat, data in categories.items()}
            }
            
            if template['condition'](data):
                # Tính potential savings
                category_key = None
                if 'ăn uống' in template['title'].lower():
                    category_key = 'ăn uống'
                elif 'giải trí' in template['title'].lower():
                    category_key = 'giải trí'
                
                potential = income * template['potential_savings']
                if category_key and category_key in categories:
                    potential = min(potential, categories[category_key]['amount'] * 0.3)
                
                description = template['template'].format(
                    percentage=categories.get(category_key, {}).get('percentage', savings_rate),
                    savings_rate=f"{savings_rate:.1f}%"
                )
                
                action_steps = self._generate_action_steps(template['trigger'], category_key)
                
                recommendations.append(SavingsRecommendation(
                    title=template['title'],
                    description=description,
                    potential_savings=potential,
                    difficulty=template['difficulty'],
                    timeframe="short-term",
                    action_steps=action_steps
                ))
        
        return recommendations
    
    def _generate_action_steps(self, trigger: str, category: str = None) -> List[str]:
        """Tạo action steps cụ thể"""
        steps_map = {
            "high_food_spending": [
                "Lập kế hoạch nấu ăn hàng tuần",
                "Mua sắm theo list để tránh mua thừa", 
                "Giảm frequency ăn ngoài từ 5 lần xuống 3 lần/tuần",
                "Tận dụng khuyến mãi và mua sỉ"
            ],
            "low_savings_rate": [
                "Thiết lập tự động chuyển tiết kiệm 20% lương",
                "Sử dụng phương pháp envelope cho chi tiêu",
                "Review và cắt giảm các subscription không cần thiết",
                "Tìm nguồn thu nhập phụ"
            ],
            "high_entertainment": [
                "Đặt budget cố định cho giải trí mỗi tháng",
                "Tìm các hoạt động giải trí miễn phí",
                "Chia sẻ chi phí giải trí với bạn bè",
                "Ưu tiên chất lượng hơn số lượng"
            ]
        }
        
        return steps_map.get(trigger, ["Tạo kế hoạch chi tiết", "Theo dõi tiến độ hàng tuần"])
    
    def create_goal_plans(self, goals: List[Dict], income: float, current_savings_rate: float) -> List[GoalPlan]:
        """Tạo kế hoạch cho các mục tiêu tài chính"""
        goal_plans = []
        
        available_monthly_savings = income * (current_savings_rate / 100)
        
        for goal in goals:
            target_amount = goal.get('target_amount', 0)
            current_amount = goal.get('current_amount', 0) 
            deadline_str = goal.get('deadline', '')
            
            try:
                if isinstance(deadline_str, str):
                    deadline = datetime.strptime(deadline_str, '%Y-%m-%d')
                else:
                    deadline = deadline_str
                    
                months_left = max(1, (deadline - datetime.now()).days // 30)
            except:
                months_left = 12  # Default 1 năm
            
            remaining_amount = target_amount - current_amount
            monthly_required = remaining_amount / months_left
            
            # Đánh giá feasibility
            if monthly_required <= available_monthly_savings * 0.5:
                feasibility = "feasible"
                recommendations = ["Có thể đạt được với kế hoạch hiện tại"]
            elif monthly_required <= available_monthly_savings:
                feasibility = "challenging"
                recommendations = [
                    "Cần tăng tỷ lệ tiết kiệm lên 25-30%",
                    "Xem xét giảm chi tiêu không cần thiết"
                ]
            else:
                feasibility = "unrealistic"
                recommendations = [
                    f"Cần tăng thu nhập thêm {monthly_required - available_monthly_savings:,.0f} VND/tháng",
                    "Hoặc kéo dài thời gian để đạt mục tiêu",
                    "Xem xét đầu tư để tăng sinh lời"
                ]
            
            goal_plans.append(GoalPlan(
                goal_name=goal.get('name', 'Mục tiêu tài chính'),
                target_amount=target_amount,
                current_amount=current_amount,
                monthly_required=monthly_required,
                deadline=deadline_str,
                feasibility=feasibility,
                recommendations=recommendations
            ))
        
        return goal_plans
    
    def generate_comprehensive_plan(
        self, 
        transactions: List[Dict], 
        income: float,
        goals: List[Dict] = None
    ) -> FinancialPlan:
        """Tạo kế hoạch tài chính tổng thể"""
        
        # 1. Phân tích spending
        spending_analysis = self.analyze_spending_pattern(transactions)
        total_spending = spending_analysis.get('total_spending', 0)
        savings_rate = ((income - total_spending) / income) * 100 if income > 0 else 0
        
        # 2. Tạo insights
        spending_insights = self.generate_spending_insights(spending_analysis, income)
        
        # 3. Tạo savings recommendations
        savings_recommendations = self.generate_savings_recommendations(spending_analysis, income)
        
        # 4. Tạo goal plans
        goal_plans = []
        if goals:
            goal_plans = self.create_goal_plans(goals, income, savings_rate)
        
        # 5. Tính overall score
        overall_score = self._calculate_financial_score(savings_rate, spending_insights, len(goal_plans))
        
        # 6. Tạo next actions
        next_actions = self._generate_next_actions(spending_insights, savings_recommendations, goal_plans)
        
        return FinancialPlan(
            monthly_income=income,
            total_spending=total_spending,
            savings_rate=savings_rate,
            spending_insights=spending_insights,
            savings_recommendations=savings_recommendations,
            goal_plans=goal_plans,
            overall_score=overall_score,
            next_actions=next_actions
        )
    
    def _calculate_financial_score(self, savings_rate: float, insights: List[SpendingInsight], goals_count: int) -> float:
        """Tính điểm tài chính tổng thể (0-100)"""
        score = 0
        
        # Savings rate score (40%)
        if savings_rate >= 20:
            score += 40
        elif savings_rate >= 10:
            score += 30
        else:
            score += savings_rate * 1.5
        
        # Spending control score (40%)
        high_severity_count = sum(1 for insight in insights if insight.severity == "high")
        medium_severity_count = sum(1 for insight in insights if insight.severity == "medium")
        
        spending_score = 40 - (high_severity_count * 15) - (medium_severity_count * 5)
        score += max(0, spending_score)
        
        # Goal planning score (20%)
        if goals_count > 0:
            score += 20
        
        return min(100, max(0, score))
    
    def _generate_next_actions(
        self, 
        insights: List[SpendingInsight], 
        recommendations: List[SavingsRecommendation],
        goal_plans: List[GoalPlan]
    ) -> List[str]:
        """Tạo next actions ưu tiên"""
        actions = []
        
        # Actions từ high severity insights
        high_severity = [insight for insight in insights if insight.severity == "high"]
        if high_severity:
            actions.append(f"Ưu tiên giảm chi tiêu {high_severity[0].category}")
        
        # Actions từ recommendations
        easy_recs = [rec for rec in recommendations if rec.difficulty == "easy"]
        if easy_recs:
            actions.append(f"Thực hiện: {easy_recs[0].title}")
        
        # Actions từ goal plans
        feasible_goals = [goal for goal in goal_plans if goal.feasibility == "feasible"]
        if feasible_goals:
            actions.append(f"Thiết lập auto-save cho mục tiêu: {feasible_goals[0].goal_name}")
        
        # Default actions
        if not actions:
            actions = [
                "Theo dõi chi tiêu hàng ngày",
                "Đặt mục tiêu tiết kiệm 20% thu nhập",
                "Review ngân sách hàng tháng"
            ]
        
        return actions[:5]  # Tối đa 5 actions

# Test function
def test_planning_service():
    """Test SmartPlanningService"""
    print("🧪 TESTING SMART PLANNING SERVICE")
    print("=" * 50)
    
    service = SmartPlanningService()
    
    # Test data
    sample_transactions = [
        {'category': 'ăn uống', 'amount': -3000000, 'description': 'Ăn tối'},
        {'category': 'di chuyển', 'amount': -1500000, 'description': 'Xăng xe'},
        {'category': 'giải trí', 'amount': -2000000, 'description': 'Xem phim'},
        {'category': 'mua sắm', 'amount': -800000, 'description': 'Quần áo'},
        {'category': 'sức khỏe', 'amount': -500000, 'description': 'Khám răng'},
    ]
    
    sample_goals = [
        {
            'name': 'Mua xe máy',
            'target_amount': 50000000,
            'current_amount': 10000000,
            'deadline': '2026-06-01'
        },
        {
            'name': 'Du lịch',
            'target_amount': 15000000,
            'current_amount': 5000000,
            'deadline': '2025-12-31'
        }
    ]
    
    # Generate plan
    plan = service.generate_comprehensive_plan(
        transactions=sample_transactions,
        income=12000000,
        goals=sample_goals
    )
    
    # Display results
    print(f"📊 Thu nhập: {plan.monthly_income:,.0f} VND")
    print(f"💸 Tổng chi tiêu: {plan.total_spending:,.0f} VND")
    print(f"💰 Tỷ lệ tiết kiệm: {plan.savings_rate:.1f}%")
    print(f"⭐ Điểm tổng thể: {plan.overall_score:.1f}/100")
    
    print(f"\n🔍 Insights chi tiêu:")
    for insight in plan.spending_insights:
        print(f"  {insight.category}: {insight.percentage:.1f}% - {insight.severity}")
    
    print(f"\n💡 Gợi ý tiết kiệm:")
    for rec in plan.savings_recommendations:
        print(f"  • {rec.title}: Tiết kiệm {rec.potential_savings:,.0f} VND")
    
    print(f"\n🎯 Kế hoạch mục tiêu:")
    for goal in plan.goal_plans:
        print(f"  {goal.goal_name}: {goal.monthly_required:,.0f} VND/tháng - {goal.feasibility}")
    
    print(f"\n🚀 Hành động tiếp theo:")
    for action in plan.next_actions:
        print(f"  • {action}")

if __name__ == "__main__":
    test_planning_service()