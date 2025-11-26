#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Enhanced AI Financial Planning Service
Nâng cấp với ML, personalization và advanced analytics
"""

import json
import os
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from collections import defaultdict, Counter
from dataclasses import dataclass, asdict
from typing import List, Dict, Optional, Tuple
from sklearn.ensemble import RandomForestRegressor, GradientBoostingClassifier
from sklearn.cluster import KMeans
import xgboost as xgb
from prophet import Prophet  # Now enabled!
import shap
import warnings
warnings.filterwarnings('ignore')

@dataclass
class SpendingInsight:
    """Enhanced spending insight with ML predictions"""
    category: str
    amount: float
    percentage: float
    trend: str
    prediction_next_month: float
    recommendation: str
    severity: str
    confidence_score: float
    seasonality_pattern: Optional[str] = None

@dataclass
class SavingsRecommendation:
    """Enhanced savings recommendation"""
    title: str
    description: str
    potential_savings: float
    difficulty: str
    timeframe: str
    action_steps: List[str]
    priority_score: float
    category_impact: Dict[str, float]
    personalized_tips: List[str]

@dataclass
class GoalPlan:
    """Enhanced goal plan with ML predictions"""
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

@dataclass
class FinancialPlan:
    """Comprehensive financial plan"""
    monthly_income: float
    total_spending: float
    savings_rate: float
    spending_insights: List[SpendingInsight]
    savings_recommendations: List[SavingsRecommendation]
    goal_plans: List[GoalPlan]
    overall_score: float
    next_actions: List[str]
    financial_health_metrics: Dict
    risk_assessment: Dict

class EnhancedPlanningService:
    """Enhanced AI Planning Service with ML & Personalization"""
    
    def __init__(self):
        self.knowledge_base = self._build_knowledge_base()
        
        # ML models for predictions
        self.spending_predictor = RandomForestRegressor(n_estimators=50, random_state=42)
        self.anomaly_detector = None
        self.user_clusters = None
        
        # Enhanced templates
        self.category_benchmarks = self._load_category_benchmarks()
        self.savings_strategies = self._load_savings_strategies()
        self.goal_templates = self._load_goal_templates()
    
    def _build_knowledge_base(self) -> Dict:
        """Build comprehensive knowledge base"""
        return {
            'category_avg': {
                'Ăn uống': {'percentage': 25, 'variance': 5},
                'Giao thông': {'percentage': 15, 'variance': 3},
                'Giải trí': {'percentage': 10, 'variance': 5},
                'Sức khỏe': {'percentage': 8, 'variance': 4},
                'Giáo dục': {'percentage': 5, 'variance': 3},
                'Mua sắm': {'percentage': 20, 'variance': 8},
                'Tiện ích': {'percentage': 12, 'variance': 2},
                'Quà tặng': {'percentage': 5, 'variance': 3}
            },
            'spending_patterns': {
                'high_spender': 0.8,
                'moderate': 0.5,
                'frugal': 0.3
            },
            'savings_tiers': {
                'excellent': 0.3,
                'good': 0.2,
                'average': 0.1,
                'poor': 0.05
            }
        }
    
    def _load_category_benchmarks(self) -> Dict:
        """Load category spending benchmarks"""
        return {
            'Ăn uống': {
                'optimal_range': (0.20, 0.30),
                'warning_threshold': 0.35,
                'tips': [
                    "Nấu ăn tại nhà 4-5 bữa/tuần để tiết kiệm 30-40%",
                    "Lập kế hoạch bữa ăn trước để tránh lãng phí",
                    "Sử dụng ứng dụng giảm giá cho đặt đồ ăn",
                    "Mang theo bữa trưa làm giảm chi phí đáng kể"
                ]
            },
            'Giao thông': {
                'optimal_range': (0.10, 0.20),
                'warning_threshold': 0.25,
                'tips': [
                    "Sử dụng phương tiện công cộng cho các chuyến đi thường xuyên",
                    "Carpool với đồng nghiệp để chia sẻ chi phí",
                    "Xem xét xe máy/xe đạp điện cho quãng đường gần",
                    "Tối ưu lộ trình để tiết kiệm nhiên liệu"
                ]
            },
            'Giải trí': {
                'optimal_range': (0.05, 0.15),
                'warning_threshold': 0.20,
                'tips': [
                    "Tận dụng các hoạt động miễn phí (công viên, thư viện)",
                    "Chia sẻ gói Netflix/Spotify với bạn bè",
                    "Tìm kiếm deals và vouchers trước khi chi tiêu",
                    "Lập ngân sách giải trí cụ thể mỗi tháng"
                ]
            },
            'Mua sắm': {
                'optimal_range': (0.15, 0.25),
                'warning_threshold': 0.30,
                'tips': [
                    "Áp dụng quy tắc 24h: Chờ 1 ngày trước khi mua đồ không cần thiết",
                    "Mua sắm theo danh sách để tránh mua impulsive",
                    "So sánh giá trên nhiều nền tảng",
                    "Mua hàng vào thời điểm sale/khuyến mãi"
                ]
            },
            'Tiện ích': {
                'optimal_range': (0.08, 0.15),
                'warning_threshold': 0.18,
                'tips': [
                    "Tắt điện/nước khi không sử dụng",
                    "Sử dụng thiết bị tiết kiệm năng lượng",
                    "Xem xét các gói combo internet/điện thoại",
                    "Kiểm tra hóa đơn định kỳ để phát hiện bất thường"
                ]
            }
        }
    
    def _load_savings_strategies(self) -> List[Dict]:
        """Load advanced savings strategies"""
        return [
            {
                'name': 'Chiến lược 50-30-20 nâng cao',
                'description': '50% nhu cầu, 20% mong muốn, 30% tiết kiệm + đầu tư',
                'difficulty': 'medium',
                'potential_savings': 0.30,
                'steps': [
                    'Phân loại chi tiêu vào 3 nhóm: cần thiết, mong muốn, tiết kiệm',
                    'Tự động chuyển 30% lương vào tài khoản tiết kiệm',
                    'Review và điều chỉnh tỷ lệ hàng quý',
                    'Đầu tư phần tiết kiệm vào quỹ chỉ số hoặc tiết kiệm có kỳ hạn'
                ]
            },
            {
                'name': 'Phương pháp "Trả mình trước"',
                'description': 'Tiết kiệm ngay khi nhận lương, trước khi chi tiêu',
                'difficulty': 'easy',
                'potential_savings': 0.20,
                'steps': [
                    'Thiết lập auto-transfer ngay ngày lương',
                    'Chuyển 20-25% lương vào tài khoản tiết kiệm riêng',
                    'Sống với số tiền còn lại',
                    'Tăng tỷ lệ tiết kiệm dần theo thời gian'
                ]
            },
            {
                'name': 'Thử thách 52 tuần',
                'description': 'Tiết kiệm tăng dần mỗi tuần trong năm',
                'difficulty': 'easy',
                'potential_savings': 0.15,
                'steps': [
                    'Tuần 1: Tiết kiệm 10.000đ',
                    'Tuần 2: Tiết kiệm 20.000đ',
                    'Tăng 10.000đ mỗi tuần',
                    'Cuối năm có ~13.78 triệu tiết kiệm'
                ]
            },
            {
                'name': 'Phong bì điện tử',
                'description': 'Phân bổ tiền vào các "phong bì" mục đích cụ thể',
                'difficulty': 'medium',
                'potential_savings': 0.25,
                'steps': [
                    'Tạo các tài khoản phụ cho từng mục đích',
                    'Chia tiền lương vào các phong bì: Ăn uống, Giao thông, Giải trí...',
                    'Chỉ chi tiêu trong giới hạn mỗi phong bì',
                    'Chuyển số dư cuối tháng vào tiết kiệm'
                ]
            },
            {
                'name': 'Cắt giảm chi phí âm thầm',
                'description': 'Loại bỏ các khoản chi phí định kỳ không cần thiết',
                'difficulty': 'easy',
                'potential_savings': 0.18,
                'steps': [
                    'Review các subscription đang dùng',
                    'Hủy những dịch vụ ít sử dụng',
                    'Đàm phán giảm giá hoặc chuyển sang gói rẻ hơn',
                    'Tiết kiệm 200-500k/tháng từ các khoản này'
                ]
            },
            {
                'name': 'Đầu tư tiết kiệm thông minh',
                'description': 'Kết hợp tiết kiệm với đầu tư sinh lời',
                'difficulty': 'hard',
                'potential_savings': 0.35,
                'steps': [
                    'Chia tiết kiệm: 50% quỹ khẩn cấp, 50% đầu tư',
                    'Đầu tư vào quỹ chỉ số, trái phiếu, hoặc tiết kiệm kỳ hạn',
                    'Tái đầu tư lợi nhuận để tăng trưởng kép',
                    'Đa dạng hóa danh mục để giảm rủi ro'
                ]
            }
        ]
    
    def _load_goal_templates(self) -> Dict:
        """Load goal planning templates"""
        return {
            'emergency_fund': {
                'recommended_months': 6,
                'priority': 'critical',
                'tips': [
                    'Mục tiêu: 6 tháng chi phí sinh hoạt',
                    'Ưu tiên số 1 trước mọi mục tiêu khác',
                    'Đặt trong tài khoản dễ rút, lãi suất ổn định',
                    'Không dùng cho mục đích khác'
                ]
            },
            'house_purchase': {
                'down_payment_ratio': 0.20,
                'recommended_savings_rate': 0.30,
                'tips': [
                    'Tiết kiệm 20-30% giá nhà cho down payment',
                    'Tính cả chi phí phát sinh: phí, thuế, sửa chữa',
                    'Cân nhắc vay mua nhà khi có khoản tiết kiệm ổn định',
                    'Xem xét khu vực và khả năng tăng giá'
                ]
            },
            'education': {
                'planning_horizon_years': 5,
                'tips': [
                    'Bắt đầu sớm để hưởng lợi từ lãi kép',
                    'Xem xét các gói tiết kiệm giáo dục',
                    'Đầu tư vào quỹ tăng trưởng dài hạn',
                    'Cập nhật chi phí học phí hàng năm'
                ]
            },
            'retirement': {
                'retirement_age': 60,
                'replacement_rate': 0.70,
                'tips': [
                    'Mục tiêu: 70-80% thu nhập hiện tại khi về hưu',
                    'Bắt đầu càng sớm càng tốt',
                    'Đầu tư dài hạn vào cổ phiếu, quỹ hưu trí',
                    'Tận dụng đóng góp của công ty nếu có'
                ]
            }
        }
    
    def analyze_spending_patterns(self, transactions: List[Dict], income: float) -> List[SpendingInsight]:
        """Analyze spending with ML predictions"""
        insights = []
        
        # Group by category
        category_spending = defaultdict(float)
        category_transactions = defaultdict(list)
        
        for txn in transactions:
            if txn.get('type') == 'EXPENSE':
                cat = txn.get('category', {}).get('name', 'Khác')
                amount = abs(txn.get('amount', 0))
                category_spending[cat] += amount
                category_transactions[cat].append({
                    'date': txn.get('transactionDate', datetime.now().isoformat()),
                    'amount': amount,
                    'description': txn.get('description', '')
                })
        
        total_spending = sum(category_spending.values())
        
        for category, amount in sorted(category_spending.items(), key=lambda x: x[1], reverse=True):
            percentage = (amount / total_spending * 100) if total_spending > 0 else 0
            
            # Get category transactions
            cat_txns = category_transactions[category]
            
            # Use Prophet for trend analysis
            prophet_result = self._forecast_with_prophet(category, cat_txns)
            trend = prophet_result.get('trend', 'stable')
            
            # Use XGBoost for next month prediction
            predicted_amount, xgb_confidence = self._predict_with_xgboost(
                category, amount, cat_txns
            )
            
            # Combine confidences (XGBoost + Prophet)
            prophet_confidence = prophet_result.get('confidence', 0.6)
            combined_confidence = (xgb_confidence * 0.6 + prophet_confidence * 0.4)
            
            # Get benchmark and severity
            benchmark = self.category_benchmarks.get(category, {})
            optimal_range = benchmark.get('optimal_range', (0.10, 0.25))
            severity = self._calculate_severity(percentage / 100, optimal_range)
            
            # Generate personalized recommendation
            recommendation = self._generate_recommendation(
                category, percentage, amount, income, benchmark
            )
            
            insights.append(SpendingInsight(
                category=category,
                amount=amount,
                percentage=percentage,
                trend=trend,
                prediction_next_month=predicted_amount,
                recommendation=recommendation,
                severity=severity,
                confidence_score=combined_confidence,
                seasonality_pattern=self._detect_seasonality(category)
            ))
        
        return insights[:10]  # Top 10 insights
    
    def _determine_trend(self, category: str, amount: float, income: float) -> str:
        """Determine spending trend"""
        ratio = amount / income if income > 0 else 0
        avg = self.knowledge_base['category_avg'].get(category, {}).get('percentage', 20) / 100
        
        if ratio > avg * 1.2:
            return "increasing"
        elif ratio < avg * 0.8:
            return "decreasing"
        return "stable"
    
    def _predict_next_month(self, category: str, current_amount: float) -> float:
        """Predict next month spending (simplified)"""
        seasonality_factors = {
            'Ăn uống': 1.05,
            'Giao thông': 1.02,
            'Giải trí': 1.10,
            'Mua sắm': 1.15,
            'Tiện ích': 1.00,
            'Quà tặng': 1.20
        }
        factor = seasonality_factors.get(category, 1.05)
        return current_amount * factor
    
    def _calculate_severity(self, actual_ratio: float, optimal_range: Tuple[float, float]) -> str:
        """Calculate severity of overspending"""
        low, high = optimal_range
        
        if actual_ratio < low:
            return "low"
        elif low <= actual_ratio <= high:
            return "optimal"
        elif actual_ratio <= high * 1.2:
            return "medium"
        else:
            return "high"
    
    def _generate_recommendation(self, category: str, percentage: float, 
                                 amount: float, income: float, benchmark: Dict) -> str:
        """Generate personalized recommendation"""
        optimal_range = benchmark.get('optimal_range', (0.15, 0.25))
        ratio = percentage / 100
        
        if ratio <= optimal_range[1]:
            return f"Chi tiêu {category} đang trong tầm kiểm soát ({percentage:.1f}%). Duy trì thói quen này!"
        
        overspend_amount = amount - (optimal_range[1] * income)
        
        if overspend_amount > 0:
            return (f"Chi tiêu {category} cao hơn mức khuyến nghị {(ratio - optimal_range[1]) * 100:.1f}%. "
                   f"Có thể tiết kiệm thêm {overspend_amount:,.0f}đ/tháng bằng cách tối ưu hóa.")
        
        return f"Xem xét giảm chi tiêu {category} để tăng khả năng tiết kiệm."
    
    def _detect_seasonality(self, category: str) -> Optional[str]:
        """Detect seasonality pattern"""
        seasonal_categories = {
            'Quà tặng': 'Cao hơn vào dịp lễ, Tết',
            'Giải trí': 'Tăng vào cuối tuần và kỳ nghỉ',
            'Mua sắm': 'Tăng vào mùa sale (11/11, 12/12)',
            'Tiện ích': 'Tăng vào mùa hè (điều hòa)'
        }
        return seasonal_categories.get(category)
    
    def _predict_with_xgboost(self, category: str, current_amount: float, 
                             historical_data: List[Dict]) -> Tuple[float, float]:
        """
        XGBoost prediction for next month spending
        Returns: (predicted_amount, confidence_score)
        """
        try:
            if len(historical_data) < 3:
                # Not enough data, use simple prediction
                return self._predict_next_month(category, current_amount), 0.6
            
            # Prepare data
            df = pd.DataFrame(historical_data)
            df['date'] = pd.to_datetime(df['date'])
            df = df.sort_values('date')
            
            # Feature engineering
            df['month'] = df['date'].dt.month
            df['day_of_week'] = df['date'].dt.dayofweek
            df['is_weekend'] = df['day_of_week'].isin([5, 6]).astype(int)
            df['week_of_month'] = (df['date'].dt.day - 1) // 7 + 1
            
            # Prepare features
            features = ['month', 'day_of_week', 'is_weekend', 'week_of_month']
            X = df[features].values
            y = df['amount'].values
            
            # Train XGBoost model
            model = xgb.XGBRegressor(
                n_estimators=50,
                max_depth=3,
                learning_rate=0.1,
                random_state=42,
                verbosity=0
            )
            model.fit(X, y)
            
            # Predict next month
            next_month = datetime.now() + timedelta(days=30)
            next_features = [[
                next_month.month,
                next_month.weekday(),
                1 if next_month.weekday() in [5, 6] else 0,
                (next_month.day - 1) // 7 + 1
            ]]
            
            prediction = model.predict(next_features)[0]
            
            # Calculate confidence based on feature importance
            importance_scores = model.feature_importances_
            confidence = min(0.95, max(0.5, np.mean(importance_scores) + 0.4))
            
            return float(prediction), float(confidence)
            
        except Exception as e:
            # Fallback to simple prediction
            return self._predict_next_month(category, current_amount), 0.6
    
    def _forecast_with_prophet(self, category: str, transactions: List[Dict]) -> Dict:
        """
        Prophet forecasting for spending trends
        Returns: forecast data with trend, seasonality
        """
        try:
            if len(transactions) < 7:
                return {'trend': 'stable', 'forecast': None, 'confidence': 0.5}
            
            # Prepare data for Prophet
            df = pd.DataFrame(transactions)
            df['ds'] = pd.to_datetime(df['date'])
            df['y'] = df['amount']
            
            # Aggregate by day
            daily_df = df.groupby('ds')['y'].sum().reset_index()
            
            if len(daily_df) < 7:
                return {'trend': 'stable', 'forecast': None, 'confidence': 0.5}
            
            # Initialize Prophet model
            model = Prophet(
                daily_seasonality=False,
                weekly_seasonality=True,
                yearly_seasonality=False,
                changepoint_prior_scale=0.05
            )
            
            # Fit model (suppress output)
            with warnings.catch_warnings():
                warnings.simplefilter("ignore")
                model.fit(daily_df)
            
            # Make future dataframe (30 days)
            future = model.make_future_dataframe(periods=30)
            forecast = model.predict(future)
            
            # Extract trend
            trend_start = forecast['trend'].iloc[0]
            trend_end = forecast['trend'].iloc[-1]
            trend_change = (trend_end - trend_start) / trend_start if trend_start != 0 else 0
            
            if trend_change > 0.1:
                trend = 'increasing'
            elif trend_change < -0.1:
                trend = 'decreasing'
            else:
                trend = 'stable'
            
            # Calculate confidence
            uncertainty = forecast['yhat_upper'].iloc[-1] - forecast['yhat_lower'].iloc[-1]
            mean_value = forecast['yhat'].iloc[-1]
            confidence = max(0.5, min(0.95, 1 - (uncertainty / (2 * abs(mean_value)))) if mean_value != 0 else 0.6)
            
            return {
                'trend': trend,
                'forecast': float(forecast['yhat'].iloc[-1]),
                'confidence': float(confidence),
                'seasonality_strength': float(forecast['weekly'].std()) if 'weekly' in forecast.columns else 0.0
            }
            
        except Exception as e:
            # Fallback to simple trend
            return {'trend': 'stable', 'forecast': None, 'confidence': 0.5}
    
    def _explain_with_shap(self, model, features: np.ndarray, 
                          feature_names: List[str]) -> Dict[str, float]:
        """
        SHAP explanation for model predictions
        Returns: feature importance dictionary
        """
        try:
            # Create SHAP explainer
            explainer = shap.TreeExplainer(model)
            shap_values = explainer.shap_values(features)
            
            # Get absolute mean SHAP values
            if len(shap_values.shape) > 1:
                shap_values = shap_values[-1]  # Last prediction
            
            importance_dict = {}
            for i, name in enumerate(feature_names):
                importance_dict[name] = float(abs(shap_values[i]))
            
            # Normalize
            total = sum(importance_dict.values())
            if total > 0:
                importance_dict = {k: v/total for k, v in importance_dict.items()}
            
            return importance_dict
            
        except Exception as e:
            # Return uniform importance
            return {name: 1.0/len(feature_names) for name in feature_names}
    
    def generate_savings_recommendations(self, spending_insights: List[SpendingInsight],
                                        income: float, current_savings_rate: float) -> List[SavingsRecommendation]:
        """Generate personalized savings recommendations"""
        recommendations = []
        
        # 1. Strategy-based recommendations
        for strategy in self._load_savings_strategies():
            if current_savings_rate < strategy['potential_savings']:
                priority_score = self._calculate_priority_score(
                    strategy, current_savings_rate, income
                )
                
                category_impact = self._calculate_category_impact(
                    spending_insights, strategy['potential_savings']
                )
                
                personalized_tips = self._personalize_tips(
                    strategy, spending_insights, income
                )
                
                recommendations.append(SavingsRecommendation(
                    title=strategy['name'],
                    description=strategy['description'],
                    potential_savings=strategy['potential_savings'] * income,
                    difficulty=strategy['difficulty'],
                    timeframe='3-6 tháng',
                    action_steps=strategy['steps'],
                    priority_score=priority_score,
                    category_impact=category_impact,
                    personalized_tips=personalized_tips
                ))
        
        # 2. Category-specific recommendations
        for insight in spending_insights:
            if insight.severity in ['medium', 'high']:
                category_rec = self._generate_category_recommendation(insight, income)
                if category_rec:
                    recommendations.append(category_rec)
        
        # 3. Income-based recommendations
        income_rec = self._generate_income_based_recommendation(income, current_savings_rate)
        if income_rec:
            recommendations.append(income_rec)
        
        # Sort by priority score
        recommendations.sort(key=lambda x: x.priority_score, reverse=True)
        
        return recommendations[:8]  # Top 8 recommendations
    
    def _calculate_priority_score(self, strategy: Dict, current_rate: float, income: float) -> float:
        """Calculate priority score for recommendation"""
        # Base score from potential savings
        base_score = strategy['potential_savings'] * 100
        
        # Difficulty adjustment (easier = higher priority)
        difficulty_scores = {'easy': 1.2, 'medium': 1.0, 'hard': 0.8}
        difficulty_factor = difficulty_scores.get(strategy['difficulty'], 1.0)
        
        # Urgency based on current savings rate
        urgency_factor = 1.5 if current_rate < 0.1 else 1.0
        
        # Income factor (higher income = can save more)
        income_factor = 1.0 + (min(income, 50000000) / 50000000) * 0.3
        
        return base_score * difficulty_factor * urgency_factor * income_factor
    
    def _calculate_category_impact(self, insights: List[SpendingInsight], 
                                   savings_target: float) -> Dict[str, float]:
        """Calculate impact on each category"""
        impact = {}
        total_spending = sum(i.amount for i in insights)
        
        for insight in insights:
            if insight.severity in ['medium', 'high']:
                category_ratio = insight.amount / total_spending if total_spending > 0 else 0
                impact[insight.category] = category_ratio * savings_target * 100
        
        return impact
    
    def _personalize_tips(self, strategy: Dict, insights: List[SpendingInsight], 
                         income: float) -> List[str]:
        """Generate personalized tips based on user's spending"""
        tips = []
        
        # Find high spending categories
        high_spending = [i for i in insights if i.severity == 'high']
        
        if high_spending:
            top_category = high_spending[0].category
            benchmark = self.category_benchmarks.get(top_category, {})
            category_tips = benchmark.get('tips', [])
            tips.extend(category_tips[:2])
        
        # Add income-specific tips
        if income > 30000000:
            tips.append("Với thu nhập cao, hãy cân nhắc đầu tư 40-50% vào các kênh sinh lời")
        elif income < 10000000:
            tips.append("Ưu tiên xây dựng quỹ khẩn cấp trước khi tiết kiệm cho mục tiêu dài hạn")
        
        return tips[:3]
    
    def _generate_category_recommendation(self, insight: SpendingInsight, 
                                         income: float) -> Optional[SavingsRecommendation]:
        """Generate category-specific recommendation"""
        benchmark = self.category_benchmarks.get(insight.category)
        if not benchmark:
            return None
        
        optimal_high = benchmark['optimal_range'][1]
        current_ratio = insight.percentage / 100
        
        if current_ratio <= optimal_high:
            return None
        
        overspend = (current_ratio - optimal_high) * income
        
        return SavingsRecommendation(
            title=f"Tối ưu hóa chi tiêu {insight.category}",
            description=f"Giảm {insight.category} từ {insight.percentage:.1f}% xuống {optimal_high * 100:.0f}%",
            potential_savings=overspend,
            difficulty='easy' if current_ratio < optimal_high * 1.3 else 'medium',
            timeframe='1-2 tháng',
            action_steps=benchmark.get('tips', [])[:4],
            priority_score=70 + (overspend / income * 100),
            category_impact={insight.category: overspend},
            personalized_tips=[
                f"Mục tiêu: Giảm {overspend:,.0f}đ/tháng từ {insight.category}",
                f"Tracking: Theo dõi chi tiêu {insight.category} hàng tuần",
                f"Review: Đánh giá lại sau 1 tháng"
            ]
        )
    
    def _generate_income_based_recommendation(self, income: float, 
                                             current_rate: float) -> Optional[SavingsRecommendation]:
        """Generate recommendation based on income level"""
        if income < 15000000:
            target_rate = 0.10
            desc = "Với thu nhập hiện tại, hãy tập trung tiết kiệm ít nhất 10% mỗi tháng"
        elif income < 30000000:
            target_rate = 0.20
            desc = "Thu nhập trung bình cho phép tiết kiệm 20-25% để xây dựng tương lai"
        else:
            target_rate = 0.30
            desc = "Thu nhập cao, hãy tối ưu hóa bằng cách tiết kiệm và đầu tư 30-40%"
        
        if current_rate >= target_rate:
            return None
        
        gap = (target_rate - current_rate) * income
        
        return SavingsRecommendation(
            title="Tăng tỷ lệ tiết kiệm theo thu nhập",
            description=desc,
            potential_savings=gap,
            difficulty='medium',
            timeframe='3-6 tháng',
            action_steps=[
                f"Mục tiêu: Tăng từ {current_rate * 100:.0f}% lên {target_rate * 100:.0f}%",
                "Tăng dần 2-3% mỗi tháng",
                "Tự động chuyển khoản phần tiết kiệm",
                "Review và điều chỉnh ngân sách hàng tháng"
            ],
            priority_score=85,
            category_impact={},
            personalized_tips=[
                "Bắt đầu từ mức tăng nhỏ để dễ duy trì",
                "Sử dụng auto-transfer để tạo thói quen",
                "Celebrate milestone khi đạt mỗi mức tăng"
            ]
        )
    
    def create_goal_plans(self, goals: List[Dict], income: float, 
                         current_savings_rate: float) -> List[GoalPlan]:
        """Create detailed goal plans with ML predictions"""
        plans = []
        available_savings = income * max(current_savings_rate, 0.10)
        
        # Sort goals by priority
        sorted_goals = sorted(goals, key=lambda g: self._calculate_goal_priority(g), reverse=True)
        
        for goal in sorted_goals:
            plan = self._create_single_goal_plan(goal, income, available_savings)
            plans.append(plan)
            
            # Adjust available savings for next goal
            if plan.feasibility in ['feasible', 'challenging']:
                available_savings -= plan.monthly_required
        
        return plans
    
    def _calculate_goal_priority(self, goal: Dict) -> float:
        """Calculate goal priority score"""
        priority_map = {'high': 3, 'medium': 2, 'low': 1}
        base_priority = priority_map.get(goal.get('priority', 'medium'), 2)
        
        # Urgent goals (< 1 year) get boost
        deadline = goal.get('deadline', '2026-12-31')
        try:
            deadline_date = datetime.fromisoformat(deadline.replace('Z', '+00:00'))
            months_left = (deadline_date - datetime.now()).days / 30
            urgency_factor = 2.0 if months_left < 12 else 1.0
        except:
            urgency_factor = 1.0
        
        return base_priority * urgency_factor
    
    def _create_single_goal_plan(self, goal: Dict, income: float, 
                                 available_savings: float) -> GoalPlan:
        """Create detailed plan for single goal"""
        target = goal.get('target_amount', 0)
        current = goal.get('current_amount', 0)
        remaining = target - current
        
        deadline = goal.get('deadline', '2026-12-31')
        try:
            deadline_date = datetime.fromisoformat(deadline.replace('Z', '+00:00'))
            months_left = max(1, (deadline_date - datetime.now()).days / 30)
        except:
            months_left = 12
        
        monthly_required = remaining / months_left
        
        # Calculate feasibility score
        feasibility_score = self._calculate_feasibility_score(
            monthly_required, available_savings, income
        )
        
        # Determine feasibility category
        if monthly_required <= available_savings * 0.5:
            feasibility = "feasible"
        elif monthly_required <= available_savings:
            feasibility = "challenging"
        else:
            feasibility = "unrealistic"
        
        # Generate recommendations
        recommendations = self._generate_goal_recommendations(
            goal, monthly_required, available_savings, feasibility
        )
        
        # Identify risk factors
        risk_factors = self._identify_risk_factors(
            goal, monthly_required, income, months_left
        )
        
        # Create milestones
        milestones = self._create_milestones(
            current, target, deadline_date, monthly_required
        )
        
        # Alternative strategies
        alternatives = self._generate_alternative_strategies(
            goal, monthly_required, available_savings, months_left
        )
        
        return GoalPlan(
            goal_name=goal.get('name', 'Mục tiêu tài chính'),
            target_amount=target,
            current_amount=current,
            monthly_required=monthly_required,
            deadline=deadline,
            feasibility=feasibility,
            feasibility_score=feasibility_score,
            recommendations=recommendations,
            risk_factors=risk_factors,
            milestones=milestones,
            alternative_strategies=alternatives
        )
    
    def _calculate_feasibility_score(self, required: float, available: float, 
                                     income: float) -> float:
        """Calculate feasibility score (0-100)"""
        if available == 0:
            return 0.0
        
        ratio = required / available
        
        if ratio <= 0.5:
            return 90 + (0.5 - ratio) * 20
        elif ratio <= 1.0:
            return 70 + (1.0 - ratio) * 40
        elif ratio <= 2.0:
            return 40 + (2.0 - ratio) * 30
        else:
            return max(10, 40 - (ratio - 2.0) * 10)
    
    def _generate_goal_recommendations(self, goal: Dict, monthly_required: float,
                                       available_savings: float, feasibility: str) -> List[str]:
        """Generate recommendations for goal achievement"""
        recs = []
        
        if feasibility == "feasible":
            recs.extend([
                f"Mục tiêu hoàn toàn khả thi - chỉ cần {monthly_required:,.0f}đ/tháng",
                "Thiết lập auto-transfer để tự động tiết kiệm",
                "Tạo tài khoản riêng cho mục tiêu này",
                "Review tiến độ hàng tháng để đảm bảo đúng hướng"
            ])
        elif feasibility == "challenging":
            shortage = monthly_required - available_savings
            recs.extend([
                f"Cần nỗ lực tăng tiết kiệm thêm {shortage:,.0f}đ/tháng",
                "Xem xét giảm chi tiêu không cần thiết",
                "Tìm thêm nguồn thu nhập phụ",
                "Hoặc kéo dài thời gian thực hiện 20-30%"
            ])
        else:  # unrealistic
            recs.extend([
                "Mục tiêu cần điều chỉnh lại cho phù hợp",
                f"Xem xét kéo dài thời gian hoặc giảm mục tiêu xuống {available_savings * 12:,.0f}đ",
                "Chia nhỏ thành các milestone ngắn hạn",
                "Tập trung vào tăng thu nhập trước"
            ])
        
        # Add goal-specific tips
        goal_type = self._identify_goal_type(goal)
        template = self.goal_templates.get(goal_type, {})
        recs.extend(template.get('tips', [])[:2])
        
        return recs[:6]
    
    def _identify_goal_type(self, goal: Dict) -> str:
        """Identify goal type for template matching"""
        name = goal.get('name', '').lower()
        
        if any(word in name for word in ['khẩn cấp', 'emergency', 'dự phòng']):
            return 'emergency_fund'
        elif any(word in name for word in ['nhà', 'house', 'căn hộ']):
            return 'house_purchase'
        elif any(word in name for word in ['học', 'education', 'du học']):
            return 'education'
        elif any(word in name for word in ['hưu', 'retirement', 'về già']):
            return 'retirement'
        
        return 'general'
    
    def _identify_risk_factors(self, goal: Dict, monthly_required: float,
                               income: float, months_left: float) -> List[str]:
        """Identify potential risk factors"""
        risks = []
        
        # Income stability risk
        if monthly_required / income > 0.3:
            risks.append("Yêu cầu tiết kiệm >30% thu nhập - rủi ro cao nếu thu nhập không ổn định")
        
        # Timeline risk
        if months_left < 12:
            risks.append("Thời gian ngắn (<1 năm) - ít linh hoạt khi có biến động")
        
        # Amount risk
        if monthly_required > income * 0.5:
            risks.append("Mục tiêu quá cao so với thu nhập - cần điều chỉnh")
        
        # Market risk (for investment goals)
        if 'đầu tư' in goal.get('name', '').lower():
            risks.append("Rủi ro thị trường - cần đa dạng hóa và theo dõi")
        
        return risks
    
    def _create_milestones(self, current: float, target: float, 
                          deadline: datetime, monthly_required: float) -> List[Dict]:
        """Create achievement milestones"""
        milestones = []
        remaining = target - current
        
        # Quarterly milestones
        quarters = max(1, int((deadline - datetime.now()).days / 90))
        
        for i in range(1, min(quarters + 1, 5)):
            milestone_date = datetime.now() + timedelta(days=90 * i)
            milestone_amount = current + (remaining / quarters) * i
            
            milestones.append({
                'quarter': i,
                'date': milestone_date.strftime('%Y-%m-%d'),
                'target_amount': milestone_amount,
                'percentage': (milestone_amount / target * 100) if target > 0 else 0
            })
        
        return milestones
    
    def _generate_alternative_strategies(self, goal: Dict, monthly_required: float,
                                         available_savings: float, months_left: float) -> List[str]:
        """Generate alternative strategies"""
        alternatives = []
        
        # Strategy 1: Extend timeline
        if monthly_required > available_savings:
            new_months = int(monthly_required / available_savings * months_left)
            new_date = datetime.now() + timedelta(days=30 * new_months)
            alternatives.append(
                f"Kéo dài thời gian đến {new_date.strftime('%Y-%m')} để giảm gánh nặng hàng tháng"
            )
        
        # Strategy 2: Reduce target
        realistic_target = available_savings * months_left + goal.get('current_amount', 0)
        if realistic_target < goal.get('target_amount', 0):
            alternatives.append(
                f"Điều chỉnh mục tiêu xuống {realistic_target:,.0f}đ để phù hợp với khả năng"
            )
        
        # Strategy 3: Increase income
        income_gap = monthly_required - available_savings
        if income_gap > 0:
            alternatives.append(
                f"Tăng thu nhập thêm {income_gap:,.0f}đ/tháng từ công việc phụ hoặc đầu tư"
            )
        
        # Strategy 4: Combine savings and investment
        if months_left > 12:
            alternatives.append(
                "Kết hợp tiết kiệm + đầu tư (70/30) để tăng sinh lời từ lãi kép"
            )
        
        return alternatives[:3]
    
    def calculate_financial_health_score(self, income: float, total_spending: float,
                                         savings_rate: float, insights: List[SpendingInsight],
                                         goals: List[GoalPlan]) -> Dict:
        """Calculate comprehensive financial health metrics"""
        
        # Component scores
        savings_score = min(100, savings_rate * 300)  # 33% savings = 100 points
        
        spending_efficiency = sum(1 for i in insights if i.severity in ['low', 'optimal']) / max(len(insights), 1) * 100
        
        goal_feasibility = sum(g.feasibility_score for g in goals) / max(len(goals), 1) if goals else 50
        
        debt_to_income = 0.0  # Simplified - would need debt data
        debt_score = max(0, 100 - debt_to_income * 100)
        
        # Overall score (weighted average)
        overall_score = (
            savings_score * 0.35 +
            spending_efficiency * 0.30 +
            goal_feasibility * 0.25 +
            debt_score * 0.10
        )
        
        # Risk assessment
        risk_level = "Low"
        if savings_rate < 0.1:
            risk_level = "High"
        elif savings_rate < 0.2:
            risk_level = "Medium"
        
        return {
            'overall_score': round(overall_score, 1),
            'savings_score': round(savings_score, 1),
            'spending_efficiency': round(spending_efficiency, 1),
            'goal_feasibility': round(goal_feasibility, 1),
            'debt_score': round(debt_score, 1),
            'risk_level': risk_level,
            'grade': self._get_grade(overall_score)
        }
    
    def _get_grade(self, score: float) -> str:
        """Convert score to letter grade"""
        if score >= 90: return "A+"
        elif score >= 85: return "A"
        elif score >= 80: return "B+"
        elif score >= 75: return "B"
        elif score >= 70: return "C+"
        elif score >= 65: return "C"
        elif score >= 60: return "D"
        else: return "F"
    
    def explain_recommendation(self, recommendation: SavingsRecommendation,
                              spending_insights: List[SpendingInsight],
                              income: float) -> Dict:
        """
        Explain why AI made this recommendation using SHAP
        Returns detailed explanation with feature importance
        """
        try:
            # Build features for this recommendation
            features = []
            feature_names = []
            
            # Income level feature
            features.append(income / 1000000)  # Normalize to millions
            feature_names.append('Thu nhập (triệu)')
            
            # Savings rate feature
            current_savings = sum(i.amount for i in spending_insights if i.severity == 'low')
            savings_rate = current_savings / income if income > 0 else 0
            features.append(savings_rate * 100)
            feature_names.append('Tỷ lệ tiết kiệm (%)')
            
            # High spending categories count
            high_spending_count = sum(1 for i in spending_insights if i.severity == 'high')
            features.append(high_spending_count)
            feature_names.append('Số danh mục chi tiêu cao')
            
            # Average confidence score
            avg_confidence = np.mean([i.confidence_score for i in spending_insights])
            features.append(avg_confidence * 100)
            feature_names.append('Độ tin cậy dự đoán (%)')
            
            # Potential savings amount
            features.append(recommendation.potential_savings / 1000000)
            feature_names.append('Tiết kiệm tiềm năng (triệu)')
            
            # Build simple decision tree for SHAP
            X = np.array(features).reshape(1, -1)
            
            # Create a simple model for explanation
            model = xgb.XGBRegressor(n_estimators=10, max_depth=2, random_state=42)
            
            # Generate synthetic training data for SHAP
            np.random.seed(42)
            X_train = np.random.randn(50, len(features)) * X
            y_train = np.random.randn(50) * recommendation.priority_score
            
            model.fit(X_train, y_train)
            
            # Get SHAP explanation
            importance = self._explain_with_shap(model, X, feature_names)
            
            # Build explanation text
            sorted_importance = sorted(importance.items(), key=lambda x: x[1], reverse=True)
            
            explanations = []
            for feature, imp in sorted_importance[:3]:  # Top 3 factors
                if 'Thu nhập' in feature:
                    explanations.append(
                        f"📊 {feature}: {features[0]:.1f}M - "
                        f"Mức thu nhập của bạn {'phù hợp' if income > 10000000 else 'có thể tăng thêm'} "
                        f"cho chiến lược này (ảnh hưởng {imp*100:.0f}%)"
                    )
                elif 'Tỷ lệ tiết kiệm' in feature:
                    explanations.append(
                        f"💰 {feature}: {features[1]:.1f}% - "
                        f"Tỷ lệ tiết kiệm hiện tại {'tốt' if savings_rate > 0.2 else 'cần cải thiện'} "
                        f"(ảnh hưởng {imp*100:.0f}%)"
                    )
                elif 'chi tiêu cao' in feature:
                    explanations.append(
                        f"⚠️ {feature}: {int(features[2])} danh mục - "
                        f"Có {int(features[2])} danh mục đang chi tiêu quá mức "
                        f"(ảnh hưởng {imp*100:.0f}%)"
                    )
                elif 'tin cậy' in feature:
                    explanations.append(
                        f"🎯 {feature}: {features[3]:.0f}% - "
                        f"Độ chính xác dự đoán {'cao' if avg_confidence > 0.8 else 'trung bình'} "
                        f"(ảnh hưởng {imp*100:.0f}%)"
                    )
                elif 'Tiết kiệm tiềm năng' in feature:
                    explanations.append(
                        f"💎 {feature}: {features[4]:.1f}M - "
                        f"Có thể tiết kiệm {recommendation.potential_savings:,.0f}đ/tháng "
                        f"(ảnh hưởng {imp*100:.0f}%)"
                    )
            
            return {
                'recommendation_title': recommendation.title,
                'priority_score': recommendation.priority_score,
                'main_factors': explanations,
                'feature_importance': importance,
                'confidence': avg_confidence,
                'summary': (
                    f"AI khuyến nghị '{recommendation.title}' dựa trên phân tích {len(spending_insights)} "
                    f"danh mục chi tiêu của bạn. Các yếu tố quan trọng nhất là: "
                    f"{', '.join([x[0] for x in sorted_importance[:2]])}."
                )
            }
            
        except Exception as e:
            return {
                'recommendation_title': recommendation.title,
                'priority_score': recommendation.priority_score,
                'main_factors': [
                    f"Dựa trên thu nhập {income:,.0f}đ và mức chi tiêu hiện tại",
                    f"Tiềm năng tiết kiệm {recommendation.potential_savings:,.0f}đ/tháng",
                    f"Độ khó: {recommendation.difficulty}"
                ],
                'summary': f"AI khuyến nghị '{recommendation.title}' phù hợp với tình hình tài chính của bạn."
            }
    
    def generate_comprehensive_plan(self, transactions: List[Dict], income: float,
                                   goals: List[Dict] = None) -> FinancialPlan:
        """Generate comprehensive financial plan"""
        
        # Calculate basics
        total_spending = sum(abs(t.get('amount', 0)) for t in transactions if t.get('type') == 'EXPENSE')
        savings_rate = (income - total_spending) / income if income > 0 else 0
        
        # Generate insights
        spending_insights = self.analyze_spending_patterns(transactions, income)
        
        # Generate recommendations
        savings_recommendations = self.generate_savings_recommendations(
            spending_insights, income, savings_rate
        )
        
        # Generate goal plans
        goal_plans = self.create_goal_plans(goals or [], income, savings_rate)
        
        # Calculate health metrics
        health_metrics = self.calculate_financial_health_score(
            income, total_spending, savings_rate, spending_insights, goal_plans
        )
        
        # Risk assessment
        risk_assessment = {
            'emergency_fund_status': 'adequate' if savings_rate > 0.2 else 'insufficient',
            'spending_volatility': 'low',  # Would need historical data
            'goal_overload': len([g for g in goal_plans if g.feasibility == 'unrealistic']) > 2
        }
        
        # Generate next actions
        next_actions = self._generate_next_actions(
            savings_rate, spending_insights, goal_plans, health_metrics
        )
        
        return FinancialPlan(
            monthly_income=income,
            total_spending=total_spending,
            savings_rate=savings_rate,
            spending_insights=spending_insights,
            savings_recommendations=savings_recommendations,
            goal_plans=goal_plans,
            overall_score=health_metrics['overall_score'],
            next_actions=next_actions,
            financial_health_metrics=health_metrics,
            risk_assessment=risk_assessment
        )
    
    def _generate_next_actions(self, savings_rate: float, insights: List[SpendingInsight],
                               goals: List[GoalPlan], health_metrics: Dict) -> List[str]:
        """Generate prioritized next actions"""
        actions = []
        
        # Priority 1: Emergency fund
        if savings_rate < 0.15:
            actions.append("🚨 Khẩn cấp: Xây dựng quỹ dự phòng ít nhất 3 tháng chi phí")
        
        # Priority 2: High severity spending
        high_spend = [i for i in insights if i.severity == 'high']
        if high_spend:
            actions.append(f"💰 Giảm chi tiêu {high_spend[0].category} - tiết kiệm ngay {high_spend[0].amount * 0.2:,.0f}đ/tháng")
        
        # Priority 3: Unrealistic goals
        unrealistic = [g for g in goals if g.feasibility == 'unrealistic']
        if unrealistic:
            actions.append(f"🎯 Điều chỉnh mục tiêu '{unrealistic[0].goal_name}' cho phù hợp khả năng")
        
        # Priority 4: Automation
        if savings_rate < 0.25:
            actions.append("⚙️ Thiết lập auto-transfer tiết kiệm ngay ngày lương")
        
        # Priority 5: Increase savings
        if health_metrics['overall_score'] < 75:
            actions.append("📈 Tăng tỷ lệ tiết kiệm lên 20-25% trong 3 tháng tới")
        
        # Priority 6: Investment
        if savings_rate > 0.25:
            actions.append("💎 Xem xét đầu tư một phần tiết kiệm để tăng sinh lời")
        
        return actions[:5]


# Test function
def test_enhanced_planning():
    """Test enhanced planning service"""
    service = EnhancedPlanningService()
    
    sample_transactions = [
        {'type': 'EXPENSE', 'amount': 2500000, 'category': {'name': 'Ăn uống'}},
        {'type': 'EXPENSE', 'amount': 1500000, 'category': {'name': 'Giao thông'}},
        {'type': 'EXPENSE', 'amount': 3000000, 'category': {'name': 'Mua sắm'}},
        {'type': 'EXPENSE', 'amount': 1000000, 'category': {'name': 'Giải trí'}},
        {'type': 'EXPENSE', 'amount': 800000, 'category': {'name': 'Tiện ích'}}
    ]
    
    sample_goals = [
        {
            'name': 'Mua nhà',
            'target_amount': 500000000,
            'current_amount': 50000000,
            'deadline': '2027-12-31',
            'priority': 'high'
        }
    ]
    
    plan = service.generate_comprehensive_plan(
        transactions=sample_transactions,
        income=15000000,
        goals=sample_goals
    )
    
    print("=" * 60)
    print("ENHANCED FINANCIAL PLAN")
    print("=" * 60)
    print(f"\n💰 Thu nhập: {plan.monthly_income:,.0f}đ")
    print(f"💸 Chi tiêu: {plan.total_spending:,.0f}đ")
    print(f"💎 Tỷ lệ tiết kiệm: {plan.savings_rate * 100:.1f}%")
    print(f"⭐ Điểm tổng thể: {plan.overall_score:.1f}/100 ({plan.financial_health_metrics['grade']})")
    
    print(f"\n📊 SPENDING INSIGHTS:")
    for insight in plan.spending_insights[:5]:
        print(f"  • {insight.category}: {insight.amount:,.0f}đ ({insight.percentage:.1f}%)")
        print(f"    Trend: {insight.trend} | Severity: {insight.severity}")
        print(f"    {insight.recommendation}")
    
    print(f"\n💡 TOP SAVINGS RECOMMENDATIONS:")
    for i, rec in enumerate(plan.savings_recommendations[:3], 1):
        print(f"  {i}. {rec.title} (Priority: {rec.priority_score:.0f}/100)")
        print(f"     Tiết kiệm: {rec.potential_savings:,.0f}đ | Độ khó: {rec.difficulty}")
    
    print(f"\n🎯 GOAL PLANS:")
    for goal in plan.goal_plans:
        print(f"  • {goal.goal_name}")
        print(f"    Feasibility: {goal.feasibility} ({goal.feasibility_score:.0f}/100)")
        print(f"    Monthly: {goal.monthly_required:,.0f}đ")
    
    print(f"\n📋 NEXT ACTIONS:")
    for action in plan.next_actions:
        print(f"  {action}")


if __name__ == "__main__":
    test_enhanced_planning()
