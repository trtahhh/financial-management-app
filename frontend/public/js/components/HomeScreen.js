// Dashboard Home Screen Component
import { formatCurrency, formatCurrencyShort } from '../utils/currencyHelpers.js';
import { getRelativeTime, formatDate } from '../utils/dateHelpers.js';
import { calculateBudgetProgress, calculateSavingsGoal } from '../utils/calculationHelpers.js';
import { CATEGORY_NAMES, CATEGORY_COLORS } from '../constants/categories.js';
import { COLORS, GRADIENTS } from '../constants/colors.js';
import { CONFIG } from '../constants/config.js';

class HomeScreen {
  constructor(container, options = {}) {
    this.container = container;
    this.options = {
      showQuickActions: true,
      showRecentTransactions: true,
      showBudgetOverview: true,
      showGoals: true,
      showInsights: true,
      period: 'monthly',
      refreshInterval: 30000, // 30 seconds
      ...options
    };
    
    this.data = {
      totalBalance: 0,
      monthlyIncome: 0,
      monthlyExpenses: 0,
      budgets: [],
      recentTransactions: [],
      goals: [],
      categories: new Map(),
      streak: 0,
      level: 1,
      xp: 0
    };
    
    this.refreshTimer = null;
    
    this.init();
  }

  init() {
    this.render();
    this.attachEventListeners();
    this.startAutoRefresh();
  }

  render() {
    this.container.innerHTML = `
      <div class="home-screen">
        ${this.renderHeader()}
        ${this.renderSummaryCards()}
        ${this.renderQuickActions()}
        ${this.renderMainContent()}
        ${this.renderBottomActions()}
      </div>
    `;

    this.renderStyles();
    this.startAnimations();
  }

  renderHeader() {
    const greeting = this.getGreeting();
    const currentDate = formatDate(new Date(), 'dd/MM/yyyy');
    
    return `
      <div class="dashboard-header">
        <div class="greeting-section">
          <h1 class="greeting">${greeting}</h1>
          <p class="current-date">${currentDate}</p>
        </div>
        
        <div class="user-stats">
          <div class="stat-item">
            <div class="stat-icon level">Lv</div>
            <span class="stat-value">${this.data.level}</span>
          </div>
          <div class="stat-item">
            <div class="stat-icon streak">🔥</div>
            <span class="stat-value">${this.data.streak}</span>
          </div>
          <div class="xp-progress">
            <div class="xp-bar">
              <div class="xp-fill" style="width: ${this.calculateXPProgress()}%"></div>
            </div>
            <span class="xp-text">${this.data.xp} XP</span>
          </div>
        </div>
      </div>
    `;
  }

  renderSummaryCards() {
    const monthlyBalance = this.data.monthlyIncome - this.data.monthlyExpenses;
    const savings = this.calculateSavingsRate();
    const budgetHealth = this.calculateBudgetHealth();
    
    return `
      <div class="summary-cards">
        <div class="summary-card balance ${monthlyBalance >= 0 ? 'positive' : 'negative'}">
          <div class="card-icon balance-icon"></div>
          <div class="card-content">
            <h3 class="card-title">Số dư</h3>
            <p class="card-value">${formatCurrency(this.data.totalBalance)}</p>
            <span class="card-trend ${monthlyBalance >= 0 ? 'up' : 'down'}">
              ${monthlyBalance >= 0 ? '↗' : '↘'} ${formatCurrencyShort(Math.abs(monthlyBalance))} tháng này
            </span>
          </div>
        </div>

        <div class="summary-card income">
          <div class="card-icon income-icon"></div>
          <div class="card-content">
            <h3 class="card-title">Thu nhập</h3>
            <p class="card-value">${formatCurrency(this.data.monthlyIncome)}</p>
            <span class="card-subtitle">Tháng ${new Date().getMonth() + 1}</span>
          </div>
        </div>

        <div class="summary-card expenses">
          <div class="card-icon expenses-icon"></div>
          <div class="card-content">
            <h3 class="card-title">Chi tiêu</h3>
            <p class="card-value">${formatCurrency(this.data.monthlyExpenses)}</p>
            <span class="card-subtitle">${this.getBudgetUsageText()}</span>
          </div>
        </div>

        <div class="summary-card savings ${savings >= 20 ? 'good' : savings >= 10 ? 'medium' : 'low'}">
          <div class="card-icon savings-icon"></div>
          <div class="card-content">
            <h3 class="card-title">Tiết kiệm</h3>
            <p class="card-value">${savings.toFixed(1)}%</p>
            <span class="card-subtitle">${this.getSavingsAdvice(savings)}</span>
          </div>
        </div>
      </div>
    `;
  }

  renderQuickActions() {
    if (!this.options.showQuickActions) return '';
    
    return `
      <div class="quick-actions">
        <h2 class="section-title">Thao tác nhanh</h2>
        
        <div class="action-grid">
          <button class="action-btn primary" id="btn-add-expense">
            <div class="action-icon expense"></div>
            <span class="action-text">Chi tiêu</span>
            <span class="action-shortcut">&lt; 10s</span>
          </button>
          
          <button class="action-btn success" id="btn-add-income">
            <div class="action-icon income"></div>
            <span class="action-text">Thu nhập</span>
          </button>
          
          <button class="action-btn info" id="btn-transfer">
            <div class="action-icon transfer"></div>
            <span class="action-text">Chuyển tiền</span>
          </button>
          
          <button class="action-btn warning" id="btn-set-budget">
            <div class="action-icon budget"></div>
            <span class="action-text">Đặt ngân sách</span>
          </button>
        </div>
      </div>
    `;
  }

  renderMainContent() {
    return `
      <div class="main-content">
        <div class="content-grid">
          <div class="content-column primary">
            ${this.renderBudgetOverview()}
            ${this.renderRecentTransactions()}
          </div>
          
          <div class="content-column secondary">
            ${this.renderGoals()}
            ${this.renderInsights()}
          </div>
        </div>
      </div>
    `;
  }

  renderBudgetOverview() {
    if (!this.options.showBudgetOverview || this.data.budgets.length === 0) {
      return `
        <div class="budget-overview-card">
          <h3 class="card-header">Ngân sách tháng này</h3>
          <div class="empty-state">
            <p>Chưa có ngân sách nào</p>
            <button class="btn-secondary" id="btn-create-budget">Tạo ngân sách đầu tiên</button>
          </div>
        </div>
      `;
    }

    const topCategories = this.getTopSpendingCategories(3);
    const totalBudget = this.data.budgets.reduce((sum, b) => sum + b.amount, 0);
    const totalSpent = this.data.budgets.reduce((sum, b) => sum + b.spent, 0);
    const overallProgress = calculateBudgetProgress(totalSpent, totalBudget);

    return `
      <div class="budget-overview-card">
        <div class="card-header">
          <h3>Ngân sách tháng này</h3>
          <button class="btn-text" id="btn-view-budgets">Xem tất cả</button>
        </div>
        
        <div class="budget-summary">
          <div class="budget-total">
            <span class="budget-spent">${formatCurrency(totalSpent)}</span>
            <span class="budget-separator">/</span>
            <span class="budget-limit">${formatCurrency(totalBudget)}</span>
          </div>
          
          <div class="budget-progress">
            <div class="progress-bar">
              <div class="progress-fill ${overallProgress.status}" 
                   style="width: ${Math.min(overallProgress.percentage, 100)}%"></div>
            </div>
            <span class="progress-text">${overallProgress.percentage.toFixed(0)}%</span>
          </div>
        </div>

        <div class="category-breakdown">
          ${topCategories.map(category => this.renderCategoryItem(category)).join('')}
        </div>
        
        ${this.renderBudgetAlert(overallProgress)}
      </div>
    `;
  }

  renderCategoryItem(category) {
    const categoryName = CATEGORY_NAMES[category.category] || category.category;
    const categoryColor = CATEGORY_COLORS[category.category] || COLORS.primary;
    const progress = calculateBudgetProgress(category.spent, category.budget);

    return `
      <div class="category-item" data-category="${category.category}">
        <div class="category-info">
          <div class="category-indicator" style="background-color: ${categoryColor}"></div>
          <span class="category-name">${categoryName}</span>
        </div>
        
        <div class="category-amount">
          <span class="spent">${formatCurrencyShort(category.spent)}</span>
          <span class="limit">/${formatCurrencyShort(category.budget)}</span>
        </div>
        
        <div class="category-progress">
          <div class="mini-progress-bar">
            <div class="mini-progress-fill ${progress.status}" 
                 style="width: ${Math.min(progress.percentage, 100)}%"></div>
          </div>
        </div>
      </div>
    `;
  }

  renderBudgetAlert(progress) {
    if (progress.status === 'on-track') return '';
    
    let alertClass = 'info';
    let alertText = '';
    
    if (progress.status === 'over-budget') {
      alertClass = 'danger';
      alertText = `Vượt ngân sách ${formatCurrency(Math.abs(progress.remaining))}`;
    } else if (progress.status === 'near-limit') {
      alertClass = 'warning';
      alertText = `Sắp hết ngân sách! Còn ${formatCurrency(progress.remaining)}`;
    } else if (progress.status === 'warning') {
      alertClass = 'info';
      alertText = `Đã chi ${progress.percentage.toFixed(0)}% ngân sách`;
    }
    
    return `
      <div class="budget-alert ${alertClass}">
        ${alertText}
      </div>
    `;
  }

  renderRecentTransactions() {
    if (!this.options.showRecentTransactions) return '';
    
    const recentTransactions = this.data.recentTransactions.slice(0, 5);
    
    if (recentTransactions.length === 0) {
      return `
        <div class="recent-transactions-card">
          <h3 class="card-header">Giao dịch gần đây</h3>
          <div class="empty-state">
            <p>Chưa có giao dịch nào</p>
            <button class="btn-secondary" id="btn-add-first-transaction">Thêm giao dịch đầu tiên</button>
          </div>
        </div>
      `;
    }

    return `
      <div class="recent-transactions-card">
        <div class="card-header">
          <h3>Giao dịch gần đây</h3>
          <button class="btn-text" id="btn-view-transactions">Xem tất cả</button>
        </div>
        
        <div class="transaction-list">
          ${recentTransactions.map(transaction => this.renderTransactionItem(transaction)).join('')}
        </div>
      </div>
    `;
  }

  renderTransactionItem(transaction) {
    const categoryName = CATEGORY_NAMES[transaction.category] || transaction.category;
    const categoryColor = CATEGORY_COLORS[transaction.category] || COLORS.primary;
    const relativeTime = getRelativeTime(new Date(transaction.date));

    return `
      <div class="transaction-item" data-transaction-id="${transaction.id}">
        <div class="transaction-info">
          <div class="transaction-category">
            <div class="category-indicator" style="background-color: ${categoryColor}"></div>
            <span class="category-text">${categoryName}</span>
          </div>
          <div class="transaction-details">
            <span class="transaction-description">${transaction.description || 'Không có mô tả'}</span>
            <span class="transaction-time">${relativeTime}</span>
          </div>
        </div>
        
        <div class="transaction-amount ${transaction.type}">
          ${transaction.type === 'income' ? '+' : '-'}${formatCurrencyShort(transaction.amount)}
        </div>
      </div>
    `;
  }

  renderGoals() {
    if (!this.options.showGoals) return '';
    
    const activeGoals = this.data.goals.filter(g => g.status === 'active').slice(0, 3);
    
    if (activeGoals.length === 0) {
      return `
        <div class="goals-card">
          <h3 class="card-header">Mục tiêu</h3>
          <div class="empty-state">
            <p>Chưa có mục tiêu nào</p>
            <button class="btn-secondary" id="btn-create-goal">Tạo mục tiêu</button>
          </div>
        </div>
      `;
    }

    return `
      <div class="goals-card">
        <div class="card-header">
          <h3>Mục tiêu</h3>
          <button class="btn-text" id="btn-view-goals">Xem tất cả</button>
        </div>
        
        <div class="goals-list">
          ${activeGoals.map(goal => this.renderGoalItem(goal)).join('')}
        </div>
      </div>
    `;
  }

  renderGoalItem(goal) {
    const progress = calculateSavingsGoal(goal.currentAmount, goal.targetAmount);
    const daysLeft = this.calculateDaysLeft(goal.deadline);
    
    return `
      <div class="goal-item" data-goal-id="${goal.id}">
        <div class="goal-header">
          <span class="goal-title">${goal.title}</span>
          <span class="goal-target">${formatCurrencyShort(goal.targetAmount)}</span>
        </div>
        
        <div class="goal-progress">
          <div class="progress-bar">
            <div class="progress-fill" style="width: ${progress.percentage}%"></div>
          </div>
          <span class="progress-text">${progress.percentage.toFixed(0)}%</span>
        </div>
        
        <div class="goal-info">
          <span class="goal-current">${formatCurrencyShort(goal.currentAmount)}</span>
          <span class="goal-deadline">${daysLeft > 0 ? `${daysLeft} ngày` : 'Đã hết hạn'}</span>
        </div>
      </div>
    `;
  }

  renderInsights() {
    if (!this.options.showInsights) return '';
    
    const insights = this.generateInsights();
    
    return `
      <div class="insights-card">
        <h3 class="card-header">Thông tin hữu ích</h3>
        
        <div class="insights-list">
          ${insights.map(insight => `
            <div class="insight-item ${insight.type}">
              <div class="insight-icon ${insight.type}"></div>
              <div class="insight-content">
                <p class="insight-text">${insight.text}</p>
                ${insight.action ? `<button class="insight-action" data-action="${insight.action}">${insight.actionText}</button>` : ''}
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }

  renderBottomActions() {
    return `
      <div class="bottom-actions">
        <button class="floating-action-btn" id="btn-quick-add">
          <span class="fab-icon">+</span>
          <span class="fab-text">Thêm nhanh</span>
        </button>
      </div>
    `;
  }

  // Helper methods
  getGreeting() {
    const hour = new Date().getHours();
    if (hour < 12) return 'Chào buổi sáng!';
    if (hour < 18) return 'Chào buổi chiều!';
    return 'Chào buổi tối!';
  }

  calculateXPProgress() {
    const xpForCurrentLevel = this.data.level * 100;
    const xpForNextLevel = (this.data.level + 1) * 100;
    const currentLevelXP = this.data.xp - xpForCurrentLevel;
    const neededForNext = xpForNextLevel - xpForCurrentLevel;
    return (currentLevelXP / neededForNext) * 100;
  }

  calculateSavingsRate() {
    if (this.data.monthlyIncome === 0) return 0;
    const savings = this.data.monthlyIncome - this.data.monthlyExpenses;
    return (savings / this.data.monthlyIncome) * 100;
  }

  calculateBudgetHealth() {
    if (this.data.budgets.length === 0) return 'none';
    
    const overBudget = this.data.budgets.filter(b => b.spent > b.amount).length;
    const nearLimit = this.data.budgets.filter(b => 
      b.spent <= b.amount && (b.spent / b.amount) > 0.8
    ).length;
    
    if (overBudget > 0) return 'danger';
    if (nearLimit > 0) return 'warning';
    return 'good';
  }

  getBudgetUsageText() {
    const totalBudget = this.data.budgets.reduce((sum, b) => sum + b.amount, 0);
    if (totalBudget === 0) return 'Chưa đặt ngân sách';
    
    const percentage = (this.data.monthlyExpenses / totalBudget) * 100;
    return `${percentage.toFixed(0)}% ngân sách`;
  }

  getSavingsAdvice(savings) {
    if (savings >= 20) return 'Xuất sắc!';
    if (savings >= 10) return 'Tốt';
    if (savings >= 0) return 'Cần cải thiện';
    return 'Cần xem xét lại';
  }

  getTopSpendingCategories(limit) {
    return this.data.budgets
      .map(budget => ({
        category: budget.category,
        spent: budget.spent,
        budget: budget.amount
      }))
      .sort((a, b) => b.spent - a.spent)
      .slice(0, limit);
  }

  calculateDaysLeft(deadline) {
    const now = new Date();
    const end = new Date(deadline);
    const diffTime = end - now;
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }

  generateInsights() {
    const insights = [];
    
    // Budget insights
    const overBudgetCategories = this.data.budgets.filter(b => b.spent > b.amount);
    if (overBudgetCategories.length > 0) {
      insights.push({
        type: 'warning',
        text: `Bạn đã vượt ngân sách ở ${overBudgetCategories.length} danh mục`,
        action: 'view-budgets',
        actionText: 'Xem chi tiết'
      });
    }
    
    // Savings insights
    const savingsRate = this.calculateSavingsRate();
    if (savingsRate < 10) {
      insights.push({
        type: 'info',
        text: 'Hãy thử tiết kiệm ít nhất 10% thu nhập mỗi tháng',
        action: 'create-goal',
        actionText: 'Đặt mục tiêu'
      });
    }
    
    // Goal insights
    const upcomingDeadlines = this.data.goals.filter(g => {
      const daysLeft = this.calculateDaysLeft(g.deadline);
      return daysLeft <= 30 && daysLeft > 0;
    });
    
    if (upcomingDeadlines.length > 0) {
      insights.push({
        type: 'info',
        text: `Bạn có ${upcomingDeadlines.length} mục tiêu sắp đến hạn`,
        action: 'view-goals',
        actionText: 'Xem mục tiêu'
      });
    }
    
    // Streak insights
    if (this.data.streak >= 7) {
      insights.push({
        type: 'success',
        text: `Tuyệt vời! Bạn đã ghi chép ${this.data.streak} ngày liên tục`,
      });
    }
    
    return insights.slice(0, 3); // Limit to 3 insights
  }

  startAnimations() {
    // Animate cards on load
    const cards = this.container.querySelectorAll('.summary-card, .action-btn');
    cards.forEach((card, index) => {
      setTimeout(() => {
        card.classList.add('animate-in');
      }, index * 100);
    });
  }

  startAutoRefresh() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
    
    this.refreshTimer = setInterval(() => {
      this.onRefreshData?.();
    }, this.options.refreshInterval);
  }

  attachEventListeners() {
    // Quick action buttons
    document.getElementById('btn-add-expense')?.addEventListener('click', () => {
      this.onAddTransaction?.('expense');
    });

    document.getElementById('btn-add-income')?.addEventListener('click', () => {
      this.onAddTransaction?.('income');
    });

    document.getElementById('btn-transfer')?.addEventListener('click', () => {
      this.onTransfer?.();
    });

    document.getElementById('btn-set-budget')?.addEventListener('click', () => {
      this.onSetBudget?.();
    });

    // Navigation buttons
    document.getElementById('btn-view-budgets')?.addEventListener('click', () => {
      this.onNavigate?.('budgets');
    });

    document.getElementById('btn-view-transactions')?.addEventListener('click', () => {
      this.onNavigate?.('transactions');
    });

    document.getElementById('btn-view-goals')?.addEventListener('click', () => {
      this.onNavigate?.('goals');
    });

    // Create buttons
    document.getElementById('btn-create-budget')?.addEventListener('click', () => {
      this.onCreateBudget?.();
    });

    document.getElementById('btn-create-goal')?.addEventListener('click', () => {
      this.onCreateGoal?.();
    });

    document.getElementById('btn-add-first-transaction')?.addEventListener('click', () => {
      this.onAddTransaction?.('expense');
    });

    // Floating action button
    document.getElementById('btn-quick-add')?.addEventListener('click', () => {
      this.onQuickAdd?.();
    });

    // Insight actions
    this.container.addEventListener('click', (e) => {
      if (e.target.classList.contains('insight-action')) {
        const action = e.target.dataset.action;
        this.onInsightAction?.(action);
      }
    });

    // Card clicks for navigation
    this.container.addEventListener('click', (e) => {
      const categoryItem = e.target.closest('.category-item');
      if (categoryItem) {
        const category = categoryItem.dataset.category;
        this.onCategoryClick?.(category);
      }

      const goalItem = e.target.closest('.goal-item');
      if (goalItem) {
        const goalId = goalItem.dataset.goalId;
        this.onGoalClick?.(goalId);
      }

      const transactionItem = e.target.closest('.transaction-item');
      if (transactionItem) {
        const transactionId = transactionItem.dataset.transactionId;
        this.onTransactionClick?.(transactionId);
      }
    });
  }

  // Public methods
  setData(data) {
    this.data = { ...this.data, ...data };
    this.render();
  }

  updateBalance(balance) {
    this.data.totalBalance = balance;
    this.render();
  }

  updateBudgets(budgets) {
    this.data.budgets = budgets || [];
    this.render();
  }

  updateTransactions(transactions) {
    this.data.recentTransactions = transactions || [];
    this.render();
  }

  updateGoals(goals) {
    this.data.goals = goals || [];
    this.render();
  }

  updateUserStats(stats) {
    this.data.streak = stats.streak || 0;
    this.data.level = stats.level || 1;
    this.data.xp = stats.xp || 0;
    this.render();
  }

  refresh() {
    this.render();
  }

  destroy() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
    }
  }

  renderStyles() {
    if (document.getElementById('home-screen-styles')) return;

    const style = document.createElement('style');
    style.id = 'home-screen-styles';
    style.textContent = `
      .home-screen {
        max-width: 1200px;
        margin: 0 auto;
        padding: 1rem;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      }

      /* Header */
      .dashboard-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 2rem;
        padding: 1.5rem 0;
        border-bottom: 1px solid #e5e7eb;
      }

      .greeting-section h1 {
        margin: 0 0 0.25rem 0;
        font-size: 2rem;
        font-weight: 700;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }

      .current-date {
        margin: 0;
        color: #6b7280;
        font-size: 0.875rem;
      }

      .user-stats {
        display: flex;
        align-items: center;
        gap: 1rem;
      }

      .stat-item {
        display: flex;
        align-items: center;
        gap: 0.25rem;
      }

      .stat-icon {
        width: 24px;
        height: 24px;
        border-radius: 6px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 0.75rem;
        font-weight: 600;
      }

      .stat-icon.level {
        background: #3b82f6;
        color: white;
      }

      .stat-icon.streak {
        background: #f59e0b;
        color: white;
      }

      .stat-value {
        font-weight: 600;
        color: #111827;
      }

      .xp-progress {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .xp-bar {
        width: 60px;
        height: 6px;
        background: #e5e7eb;
        border-radius: 3px;
        overflow: hidden;
      }

      .xp-fill {
        height: 100%;
        background: linear-gradient(90deg, #10b981, #34d399);
        transition: width 0.3s ease;
      }

      .xp-text {
        font-size: 0.75rem;
        color: #6b7280;
        font-weight: 500;
      }

      /* Summary Cards */
      .summary-cards {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 1rem;
        margin-bottom: 2rem;
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
        transition: all 0.3s ease;
        opacity: 0;
        transform: translateY(20px);
      }

      .summary-card.animate-in {
        opacity: 1;
        transform: translateY(0);
      }

      .summary-card:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
      }

      .card-icon {
        width: 48px;
        height: 48px;
        border-radius: 12px;
        flex-shrink: 0;
      }

      .card-icon.balance-icon {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }

      .card-icon.income-icon {
        background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
      }

      .card-icon.expenses-icon {
        background: linear-gradient(135deg, #ef4444 0%, #f87171 100%);
      }

      .card-icon.savings-icon {
        background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
      }

      .card-content {
        flex: 1;
        min-width: 0;
      }

      .card-title {
        margin: 0 0 0.5rem 0;
        font-size: 0.875rem;
        color: #6b7280;
        font-weight: 500;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .card-value {
        margin: 0 0 0.25rem 0;
        font-size: 1.75rem;
        font-weight: 700;
        color: #111827;
      }

      .card-trend {
        font-size: 0.75rem;
        font-weight: 500;
        display: flex;
        align-items: center;
        gap: 0.25rem;
      }

      .card-trend.up {
        color: #10b981;
      }

      .card-trend.down {
        color: #ef4444;
      }

      .card-subtitle {
        font-size: 0.75rem;
        color: #6b7280;
      }

      .summary-card.positive {
        border-left: 4px solid #10b981;
      }

      .summary-card.negative {
        border-left: 4px solid #ef4444;
      }

      .summary-card.savings.good {
        border-left: 4px solid #10b981;
      }

      .summary-card.savings.medium {
        border-left: 4px solid #f59e0b;
      }

      .summary-card.savings.low {
        border-left: 4px solid #ef4444;
      }

      /* Quick Actions */
      .quick-actions {
        margin-bottom: 2rem;
      }

      .section-title {
        margin: 0 0 1rem 0;
        font-size: 1.25rem;
        font-weight: 600;
        color: #111827;
      }

      .action-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 1rem;
      }

      .action-btn {
        background: white;
        border: 2px solid #e5e7eb;
        border-radius: 12px;
        padding: 1.5rem;
        cursor: pointer;
        transition: all 0.3s ease;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.75rem;
        opacity: 0;
        transform: translateY(20px);
      }

      .action-btn.animate-in {
        opacity: 1;
        transform: translateY(0);
      }

      .action-btn:hover {
        transform: translateY(-4px);
        box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
      }

      .action-btn.primary {
        border-color: #3b82f6;
        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
      }

      .action-btn.primary:hover {
        background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
      }

      .action-btn.success {
        border-color: #10b981;
        background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
      }

      .action-btn.success:hover {
        background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
      }

      .action-btn.info {
        border-color: #06b6d4;
        background: linear-gradient(135deg, #f0fdfa 0%, #ccfbf1 100%);
      }

      .action-btn.info:hover {
        background: linear-gradient(135deg, #ccfbf1 0%, #99f6e4 100%);
      }

      .action-btn.warning {
        border-color: #f59e0b;
        background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
      }

      .action-btn.warning:hover {
        background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
      }

      .action-icon {
        width: 40px;
        height: 40px;
        border-radius: 10px;
      }

      .action-icon.expense {
        background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
      }

      .action-icon.income {
        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
      }

      .action-icon.transfer {
        background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
      }

      .action-icon.budget {
        background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
      }

      .action-text {
        font-weight: 600;
        color: #111827;
        font-size: 1rem;
      }

      .action-shortcut {
        font-size: 0.75rem;
        color: #6b7280;
        background: #f3f4f6;
        padding: 0.25rem 0.5rem;
        border-radius: 4px;
      }

      /* Main Content */
      .main-content {
        margin-bottom: 2rem;
      }

      .content-grid {
        display: grid;
        grid-template-columns: 2fr 1fr;
        gap: 2rem;
      }

      .content-column {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
      }

      /* Cards */
      .budget-overview-card,
      .recent-transactions-card,
      .goals-card,
      .insights-card {
        background: white;
        border-radius: 12px;
        padding: 1.5rem;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        border: 1px solid #e5e7eb;
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1.5rem;
      }

      .card-header h3 {
        margin: 0;
        font-size: 1.125rem;
        font-weight: 600;
        color: #111827;
      }

      .btn-text {
        background: none;
        border: none;
        color: #3b82f6;
        font-size: 0.875rem;
        font-weight: 500;
        cursor: pointer;
        padding: 0.25rem 0.5rem;
        border-radius: 4px;
        transition: background 0.2s;
      }

      .btn-text:hover {
        background: #eff6ff;
      }

      .btn-secondary {
        background: #f3f4f6;
        border: 1px solid #d1d5db;
        color: #374151;
        padding: 0.5rem 1rem;
        border-radius: 6px;
        cursor: pointer;
        font-size: 0.875rem;
        font-weight: 500;
        transition: all 0.2s;
      }

      .btn-secondary:hover {
        background: #e5e7eb;
        border-color: #9ca3af;
      }

      /* Budget Overview */
      .budget-summary {
        margin-bottom: 1.5rem;
      }

      .budget-total {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 0.75rem;
      }

      .budget-spent {
        font-size: 1.5rem;
        font-weight: 700;
        color: #ef4444;
      }

      .budget-separator {
        font-size: 1.25rem;
        color: #6b7280;
      }

      .budget-limit {
        font-size: 1.5rem;
        font-weight: 700;
        color: #374151;
      }

      .budget-progress {
        display: flex;
        align-items: center;
        gap: 0.75rem;
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
        min-width: 35px;
      }

      .category-breakdown {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .category-item {
        display: grid;
        grid-template-columns: 1fr auto auto;
        align-items: center;
        gap: 1rem;
        padding: 0.75rem;
        background: #f9fafb;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.2s;
      }

      .category-item:hover {
        background: #f3f4f6;
        transform: translateX(4px);
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

      .category-amount {
        display: flex;
        align-items: center;
        gap: 0.125rem;
        font-size: 0.875rem;
      }

      .category-amount .spent {
        font-weight: 600;
        color: #ef4444;
      }

      .category-amount .limit {
        color: #6b7280;
      }

      .category-progress {
        width: 60px;
      }

      .mini-progress-bar {
        width: 100%;
        height: 4px;
        background: #e5e7eb;
        border-radius: 2px;
        overflow: hidden;
      }

      .mini-progress-fill {
        height: 100%;
        transition: width 0.3s ease;
      }

      .mini-progress-fill.on-track {
        background: #10b981;
      }

      .mini-progress-fill.warning {
        background: #f59e0b;
      }

      .mini-progress-fill.near-limit {
        background: #f97316;
      }

      .mini-progress-fill.over-budget {
        background: #ef4444;
      }

      .budget-alert {
        margin-top: 1rem;
        padding: 0.75rem;
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

      /* Recent Transactions */
      .transaction-list {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .transaction-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem;
        background: #f9fafb;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.2s;
      }

      .transaction-item:hover {
        background: #f3f4f6;
        transform: translateX(4px);
      }

      .transaction-info {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
        flex: 1;
        min-width: 0;
      }

      .transaction-category {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .category-text {
        font-size: 0.875rem;
        font-weight: 500;
        color: #374151;
      }

      .transaction-details {
        display: flex;
        flex-direction: column;
        gap: 0.125rem;
      }

      .transaction-description {
        font-size: 0.875rem;
        color: #6b7280;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .transaction-time {
        font-size: 0.75rem;
        color: #9ca3af;
      }

      .transaction-amount {
        font-weight: 600;
        font-size: 1rem;
      }

      .transaction-amount.income {
        color: #10b981;
      }

      .transaction-amount.expense {
        color: #ef4444;
      }

      /* Goals */
      .goals-list {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .goal-item {
        padding: 1rem;
        background: #f9fafb;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.2s;
      }

      .goal-item:hover {
        background: #f3f4f6;
        transform: translateY(-2px);
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      }

      .goal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.75rem;
      }

      .goal-title {
        font-weight: 600;
        color: #111827;
      }

      .goal-target {
        font-weight: 600;
        color: #3b82f6;
      }

      .goal-progress {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        margin-bottom: 0.5rem;
      }

      .goal-info {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 0.75rem;
      }

      .goal-current {
        color: #10b981;
        font-weight: 600;
      }

      .goal-deadline {
        color: #6b7280;
      }

      /* Insights */
      .insights-list {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .insight-item {
        display: flex;
        align-items: flex-start;
        gap: 0.75rem;
        padding: 1rem;
        border-radius: 8px;
      }

      .insight-item.success {
        background: #f0fdf4;
        border: 1px solid #bbf7d0;
      }

      .insight-item.warning {
        background: #fffbeb;
        border: 1px solid #fed7aa;
      }

      .insight-item.info {
        background: #eff6ff;
        border: 1px solid #bfdbfe;
      }

      .insight-icon {
        width: 24px;
        height: 24px;
        border-radius: 6px;
        flex-shrink: 0;
      }

      .insight-icon.success {
        background: #22c55e;
      }

      .insight-icon.warning {
        background: #f59e0b;
      }

      .insight-icon.info {
        background: #3b82f6;
      }

      .insight-content {
        flex: 1;
      }

      .insight-text {
        margin: 0 0 0.5rem 0;
        font-size: 0.875rem;
        color: #374151;
        line-height: 1.5;
      }

      .insight-action {
        background: none;
        border: none;
        color: #3b82f6;
        font-size: 0.75rem;
        font-weight: 500;
        cursor: pointer;
        text-decoration: underline;
        padding: 0;
      }

      .insight-action:hover {
        color: #1d4ed8;
      }

      /* Empty States */
      .empty-state {
        text-align: center;
        padding: 2rem 1rem;
        color: #6b7280;
      }

      .empty-state p {
        margin-bottom: 1rem;
        font-size: 0.875rem;
      }

      /* Bottom Actions */
      .bottom-actions {
        position: fixed;
        bottom: 2rem;
        right: 2rem;
        z-index: 1000;
      }

      .floating-action-btn {
        background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
        color: white;
        border: none;
        border-radius: 50px;
        padding: 1rem 1.5rem;
        cursor: pointer;
        font-weight: 600;
        box-shadow: 0 8px 25px rgba(59, 130, 246, 0.3);
        transition: all 0.3s ease;
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .floating-action-btn:hover {
        transform: translateY(-4px);
        box-shadow: 0 12px 40px rgba(59, 130, 246, 0.4);
      }

      .fab-icon {
        font-size: 1.25rem;
      }

      .fab-text {
        font-size: 0.875rem;
      }

      /* Responsive Design */
      @media (max-width: 1024px) {
        .content-grid {
          grid-template-columns: 1fr;
        }

        .content-column.secondary {
          order: -1;
        }
      }

      @media (max-width: 768px) {
        .home-screen {
          padding: 0.5rem;
        }

        .dashboard-header {
          flex-direction: column;
          gap: 1rem;
          text-align: center;
        }

        .user-stats {
          justify-content: center;
        }

        .summary-cards {
          grid-template-columns: 1fr;
        }

        .action-grid {
          grid-template-columns: repeat(2, 1fr);
        }

        .category-item {
          grid-template-columns: 1fr;
          gap: 0.5rem;
        }

        .category-progress {
          width: 100%;
        }

        .transaction-item {
          flex-direction: column;
          align-items: stretch;
          gap: 0.75rem;
        }

        .bottom-actions {
          bottom: 1rem;
          right: 1rem;
        }

        .floating-action-btn {
          padding: 0.75rem 1rem;
        }

        .fab-text {
          display: none;
        }
      }

      @media (max-width: 480px) {
        .greeting-section h1 {
          font-size: 1.5rem;
        }

        .card-value {
          font-size: 1.5rem;
        }

        .action-grid {
          grid-template-columns: 1fr;
        }

        .goal-header,
        .goal-info {
          flex-direction: column;
          align-items: flex-start;
          gap: 0.25rem;
        }

        .budget-total {
          flex-direction: column;
          align-items: flex-start;
          gap: 0.25rem;
        }
      }
    `;
    
    document.head.appendChild(style);
  }
}

export default HomeScreen;