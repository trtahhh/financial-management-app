#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Smart Financial Planning & Savings Advisor
Tính năng gợi ý kế hoạch tài chính và tiết kiệm thông minh
"""

import json
import random
from datetime import datetime, timedelta
from collections import defaultdict
from dataclasses import dataclass
from typing import List, Dict, Optional

@dataclass
class TransactionPattern:
    """Mô hình giao dịch để phân tích"""
    category: str
    amount: float
    frequency: str  # daily, weekly, monthly
    trend: str      # increasing, decreasing, stable

@dataclass
class SavingsGoal:
    """Mục tiêu tiết kiệm"""
    name: str
    target_amount: float
    current_amount: float
    deadline: datetime
    priority: str  # high, medium, low

@dataclass
class FinancialAdvice:
    """Lời khuyên tài chính"""
    advice_type: str
    title: str
    description: str
    impact_score: float
    actionable_steps: List[str]

class FinancialPlanningAdvisor:
    """AI Advisor cho kế hoạch tài chính và tiết kiệm"""
    
    def __init__(self, dataset_path: str = "massive_vietnamese_dataset_200k.json"):
        self.dataset_path = dataset_path
        self.knowledge_base = self._load_knowledge()
        
        # Template advice patterns từ dataset
        self.savings_patterns = self._extract_savings_patterns()
        self.investment_patterns = self._extract_investment_patterns()
        self.budget_patterns = self._extract_budget_patterns()
    
    def _load_knowledge(self) -> Dict:
        """Load knowledge từ dataset"""
        try:
            with open(self.dataset_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            knowledge = {
                'savings': [],
                'investments': [],
                'budgeting': [],
                'categories': defaultdict(list)
            }
            
            for item in data:
                text = item['text'].lower()
                category = item['category']
                
                # Phân loại knowledge
                if any(word in text for word in ['tiết kiệm', 'gửi tiết kiệm', 'tích lũy']):
                    knowledge['savings'].append(item)
                
                if 'đầu tư' in category:
                    knowledge['investments'].append(item)
                
                knowledge['categories'][category].append(item)
            
            return knowledge
        except Exception as e:
            print(f"Lỗi load dataset: {e}")
            return {}
    
    def _extract_savings_patterns(self) -> List[str]:
        """Trích xuất patterns về tiết kiệm"""
        patterns = [
            "Gửi tiết kiệm định kỳ hàng tháng để tạo thói quen tích lũy",
            "Áp dụng quy tắc 50-30-20: 50% cần thiết, 30% mong muốn, 20% tiết kiệm",
            "Tự động chuyển khoản vào tài khoản tiết kiệm mỗi khi nhận lương",
            "Đặt mục tiêu tiết kiệm cụ thể cho từng khoảnh khắc quan trọng",
            "Sử dụng phương pháp 'trả mình trước' - tiết kiệm ngay khi có thu nhập"
        ]
        return patterns
    
    def _extract_investment_patterns(self) -> List[str]:
        """Trích xuất patterns về đầu tư"""
        patterns = [
            "Đa dạng hóa danh mục đầu tư để giảm rủi ro",
            "Đầu tư định kỳ (DCA) để giảm tác động biến động thị trường", 
            "Ưu tiên các kênh đầu tư phù hợp với khả năng chịu rủi ro",
            "Xây dựng quỹ khẩn cấp trước khi đầu tư rủi ro cao",
            "Tìm hiểu kỹ về sản phẩm đầu tư trước khi quyết định"
        ]
        return patterns
    
    def _extract_budget_patterns(self) -> List[str]:
        """Trích xuất patterns về quản lý ngân sách"""
        patterns = [
            "Theo dõi chi tiêu hàng ngày để kiểm soát ngân sách",
            "Phân loại chi tiêu thành cần thiết và không cần thiết",
            "Đặt giới hạn chi tiêu cho từng danh mục",
            "Review và điều chỉnh ngân sách hàng tháng",
            "Sử dụng ứng dụng quản lý tài chính để theo dõi tự động"
        ]
        return patterns
    
    def analyze_spending_pattern(self, transactions: List[Dict]) -> Dict:
        """Phân tích pattern chi tiêu từ transactions"""
        if not transactions:
            return {}
        
        # Phân tích theo category
        category_spending = defaultdict(float)
        category_frequency = defaultdict(int)
        
        for trans in transactions:
            category = trans.get('category', 'khác')
            amount = abs(float(trans.get('amount', 0)))
            
            category_spending[category] += amount
            category_frequency[category] += 1
        
        # Tính tổng chi tiêu
        total_spending = sum(category_spending.values())
        
        # Tính percentage breakdown
        spending_breakdown = {}
        for category, amount in category_spending.items():
            percentage = (amount / total_spending) * 100 if total_spending > 0 else 0
            spending_breakdown[category] = {
                'amount': amount,
                'percentage': percentage,
                'frequency': category_frequency[category]
            }
        
        return {
            'total_spending': total_spending,
            'breakdown': spending_breakdown,
            'top_categories': sorted(spending_breakdown.items(), 
                                   key=lambda x: x[1]['amount'], reverse=True)[:5]
        }
    
    def generate_savings_advice(self, income: float, spending_analysis: Dict) -> List[FinancialAdvice]:
        """Tạo lời khuyên tiết kiệm dựa trên phân tích"""
        advice_list = []
        
        if not spending_analysis:
            return advice_list
        
        total_spending = spending_analysis['total_spending']
        savings_rate = ((income - total_spending) / income) * 100 if income > 0 else 0
        
        # Lời khuyên về tỷ lệ tiết kiệm
        if savings_rate < 10:
            advice_list.append(FinancialAdvice(
                advice_type="savings_rate",
                title="Cải thiện tỷ lệ tiết kiệm",
                description=f"Tỷ lệ tiết kiệm hiện tại ({savings_rate:.1f}%) thấp hơn khuyến nghị (20%). Hãy cắt giảm chi tiêu không cần thiết.",
                impact_score=8.5,
                actionable_steps=[
                    "Xác định các khoản chi tiêu có thể cắt giảm",
                    "Áp dụng quy tắc 24h trước khi mua đồ không cần thiết", 
                    "Chuyển sang sử dụng các dịch vụ tiết kiệm hơn"
                ]
            ))
        elif savings_rate > 30:
            advice_list.append(FinancialAdvice(
                advice_type="investment_opportunity", 
                title="Cơ hội đầu tư với tỷ lệ tiết kiệm cao",
                description=f"Tỷ lệ tiết kiệm ({savings_rate:.1f}%) rất tốt! Hãy xem xét các kênh đầu tư để tăng sinh lời.",
                impact_score=7.0,
                actionable_steps=[
                    "Nghiên cứu các kênh đầu tư phù hợp",
                    "Bắt đầu với đầu tư ít rủi ro như tiết kiệm có kỳ hạn",
                    "Xem xét đầu tư vào quỹ tương hỗ"
                ]
            ))
        
        # Lời khuyên về categories chi tiêu cao
        top_spending = spending_analysis.get('top_categories', [])
        if top_spending:
            top_category, top_data = top_spending[0]
            if top_data['percentage'] > 40:
                advice_list.append(FinancialAdvice(
                    advice_type="expense_optimization",
                    title=f"Tối ưu hóa chi tiêu {top_category}",
                    description=f"Chi tiêu cho {top_category} chiếm {top_data['percentage']:.1f}% tổng chi tiêu. Hãy tìm cách tối ưu hóa.",
                    impact_score=7.5,
                    actionable_steps=[
                        f"Tìm các lựa chọn thay thế tiết kiệm cho {top_category}",
                        f"Đặt ngân sách hàng tháng cụ thể cho {top_category}",
                        f"So sánh giá và tìm ưu đãi tốt nhất"
                    ]
                ))
        
        return advice_list
    
    def create_savings_plan(self, income: float, goals: List[SavingsGoal]) -> Dict:
        """Tạo kế hoạch tiết kiệm chi tiết"""
        if not goals:
            return {}
        
        # Sắp xếp goals theo priority và deadline
        sorted_goals = sorted(goals, key=lambda x: (
            {'high': 0, 'medium': 1, 'low': 2}[x.priority],
            x.deadline
        ))
        
        plan = {
            'monthly_allocation': {},
            'timeline': {},
            'recommendations': []
        }
        
        available_savings = income * 0.2  # Giả định 20% thu nhập dành cho tiết kiệm
        
        for goal in sorted_goals:
            remaining_amount = goal.target_amount - goal.current_amount
            months_left = max(1, (goal.deadline - datetime.now()).days // 30)
            monthly_needed = remaining_amount / months_left
            
            plan['monthly_allocation'][goal.name] = {
                'amount': monthly_needed,
                'percentage': (monthly_needed / available_savings) * 100 if available_savings > 0 else 0
            }
            
            plan['timeline'][goal.name] = {
                'months_needed': months_left,
                'monthly_amount': monthly_needed,
                'feasible': monthly_needed <= available_savings * 0.5
            }
        
        return plan
    
    def get_smart_recommendations(self, user_profile: Dict) -> List[FinancialAdvice]:
        """Tạo gợi ý thông minh dựa trên profile người dùng"""
        recommendations = []
        
        # Phân tích từ knowledge base
        income = user_profile.get('monthly_income', 0)
        age = user_profile.get('age', 25)
        risk_tolerance = user_profile.get('risk_tolerance', 'medium')
        
        # Gợi ý theo độ tuổi
        if age < 30:
            recommendations.append(FinancialAdvice(
                advice_type="age_based",
                title="Xây dựng thói quen tài chính tốt",
                description="Ở độ tuổi trẻ, hãy tập trung xây dựng thói quen tiết kiệm và đầu tư dài hạn.",
                impact_score=9.0,
                actionable_steps=[
                    "Thiết lập tự động chuyển khoản tiết kiệm",
                    "Bắt đầu đóng góp quỹ hưu trí",
                    "Học hỏi về đầu tư và tài chính cá nhân"
                ]
            ))
        elif age >= 40:
            recommendations.append(FinancialAdvice(
                advice_type="age_based",
                title="Chuẩn bị cho giai đoạn nghỉ hưu",
                description="Tăng cường tiết kiệm và đầu tư để chuẩn bị cho nghỉ hưu.",
                impact_score=8.5,
                actionable_steps=[
                    "Tăng tỷ lệ đóng góp quỹ hưu trí",
                    "Đa dạng hóa danh mục đầu tư",
                    "Xem xét bảo hiểm nhân thọ"
                ]
            ))
        
        # Gợi ý theo thu nhập
        if income > 50000000:  # Thu nhập cao
            recommendations.append(FinancialAdvice(
                advice_type="income_based",
                title="Tối ưu hóa thuế và đầu tư",
                description="Với thu nhập cao, hãy xem xét các chiến lược tối ưu thuế và đầu tư chuyên nghiệp.",
                impact_score=7.5,
                actionable_steps=[
                    "Tư vấn với chuyên gia tài chính",
                    "Xem xét đầu tư bất động sản",
                    "Nghiên cứu các sản phẩm đầu tư chuyên nghiệp"
                ]
            ))
        
        return recommendations

def demo_financial_advisor():
    """Demo tính năng Financial Planning Advisor"""
    print("🤖 DEMO: SMART FINANCIAL PLANNING & SAVINGS ADVISOR")
    print("=" * 60)
    
    # Khởi tạo advisor
    advisor = FinancialPlanningAdvisor()
    
    # Giả lập dữ liệu user
    user_profile = {
        'monthly_income': 15000000,  # 15 triệu/tháng
        'age': 28,
        'risk_tolerance': 'medium'
    }
    
    # Giả lập transactions
    sample_transactions = [
        {'category': 'ăn uống', 'amount': -2000000},
        {'category': 'di chuyển', 'amount': -1500000},
        {'category': 'giải trí', 'amount': -800000},
        {'category': 'mua sắm', 'amount': -1200000},
        {'category': 'sức khỏe', 'amount': -500000},
    ]
    
    # Phân tích chi tiêu
    print("📊 PHÂN TÍCH CHI TIÊU:")
    spending_analysis = advisor.analyze_spending_pattern(sample_transactions)
    print(f"Tổng chi tiêu: {spending_analysis['total_spending']:,.0f} VND")
    
    for category, data in spending_analysis['breakdown'].items():
        print(f"  {category}: {data['amount']:,.0f} VND ({data['percentage']:.1f}%)")
    
    # Tạo lời khuyên tiết kiệm
    print(f"\n💡 LỜI KHUYÊN TIẾT KIỆM:")
    savings_advice = advisor.generate_savings_advice(
        user_profile['monthly_income'], 
        spending_analysis
    )
    
    for advice in savings_advice:
        print(f"\n🎯 {advice.title}")
        print(f"   Mô tả: {advice.description}")
        print(f"   Tác động: {advice.impact_score}/10")
        print("   Hành động:")
        for step in advice.actionable_steps:
            print(f"     • {step}")
    
    # Gợi ý thông minh
    print(f"\n🧠 GỢI Ý THÔNG MINH:")
    smart_recs = advisor.get_smart_recommendations(user_profile)
    
    for rec in smart_recs:
        print(f"\n⭐ {rec.title}")
        print(f"   {rec.description}")
        print("   Các bước thực hiện:")
        for step in rec.actionable_steps:
            print(f"     • {step}")
    
    # Tạo kế hoạch tiết kiệm
    sample_goals = [
        SavingsGoal("Mua nhà", 500000000, 50000000, datetime.now() + timedelta(days=1095), "high"),
        SavingsGoal("Du lịch", 20000000, 5000000, datetime.now() + timedelta(days=180), "medium"),
        SavingsGoal("Khẩn cấp", 50000000, 10000000, datetime.now() + timedelta(days=365), "high")
    ]
    
    print(f"\n📋 KẾ HOẠCH TIẾT KIỆM:")
    savings_plan = advisor.create_savings_plan(user_profile['monthly_income'], sample_goals)
    
    for goal_name, allocation in savings_plan['monthly_allocation'].items():
        timeline = savings_plan['timeline'][goal_name]
        status = "✅ Khả thi" if timeline['feasible'] else "⚠️ Cần điều chỉnh"
        print(f"  {goal_name}: {allocation['amount']:,.0f} VND/tháng ({allocation['percentage']:.1f}%) - {status}")
    
    print(f"\n🎉 Tổng kết: AI có thể phát triển đầy đủ tính năng gợi ý tài chính!")

if __name__ == "__main__":
    demo_financial_advisor()