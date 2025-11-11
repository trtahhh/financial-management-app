// Category Card Component
import { formatCurrency, formatCurrencyShort } from '../utils/currencyHelpers.js';
import { calculateBudgetProgress } from '../utils/calculationHelpers.js';
import { CATEGORY_NAMES, CATEGORY_COLORS } from '../constants/categories.js';
import { COLORS } from '../constants/colors.js';

class CategoryCard {
  constructor(container, category, options = {}) {
    this.container = container;
    this.category = category;
    this.options = {
      showBudget: true,
      showTransactions: true,
      showTrend: true,
      size: 'medium', // small, medium, large
      interactive: true,
      period: 'monthly',
      ...options
    };
    
    this.budget = null;
    this.transactions = [];
    this.spent = 0;
    this.transactionCount = 0;
    
    this.init();
  }

  init() {
    this.render();
    this.attachEventListeners();
  }

  render() {
    const categoryName = CATEGORY_NAMES[this.category] || this.category;
    const categoryColor = CATEGORY_COLORS[this.category] || COLORS.primary;
    const progress = this.budget ? calculateBudgetProgress(this.spent, this.budget.amount) : null;

    this.container.innerHTML = `
      <div class="category-card ${this.options.size} ${this.options.interactive ? 'interactive' : ''}" 
           data-category="${this.category}">
        <div class="card-header">
          <div class="category-info">
            <div class="category-icon" style="background-color: ${categoryColor}"></div>
            <div class="category-details">
              <h3 class="category-name">${categoryName}</h3>
              ${this.renderSubtitle()}
            </div>
          </div>
          ${this.renderActions()}
        </div>

        ${this.renderSpending()}
        ${this.options.showBudget && this.budget ? this.renderBudgetProgress(progress) : ''}
        ${this.options.showTransactions ? this.renderTransactionSummary() : ''}
        ${this.options.showTrend ? this.renderTrend() : ''}
      </div>
    `;

    this.renderStyles();
  }

  renderSubtitle() {
    if (this.options.size === 'small') {
      return `<span class="transaction-count">${this.transactionCount} giao dịch</span>`;
    }
    
    return `
      <div class="category-subtitle">
        <span class="transaction-count">${this.transactionCount} giao dịch</span>
        ${this.budget ? `<span class="budget-period">${this.options.period === 'monthly' ? 'Tháng này' : 'Tuần này'}</span>` : ''}
      </div>
    `;
  }

  renderActions() {
    if (!this.options.interactive || this.options.size === 'small') {
      return '';
    }

    return `
      <div class="card-actions">
        <button class="btn-action" id="btn-add-transaction" title="Thêm giao dịch">+</button>
        ${this.budget ? '<button class="btn-action" id="btn-edit-budget" title="Sửa ngân sách">⚙</button>' : '<button class="btn-action" id="btn-set-budget" title="Đặt ngân sách">$</button>'}
      </div>
    `;
  }

  renderSpending() {
    const percentageOfTotal = this.getTotalSpentPercentage();
    
    return `
      <div class="spending-section">
        <div class="spending-amount">
          <span class="amount-label">Đã chi</span>
          <span class="amount-value">${formatCurrency(this.spent)}</span>
        </div>
        
        ${percentageOfTotal > 0 ? `
          <div class="spending-percentage">
            <span class="percentage-text">${percentageOfTotal.toFixed(1)}% tổng chi tiêu</span>
          </div>
        ` : ''}
        
        ${this.renderSpendingTrend()}
      </div>
    `;
  }

  renderSpendingTrend() {
    if (this.options.size === 'small') return '';
    
    const trend = this.calculateSpendingTrend();
    if (!trend) return '';

    let trendIcon = '';
    let trendClass = '';
    let trendText = '';

    if (trend.direction === 'up') {
      trendIcon = '↗';
      trendClass = trend.percentage > 20 ? 'trend-danger' : 'trend-warning';
      trendText = `+${trend.percentage.toFixed(0)}% so với ${trend.comparisonPeriod}`;
    } else if (trend.direction === 'down') {
      trendIcon = '↘';
      trendClass = 'trend-success';
      trendText = `-${Math.abs(trend.percentage).toFixed(0)}% so với ${trend.comparisonPeriod}`;
    } else {
      trendIcon = '→';
      trendClass = 'trend-neutral';
      trendText = `Không đổi so với ${trend.comparisonPeriod}`;
    }

    return `
      <div class="spending-trend ${trendClass}">
        <span class="trend-icon">${trendIcon}</span>
        <span class="trend-text">${trendText}</span>
      </div>
    `;
  }

  renderBudgetProgress(progress) {
    if (!progress) return '';

    const remaining = progress.remaining;
    const daysLeft = this.getDaysLeftInPeriod();
    const dailyBudget = remaining / Math.max(daysLeft, 1);

    return `
      <div class="budget-section">
        <div class="budget-header">
          <span class="budget-label">Ngân sách</span>
          <span class="budget-amount">${formatCurrency(this.budget.amount)}</span>
        </div>
        
        <div class="progress-container">
          <div class="progress-bar">
            <div class="progress-fill ${progress.status}" 
                 style="width: ${Math.min(progress.percentage, 100)}%"></div>
          </div>
          <span class="progress-percentage">${progress.percentage.toFixed(0)}%</span>
        </div>
        
        <div class="budget-details">
          <div class="budget-remaining">
            <span class="remaining-label">${remaining >= 0 ? 'Còn lại' : 'Vượt'}</span>
            <span class="remaining-amount ${remaining >= 0 ? 'positive' : 'negative'}">
              ${formatCurrency(Math.abs(remaining))}
            </span>
          </div>
          
          ${daysLeft > 0 && remaining > 0 ? `
            <div class="daily-budget">
              <span class="daily-label">Có thể chi/ngày</span>
              <span class="daily-amount">${formatCurrencyShort(dailyBudget)}</span>
            </div>
          ` : ''}
        </div>

        ${this.renderBudgetAlert(progress)}
      </div>
    `;
  }

  renderBudgetAlert(progress) {
    if (this.options.size === 'small') return '';

    let alert = '';
    
    if (progress.status === 'over-budget') {
      alert = `
        <div class="budget-alert danger">
          Đã vượt ngân sách!
        </div>
      `;
    } else if (progress.status === 'near-limit') {
      alert = `
        <div class="budget-alert warning">
          Sắp hết ngân sách
        </div>
      `;
    } else if (progress.status === 'warning') {
      alert = `
        <div class="budget-alert info">
          Cần chú ý chi tiêu
        </div>
      `;
    }
    
    return alert;
  }

  renderTransactionSummary() {
    if (this.transactions.length === 0) {
      return `
        <div class="transaction-summary empty">
          <span class="empty-text">Chưa có giao dịch nào</span>
        </div>
      `;
    }

    const recentTransactions = this.transactions.slice(0, 3);
    const averageAmount = this.spent / this.transactions.length;

    return `
      <div class="transaction-summary">
        ${this.options.size !== 'small' ? `
          <div class="summary-stats">
            <div class="stat-item">
              <span class="stat-label">Trung bình</span>
              <span class="stat-value">${formatCurrencyShort(averageAmount)}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Gần nhất</span>
              <span class="stat-value">${recentTransactions.length > 0 ? this.formatRelativeDate(recentTransactions[0].date) : '-'}</span>
            </div>
          </div>
        ` : ''}
        
        ${this.options.size === 'large' ? this.renderRecentTransactions(recentTransactions) : ''}
      </div>
    `;
  }

  renderRecentTransactions(transactions) {
    if (transactions.length === 0) return '';

    return `
      <div class="recent-transactions">
        <h4 class="recent-title">Giao dịch gần đây</h4>
        <div class="transaction-list">
          ${transactions.map(transaction => `
            <div class="transaction-item">
              <div class="transaction-info">
                <span class="transaction-description">${transaction.description || 'Không có mô tả'}</span>
                <span class="transaction-date">${this.formatRelativeDate(transaction.date)}</span>
              </div>
              <span class="transaction-amount">${formatCurrencyShort(transaction.amount)}</span>
            </div>
          `).join('')}
        </div>
        
        ${this.transactions.length > 3 ? `
          <button class="btn-view-all" id="btn-view-all">Xem tất cả (${this.transactions.length})</button>
        ` : ''}
      </div>
    `;
  }

  renderTrend() {
    if (this.options.size === 'small') return '';
    
    const trendData = this.calculateTrendData();
    if (!trendData || trendData.length < 2) return '';

    return `
      <div class="trend-section">
        <h4 class="trend-title">Xu hướng chi tiêu</h4>
        <div class="trend-chart">
          ${this.renderMiniChart(trendData)}
        </div>
      </div>
    `;
  }

  renderMiniChart(data) {
    const maxValue = Math.max(...data.map(d => d.amount));
    const chartHeight = 60;
    
    return `
      <div class="mini-chart" style="height: ${chartHeight}px;">
        ${data.map((point, index) => {
          const height = (point.amount / maxValue) * chartHeight;
          const left = (index / (data.length - 1)) * 100;
          
          return `
            <div class="chart-bar" 
                 style="left: ${left}%; height: ${height}px; bottom: 0;">
              <div class="bar-tooltip">
                <span class="tooltip-date">${this.formatShortDate(point.date)}</span>
                <span class="tooltip-amount">${formatCurrencyShort(point.amount)}</span>
              </div>
            </div>
          `;
        }).join('')}
      </div>
    `;
  }

  // Helper methods
  getTotalSpentPercentage() {
    if (!window.totalSpentAllCategories) return 0;
    return (this.spent / window.totalSpentAllCategories) * 100;
  }

  calculateSpendingTrend() {
    // Compare with previous period
    const previousPeriodSpent = this.getPreviousPeriodSpent();
    if (previousPeriodSpent === null) return null;

    const percentage = ((this.spent - previousPeriodSpent) / Math.max(previousPeriodSpent, 1)) * 100;
    
    return {
      direction: percentage > 5 ? 'up' : percentage < -5 ? 'down' : 'same',
      percentage: Math.abs(percentage),
      comparisonPeriod: this.options.period === 'monthly' ? 'tháng trước' : 'tuần trước'
    };
  }

  calculateTrendData() {
    // Generate trend data for mini chart
    const days = this.options.period === 'monthly' ? 30 : 7;
    const data = [];
    
    for (let i = days - 1; i >= 0; i--) {
      const date = new Date();
      date.setDate(date.getDate() - i);
      
      const dayTransactions = this.transactions.filter(t => 
        new Date(t.date).toDateString() === date.toDateString()
      );
      
      const amount = dayTransactions.reduce((sum, t) => sum + t.amount, 0);
      data.push({ date, amount });
    }
    
    return data;
  }

  getDaysLeftInPeriod() {
    const now = new Date();
    if (this.options.period === 'monthly') {
      const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0);
      return Math.max(0, Math.ceil((endOfMonth - now) / (24 * 60 * 60 * 1000)));
    } else {
      const startOfWeek = this.getStartOfWeek(now);
      const endOfWeek = new Date(startOfWeek.getTime() + 6 * 24 * 60 * 60 * 1000);
      return Math.max(0, Math.ceil((endOfWeek - now) / (24 * 60 * 60 * 1000)));
    }
  }

  getStartOfWeek(date) {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    return new Date(d.setDate(diff));
  }

  getPreviousPeriodSpent() {
    // This would typically fetch from service
    // For now, return null to indicate no data
    return null;
  }

  formatRelativeDate(date) {
    const now = new Date();
    const diffTime = now - new Date(date);
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) return 'Hôm nay';
    if (diffDays === 1) return 'Hôm qua';
    if (diffDays < 7) return `${diffDays} ngày trước`;
    if (diffDays < 30) return `${Math.floor(diffDays / 7)} tuần trước`;
    return new Date(date).toLocaleDateString('vi-VN');
  }

  formatShortDate(date) {
    return new Date(date).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit'
    });
  }

  attachEventListeners() {
    if (!this.options.interactive) return;

    // Card click for navigation
    this.container.querySelector('.category-card').addEventListener('click', (e) => {
      // Don't trigger if clicking on buttons
      if (e.target.closest('.btn-action, .btn-view-all')) return;
      
      this.onCategoryClick?.(this.category);
    });

    // Action buttons
    document.getElementById('btn-add-transaction')?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.onAddTransaction?.(this.category);
    });

    document.getElementById('btn-edit-budget')?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.onEditBudget?.(this.category, this.budget);
    });

    document.getElementById('btn-set-budget')?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.onSetBudget?.(this.category);
    });

    document.getElementById('btn-view-all')?.addEventListener('click', (e) => {
      e.stopPropagation();
      this.onViewAllTransactions?.(this.category);
    });
  }

  // Public methods
  setData(data) {
    this.budget = data.budget || null;
    this.transactions = data.transactions || [];
    this.spent = data.spent || 0;
    this.transactionCount = this.transactions.length;
    this.render();
  }

  updateBudget(budget) {
    this.budget = budget;
    this.render();
  }

  updateTransactions(transactions) {
    this.transactions = transactions || [];
    this.transactionCount = this.transactions.length;
    this.spent = transactions.reduce((sum, t) => sum + t.amount, 0);
    this.render();
  }

  refresh() {
    this.render();
  }

  renderStyles() {
    if (document.getElementById('category-card-styles')) return;

    const style = document.createElement('style');
    style.id = 'category-card-styles';
    style.textContent = `
      .category-card {
        background: white;
        border-radius: 12px;
        border: 1px solid #e5e7eb;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        transition: all 0.2s ease;
        overflow: hidden;
      }

      .category-card.interactive {
        cursor: pointer;
      }

      .category-card.interactive:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        border-color: #d1d5db;
      }

      .category-card.small {
        padding: 1rem;
      }

      .category-card.medium {
        padding: 1.25rem;
      }

      .category-card.large {
        padding: 1.5rem;
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 1rem;
      }

      .category-info {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        flex: 1;
      }

      .category-icon {
        width: 40px;
        height: 40px;
        border-radius: 8px;
        flex-shrink: 0;
      }

      .category-card.small .category-icon {
        width: 32px;
        height: 32px;
      }

      .category-card.large .category-icon {
        width: 48px;
        height: 48px;
      }

      .category-details {
        flex: 1;
        min-width: 0;
      }

      .category-name {
        margin: 0;
        font-size: 1rem;
        font-weight: 600;
        color: #111827;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .category-card.small .category-name {
        font-size: 0.875rem;
      }

      .category-card.large .category-name {
        font-size: 1.125rem;
      }

      .category-subtitle {
        display: flex;
        gap: 0.5rem;
        margin-top: 0.25rem;
        font-size: 0.75rem;
        color: #6b7280;
      }

      .transaction-count {
        font-size: 0.75rem;
        color: #6b7280;
      }

      .budget-period {
        font-size: 0.75rem;
        color: #6b7280;
      }

      .card-actions {
        display: flex;
        gap: 0.25rem;
      }

      .btn-action {
        width: 32px;
        height: 32px;
        border: 1px solid #d1d5db;
        background: white;
        border-radius: 6px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 0.875rem;
        font-weight: 600;
        color: #374151;
        transition: all 0.2s;
      }

      .btn-action:hover {
        background: #f3f4f6;
        border-color: #9ca3af;
      }

      .spending-section {
        margin-bottom: 1rem;
      }

      .spending-amount {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.5rem;
      }

      .amount-label {
        font-size: 0.875rem;
        color: #6b7280;
        font-weight: 500;
      }

      .amount-value {
        font-size: 1.25rem;
        font-weight: 700;
        color: #111827;
      }

      .category-card.small .amount-value {
        font-size: 1rem;
      }

      .category-card.large .amount-value {
        font-size: 1.5rem;
      }

      .spending-percentage {
        margin-bottom: 0.5rem;
      }

      .percentage-text {
        font-size: 0.75rem;
        color: #6b7280;
      }

      .spending-trend {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        font-size: 0.75rem;
        font-weight: 500;
      }

      .spending-trend.trend-success {
        color: #16a34a;
      }

      .spending-trend.trend-warning {
        color: #d97706;
      }

      .spending-trend.trend-danger {
        color: #dc2626;
      }

      .spending-trend.trend-neutral {
        color: #6b7280;
      }

      .trend-icon {
        font-size: 1rem;
      }

      .budget-section {
        margin-bottom: 1rem;
        padding: 1rem;
        background: #f9fafb;
        border-radius: 8px;
      }

      .budget-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.75rem;
      }

      .budget-label {
        font-size: 0.875rem;
        color: #6b7280;
        font-weight: 500;
      }

      .budget-amount {
        font-size: 1rem;
        font-weight: 600;
        color: #374151;
      }

      .progress-container {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        margin-bottom: 0.75rem;
      }

      .progress-bar {
        flex: 1;
        height: 6px;
        background: #e5e7eb;
        border-radius: 3px;
        overflow: hidden;
      }

      .progress-fill {
        height: 100%;
        transition: width 0.3s ease;
      }

      .progress-fill.on-track {
        background: linear-gradient(90deg, #10b981, #34d399);
      }

      .progress-fill.warning {
        background: linear-gradient(90deg, #f59e0b, #fbbf24);
      }

      .progress-fill.near-limit {
        background: linear-gradient(90deg, #f97316, #fb923c);
      }

      .progress-fill.over-budget {
        background: linear-gradient(90deg, #ef4444, #f87171);
      }

      .progress-percentage {
        font-size: 0.875rem;
        font-weight: 600;
        color: #374151;
        min-width: 35px;
      }

      .budget-details {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 0.75rem;
      }

      .budget-remaining,
      .daily-budget {
        display: flex;
        flex-direction: column;
        align-items: center;
      }

      .remaining-label,
      .daily-label {
        color: #6b7280;
        margin-bottom: 0.125rem;
      }

      .remaining-amount.positive,
      .daily-amount {
        color: #16a34a;
        font-weight: 600;
      }

      .remaining-amount.negative {
        color: #dc2626;
        font-weight: 600;
      }

      .budget-alert {
        margin-top: 0.75rem;
        padding: 0.5rem;
        border-radius: 4px;
        font-size: 0.75rem;
        font-weight: 500;
        text-align: center;
      }

      .budget-alert.info {
        background: #eff6ff;
        color: #1d4ed8;
      }

      .budget-alert.warning {
        background: #fffbeb;
        color: #d97706;
      }

      .budget-alert.danger {
        background: #fef2f2;
        color: #dc2626;
      }

      .transaction-summary.empty {
        text-align: center;
        padding: 1rem;
        color: #6b7280;
        font-size: 0.875rem;
      }

      .summary-stats {
        display: flex;
        justify-content: space-between;
        margin-bottom: 1rem;
      }

      .stat-item {
        display: flex;
        flex-direction: column;
        align-items: center;
      }

      .stat-label {
        font-size: 0.75rem;
        color: #6b7280;
        margin-bottom: 0.125rem;
      }

      .stat-value {
        font-size: 0.875rem;
        font-weight: 600;
        color: #374151;
      }

      .recent-transactions {
        border-top: 1px solid #e5e7eb;
        padding-top: 1rem;
      }

      .recent-title {
        margin: 0 0 0.75rem 0;
        font-size: 0.875rem;
        font-weight: 600;
        color: #374151;
      }

      .transaction-list {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        margin-bottom: 0.75rem;
      }

      .transaction-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.5rem;
        background: #f9fafb;
        border-radius: 4px;
      }

      .transaction-info {
        display: flex;
        flex-direction: column;
        flex: 1;
        min-width: 0;
      }

      .transaction-description {
        font-size: 0.875rem;
        color: #374151;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .transaction-date {
        font-size: 0.75rem;
        color: #6b7280;
      }

      .transaction-amount {
        font-size: 0.875rem;
        font-weight: 600;
        color: #ef4444;
      }

      .btn-view-all {
        width: 100%;
        padding: 0.5rem;
        background: none;
        border: 1px solid #d1d5db;
        border-radius: 4px;
        cursor: pointer;
        font-size: 0.75rem;
        color: #6b7280;
        transition: all 0.2s;
      }

      .btn-view-all:hover {
        background: #f3f4f6;
        color: #374151;
      }

      .trend-section {
        border-top: 1px solid #e5e7eb;
        padding-top: 1rem;
      }

      .trend-title {
        margin: 0 0 0.75rem 0;
        font-size: 0.875rem;
        font-weight: 600;
        color: #374151;
      }

      .mini-chart {
        position: relative;
        width: 100%;
        background: #f9fafb;
        border-radius: 4px;
        padding: 0.5rem;
      }

      .chart-bar {
        position: absolute;
        width: 8px;
        background: #3b82f6;
        border-radius: 2px 2px 0 0;
        cursor: pointer;
        transition: all 0.2s;
      }

      .chart-bar:hover {
        background: #2563eb;
        transform: scaleY(1.1);
      }

      .bar-tooltip {
        position: absolute;
        bottom: 100%;
        left: 50%;
        transform: translateX(-50%);
        background: #111827;
        color: white;
        padding: 0.25rem 0.5rem;
        border-radius: 4px;
        font-size: 0.75rem;
        white-space: nowrap;
        opacity: 0;
        pointer-events: none;
        transition: opacity 0.2s;
      }

      .chart-bar:hover .bar-tooltip {
        opacity: 1;
      }

      .tooltip-date {
        display: block;
      }

      .tooltip-amount {
        display: block;
        font-weight: 600;
      }

      @media (max-width: 768px) {
        .card-header {
          flex-direction: column;
          align-items: stretch;
          gap: 0.75rem;
        }

        .card-actions {
          justify-content: flex-end;
        }

        .spending-amount {
          flex-direction: column;
          align-items: stretch;
          gap: 0.25rem;
        }

        .budget-details {
          flex-direction: column;
          gap: 0.5rem;
        }

        .summary-stats {
          flex-direction: column;
          gap: 0.5rem;
        }

        .stat-item {
          flex-direction: row;
          justify-content: space-between;
        }
      }

      @media (max-width: 480px) {
        .category-card.medium,
        .category-card.large {
          padding: 1rem;
        }

        .category-name {
          font-size: 0.875rem;
        }

        .amount-value {
          font-size: 1.125rem;
        }

        .chart-bar {
          width: 6px;
        }
      }
    `;
    
    document.head.appendChild(style);
  }
}

export default CategoryCard;