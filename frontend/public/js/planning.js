/**
 * Financial Planning JavaScript
 * AI-powered financial planning and analysis
 */

class FinancialPlanningManager {
    constructor() {
        this.apiBaseUrl = window.location.origin + '/api/planning';
        this.transactionsData = [];
        this.monthlyIncome = 0;
        this.goals = [];
        
        this.initializeEventListeners();
        this.loadExistingData();
    }

    initializeEventListeners() {
        // Form submission
        document.getElementById('planningForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.analyzePlan();
        });

        // Add goal button
        document.getElementById('addGoalBtn').addEventListener('click', () => {
            this.addGoalInput();
        });

        // Generate plan button
        document.getElementById('generatePlanBtn').addEventListener('click', () => {
            this.generateQuickPlan();
        });

        // Monthly income input change
        document.getElementById('monthlyIncome').addEventListener('input', (e) => {
            this.monthlyIncome = parseFloat(e.target.value) || 0;
            this.updateIncomeDisplay();
            this.generateQuickInsights();
        });

        // Remove goal buttons
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('remove-goal') || e.target.closest('.remove-goal')) {
                e.target.closest('.goal-item').remove();
            }
        });
    }

    addGoalInput() {
        const container = document.getElementById('goalsContainer');
        const goalItem = document.createElement('div');
        goalItem.className = 'goal-item mb-2';
        goalItem.innerHTML = `
            <div class="input-group">
                <input type="text" class="form-control goal-name" 
                       placeholder="Tên mục tiêu (VD: Mua xe)">
                <input type="number" class="form-control goal-amount" 
                       placeholder="Số tiền" min="0" step="1000000">
                <span class="input-group-text">₫</span>
                <button type="button" class="btn btn-outline-danger remove-goal">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `;
        container.appendChild(goalItem);
    }

    updateIncomeDisplay() {
        document.getElementById('monthlyIncomeDisplay').textContent = 
            this.formatCurrency(this.monthlyIncome);
    }

    async loadExistingData() {
        try {
            // Load recent transactions for analysis
            const response = await fetch('/api/transactions/recent');
            if (response.ok) {
                const data = await response.json();
                this.transactionsData = data.transactions || [];
                this.calculateCurrentSpending();
            }
        } catch (error) {
            console.error('Error loading existing data:', error);
        }
    }

    calculateCurrentSpending() {
        const currentMonth = new Date().getMonth();
        const currentYear = new Date().getFullYear();
        
        const monthlySpending = this.transactionsData
            .filter(t => {
                const transDate = new Date(t.date);
                return transDate.getMonth() === currentMonth && 
                       transDate.getFullYear() === currentYear &&
                       t.amount < 0; // Spending transactions
            })
            .reduce((sum, t) => sum + Math.abs(t.amount), 0);

        document.getElementById('monthlySpendingDisplay').textContent = 
            this.formatCurrency(monthlySpending);

        // Calculate savings rate
        if (this.monthlyIncome > 0) {
            const savingsRate = ((this.monthlyIncome - monthlySpending) / this.monthlyIncome * 100).toFixed(1);
            document.getElementById('savingsRateDisplay').textContent = `${savingsRate}%`;
        }
    }

    async generateQuickInsights() {
        if (this.monthlyIncome <= 0) return;

        try {
            const quickInsightsContainer = document.getElementById('quickInsights');
            
            // Basic financial health check
            const spending = this.calculateMonthlySpending();
            const savingsRate = ((this.monthlyIncome - spending) / this.monthlyIncome * 100);
            
            let insights = [];
            let overallHealth = 'good';

            if (savingsRate < 10) {
                insights.push({
                    type: 'warning',
                    icon: 'fas fa-exclamation-triangle',
                    text: 'Tỷ lệ tiết kiệm thấp. Nên tiết kiệm ít nhất 10% thu nhập.'
                });
                overallHealth = 'poor';
            } else if (savingsRate >= 20) {
                insights.push({
                    type: 'success',
                    icon: 'fas fa-thumbs-up',
                    text: 'Tuyệt vời! Bạn đang tiết kiệm được hơn 20% thu nhập.'
                });
            }

            if (spending > this.monthlyIncome * 0.8) {
                insights.push({
                    type: 'danger',
                    icon: 'fas fa-credit-card',
                    text: 'Chi tiêu cao. Hãy xem xét cắt giảm một số khoản chi không cần thiết.'
                });
            }

            // Emergency fund check
            const emergencyFund = this.monthlyIncome * 3; // 3 months
            insights.push({
                type: 'info',
                icon: 'fas fa-shield-alt',
                text: `Quỹ khẩn cấp nên có: ${this.formatCurrency(emergencyFund)} (3 tháng sinh hoạt)`
            });

            this.renderQuickInsights(insights);
            
        } catch (error) {
            console.error('Error generating quick insights:', error);
        }
    }

    renderQuickInsights(insights) {
        const container = document.getElementById('quickInsights');
        
        if (insights.length === 0) {
            container.innerHTML = `
                <div class="text-center text-muted">
                    <i class="fas fa-robot fa-3x mb-3"></i>
                    <p>Nhập thông tin tài chính để nhận gợi ý từ AI</p>
                </div>
            `;
            return;
        }

        container.innerHTML = insights.map(insight => `
            <div class="alert alert-${insight.type} mb-2">
                <i class="${insight.icon} me-2"></i>
                ${insight.text}
            </div>
        `).join('');
    }

    calculateMonthlySpending() {
        const currentMonth = new Date().getMonth();
        const currentYear = new Date().getFullYear();
        
        return this.transactionsData
            .filter(t => {
                const transDate = new Date(t.date);
                return transDate.getMonth() === currentMonth && 
                       transDate.getFullYear() === currentYear &&
                       t.amount < 0;
            })
            .reduce((sum, t) => sum + Math.abs(t.amount), 0);
    }

    async analyzePlan() {
        const form = document.getElementById('planningForm');
        const formData = new FormData(form);
        
        // Get form data
        const monthlyIncome = parseFloat(document.getElementById('monthlyIncome').value);
        const analysisperiod = document.getElementById('analysisperiod').value;
        
        if (!monthlyIncome || monthlyIncome <= 0) {
            this.showError('Vui lòng nhập thu nhập hàng tháng hợp lệ');
            return;
        }

        // Collect goals
        const goals = [];
        document.querySelectorAll('.goal-item').forEach(item => {
            const name = item.querySelector('.goal-name').value.trim();
            const amount = parseFloat(item.querySelector('.goal-amount').value);
            
            if (name && amount > 0) {
                goals.push({
                    goal_name: name,
                    target_amount: amount,
                    current_amount: 0,
                    deadline: "12 months" // Default
                });
            }
        });

        this.showLoading(true);

        try {
            // Prepare transactions data based on analysis period
            const transactions = this.prepareTransactionsData(analysisperiod);
            
            const requestData = {
                transactions: transactions,
                monthly_income: monthlyIncome,
                goals: goals,
                user_id: this.getCurrentUserId()
            };

            const response = await fetch(`${this.apiBaseUrl}/analyze`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.getAuthToken()}`
                },
                body: JSON.stringify(requestData)
            });

            if (response.ok) {
                const analysisResult = await response.json();
                this.displayAnalysisResults(analysisResult);
                this.updateFinancialScore(analysisResult.overall_score);
            } else {
                const error = await response.json();
                this.showError(error.error || 'Có lỗi xảy ra khi phân tích kế hoạch');
            }
            
        } catch (error) {
            console.error('Error analyzing plan:', error);
            this.showError('Không thể kết nối đến server. Vui lòng thử lại.');
        } finally {
            this.showLoading(false);
        }
    }

    prepareTransactionsData(period) {
        const now = new Date();
        let startDate;
        
        switch (period) {
            case 'current_month':
                startDate = new Date(now.getFullYear(), now.getMonth(), 1);
                break;
            case 'last_3_months':
                startDate = new Date(now.getFullYear(), now.getMonth() - 3, 1);
                break;
            case 'last_6_months':
                startDate = new Date(now.getFullYear(), now.getMonth() - 6, 1);
                break;
            default:
                startDate = new Date(now.getFullYear(), now.getMonth(), 1);
        }

        return this.transactionsData
            .filter(t => new Date(t.date) >= startDate)
            .map(t => ({
                description: t.description || '',
                amount: Math.abs(t.amount),
                category: t.category || 'Other',
                date: t.date,
                type: t.amount < 0 ? 'expense' : 'income'
            }));
    }

    displayAnalysisResults(result) {
        // Update stats
        document.getElementById('monthlyIncomeDisplay').textContent = 
            this.formatCurrency(result.monthly_income);
        document.getElementById('monthlySpendingDisplay').textContent = 
            this.formatCurrency(result.total_spending);
        document.getElementById('savingsRateDisplay').textContent = 
            `${result.savings_rate.toFixed(1)}%`;

        // Show results section
        document.getElementById('analysisResults').style.display = 'block';
        
        // Display spending insights
        this.renderSpendingInsights(result.spending_insights);
        
        // Display savings recommendations
        this.renderSavingsRecommendations(result.savings_recommendations);
        
        // Display goal plans
        this.renderGoalPlans(result.goal_plans);
        
        // Display next actions
        this.renderNextActions(result.next_actions);

        // Scroll to results
        document.getElementById('analysisResults').scrollIntoView({ 
            behavior: 'smooth' 
        });
    }

    renderSpendingInsights(insights) {
        const container = document.getElementById('spendingInsights');
        
        if (!insights || insights.length === 0) {
            container.innerHTML = '<p class="text-muted">Không có dữ liệu chi tiêu để phân tích.</p>';
            return;
        }

        container.innerHTML = insights.map(insight => `
            <div class="insight-card severity-${insight.severity}">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <h6 class="fw-bold">${insight.category}</h6>
                        <p class="mb-1">${insight.recommendation}</p>
                        <small class="text-muted">Xu hướng: ${insight.trend}</small>
                    </div>
                    <div class="text-end">
                        <strong class="fs-5">${this.formatCurrency(insight.amount)}</strong>
                        <br>
                        <span class="badge bg-secondary">${insight.percentage.toFixed(1)}%</span>
                    </div>
                </div>
            </div>
        `).join('');
    }

    renderSavingsRecommendations(recommendations) {
        const container = document.getElementById('savingsRecommendations');
        
        if (!recommendations || recommendations.length === 0) {
            container.innerHTML = '<p class="text-muted">Không có gợi ý tiết kiệm.</p>';
            return;
        }

        container.innerHTML = recommendations.map(rec => `
            <div class="recommendation-card">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h6 class="fw-bold mb-0">${rec.title}</h6>
                    <span class="badge bg-success">${this.formatCurrency(rec.potential_savings)}/tháng</span>
                </div>
                <p class="text-muted mb-2">${rec.description}</p>
                <div class="row">
                    <div class="col-md-6">
                        <small><strong>Độ khó:</strong> ${rec.difficulty}</small>
                    </div>
                    <div class="col-md-6">
                        <small><strong>Thời gian:</strong> ${rec.timeframe}</small>
                    </div>
                </div>
                <div class="mt-2">
                    <strong>Các bước thực hiện:</strong>
                    <ul class="mt-1 mb-0">
                        ${rec.action_steps.map(step => `<li>${step}</li>`).join('')}
                    </ul>
                </div>
            </div>
        `).join('');
    }

    renderGoalPlans(goalPlans) {
        const container = document.getElementById('goalPlans');
        
        if (!goalPlans || goalPlans.length === 0) {
            container.innerHTML = '<p class="text-muted">Chưa có mục tiêu tài chính nào được thiết lập.</p>';
            return;
        }

        container.innerHTML = goalPlans.map(goal => {
            const progress = (goal.current_amount / goal.target_amount * 100).toFixed(1);
            const progressClass = goal.feasibility === 'Khả thi' ? 'bg-success' : 
                                 goal.feasibility === 'Trung bình' ? 'bg-warning' : 'bg-danger';
            
            return `
                <div class="card mb-3">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <h6 class="fw-bold mb-0">${goal.goal_name}</h6>
                            <span class="badge ${progressClass.replace('bg-', 'bg-opacity-10 text-')}">${goal.feasibility}</span>
                        </div>
                        
                        <div class="row mb-2">
                            <div class="col-md-6">
                                <small><strong>Mục tiêu:</strong> ${this.formatCurrency(goal.target_amount)}</small>
                            </div>
                            <div class="col-md-6">
                                <small><strong>Cần tiết kiệm:</strong> ${this.formatCurrency(goal.monthly_required)}/tháng</small>
                            </div>
                        </div>
                        
                        <div class="goal-progress mb-2">
                            <div class="goal-progress-bar ${progressClass}" 
                                 style="width: ${progress}%"></div>
                        </div>
                        <small class="text-muted">${progress}% hoàn thành • Hạn: ${goal.deadline}</small>
                        
                        <div class="mt-2">
                            <strong>Gợi ý:</strong>
                            <ul class="mt-1 mb-0">
                                ${goal.recommendations.map(rec => `<li>${rec}</li>`).join('')}
                            </ul>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    }

    renderNextActions(actions) {
        const container = document.getElementById('nextActions');
        
        if (!actions || actions.length === 0) {
            container.innerHTML = '<p class="text-muted">Không có hành động cụ thể nào được đề xuất.</p>';
            return;
        }

        container.innerHTML = `
            <div class="row">
                ${actions.map((action, index) => `
                    <div class="col-md-6 mb-3">
                        <div class="card h-100">
                            <div class="card-body">
                                <div class="d-flex align-items-start">
                                    <div class="badge bg-primary rounded-circle me-2 mt-1" 
                                         style="width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;">
                                        ${index + 1}
                                    </div>
                                    <p class="mb-0">${action}</p>
                                </div>
                            </div>
                        </div>
                    </div>
                `).join('')}
            </div>
        `;
    }

    updateFinancialScore(score) {
        const scoreElement = document.getElementById('financialScoreDisplay');
        scoreElement.textContent = `${score}/100`;
        
        // Add color coding
        scoreElement.className = '';
        if (score >= 80) {
            scoreElement.classList.add('text-success');
        } else if (score >= 60) {
            scoreElement.classList.add('text-warning');
        } else {
            scoreElement.classList.add('text-danger');
        }
    }

    async generateQuickPlan() {
        if (!this.monthlyIncome || this.monthlyIncome <= 0) {
            this.showError('Vui lòng nhập thu nhập hàng tháng trước');
            return;
        }

        // Auto-fill some basic goals
        const container = document.getElementById('goalsContainer');
        container.innerHTML = '';
        
        // Emergency fund
        this.addGoalWithData('Quỹ khẩn cấp', this.monthlyIncome * 3);
        
        // Savings goal
        this.addGoalWithData('Tiết kiệm dài hạn', this.monthlyIncome * 6);

        // Auto analyze
        this.analyzePlan();
    }

    addGoalWithData(name, amount) {
        const container = document.getElementById('goalsContainer');
        const goalItem = document.createElement('div');
        goalItem.className = 'goal-item mb-2';
        goalItem.innerHTML = `
            <div class="input-group">
                <input type="text" class="form-control goal-name" 
                       value="${name}">
                <input type="number" class="form-control goal-amount" 
                       value="${amount}" min="0" step="1000000">
                <span class="input-group-text">₫</span>
                <button type="button" class="btn btn-outline-danger remove-goal">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `;
        container.appendChild(goalItem);
    }

    // Utility methods
    formatCurrency(amount) {
        if (isNaN(amount)) return '0₫';
        return Math.round(amount).toLocaleString('vi-VN') + '₫';
    }

    showLoading(show) {
        document.getElementById('loadingOverlay').style.display = show ? 'flex' : 'none';
    }

    showError(message) {
        // Use toast or alert
        if (window.showToast) {
            window.showToast(message, 'error');
        } else {
            alert(message);
        }
    }

    showSuccess(message) {
        if (window.showToast) {
            window.showToast(message, 'success');
        } else {
            alert(message);
        }
    }

    getAuthToken() {
        return localStorage.getItem('authToken') || 
               sessionStorage.getItem('authToken') || 
               '';
    }

    getCurrentUserId() {
        const userStr = localStorage.getItem('user') || sessionStorage.getItem('user');
        if (userStr) {
            try {
                const user = JSON.parse(userStr);
                return user.id || null;
            } catch (e) {
                return null;
            }
        }
        return null;
    }
}

// Initialize when page loads
document.addEventListener('DOMContentLoaded', function() {
    window.planningManager = new FinancialPlanningManager();
    
    console.log('🎯 Financial Planning Manager initialized');
    
    // 🆕 Initialize Savings Tips Library
    initializeSavingsTipsLibrary();
});

/**
 * 🆕 Savings Tips Library Manager
 */
function initializeSavingsTipsLibrary() {
    const tabButtons = document.querySelectorAll('#tips-tabs button[data-category]');
    const searchBtn = document.getElementById('tips-search-btn');
    const searchInput = document.getElementById('tips-search-input');
    const contentDiv = document.getElementById('savings-tips-content');
    const statsDiv = document.getElementById('tips-stats');
    const tipsCountSpan = document.getElementById('tips-count');
    
    let currentCategory = 'general';
    
    // Load general tips on init
    loadTipsByCategory('general');
    
    // Tab click handlers
    tabButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            // Update active state
            tabButtons.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            
            const category = this.getAttribute('data-category');
            currentCategory = category;
            
            if (category === 'emergency') {
                loadEmergencyTips();
            } else if (category === 'general') {
                loadGeneralTips();
            } else {
                loadTipsByCategory(category);
            }
        });
    });
    
    // Search handler
    searchBtn.addEventListener('click', () => {
        const keyword = searchInput.value.trim();
        if (keyword) {
            searchTips(keyword);
        }
    });
    
    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            searchBtn.click();
        }
    });
    
    /**
     * Load tips by category
     */
    async function loadTipsByCategory(category, spendingLevel = 50) {
        showLoading();
        
        try {
            const response = await fetch(`http://localhost:8080/api/ai/savings-tips/${category}?spendingLevel=${spendingLevel}`, {
                headers: getAuthHeaders()
            });
            
            const data = await response.json();
            
            if (data.success && data.tips) {
                displayTips(data.tips, `Mẹo tiết kiệm cho ${category}`);
                updateStats(data.totalTips || data.tips.length);
            } else {
                showError('Không thể tải mẹo tiết kiệm');
            }
        } catch (error) {
            console.error('Error loading tips:', error);
            showError('Lỗi kết nối: ' + error.message);
        }
    }
    
    /**
     * Load general tips
     */
    async function loadGeneralTips(count = 10) {
        showLoading();
        
        try {
            const response = await fetch(`http://localhost:8080/api/ai/general-tips?count=${count}`, {
                headers: getAuthHeaders()
            });
            
            const data = await response.json();
            
            if (data.success && data.tips) {
                displayTips(data.tips, 'Mẹo tiết kiệm chung');
                updateStats(data.tips.length);
            } else {
                showError('Không thể tải mẹo chung');
            }
        } catch (error) {
            console.error('Error loading general tips:', error);
            showError('Lỗi kết nối: ' + error.message);
        }
    }
    
    /**
     * Load emergency tips
     */
    async function loadEmergencyTips() {
        showLoading();
        
        try {
            const response = await fetch('http://localhost:8080/api/ai/emergency-tips', {
                headers: getAuthHeaders()
            });
            
            const data = await response.json();
            
            if (data.success && data.tips) {
                displayTips(data.tips, '🚨 Giải pháp khẩn cấp giảm chi tiêu', true);
                updateStats(data.tips.length);
            } else {
                showError('Không thể tải mẹo khẩn cấp');
            }
        } catch (error) {
            console.error('Error loading emergency tips:', error);
            showError('Lỗi kết nối: ' + error.message);
        }
    }
    
    /**
     * Search tips by keyword
     */
    async function searchTips(keyword) {
        showLoading();
        
        try {
            const response = await fetch(`http://localhost:8080/api/ai/search-tips?keyword=${encodeURIComponent(keyword)}`, {
                headers: getAuthHeaders()
            });
            
            const data = await response.json();
            
            if (data.success && data.tips) {
                displayTips(data.tips, `Kết quả tìm kiếm: "${keyword}"`);
                updateStats(data.resultsCount || data.tips.length);
            } else {
                showError('Không tìm thấy kết quả');
            }
        } catch (error) {
            console.error('Error searching tips:', error);
            showError('Lỗi tìm kiếm: ' + error.message);
        }
    }
    
    /**
     * Display tips in cards
     */
    function displayTips(tips, title, isEmergency = false) {
        if (!tips || tips.length === 0) {
            contentDiv.innerHTML = '<div class="alert alert-warning">Không có mẹo nào trong danh mục này</div>';
            return;
        }
        
        let html = `<h6 class="mb-3">${title}</h6>`;
        html += '<div class="row">';
        
        tips.forEach((tip, index) => {
            const cardClass = isEmergency ? 'border-danger' : 'border-primary';
            const iconClass = isEmergency ? 'fa-exclamation-triangle text-danger' : 'fa-lightbulb text-warning';
            
            html += '<div class="col-md-6 mb-3">';
            html += `<div class="card ${cardClass} h-100">`;
            html += '<div class="card-body">';
            html += `<div class="d-flex align-items-start">`;
            html += `<i class="fas ${iconClass} fa-2x me-3"></i>`;
            html += '<div class="flex-grow-1">';
            
            if (tip.title) {
                html += `<h6 class="card-title">${tip.title}</h6>`;
            }
            
            html += `<p class="card-text">${tip.tip || tip.description || tip}</p>`;
            
            if (tip.potentialSavings) {
                html += `<p class="text-success mb-0"><strong>💰 Tiết kiệm: ${tip.potentialSavings.toLocaleString('vi-VN')} VNĐ/tháng</strong></p>`;
            }
            
            if (tip.difficulty) {
                const diffColor = tip.difficulty === 'EASY' ? 'success' : tip.difficulty === 'MEDIUM' ? 'warning' : 'danger';
                const diffText = tip.difficulty === 'EASY' ? 'Dễ' : tip.difficulty === 'MEDIUM' ? 'Trung bình' : 'Khó';
                html += `<span class="badge bg-${diffColor} mt-2">${diffText}</span>`;
            }
            
            html += '</div></div>';
            html += '</div></div>';
            html += '</div>';
        });
        
        html += '</div>';
        contentDiv.innerHTML = html;
    }
    
    /**
     * Show loading state
     */
    function showLoading() {
        contentDiv.innerHTML = `
            <div class="text-center text-muted py-4">
                <div class="spinner-border" role="status">
                    <span class="visually-hidden">Đang tải...</span>
                </div>
                <p class="mt-2">Đang tải mẹo tiết kiệm...</p>
            </div>
        `;
        statsDiv.style.display = 'none';
    }
    
    /**
     * Show error message
     */
    function showError(message) {
        contentDiv.innerHTML = `<div class="alert alert-danger">${message}</div>`;
        statsDiv.style.display = 'none';
    }
    
    /**
     * Update statistics
     */
    function updateStats(count) {
        tipsCountSpan.textContent = count;
        statsDiv.style.display = 'block';
    }
    
    /**
     * Get auth headers
     */
    function getAuthHeaders() {
        const token = localStorage.getItem('authToken');
        return {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
        };
    }
}

// Export for external access
window.FinancialPlanningManager = FinancialPlanningManager;