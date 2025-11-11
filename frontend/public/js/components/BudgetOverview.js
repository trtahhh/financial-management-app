// Budget Overview Component
import { formatCurrency, formatCurrencyShort } from '../utils/currencyHelpers.js';
import { calculateBudgetProgress } from '../utils/calculationHelpers.js';
import { CATEGORY_NAMES, CATEGORY_COLORS } from '../constants/categories.js';
import { COLORS } from '../constants/colors.js';

class BudgetOverview {
  constructor(container, options = {}) {
    this.container = container;
    this.options = {
      showProgress: true,
      showCategories: true,
      allowEdit: true,
      period: 'monthly', // monthly, weekly
      ...options
    };
    
    this.budgets = [];
    this.totalBudget = 0;
    this.totalSpent = 0;
    this.currentPeriod = new Date();
    
    this.init();
  }

  init() {
    this.render();
    this.attachEventListeners();
  }

  render() {
    this.container.innerHTML = `
      <div class="budget-overview">
        <div class="budget-header">
          <div class="budget-title">
            <h2>Ngân sách ${this.options.period === 'monthly' ? 'tháng' : 'tuần'}</h2>
            <div class="period-selector">
              <button class="btn-period-prev" id="btn-period-prev">‹</button>
              <span class="current-period" id="current-period">${this.formatPeriod()}</span>
              <button class="btn-period-next" id="btn-period-next">›</button>
            </div>
          </div>
          ${this.options.allowEdit ? '<button class="btn-add-budget" id="btn-add-budget">+ Thêm ngân sách</button>' : ''}
        </div>

        ${this.renderOverallProgress()}
        ${this.options.showCategories ? this.renderCategoryBudgets() : ''}
      </div>
    `;

    this.renderStyles();
  }

  renderOverallProgress() {
    const progress = calculateBudgetProgress(this.totalSpent, this.totalBudget);
    const remaining = progress.remaining;
    const daysInPeriod = this.options.period === 'monthly' ? this.getDaysInMonth() : 7;
    const daysLeft = this.getDaysLeftInPeriod();
    const dailyBudget = remaining / Math.max(daysLeft, 1);

    return `
      <div class="overall-progress">
        <div class="progress-card">
          <div class="progress-header">
            <span class="progress-label">Tổng ngân sách</span>
            <span class="progress-amount">${formatCurrency(this.totalBudget)}</span>
          </div>
          
          <div class="progress-bar-container">
            <div class="progress-bar">
              <div 
                class="progress-fill ${progress.status}" 
                style="width: ${Math.min(progress.percentage, 100)}%"
              ></div>
            </div>
            <div class="progress-text">
              <span>${progress.percentage.toFixed(1)}%</span>
            </div>
          </div>
          
          <div class="progress-stats">
            <div class="stat-item">
              <span class="stat-label">Đã chi</span>
              <span class="stat-value spent">${formatCurrency(this.totalSpent)}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Còn lại</span>
              <span class="stat-value remaining ${remaining >= 0 ? 'positive' : 'negative'}">
                ${formatCurrency(Math.abs(remaining))}
              </span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Trung bình/ngày</span>
              <span class="stat-value daily">${formatCurrencyShort(dailyBudget)}</span>
            </div>
          </div>
          
          ${this.renderBudgetAlert(progress)}
        </div>
      </div>
    `;
  }

  renderBudgetAlert(progress) {
    let alert = '';
    
    if (progress.status === 'over-budget') {
      alert = `
        <div class="budget-alert danger">
          Đã vượt ngân sách ${formatCurrency(Math.abs(progress.remaining))}
        </div>
      `;
    } else if (progress.status === 'near-limit') {
      alert = `
        <div class="budget-alert warning">
          Sắp hết ngân sách! Còn ${formatCurrency(progress.remaining)}
        </div>
      `;
    } else if (progress.status === 'warning') {
      alert = `
        <div class="budget-alert info">
          Đã chi ${progress.percentage.toFixed(0)}% ngân sách
        </div>
      `;
    }
    
    return alert;
  }

  renderCategoryBudgets() {
    if (this.budgets.length === 0) {
      return `
        <div class="empty-budgets">
          <p>Chưa có ngân sách nào</p>
          ${this.options.allowEdit ? '<button class="btn-primary" onclick="this.addBudget()">Tạo ngân sách đầu tiên</button>' : ''}
        </div>
      `;
    }

    return `
      <div class="category-budgets">
        <h3>Chi tiết theo danh mục</h3>
        <div class="budget-list">
          ${this.budgets.map(budget => this.renderBudgetItem(budget)).join('')}
        </div>
      </div>
    `;
  }

  renderBudgetItem(budget) {
    const progress = calculateBudgetProgress(budget.spent, budget.amount);
    const categoryName = CATEGORY_NAMES[budget.category] || budget.category;
    const categoryColor = CATEGORY_COLORS[budget.category] || COLORS.primary;

    return `
      <div class="budget-item" data-budget-id="${budget.id}">
        <div class="budget-item-header">
          <div class="category-info">
            <div class="category-indicator" style="background-color: ${categoryColor}"></div>
            <span class="category-name">${categoryName}</span>
          </div>
          
          <div class="budget-amount">
            <span class="spent">${formatCurrencyShort(budget.spent)}</span>
            <span class="separator">/</span>
            <span class="limit">${formatCurrencyShort(budget.amount)}</span>
          </div>
          
          ${this.options.allowEdit ? `
            <div class="budget-actions">
              <button class="btn-edit-budget" data-budget-id="${budget.id}">✏️</button>
              <button class="btn-delete-budget" data-budget-id="${budget.id}">🗑️</button>
            </div>
          ` : ''}
        </div>
        
        <div class="budget-progress">
          <div class="progress-bar">
            <div 
              class="progress-fill ${progress.status}" 
              style="width: ${Math.min(progress.percentage, 100)}%"
            ></div>
          </div>
          
          <div class="progress-details">
            <span class="percentage">${progress.percentage.toFixed(0)}%</span>
            <span class="remaining ${progress.remaining >= 0 ? 'positive' : 'negative'}">
              ${progress.remaining >= 0 ? 'Còn' : 'Vượt'} ${formatCurrencyShort(Math.abs(progress.remaining))}
            </span>
          </div>
        </div>

        ${this.renderBudgetTrend(budget)}
      </div>
    `;
  }

  renderBudgetTrend(budget) {
    // Calculate trend based on spending pattern
    const dailyAverage = budget.spent / this.getDaysElapsed();
    const projectedTotal = dailyAverage * this.getDaysInPeriod();
    const trendPercentage = ((projectedTotal - budget.amount) / budget.amount) * 100;

    let trendClass = 'neutral';
    let trendText = 'Bình thường';
    
    if (trendPercentage > 20) {
      trendClass = 'danger';
      trendText = 'Có thể vượt ngân sách';
    } else if (trendPercentage > 10) {
      trendClass = 'warning';
      trendText = 'Cần chú ý';
    } else if (trendPercentage < -20) {
      trendClass = 'success';
      trendText = 'Tiết kiệm tốt';
    }

    return `
      <div class="budget-trend ${trendClass}">
        <span class="trend-text">${trendText}</span>
        <span class="trend-projection">Dự kiến: ${formatCurrencyShort(projectedTotal)}</span>
      </div>
    `;
  }

  formatPeriod() {
    if (this.options.period === 'monthly') {
      return `Tháng ${this.currentPeriod.getMonth() + 1}/${this.currentPeriod.getFullYear()}`;
    } else {
      // Weekly format
      const startOfWeek = this.getStartOfWeek(this.currentPeriod);
      const endOfWeek = new Date(startOfWeek.getTime() + 6 * 24 * 60 * 60 * 1000);
      return `${startOfWeek.getDate()}/${startOfWeek.getMonth() + 1} - ${endOfWeek.getDate()}/${endOfWeek.getMonth() + 1}`;
    }
  }

  getDaysInMonth() {
    return new Date(this.currentPeriod.getFullYear(), this.currentPeriod.getMonth() + 1, 0).getDate();
  }

  getDaysInPeriod() {
    return this.options.period === 'monthly' ? this.getDaysInMonth() : 7;
  }

  getDaysElapsed() {
    if (this.options.period === 'monthly') {
      return this.currentPeriod.getDate();
    } else {
      const startOfWeek = this.getStartOfWeek(this.currentPeriod);
      const now = new Date();
      return Math.min(Math.ceil((now - startOfWeek) / (24 * 60 * 60 * 1000)) + 1, 7);
    }
  }

  getDaysLeftInPeriod() {
    return this.getDaysInPeriod() - this.getDaysElapsed();
  }

  getStartOfWeek(date) {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    return new Date(d.setDate(diff));
  }

  attachEventListeners() {
    // Period navigation
    document.getElementById('btn-period-prev')?.addEventListener('click', () => {
      this.navigatePeriod(-1);
    });

    document.getElementById('btn-period-next')?.addEventListener('click', () => {
      this.navigatePeriod(1);
    });

    // Add budget
    document.getElementById('btn-add-budget')?.addEventListener('click', () => {
      this.onAddBudget?.();
    });

    // Budget actions
    this.container.addEventListener('click', (e) => {
      if (e.target.classList.contains('btn-edit-budget')) {
        const budgetId = e.target.dataset.budgetId;
        this.onEditBudget?.(budgetId);
      }

      if (e.target.classList.contains('btn-delete-budget')) {
        const budgetId = e.target.dataset.budgetId;
        this.onDeleteBudget?.(budgetId);
      }
    });
  }

  navigatePeriod(direction) {
    if (this.options.period === 'monthly') {
      this.currentPeriod.setMonth(this.currentPeriod.getMonth() + direction);
    } else {
      this.currentPeriod.setDate(this.currentPeriod.getDate() + (direction * 7));
    }
    
    document.getElementById('current-period').textContent = this.formatPeriod();
    this.onPeriodChange?.(this.currentPeriod);
  }

  // Public methods
  setBudgets(budgets) {
    this.budgets = budgets || [];
    this.calculateTotals();
    this.render();
  }

  updateBudget(budgetId, updates) {
    const index = this.budgets.findIndex(b => b.id === budgetId);
    if (index >= 0) {
      this.budgets[index] = { ...this.budgets[index], ...updates };
      this.calculateTotals();
      this.render();
    }
  }

  calculateTotals() {
    this.totalBudget = this.budgets.reduce((sum, b) => sum + (b.amount || 0), 0);
    this.totalSpent = this.budgets.reduce((sum, b) => sum + (b.spent || 0), 0);
  }

  refresh() {
    this.render();
  }

  renderStyles() {
    if (document.getElementById('budget-overview-styles')) return;

    const style = document.createElement('style');
    style.id = 'budget-overview-styles';
    style.textContent = `
      .budget-overview {
        max-width: 800px;
        margin: 0 auto;
      }

      .budget-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 1.5rem;
      }

      .budget-title h2 {
        margin: 0 0 0.5rem 0;
        color: #111827;
        font-size: 1.5rem;
      }

      .period-selector {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .btn-period-prev,
      .btn-period-next {
        width: 32px;
        height: 32px;
        border: 1px solid #d1d5db;
        background: white;
        border-radius: 6px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;
      }

      .btn-period-prev:hover,
      .btn-period-next:hover {
        background: #f3f4f6;
        border-color: #9ca3af;
      }

      .current-period {
        font-weight: 600;
        color: #374151;
        min-width: 120px;
        text-align: center;
      }

      .btn-add-budget {
        padding: 0.5rem 1rem;
        background: #3b82f6;
        color: white;
        border: none;
        border-radius: 6px;
        cursor: pointer;
        font-size: 0.875rem;
        font-weight: 500;
      }

      .btn-add-budget:hover {
        background: #2563eb;
      }

      .overall-progress {
        margin-bottom: 2rem;
      }

      .progress-card {
        background: white;
        border-radius: 12px;
        padding: 1.5rem;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        border: 1px solid #e5e7eb;
      }

      .progress-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
      }

      .progress-label {
        font-size: 0.875rem;
        color: #6b7280;
        font-weight: 500;
      }

      .progress-amount {
        font-size: 1.5rem;
        font-weight: 700;
        color: #111827;
      }

      .progress-bar-container {
        display: flex;
        align-items: center;
        gap: 1rem;
        margin-bottom: 1rem;
      }

      .progress-bar {
        flex: 1;
        height: 8px;
        background: #f3f4f6;
        border-radius: 4px;
        overflow: hidden;
      }

      .progress-fill {
        height: 100%;
        transition: width 0.3s ease;
        border-radius: 4px;
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

      .progress-text {
        font-weight: 600;
        color: #374151;
        min-width: 40px;
      }

      .progress-stats {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 1rem;
      }

      .stat-item {
        text-align: center;
      }

      .stat-label {
        display: block;
        font-size: 0.75rem;
        color: #6b7280;
        margin-bottom: 0.25rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .stat-value {
        display: block;
        font-size: 1.125rem;
        font-weight: 600;
      }

      .stat-value.spent {
        color: #ef4444;
      }

      .stat-value.remaining.positive {
        color: #10b981;
      }

      .stat-value.remaining.negative {
        color: #ef4444;
      }

      .stat-value.daily {
        color: #3b82f6;
      }

      .budget-alert {
        margin-top: 1rem;
        padding: 0.75rem 1rem;
        border-radius: 6px;
        font-size: 0.875rem;
        font-weight: 500;
        text-align: center;
      }

      .budget-alert.info {
        background: #eff6ff;
        color: #1d4ed8;
        border: 1px solid #bfdbfe;
      }

      .budget-alert.warning {
        background: #fffbeb;
        color: #d97706;
        border: 1px solid #fed7aa;
      }

      .budget-alert.danger {
        background: #fef2f2;
        color: #dc2626;
        border: 1px solid #fecaca;
      }

      .category-budgets h3 {
        margin: 0 0 1rem 0;
        color: #111827;
        font-size: 1.125rem;
      }

      .budget-list {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .budget-item {
        background: white;
        border-radius: 8px;
        padding: 1rem;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        border: 1px solid #e5e7eb;
        transition: transform 0.2s, box-shadow 0.2s;
      }

      .budget-item:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      }

      .budget-item-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.75rem;
      }

      .category-info {
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
        font-weight: 500;
        color: #374151;
      }

      .budget-amount {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        font-weight: 600;
      }

      .budget-amount .spent {
        color: #ef4444;
      }

      .budget-amount .separator {
        color: #6b7280;
      }

      .budget-amount .limit {
        color: #374151;
      }

      .budget-actions {
        display: flex;
        gap: 0.25rem;
      }

      .btn-edit-budget,
      .btn-delete-budget {
        width: 28px;
        height: 28px;
        border: none;
        background: none;
        cursor: pointer;
        border-radius: 4px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 0.875rem;
        transition: background 0.2s;
      }

      .btn-edit-budget:hover {
        background: #f3f4f6;
      }

      .btn-delete-budget:hover {
        background: #fef2f2;
      }

      .budget-progress {
        margin-bottom: 0.75rem;
      }

      .progress-details {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 0.5rem;
        font-size: 0.875rem;
      }

      .progress-details .percentage {
        font-weight: 600;
        color: #374151;
      }

      .progress-details .remaining.positive {
        color: #10b981;
      }

      .progress-details .remaining.negative {
        color: #ef4444;
      }

      .budget-trend {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 0.75rem;
        padding: 0.5rem;
        border-radius: 4px;
      }

      .budget-trend.success {
        background: #f0fdf4;
        color: #16a34a;
      }

      .budget-trend.warning {
        background: #fffbeb;
        color: #d97706;
      }

      .budget-trend.danger {
        background: #fef2f2;
        color: #dc2626;
      }

      .budget-trend.neutral {
        background: #f9fafb;
        color: #6b7280;
      }

      .trend-text {
        font-weight: 500;
      }

      .trend-projection {
        font-weight: 600;
      }

      .empty-budgets {
        text-align: center;
        padding: 3rem 1rem;
        color: #6b7280;
      }

      .empty-budgets p {
        margin-bottom: 1rem;
        font-size: 1.125rem;
      }

      .btn-primary {
        background: #3b82f6;
        color: white;
        padding: 0.75rem 1.5rem;
        border: none;
        border-radius: 6px;
        cursor: pointer;
        font-size: 0.875rem;
        font-weight: 500;
      }

      .btn-primary:hover {
        background: #2563eb;
      }

      @media (max-width: 768px) {
        .budget-header {
          flex-direction: column;
          gap: 1rem;
          align-items: stretch;
        }

        .budget-title {
          text-align: center;
        }

        .progress-stats {
          grid-template-columns: 1fr;
          gap: 0.75rem;
        }

        .budget-item-header {
          flex-direction: column;
          align-items: flex-start;
          gap: 0.5rem;
        }

        .budget-amount,
        .budget-actions {
          align-self: flex-end;
        }

        .budget-trend {
          flex-direction: column;
          text-align: center;
          gap: 0.25rem;
        }
      }

      @media (max-width: 480px) {
        .period-selector {
          flex-direction: column;
          gap: 0.75rem;
        }

        .current-period {
          min-width: auto;
        }

        .progress-amount {
          font-size: 1.25rem;
        }

        .stat-value {
          font-size: 1rem;
        }
      }
    `;
    
    document.head.appendChild(style);
  }
}

export default BudgetOverview;