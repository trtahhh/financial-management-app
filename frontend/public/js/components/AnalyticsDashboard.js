// Interactive Analytics Dashboard
import { formatCurrency } from '../utils/currencyHelpers.js';
import { formatDate } from '../utils/dateHelpers.js';
import { CATEGORY_NAMES, CATEGORY_COLORS } from '../constants/categories.js';
import { COLORS } from '../constants/colors.js';

class AnalyticsDashboard {
  constructor(container, dataService, options = {}) {
    this.container = container;
    this.dataService = dataService;
    this.options = {
      enableCharts: true,
      enableExport: true,
      enableComparisons: true,
      enableForecasting: true,
      chartTypes: ['line', 'bar', 'pie', 'area', 'scatter'],
      timeframes: ['week', 'month', 'quarter', 'year'],
      ...options
    };
    
    this.currentTimeframe = 'month';
    this.currentChartType = 'line';
    this.currentData = null;
    this.charts = {};
    
    this.init();
  }

  async init() {
    this.render();
    this.setupEventListeners();
    await this.loadData();
    this.renderCharts();
  }

  render() {
    this.container.innerHTML = `
      <div class="analytics-dashboard">
        <div class="dashboard-header">
          <h2>Phân tích chi tiêu</h2>
          
          <div class="dashboard-controls">
            <div class="timeframe-selector">
              <label>Thời gian:</label>
              <select id="timeframe-select">
                <option value="week">Tuần này</option>
                <option value="month" selected>Tháng này</option>
                <option value="quarter">Quý này</option>
                <option value="year">Năm này</option>
                <option value="custom">Tùy chọn</option>
              </select>
            </div>
            
            <div class="chart-type-selector">
              <label>Biểu đồ:</label>
              <select id="chart-type-select">
                <option value="line">Đường</option>
                <option value="bar">Cột</option>
                <option value="pie">Tròn</option>
                <option value="area">Vùng</option>
                <option value="scatter">Phân tán</option>
              </select>
            </div>
            
            <div class="dashboard-actions">
              <button class="btn-export" id="btn-export-report">📊 Xuất báo cáo</button>
              <button class="btn-compare" id="btn-compare-periods">🔄 So sánh</button>
              <button class="btn-forecast" id="btn-show-forecast">🔮 Dự báo</button>
            </div>
          </div>
        </div>

        <div class="analytics-grid">
          ${this.renderSummaryCards()}
          ${this.renderMainChart()}
          ${this.renderCategoryAnalysis()}
          ${this.renderTrendAnalysis()}
          ${this.renderInsightsPanel()}
        </div>

        <div class="detailed-reports">
          ${this.renderDetailedTable()}
          ${this.renderPatternAnalysis()}
        </div>
      </div>
    `;

    this.renderStyles();
  }

  renderSummaryCards() {
    return `
      <div class="summary-cards">
        <div class="summary-card total-spending">
          <div class="card-icon">💰</div>
          <div class="card-content">
            <h3>Tổng chi tiêu</h3>
            <p class="amount" id="total-spending">--</p>
            <span class="change" id="spending-change">--</span>
          </div>
        </div>

        <div class="summary-card avg-transaction">
          <div class="card-icon">📊</div>
          <div class="card-content">
            <h3>TB giao dịch</h3>
            <p class="amount" id="avg-transaction">--</p>
            <span class="change" id="transaction-change">--</span>
          </div>
        </div>

        <div class="summary-card top-category">
          <div class="card-icon">🏆</div>
          <div class="card-content">
            <h3>Danh mục hàng đầu</h3>
            <p class="category" id="top-category">--</p>
            <span class="amount" id="top-category-amount">--</span>
          </div>
        </div>

        <div class="summary-card budget-status">
          <div class="card-icon">🎯</div>
          <div class="card-content">
            <h3>Trạng thái ngân sách</h3>
            <p class="status" id="budget-status">--</p>
            <span class="percentage" id="budget-percentage">--</span>
          </div>
        </div>
      </div>
    `;
  }

  renderMainChart() {
    return `
      <div class="chart-container main-chart">
        <div class="chart-header">
          <h3>Xu hướng chi tiêu</h3>
          <div class="chart-options">
            <button class="btn-chart-option active" data-view="spending">Chi tiêu</button>
            <button class="btn-chart-option" data-view="income">Thu nhập</button>
            <button class="btn-chart-option" data-view="balance">Cân đối</button>
            <button class="btn-chart-option" data-view="budget">So với ngân sách</button>
          </div>
        </div>
        <div class="chart-canvas">
          <canvas id="main-chart" width="800" height="400"></canvas>
        </div>
        <div class="chart-legend" id="main-chart-legend"></div>
      </div>
    `;
  }

  renderCategoryAnalysis() {
    return `
      <div class="chart-container category-chart">
        <div class="chart-header">
          <h3>Phân bổ theo danh mục</h3>
          <div class="view-toggles">
            <button class="btn-view-toggle active" data-view="pie">Tròn</button>
            <button class="btn-view-toggle" data-view="bar">Cột</button>
            <button class="btn-view-toggle" data-view="list">Danh sách</button>
          </div>
        </div>
        <div class="chart-content">
          <canvas id="category-chart" width="400" height="400"></canvas>
          <div class="category-breakdown" id="category-breakdown"></div>
        </div>
      </div>
    `;
  }

  renderTrendAnalysis() {
    return `
      <div class="trend-analysis">
        <h3>Phân tích xu hướng</h3>
        <div class="trend-metrics">
          <div class="trend-item">
            <span class="trend-label">Xu hướng tổng thể:</span>
            <span class="trend-value" id="overall-trend">--</span>
          </div>
          <div class="trend-item">
            <span class="trend-label">Biến động:</span>
            <span class="trend-value" id="volatility">--</span>
          </div>
          <div class="trend-item">
            <span class="trend-label">Tính chu kỳ:</span>
            <span class="trend-value" id="seasonality">--</span>
          </div>
        </div>
        
        <div class="trend-chart">
          <canvas id="trend-chart" width="600" height="200"></canvas>
        </div>
        
        <div class="trend-insights" id="trend-insights">
          <h4>Nhận xét:</h4>
          <ul id="trend-insights-list"></ul>
        </div>
      </div>
    `;
  }

  renderInsightsPanel() {
    return `
      <div class="insights-panel">
        <h3>Thông tin chi tiết</h3>
        
        <div class="insight-tabs">
          <button class="insight-tab active" data-tab="patterns">Thói quen</button>
          <button class="insight-tab" data-tab="anomalies">Bất thường</button>
          <button class="insight-tab" data-tab="forecast">Dự báo</button>
          <button class="insight-tab" data-tab="recommendations">Khuyến nghị</button>
        </div>
        
        <div class="insight-content">
          <div class="insight-pane active" id="patterns-pane">
            <div class="patterns-analysis" id="patterns-analysis">
              <h4>Phân tích thói quen chi tiêu:</h4>
              <div class="pattern-list" id="pattern-list"></div>
            </div>
          </div>
          
          <div class="insight-pane" id="anomalies-pane">
            <div class="anomalies-detection" id="anomalies-detection">
              <h4>Phát hiện chi tiêu bất thường:</h4>
              <div class="anomaly-list" id="anomaly-list"></div>
            </div>
          </div>
          
          <div class="insight-pane" id="forecast-pane">
            <div class="forecast-analysis" id="forecast-analysis">
              <h4>Dự báo chi tiêu:</h4>
              <div class="forecast-charts" id="forecast-charts"></div>
            </div>
          </div>
          
          <div class="insight-pane" id="recommendations-pane">
            <div class="recommendations-list" id="recommendations-list">
              <h4>Khuyến nghị cải thiện:</h4>
              <div class="recommendation-items" id="recommendation-items"></div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  renderDetailedTable() {
    return `
      <div class="detailed-table">
        <div class="table-header">
          <h3>Báo cáo chi tiết</h3>
          <div class="table-controls">
            <input type="text" id="search-transactions" placeholder="Tìm kiếm...">
            <select id="filter-category">
              <option value="">Tất cả danh mục</option>
              ${Object.entries(CATEGORY_NAMES).map(([key, name]) => 
                `<option value="${key}">${name}</option>`
              ).join('')}
            </select>
            <button class="btn-filter" id="btn-apply-filter">Lọc</button>
          </div>
        </div>
        
        <div class="table-container">
          <table id="transactions-table">
            <thead>
              <tr>
                <th data-sort="date">Ngày <span class="sort-icon">↕</span></th>
                <th data-sort="category">Danh mục <span class="sort-icon">↕</span></th>
                <th data-sort="description">Mô tả <span class="sort-icon">↕</span></th>
                <th data-sort="amount">Số tiền <span class="sort-icon">↕</span></th>
                <th data-sort="running-total">Tổng tích lũy <span class="sort-icon">↕</span></th>
                <th>Phân tích</th>
              </tr>
            </thead>
            <tbody id="transactions-tbody">
            </tbody>
          </table>
        </div>
        
        <div class="table-pagination">
          <button class="btn-page" id="btn-prev-page">‹ Trước</button>
          <span id="page-info">Trang 1 / 1</span>
          <button class="btn-page" id="btn-next-page">Sau ›</button>
        </div>
      </div>
    `;
  }

  renderPatternAnalysis() {
    return `
      <div class="pattern-analysis-detail">
        <h3>Phân tích mẫu chi tiêu sâu</h3>
        
        <div class="pattern-grid">
          <div class="pattern-card">
            <h4>Theo thời gian</h4>
            <canvas id="time-pattern-chart" width="300" height="200"></canvas>
            <div class="pattern-summary" id="time-pattern-summary"></div>
          </div>
          
          <div class="pattern-card">
            <h4>Theo ngày trong tuần</h4>
            <canvas id="weekday-pattern-chart" width="300" height="200"></canvas>
            <div class="pattern-summary" id="weekday-pattern-summary"></div>
          </div>
          
          <div class="pattern-card">
            <h4>Theo khoảng tiền</h4>
            <canvas id="amount-pattern-chart" width="300" height="200"></canvas>
            <div class="pattern-summary" id="amount-pattern-summary"></div>
          </div>
          
          <div class="pattern-card">
            <h4>Tương quan danh mục</h4>
            <canvas id="correlation-chart" width="300" height="200"></canvas>
            <div class="pattern-summary" id="correlation-summary"></div>
          </div>
        </div>
      </div>
    `;
  }

  async loadData() {
    try {
      this.currentData = await this.dataService.getAnalyticsData(this.currentTimeframe);
      this.updateSummaryCards();
      this.analyzePatterns();
      this.detectAnomalies();
      this.generateForecast();
      this.generateRecommendations();
    } catch (error) {
      console.error('Error loading analytics data:', error);
      this.showError('Không thể tải dữ liệu phân tích');
    }
  }

  updateSummaryCards() {
    if (!this.currentData) return;
    
    const data = this.currentData;
    
    // Update total spending
    document.getElementById('total-spending').textContent = formatCurrency(data.totalSpending);
    document.getElementById('spending-change').textContent = this.formatChange(data.spendingChange);
    document.getElementById('spending-change').className = `change ${data.spendingChange >= 0 ? 'positive' : 'negative'}`;
    
    // Update average transaction
    document.getElementById('avg-transaction').textContent = formatCurrency(data.avgTransaction);
    document.getElementById('transaction-change').textContent = this.formatChange(data.transactionChange);
    
    // Update top category
    if (data.topCategory) {
      document.getElementById('top-category').textContent = CATEGORY_NAMES[data.topCategory.category] || data.topCategory.category;
      document.getElementById('top-category-amount').textContent = formatCurrency(data.topCategory.amount);
    }
    
    // Update budget status
    document.getElementById('budget-status').textContent = this.getBudgetStatusText(data.budgetUsage);
    document.getElementById('budget-percentage').textContent = `${(data.budgetUsage * 100).toFixed(1)}%`;
  }

  renderCharts() {
    this.renderMainChart();
    this.renderCategoryChart();
    this.renderTrendChart();
    this.renderPatternCharts();
  }

  renderMainChart() {
    const ctx = document.getElementById('main-chart').getContext('2d');
    const data = this.prepareMainChartData();
    
    if (this.charts.main) {
      this.charts.main.destroy();
    }
    
    this.charts.main = new Chart(ctx, {
      type: this.currentChartType,
      data: data,
      options: this.getChartOptions('main')
    });
  }

  renderCategoryChart() {
    const ctx = document.getElementById('category-chart').getContext('2d');
    const data = this.prepareCategoryChartData();
    
    if (this.charts.category) {
      this.charts.category.destroy();
    }
    
    this.charts.category = new Chart(ctx, {
      type: 'pie',
      data: data,
      options: this.getChartOptions('category')
    });
    
    this.updateCategoryBreakdown();
  }

  prepareMainChartData() {
    if (!this.currentData || !this.currentData.dailyData) {
      return { labels: [], datasets: [] };
    }
    
    const labels = this.currentData.dailyData.map(d => formatDate(new Date(d.date), 'DD/MM'));
    const spendingData = this.currentData.dailyData.map(d => d.spending);
    const budgetData = this.currentData.dailyData.map(d => d.budget || 0);
    
    return {
      labels: labels,
      datasets: [
        {
          label: 'Chi tiêu',
          data: spendingData,
          borderColor: COLORS.primary,
          backgroundColor: this.currentChartType === 'area' ? `${COLORS.primary}20` : COLORS.primary,
          fill: this.currentChartType === 'area'
        },
        {
          label: 'Ngân sách',
          data: budgetData,
          borderColor: COLORS.secondary,
          backgroundColor: `${COLORS.secondary}20`,
          borderDash: [5, 5]
        }
      ]
    };
  }

  prepareCategoryChartData() {
    if (!this.currentData || !this.currentData.categoryBreakdown) {
      return { labels: [], datasets: [] };
    }
    
    const categories = Object.entries(this.currentData.categoryBreakdown);
    const labels = categories.map(([cat, data]) => CATEGORY_NAMES[cat] || cat);
    const amounts = categories.map(([cat, data]) => data.amount);
    const colors = categories.map(([cat, data]) => CATEGORY_COLORS[cat] || COLORS.primary);
    
    return {
      labels: labels,
      datasets: [{
        data: amounts,
        backgroundColor: colors,
        borderWidth: 2,
        borderColor: '#ffffff'
      }]
    };
  }

  updateCategoryBreakdown() {
    const breakdown = document.getElementById('category-breakdown');
    if (!this.currentData || !this.currentData.categoryBreakdown) {
      breakdown.innerHTML = '<p>Không có dữ liệu</p>';
      return;
    }
    
    const categories = Object.entries(this.currentData.categoryBreakdown)
      .sort((a, b) => b[1].amount - a[1].amount);
    
    breakdown.innerHTML = `
      <div class="breakdown-list">
        ${categories.map(([cat, data]) => `
          <div class="breakdown-item">
            <div class="item-info">
              <div class="category-indicator" style="background-color: ${CATEGORY_COLORS[cat] || COLORS.primary}"></div>
              <span class="category-name">${CATEGORY_NAMES[cat] || cat}</span>
            </div>
            <div class="item-stats">
              <span class="amount">${formatCurrency(data.amount)}</span>
              <span class="percentage">${((data.amount / this.currentData.totalSpending) * 100).toFixed(1)}%</span>
            </div>
          </div>
        `).join('')}
      </div>
    `;
  }

  analyzePatterns() {
    if (!this.currentData) return;
    
    const patterns = this.identifySpendingPatterns(this.currentData.transactions);
    this.updatePatternsPane(patterns);
  }

  identifySpendingPatterns(transactions) {
    const patterns = [];
    
    // Day of week pattern
    const dayOfWeekSpending = new Array(7).fill(0);
    const dayOfWeekCounts = new Array(7).fill(0);
    
    transactions.forEach(t => {
      const day = new Date(t.date).getDay();
      dayOfWeekSpending[day] += Math.abs(t.amount);
      dayOfWeekCounts[day]++;
    });
    
    const avgDaySpending = dayOfWeekSpending.map((total, i) => 
      dayOfWeekCounts[i] > 0 ? total / dayOfWeekCounts[i] : 0
    );
    
    const maxDay = avgDaySpending.indexOf(Math.max(...avgDaySpending));
    const minDay = avgDaySpending.indexOf(Math.min(...avgDaySpending));
    
    const dayNames = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];
    
    patterns.push({
      type: 'weekday',
      title: 'Thói quen theo ngày trong tuần',
      description: `Bạn chi tiêu nhiều nhất vào ${dayNames[maxDay]} (${formatCurrency(avgDaySpending[maxDay])}) và ít nhất vào ${dayNames[minDay]} (${formatCurrency(avgDaySpending[minDay])})`,
      impact: 'medium',
      actionable: true
    });
    
    // Time of day pattern
    const hourlySpending = new Array(24).fill(0);
    const hourlyCounts = new Array(24).fill(0);
    
    transactions.forEach(t => {
      const hour = new Date(t.date).getHours();
      hourlySpending[hour] += Math.abs(t.amount);
      hourlyCounts[hour]++;
    });
    
    const peakHour = hourlySpending.indexOf(Math.max(...hourlySpending));
    
    patterns.push({
      type: 'time',
      title: 'Giờ chi tiêu cao điểm',
      description: `${peakHour}:00 - ${peakHour + 1}:00 là thời gian bạn chi tiêu nhiều nhất`,
      impact: 'low',
      actionable: false
    });
    
    // Amount clustering
    const amounts = transactions.map(t => Math.abs(t.amount));
    const { clusters, insights } = this.clusterAmounts(amounts);
    
    patterns.push({
      type: 'amount',
      title: 'Phân nhóm số tiền',
      description: insights,
      impact: 'medium',
      actionable: true
    });
    
    return patterns;
  }

  clusterAmounts(amounts) {
    // Simple clustering: small (< 50k), medium (50k-200k), large (> 200k)
    const small = amounts.filter(a => a < 50000);
    const medium = amounts.filter(a => a >= 50000 && a < 200000);
    const large = amounts.filter(a => a >= 200000);
    
    const clusters = [
      { name: 'Nhỏ (< 50K)', count: small.length, percentage: (small.length / amounts.length) * 100 },
      { name: 'Trung bình (50K-200K)', count: medium.length, percentage: (medium.length / amounts.length) * 100 },
      { name: 'Lớn (> 200K)', count: large.length, percentage: (large.length / amounts.length) * 100 }
    ];
    
    const dominant = clusters.reduce((max, cluster) => cluster.count > max.count ? cluster : max);
    
    const insights = `Bạn có xu hướng chi tiêu ${dominant.name.toLowerCase()} chiếm ${dominant.percentage.toFixed(1)}% giao dịch`;
    
    return { clusters, insights };
  }

  detectAnomalies() {
    if (!this.currentData) return;
    
    const anomalies = this.identifyAnomalousTransactions(this.currentData.transactions);
    this.updateAnomaliesPane(anomalies);
  }

  identifyAnomalousTransactions(transactions) {
    const anomalies = [];
    
    // Calculate category statistics
    const categoryStats = {};
    
    transactions.forEach(t => {
      const cat = t.category;
      if (!categoryStats[cat]) {
        categoryStats[cat] = { amounts: [], mean: 0, stdDev: 0 };
      }
      categoryStats[cat].amounts.push(Math.abs(t.amount));
    });
    
    // Calculate mean and standard deviation for each category
    Object.keys(categoryStats).forEach(cat => {
      const amounts = categoryStats[cat].amounts;
      const mean = amounts.reduce((sum, a) => sum + a, 0) / amounts.length;
      const variance = amounts.reduce((sum, a) => sum + Math.pow(a - mean, 2), 0) / amounts.length;
      const stdDev = Math.sqrt(variance);
      
      categoryStats[cat].mean = mean;
      categoryStats[cat].stdDev = stdDev;
    });
    
    // Identify anomalies (transactions > 2 standard deviations from mean)
    transactions.forEach(t => {
      const cat = t.category;
      const amount = Math.abs(t.amount);
      const stats = categoryStats[cat];
      
      if (stats.stdDev > 0) {
        const zScore = (amount - stats.mean) / stats.stdDev;
        
        if (Math.abs(zScore) > 2) {
          anomalies.push({
            transaction: t,
            type: zScore > 0 ? 'unusually_high' : 'unusually_low',
            severity: Math.abs(zScore) > 3 ? 'high' : 'medium',
            zScore: zScore,
            explanation: `Chi tiêu ${zScore > 0 ? 'cao' : 'thấp'} bất thường so với trung bình danh mục`
          });
        }
      }
    });
    
    return anomalies.sort((a, b) => Math.abs(b.zScore) - Math.abs(a.zScore));
  }

  generateForecast() {
    if (!this.currentData) return;
    
    const forecast = this.calculateSpendingForecast(this.currentData.transactions);
    this.updateForecastPane(forecast);
  }

  calculateSpendingForecast(transactions) {
    // Simple linear regression forecast
    const dailyTotals = this.aggregateDailySpending(transactions);
    
    if (dailyTotals.length < 7) {
      return {
        nextWeek: null,
        nextMonth: null,
        confidence: 0,
        explanation: 'Không đủ dữ liệu để dự báo chính xác'
      };
    }
    
    const { slope, intercept, rSquared } = this.linearRegression(dailyTotals);
    
    const lastDay = dailyTotals.length;
    const nextWeekTotal = Array.from({ length: 7 }, (_, i) => 
      slope * (lastDay + i + 1) + intercept
    ).reduce((sum, val) => sum + Math.max(0, val), 0);
    
    const nextMonthTotal = Array.from({ length: 30 }, (_, i) => 
      slope * (lastDay + i + 1) + intercept
    ).reduce((sum, val) => sum + Math.max(0, val), 0);
    
    return {
      nextWeek: nextWeekTotal,
      nextMonth: nextMonthTotal,
      confidence: rSquared,
      trend: slope > 0 ? 'increasing' : slope < 0 ? 'decreasing' : 'stable',
      explanation: this.getForecastExplanation(slope, rSquared)
    };
  }

  getForecastExplanation(slope, rSquared) {
    let trend = '';
    if (slope > 100) trend = 'Chi tiêu có xu hướng tăng mạnh';
    else if (slope > 0) trend = 'Chi tiêu có xu hướng tăng nhẹ';
    else if (slope < -100) trend = 'Chi tiêu có xu hướng giảm mạnh';
    else if (slope < 0) trend = 'Chi tiêu có xu hướng giảm nhẹ';
    else trend = 'Chi tiêu tương đối ổn định';
    
    const confidence = rSquared > 0.7 ? 'cao' : rSquared > 0.4 ? 'trung bình' : 'thấp';
    
    return `${trend}. Độ tin cậy dự báo: ${confidence} (${(rSquared * 100).toFixed(1)}%)`;
  }

  generateRecommendations() {
    if (!this.currentData) return;
    
    const recommendations = this.analyzeAndRecommend(this.currentData);
    this.updateRecommendationsPane(recommendations);
  }

  analyzeAndRecommend(data) {
    const recommendations = [];
    
    // Budget-based recommendations
    if (data.budgetUsage > 0.8) {
      recommendations.push({
        type: 'budget_alert',
        priority: 'high',
        title: 'Cảnh báo ngân sách',
        message: 'Bạn đã chi gần hết ngân sách tháng này',
        action: 'Hãy xem xét giảm chi tiêu hoặc tăng ngân sách',
        impact: 'Tránh vượt ngân sách'
      });
    }
    
    // Category-based recommendations
    const topCategory = Object.entries(data.categoryBreakdown || {})
      .sort((a, b) => b[1].amount - a[1].amount)[0];
    
    if (topCategory && topCategory[1].percentage > 40) {
      recommendations.push({
        type: 'category_balance',
        priority: 'medium',
        title: 'Cân bằng danh mục',
        message: `${CATEGORY_NAMES[topCategory[0]]} chiếm ${topCategory[1].percentage.toFixed(1)}% tổng chi tiêu`,
        action: 'Hãy cân nhắc phân bổ lại ngân sách cho các danh mục khác',
        impact: 'Cải thiện cân bằng tài chính'
      });
    }
    
    // Savings recommendations
    const savingsRate = data.savingsRate || 0;
    if (savingsRate < 0.1) {
      recommendations.push({
        type: 'savings',
        priority: 'high',
        title: 'Tăng tiết kiệm',
        message: `Tỷ lệ tiết kiệm hiện tại: ${(savingsRate * 100).toFixed(1)}%`,
        action: 'Mục tiêu nên tiết kiệm ít nhất 10-20% thu nhập',
        impact: 'Xây dựng quỹ dự phòng tương lai'
      });
    }
    
    return recommendations.sort((a, b) => {
      const priority = { high: 3, medium: 2, low: 1 };
      return priority[b.priority] - priority[a.priority];
    });
  }

  // Event Handlers
  setupEventListeners() {
    // Timeframe selector
    document.getElementById('timeframe-select').addEventListener('change', (e) => {
      this.currentTimeframe = e.target.value;
      this.loadData();
    });
    
    // Chart type selector
    document.getElementById('chart-type-select').addEventListener('change', (e) => {
      this.currentChartType = e.target.value;
      this.renderMainChart();
    });
    
    // Export button
    document.getElementById('btn-export-report')?.addEventListener('click', () => {
      this.exportReport();
    });
    
    // Insight tabs
    document.querySelectorAll('.insight-tab').forEach(tab => {
      tab.addEventListener('click', (e) => {
        this.switchInsightTab(e.target.dataset.tab);
      });
    });
    
    // Chart view options
    document.querySelectorAll('.btn-chart-option').forEach(btn => {
      btn.addEventListener('click', (e) => {
        this.switchChartView(e.target.dataset.view);
      });
    });
  }

  switchInsightTab(tabName) {
    // Update tab buttons
    document.querySelectorAll('.insight-tab').forEach(tab => {
      tab.classList.toggle('active', tab.dataset.tab === tabName);
    });
    
    // Update content panes
    document.querySelectorAll('.insight-pane').forEach(pane => {
      pane.classList.toggle('active', pane.id === `${tabName}-pane`);
    });
  }

  switchChartView(view) {
    // Update button states
    document.querySelectorAll('.btn-chart-option').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.view === view);
    });
    
    // Update chart data based on view
    this.currentChartView = view;
    this.renderMainChart();
  }

  async exportReport() {
    try {
      // Generate comprehensive report
      const report = await this.generateComprehensiveReport();
      
      // Export as PDF (would need PDF library in real implementation)
      this.downloadReport(report, 'pdf');
      
    } catch (error) {
      console.error('Error exporting report:', error);
      this.showNotification('Lỗi xuất báo cáo', 'error');
    }
  }

  // Utility methods
  formatChange(change) {
    const abs = Math.abs(change);
    const sign = change >= 0 ? '+' : '-';
    return `${sign}${(abs * 100).toFixed(1)}%`;
  }

  getBudgetStatusText(usage) {
    if (usage > 1) return 'Vượt ngân sách';
    if (usage > 0.8) return 'Gần hết ngân sách';
    if (usage > 0.5) return 'Đang theo dõi';
    return 'Trong tầm kiểm soát';
  }

  getChartOptions(chartType) {
    const baseOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom'
        }
      }
    };
    
    if (chartType === 'main') {
      return {
        ...baseOptions,
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: function(value) {
                return formatCurrency(value);
              }
            }
          }
        }
      };
    }
    
    return baseOptions;
  }

  linearRegression(data) {
    const n = data.length;
    const x = data.map((_, i) => i + 1);
    const y = data;
    
    const sumX = x.reduce((sum, val) => sum + val, 0);
    const sumY = y.reduce((sum, val) => sum + val, 0);
    const sumXY = x.reduce((sum, val, i) => sum + val * y[i], 0);
    const sumXX = x.reduce((sum, val) => sum + val * val, 0);
    const sumYY = y.reduce((sum, val) => sum + val * val, 0);
    
    const slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    const intercept = (sumY - slope * sumX) / n;
    
    // Calculate R-squared
    const yMean = sumY / n;
    const totalSumSquares = y.reduce((sum, val) => sum + Math.pow(val - yMean, 2), 0);
    const residualSumSquares = y.reduce((sum, val, i) => {
      const predicted = slope * x[i] + intercept;
      return sum + Math.pow(val - predicted, 2);
    }, 0);
    
    const rSquared = 1 - (residualSumSquares / totalSumSquares);
    
    return { slope, intercept, rSquared };
  }

  aggregateDailySpending(transactions) {
    const daily = {};
    
    transactions.forEach(t => {
      const date = new Date(t.date).toDateString();
      daily[date] = (daily[date] || 0) + Math.abs(t.amount);
    });
    
    return Object.values(daily);
  }

  showNotification(message, type = 'info') {
    if (window.showNotification) {
      window.showNotification(message, type);
    } else {
      console.log(`${type.toUpperCase()}: ${message}`);
    }
  }

  renderStyles() {
    if (document.getElementById('analytics-dashboard-styles')) return;

    const style = document.createElement('style');
    style.id = 'analytics-dashboard-styles';
    style.textContent = `
      .analytics-dashboard {
        max-width: 1200px;
        margin: 0 auto;
        padding: 1rem;
      }

      .dashboard-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 2rem;
        flex-wrap: wrap;
        gap: 1rem;
      }

      .dashboard-header h2 {
        margin: 0;
        color: #111827;
        font-size: 1.875rem;
      }

      .dashboard-controls {
        display: flex;
        gap: 1rem;
        align-items: center;
        flex-wrap: wrap;
      }

      .dashboard-controls label {
        font-size: 0.875rem;
        color: #6b7280;
        margin-right: 0.5rem;
      }

      .dashboard-controls select {
        padding: 0.5rem;
        border: 1px solid #d1d5db;
        border-radius: 6px;
        background: white;
      }

      .dashboard-actions {
        display: flex;
        gap: 0.5rem;
      }

      .btn-export,
      .btn-compare,
      .btn-forecast {
        padding: 0.5rem 1rem;
        border: 1px solid #d1d5db;
        border-radius: 6px;
        background: white;
        cursor: pointer;
        font-size: 0.875rem;
        transition: all 0.2s;
      }

      .btn-export:hover,
      .btn-compare:hover,
      .btn-forecast:hover {
        background: #f3f4f6;
        border-color: #9ca3af;
      }

      .analytics-grid {
        display: grid;
        grid-template-columns: 1fr;
        gap: 1.5rem;
        margin-bottom: 2rem;
      }

      .summary-cards {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 1rem;
        margin-bottom: 1.5rem;
      }

      .summary-card {
        background: white;
        border-radius: 12px;
        padding: 1.5rem;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        border: 1px solid #e5e7eb;
        display: flex;
        align-items: center;
        gap: 1rem;
      }

      .card-icon {
        font-size: 2rem;
        opacity: 0.8;
      }

      .card-content h3 {
        margin: 0 0 0.5rem 0;
        font-size: 0.875rem;
        color: #6b7280;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .card-content .amount {
        margin: 0;
        font-size: 1.5rem;
        font-weight: 700;
        color: #111827;
      }

      .card-content .change {
        font-size: 0.875rem;
        font-weight: 500;
      }

      .change.positive {
        color: #10b981;
      }

      .change.negative {
        color: #ef4444;
      }

      .chart-container {
        background: white;
        border-radius: 12px;
        padding: 1.5rem;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        border: 1px solid #e5e7eb;
      }

      .chart-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
      }

      .chart-header h3 {
        margin: 0;
        color: #111827;
        font-size: 1.125rem;
      }

      .chart-options,
      .view-toggles {
        display: flex;
        gap: 0.25rem;
      }

      .btn-chart-option,
      .btn-view-toggle {
        padding: 0.5rem 1rem;
        border: 1px solid #d1d5db;
        background: white;
        border-radius: 6px;
        cursor: pointer;
        font-size: 0.875rem;
        transition: all 0.2s;
      }

      .btn-chart-option.active,
      .btn-view-toggle.active {
        background: #3b82f6;
        color: white;
        border-color: #3b82f6;
      }

      .chart-canvas {
        position: relative;
        height: 400px;
        margin-bottom: 1rem;
      }

      .category-chart .chart-content {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 2rem;
        align-items: center;
      }

      .breakdown-list {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .breakdown-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.75rem;
        background: #f9fafb;
        border-radius: 8px;
      }

      .item-info {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .category-indicator {
        width: 12px;
        height: 12px;
        border-radius: 50%;
      }

      .category-name {
        font-size: 0.875rem;
        color: #374151;
        font-weight: 500;
      }

      .item-stats {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        gap: 0.125rem;
      }

      .item-stats .amount {
        font-size: 0.875rem;
        font-weight: 600;
        color: #111827;
      }

      .item-stats .percentage {
        font-size: 0.75rem;
        color: #6b7280;
      }

      .insights-panel {
        grid-column: 1 / -1;
        background: white;
        border-radius: 12px;
        padding: 1.5rem;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        border: 1px solid #e5e7eb;
      }

      .insight-tabs {
        display: flex;
        gap: 0.25rem;
        margin-bottom: 1.5rem;
        border-bottom: 2px solid #f3f4f6;
      }

      .insight-tab {
        padding: 0.75rem 1rem;
        border: none;
        background: none;
        cursor: pointer;
        font-size: 0.875rem;
        color: #6b7280;
        border-bottom: 2px solid transparent;
        transition: all 0.2s;
      }

      .insight-tab.active {
        color: #3b82f6;
        border-bottom-color: #3b82f6;
      }

      .insight-pane {
        display: none;
      }

      .insight-pane.active {
        display: block;
      }

      .pattern-list,
      .anomaly-list,
      .recommendation-items {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .pattern-item,
      .anomaly-item,
      .recommendation-item {
        padding: 1rem;
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        background: #f9fafb;
      }

      .pattern-item h5,
      .anomaly-item h5,
      .recommendation-item h5 {
        margin: 0 0 0.5rem 0;
        color: #111827;
        font-size: 1rem;
      }

      .pattern-item p,
      .anomaly-item p,
      .recommendation-item p {
        margin: 0;
        color: #6b7280;
        font-size: 0.875rem;
      }

      .detailed-table {
        background: white;
        border-radius: 12px;
        padding: 1.5rem;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        border: 1px solid #e5e7eb;
        margin-top: 2rem;
      }

      .table-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
      }

      .table-controls {
        display: flex;
        gap: 0.5rem;
        align-items: center;
      }

      .table-controls input,
      .table-controls select {
        padding: 0.5rem;
        border: 1px solid #d1d5db;
        border-radius: 6px;
        font-size: 0.875rem;
      }

      .table-container {
        overflow-x: auto;
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        margin-bottom: 1rem;
      }

      #transactions-table {
        width: 100%;
        border-collapse: collapse;
      }

      #transactions-table th,
      #transactions-table td {
        padding: 0.75rem;
        text-align: left;
        border-bottom: 1px solid #e5e7eb;
      }

      #transactions-table th {
        background: #f9fafb;
        font-weight: 600;
        color: #374151;
        cursor: pointer;
        user-select: none;
      }

      .sort-icon {
        margin-left: 0.25rem;
        opacity: 0.5;
      }

      #transactions-table th:hover {
        background: #f3f4f6;
      }

      .table-pagination {
        display: flex;
        justify-content: center;
        align-items: center;
        gap: 1rem;
      }

      .btn-page {
        padding: 0.5rem 1rem;
        border: 1px solid #d1d5db;
        background: white;
        border-radius: 6px;
        cursor: pointer;
      }

      .btn-page:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      @media (max-width: 1024px) {
        .analytics-grid {
          grid-template-columns: 1fr;
        }
        
        .category-chart .chart-content {
          grid-template-columns: 1fr;
        }
        
        .dashboard-header {
          flex-direction: column;
          align-items: stretch;
        }
        
        .dashboard-controls {
          justify-content: center;
        }
      }

      @media (max-width: 768px) {
        .analytics-dashboard {
          padding: 0.5rem;
        }
        
        .summary-cards {
          grid-template-columns: 1fr;
        }
        
        .chart-header {
          flex-direction: column;
          gap: 1rem;
        }
        
        .dashboard-controls {
          flex-direction: column;
          gap: 0.5rem;
        }
        
        .insight-tabs {
          overflow-x: auto;
          white-space: nowrap;
        }
      }
    `;
    
    document.head.appendChild(style);
  }
}

export default AnalyticsDashboard;