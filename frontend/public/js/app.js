// Main Application Controller - Tích hợp tất cả components
import { CONFIG } from './constants/config.js';
import { COLORS } from './constants/colors.js';

// Services
import ApiService from './services/apiService.js';
import TransactionService from './services/transactionService.js';
import StorageService from './services/storageService.js';

// Components  
import HomeScreen from './components/HomeScreen.js';
import TransactionList from './components/TransactionList.js';
import QuickAddTransaction from './components/QuickAddTransaction.js';
import BudgetOverview from './components/BudgetOverview.js';
import CategoryCard from './components/CategoryCard.js';
import ProgressBar from './components/ProgressBar.js';

// Gamification Systems
import AchievementSystem from './utils/achievementSystem.js';
import LevelSystem from './utils/levelSystem.js';
import StreakTracker from './utils/streakTracker.js';

// Utilities
import { formatCurrency } from './utils/currencyHelpers.js';
import { getRelativeTime } from './utils/dateHelpers.js';
import { validateTransaction, validateBudget } from './utils/validators.js';

class FinancialApp {
  constructor() {
    this.isInitialized = false;
    this.currentView = 'dashboard';
    this.isOnline = navigator.onLine;
    
    // Services
    this.apiService = new ApiService({
      baseURL: CONFIG.API.BASE_URL,
      timeout: CONFIG.API.TIMEOUT
    });
    
    this.transactionService = new TransactionService(this.apiService);
    this.storageService = new StorageService();
    
    // Gamification Systems
    this.achievementSystem = new AchievementSystem({
      enableNotifications: true,
      autoSave: true
    });
    
    this.levelSystem = new LevelSystem({
      enableNotifications: true,
      autoSave: true
    });
    
    this.streakTracker = new StreakTracker({
      enableNotifications: true,
      autoSave: true
    });
    
    // Components
    this.components = new Map();
    
    // App state
    this.state = {
      user: null,
      balance: 0,
      transactions: [],
      budgets: [],
      goals: [],
      categories: new Map(),
      userStats: {
        level: 1,
        xp: 0,
        streak: 0
      }
    };
    
    // Event handlers
    this.eventHandlers = new Map();
    
    this.init();
  }

  async init() {
    try {
      console.log('🚀 Initializing Financial Management App...');
      
      // Load saved data
      await this.loadData();
      
      // Initialize UI
      this.initializeUI();
      
      // Setup event listeners
      this.setupEventListeners();
      
      // Setup gamification callbacks
      this.setupGamificationCallbacks();
      
      // Setup offline/online handlers
      this.setupNetworkHandlers();
      
      // Start periodic sync
      this.startPeriodicSync();
      
      // Initialize components
      await this.initializeComponents();
      
      // Load initial view
      this.navigateToView('dashboard');
      
      this.isInitialized = true;
      console.log('✅ App initialized successfully');
      
      // Show welcome notification for new users
      this.checkWelcomeFlow();
      
    } catch (error) {
      console.error('❌ Failed to initialize app:', error);
      this.handleInitializationError(error);
    }
  }

  async loadData() {
    try {
      // Load from local storage first (offline-first approach)
      const localData = this.storageService.getAppData();
      if (localData) {
        this.state = { ...this.state, ...localData };
      }
      
      // Try to sync with server if online
      if (this.isOnline) {
        await this.syncWithServer();
      }
      
      // Update gamification stats
      this.updateGamificationStats();
      
    } catch (error) {
      console.error('Failed to load data:', error);
      // Continue with local data
    }
  }

  async syncWithServer() {
    try {
      console.log('🔄 Syncing with server...');
      
      // Sync transactions
      const serverTransactions = await this.transactionService.getTransactions();
      if (serverTransactions.length > 0) {
        this.state.transactions = serverTransactions;
      }
      
      // Sync other data as needed
      // TODO: Implement budget, goals sync
      
      // Save synced data locally
      this.storageService.saveAppData(this.state);
      
      console.log('✅ Sync completed');
      
    } catch (error) {
      console.error('Sync failed:', error);
      // Continue with local data
    }
  }

  initializeUI() {
    // Create main app structure
    document.body.innerHTML = `
      <div class="app-container">
        <nav class="app-navigation">
          <div class="nav-brand">
            <h1>Quản Lý Tài Chính</h1>
          </div>
          
          <div class="nav-menu">
            <button class="nav-item active" data-view="dashboard">
              <span class="nav-text">Trang chủ</span>
            </button>
            <button class="nav-item" data-view="transactions">
              <span class="nav-text">Giao dịch</span>
            </button>
            <button class="nav-item" data-view="budgets">
              <span class="nav-text">Ngân sách</span>
            </button>
            <button class="nav-item" data-view="goals">
              <span class="nav-text">Mục tiêu</span>
            </button>
            <button class="nav-item" data-view="achievements">
              <span class="nav-text">Thành tích</span>
            </button>
          </div>
          
          <div class="nav-user">
            <div class="user-stats">
              <div class="user-level">
                <span class="level-text">Lv ${this.state.userStats.level}</span>
              </div>
              <div class="user-streak">
                <span class="streak-icon">🔥</span>
                <span class="streak-text">${this.state.userStats.streak}</span>
              </div>
            </div>
          </div>
        </nav>

        <main class="app-main">
          <div class="view-container" id="view-container">
            <!-- Views will be loaded here -->
          </div>
        </main>

        <div class="app-overlay" id="app-overlay" style="display: none;"></div>
      </div>
    `;
    
    // Add app styles
    this.addAppStyles();
  }

  async initializeComponents() {
    const container = document.getElementById('view-container');
    
    // Initialize dashboard
    this.components.set('dashboard', new HomeScreen(container, {
      showQuickActions: true,
      showRecentTransactions: true,
      showBudgetOverview: true,
      showGoals: true,
      showInsights: true
    }));
    
    // Setup dashboard callbacks
    const dashboard = this.components.get('dashboard');
    dashboard.onAddTransaction = (type) => this.showQuickAddTransaction(type);
    dashboard.onTransfer = () => this.showTransferDialog();
    dashboard.onSetBudget = () => this.navigateToView('budgets');
    dashboard.onNavigate = (view) => this.navigateToView(view);
    dashboard.onCreateBudget = () => this.showBudgetDialog();
    dashboard.onCreateGoal = () => this.showGoalDialog();
    dashboard.onQuickAdd = () => this.showQuickAddTransaction();
    dashboard.onRefreshData = () => this.refreshData();
    dashboard.onCategoryClick = (category) => this.showCategoryDetails(category);
    dashboard.onGoalClick = (goalId) => this.showGoalDetails(goalId);
    dashboard.onTransactionClick = (transactionId) => this.showTransactionDetails(transactionId);
    dashboard.onInsightAction = (action) => this.handleInsightAction(action);
    
    // Set initial data
    dashboard.setData(this.state);
  }

  setupEventListeners() {
    // Navigation
    document.addEventListener('click', (e) => {
      const navItem = e.target.closest('.nav-item');
      if (navItem) {
        const view = navItem.dataset.view;
        this.navigateToView(view);
      }
    });
    
    // Keyboard shortcuts
    document.addEventListener('keydown', (e) => {
      if (e.ctrlKey || e.metaKey) {
        switch (e.key) {
          case 'n':
            e.preventDefault();
            this.showQuickAddTransaction();
            break;
          case 'b':
            e.preventDefault();
            this.navigateToView('budgets');
            break;
          case 'g':
            e.preventDefault();
            this.navigateToView('goals');
            break;
          case 'h':
            e.preventDefault();
            this.navigateToView('dashboard');
            break;
        }
      }
      
      if (e.key === 'Escape') {
        this.closeOverlays();
      }
    });
  }

  setupGamificationCallbacks() {
    // Achievement system callbacks
    this.achievementSystem.onAchievementUnlocked = (achievement) => {
      this.levelSystem.addXP('unlockAchievement', achievement.xpReward);
      this.updateUserStats();
    };
    
    // Level system callbacks
    this.levelSystem.onLevelUpCallback = (oldLevel, newLevel, levelData) => {
      this.state.userStats.level = newLevel;
      this.updateUserStats();
      this.updateUI();
    };
    
    this.levelSystem.onXPGainedCallback = (xpGained, source, context) => {
      this.state.userStats.xp = this.levelSystem.getTotalXP();
      this.updateUserStats();
    };
  }

  setupNetworkHandlers() {
    window.addEventListener('online', () => {
      this.isOnline = true;
      console.log('📶 Back online - starting sync...');
      this.syncWithServer();
    });
    
    window.addEventListener('offline', () => {
      this.isOnline = false;
      console.log('📱 Offline mode - using local data');
    });
  }

  startPeriodicSync() {
    setInterval(() => {
      if (this.isOnline) {
        this.syncWithServer();
      }
    }, CONFIG.SYNC_INTERVAL || 300000); // 5 minutes
  }

  updateGamificationStats() {
    // Update user stats from gamification systems
    this.state.userStats = {
      level: this.levelSystem.getCurrentLevel(),
      xp: this.levelSystem.getTotalXP(),
      streak: this.streakTracker.getCurrentStreak()
    };
    
    // Update achievement system with current stats
    this.achievementSystem.updateSavings(this.calculateTotalSavings());
    this.achievementSystem.updateStreak(this.state.userStats.streak);
  }

  // Transaction Management
  async addTransaction(transactionData) {
    try {
      const validation = validateTransaction(transactionData);
      if (!validation.isValid) {
        throw new Error(validation.errors.join(', '));
      }
      
      // Add transaction
      const transaction = await this.transactionService.addTransaction(transactionData);
      this.state.transactions.unshift(transaction);
      
      // Update balance
      this.updateBalance();
      
      // Record gamification activities
      this.recordTransactionActivity(transaction);
      
      // Save data
      this.saveData();
      
      // Update UI
      this.updateComponents();
      
      console.log('✅ Transaction added successfully');
      return transaction;
      
    } catch (error) {
      console.error('Failed to add transaction:', error);
      throw error;
    }
  }

  recordTransactionActivity(transaction) {
    // Record streak activity
    this.streakTracker.recordActivity('transaction');
    
    // Record achievement progress
    this.achievementSystem.recordTransaction(transaction);
    
    // Award XP based on transaction details
    let xpSource = 'addTransaction';
    if (transaction.description && transaction.description.length > 5) {
      xpSource = 'addDetailedTransaction';
    }
    
    const context = {
      streakDays: this.streakTracker.getCurrentStreak(),
      isPerfectDay: this.checkPerfectDay(),
      isUnderBudget: this.checkUnderBudget(transaction.category),
      isQuickEntry: transaction.entryTime < 10000 // Under 10 seconds
    };
    
    this.levelSystem.addXP(xpSource, null, context);
    
    // Check for first transaction of day
    const today = new Date().toDateString();
    const todayTransactions = this.state.transactions.filter(t => 
      new Date(t.date).toDateString() === today
    );
    
    if (todayTransactions.length === 1) {
      this.levelSystem.addXP('firstTransactionOfDay', null, context);
    }
    
    this.updateUserStats();
  }

  // UI Management
  navigateToView(viewName) {
    if (!this.isInitialized) return;
    
    console.log(`📄 Navigating to ${viewName}`);
    
    // Update navigation
    document.querySelectorAll('.nav-item').forEach(item => {
      item.classList.remove('active');
      if (item.dataset.view === viewName) {
        item.classList.add('active');
      }
    });
    
    this.currentView = viewName;
    
    // Load view content
    this.loadView(viewName);
  }

  async loadView(viewName) {
    const container = document.getElementById('view-container');
    
    switch (viewName) {
      case 'dashboard':
        await this.loadDashboardView(container);
        break;
      case 'transactions':
        await this.loadTransactionsView(container);
        break;
      case 'budgets':
        await this.loadBudgetsView(container);
        break;
      case 'goals':
        await this.loadGoalsView(container);
        break;
      case 'achievements':
        await this.loadAchievementsView(container);
        break;
      default:
        console.warn(`Unknown view: ${viewName}`);
    }
  }

  async loadDashboardView(container) {
    if (!this.components.has('dashboard')) {
      await this.initializeComponents();
    }
    
    const dashboard = this.components.get('dashboard');
    dashboard.setData(this.state);
    dashboard.refresh();
  }

  async loadTransactionsView(container) {
    container.innerHTML = `
      <div class="transactions-view">
        <div class="view-header">
          <h2>Giao dịch</h2>
          <button class="btn-primary" id="btn-add-transaction">+ Thêm giao dịch</button>
        </div>
        <div id="transactions-container"></div>
      </div>
    `;
    
    // Initialize transaction list component
    const transactionContainer = document.getElementById('transactions-container');
    if (!this.components.has('transactionList')) {
      this.components.set('transactionList', new TransactionList(transactionContainer, {
        showFilters: true,
        showPagination: true,
        showGrouping: true,
        itemsPerPage: 20
      }));
    }
    
    const transactionList = this.components.get('transactionList');
    transactionList.setTransactions(this.state.transactions);
    
    // Setup event listeners
    document.getElementById('btn-add-transaction').addEventListener('click', () => {
      this.showQuickAddTransaction();
    });
  }

  async loadBudgetsView(container) {
    container.innerHTML = `
      <div class="budgets-view">
        <div class="view-header">
          <h2>Ngân sách</h2>
          <button class="btn-primary" id="btn-add-budget">+ Thêm ngân sách</button>
        </div>
        <div id="budgets-container"></div>
      </div>
    `;
    
    const budgetContainer = document.getElementById('budgets-container');
    if (!this.components.has('budgetOverview')) {
      this.components.set('budgetOverview', new BudgetOverview(budgetContainer, {
        showProgress: true,
        showCategories: true,
        allowEdit: true,
        period: 'monthly'
      }));
    }
    
    const budgetOverview = this.components.get('budgetOverview');
    budgetOverview.setBudgets(this.state.budgets);
    
    document.getElementById('btn-add-budget').addEventListener('click', () => {
      this.showBudgetDialog();
    });
  }

  async loadGoalsView(container) {
    container.innerHTML = `
      <div class="goals-view">
        <div class="view-header">
          <h2>Mục tiêu</h2>
          <button class="btn-primary" id="btn-add-goal">+ Thêm mục tiêu</button>
        </div>
        <div id="goals-container">
          <p>Tính năng mục tiêu đang được phát triển...</p>
        </div>
      </div>
    `;
    
    document.getElementById('btn-add-goal').addEventListener('click', () => {
      this.showGoalDialog();
    });
  }

  async loadAchievementsView(container) {
    const achievements = this.achievementSystem.getProgressAchievements();
    const stats = this.achievementSystem.getTierStats();
    const levelData = this.levelSystem.getLevelData();
    
    container.innerHTML = `
      <div class="achievements-view">
        <div class="view-header">
          <h2>Thành tích</h2>
        </div>
        
        <div class="achievement-overview">
          <div class="level-card">
            <h3>Cấp độ hiện tại</h3>
            <div class="level-info">
              <div class="level-number">${levelData.level}</div>
              <div class="level-details">
                <p class="level-title">${levelData.title}</p>
                <div class="xp-progress">
                  <div class="xp-bar">
                    <div class="xp-fill" style="width: ${this.levelSystem.getXPProgress()}%"></div>
                  </div>
                  <p class="xp-text">${this.levelSystem.getTotalXP()} XP</p>
                </div>
              </div>
            </div>
          </div>
          
          <div class="stats-grid">
            <div class="stat-card">
              <h4>Chuỗi ngày</h4>
              <p class="stat-value">${this.streakTracker.getCurrentStreak()}</p>
            </div>
            <div class="stat-card">
              <h4>Thành tích đạt được</h4>
              <p class="stat-value">${this.achievementSystem.getUnlockedAchievements().length}</p>
            </div>
            <div class="stat-card">
              <h4>Tiến độ hoàn thành</h4>
              <p class="stat-value">${this.achievementSystem.getCompletionPercentage().toFixed(0)}%</p>
            </div>
          </div>
        </div>
        
        <div class="achievements-list">
          <h3>Danh sách thành tích</h3>
          ${achievements.map(achievement => this.renderAchievementCard(achievement)).join('')}
        </div>
      </div>
    `;
  }

  renderAchievementCard(achievement) {
    return `
      <div class="achievement-card ${achievement.isUnlocked ? 'unlocked' : 'locked'}">
        <div class="achievement-icon ${achievement.tier}">
          ${achievement.icon}
        </div>
        <div class="achievement-info">
          <h4 class="achievement-title">${achievement.title}</h4>
          <p class="achievement-description">${achievement.description}</p>
          <div class="achievement-progress">
            <div class="progress-bar">
              <div class="progress-fill" style="width: ${achievement.progress}%"></div>
            </div>
            <span class="progress-text">${achievement.progress.toFixed(0)}%</span>
          </div>
          <p class="achievement-reward">+${achievement.xpReward} XP</p>
        </div>
      </div>
    `;
  }

  // Dialog Management
  showQuickAddTransaction(type = 'expense') {
    const overlay = document.getElementById('app-overlay');
    overlay.style.display = 'flex';
    
    const container = document.createElement('div');
    container.className = 'modal-container';
    overlay.appendChild(container);
    
    const quickAdd = new QuickAddTransaction(container, {
      defaultType: type,
      quickMode: true,
      showCategories: true
    });
    
    quickAdd.onSubmit = async (transactionData) => {
      try {
        await this.addTransaction(transactionData);
        this.closeOverlays();
      } catch (error) {
        console.error('Failed to add transaction:', error);
        // Show error message
      }
    };
    
    quickAdd.onCancel = () => {
      this.closeOverlays();
    };
  }

  closeOverlays() {
    const overlay = document.getElementById('app-overlay');
    overlay.style.display = 'none';
    overlay.innerHTML = '';
  }

  // Utility methods
  updateBalance() {
    const income = this.state.transactions
      .filter(t => t.type === 'income')
      .reduce((sum, t) => sum + t.amount, 0);
      
    const expenses = this.state.transactions
      .filter(t => t.type === 'expense')
      .reduce((sum, t) => sum + t.amount, 0);
      
    this.state.balance = income - expenses;
  }

  calculateTotalSavings() {
    // Calculate savings based on income vs expenses
    const currentMonth = new Date().getMonth();
    const currentYear = new Date().getFullYear();
    
    const monthlyIncome = this.state.transactions
      .filter(t => {
        const date = new Date(t.date);
        return t.type === 'income' && 
               date.getMonth() === currentMonth && 
               date.getFullYear() === currentYear;
      })
      .reduce((sum, t) => sum + t.amount, 0);
      
    const monthlyExpenses = this.state.transactions
      .filter(t => {
        const date = new Date(t.date);
        return t.type === 'expense' && 
               date.getMonth() === currentMonth && 
               date.getFullYear() === currentYear;
      })
      .reduce((sum, t) => sum + t.amount, 0);
      
    return Math.max(0, monthlyIncome - monthlyExpenses);
  }

  checkPerfectDay() {
    const today = new Date().toDateString();
    const todayTransactions = this.state.transactions.filter(t => 
      new Date(t.date).toDateString() === today
    );
    
    // Perfect day = at least 1 transaction, all within budget
    return todayTransactions.length > 0 && 
           todayTransactions.every(t => this.checkUnderBudget(t.category));
  }

  checkUnderBudget(category) {
    const budget = this.state.budgets.find(b => b.category === category);
    if (!budget) return true;
    
    const spent = this.state.transactions
      .filter(t => t.category === category && t.type === 'expense')
      .reduce((sum, t) => sum + t.amount, 0);
      
    return spent <= budget.amount;
  }

  updateUserStats() {
    this.state.userStats = {
      level: this.levelSystem.getCurrentLevel(),
      xp: this.levelSystem.getTotalXP(),
      streak: this.streakTracker.getCurrentStreak()
    };
    
    this.updateUI();
  }

  updateUI() {
    // Update user stats in navigation
    const levelText = document.querySelector('.level-text');
    const streakText = document.querySelector('.streak-text');
    
    if (levelText) {
      levelText.textContent = `Lv ${this.state.userStats.level}`;
    }
    
    if (streakText) {
      streakText.textContent = this.state.userStats.streak;
    }
  }

  updateComponents() {
    // Update dashboard if active
    if (this.currentView === 'dashboard' && this.components.has('dashboard')) {
      this.components.get('dashboard').setData(this.state);
    }
    
    // Update transaction list if active
    if (this.components.has('transactionList')) {
      this.components.get('transactionList').setTransactions(this.state.transactions);
    }
    
    // Update budget overview if active
    if (this.components.has('budgetOverview')) {
      this.components.get('budgetOverview').setBudgets(this.state.budgets);
    }
  }

  async refreshData() {
    try {
      await this.loadData();
      this.updateComponents();
      console.log('✅ Data refreshed');
    } catch (error) {
      console.error('Failed to refresh data:', error);
    }
  }

  saveData() {
    this.storageService.saveAppData(this.state);
  }

  checkWelcomeFlow() {
    const isFirstTime = !localStorage.getItem('app_initialized');
    if (isFirstTime) {
      localStorage.setItem('app_initialized', 'true');
      // Show welcome message or tutorial
      console.log('👋 Welcome to Financial Management App!');
    }
  }

  handleInitializationError(error) {
    document.body.innerHTML = `
      <div class="error-container">
        <h1>Lỗi khởi tạo ứng dụng</h1>
        <p>Có lỗi xảy ra khi khởi tạo ứng dụng. Vui lòng thử lại.</p>
        <button onclick="location.reload()" class="btn-primary">Tải lại</button>
      </div>
    `;
  }

  addAppStyles() {
    const existingStyle = document.getElementById('app-styles');
    if (existingStyle) return;

    const style = document.createElement('style');
    style.id = 'app-styles';
    style.textContent = `
      * {
        margin: 0;
        padding: 0;
        box-sizing: border-box;
      }

      body {
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        background: #f8fafc;
        color: #1a202c;
      }

      .app-container {
        height: 100vh;
        display: flex;
        flex-direction: column;
      }

      .app-navigation {
        background: white;
        border-bottom: 1px solid #e2e8f0;
        padding: 1rem 2rem;
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .nav-brand h1 {
        font-size: 1.25rem;
        font-weight: 700;
        color: #2d3748;
      }

      .nav-menu {
        display: flex;
        gap: 0.5rem;
      }

      .nav-item {
        background: none;
        border: none;
        padding: 0.75rem 1rem;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 500;
        color: #4a5568;
        transition: all 0.2s;
      }

      .nav-item:hover {
        background: #f7fafc;
        color: #2d3748;
      }

      .nav-item.active {
        background: #667eea;
        color: white;
      }

      .nav-user {
        display: flex;
        align-items: center;
      }

      .user-stats {
        display: flex;
        align-items: center;
        gap: 1rem;
      }

      .user-level {
        background: #667eea;
        color: white;
        padding: 0.25rem 0.75rem;
        border-radius: 12px;
        font-size: 0.875rem;
        font-weight: 600;
      }

      .user-streak {
        display: flex;
        align-items: center;
        gap: 0.25rem;
        font-weight: 600;
        color: #f56500;
      }

      .app-main {
        flex: 1;
        overflow-y: auto;
        padding: 2rem;
      }

      .view-container {
        max-width: 1200px;
        margin: 0 auto;
      }

      .app-overlay {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        z-index: 9999;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .modal-container {
        background: white;
        border-radius: 12px;
        padding: 2rem;
        max-width: 500px;
        width: 90%;
        max-height: 90%;
        overflow-y: auto;
      }

      .view-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 2rem;
      }

      .view-header h2 {
        font-size: 1.75rem;
        font-weight: 700;
        color: #2d3748;
      }

      .btn-primary {
        background: #667eea;
        color: white;
        border: none;
        padding: 0.75rem 1.5rem;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 600;
        transition: all 0.2s;
      }

      .btn-primary:hover {
        background: #5a67d8;
        transform: translateY(-1px);
      }

      .achievement-overview {
        margin-bottom: 2rem;
      }

      .level-card {
        background: white;
        padding: 1.5rem;
        border-radius: 12px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        margin-bottom: 1rem;
      }

      .level-info {
        display: flex;
        align-items: center;
        gap: 1rem;
      }

      .level-number {
        font-size: 3rem;
        font-weight: 700;
        color: #667eea;
      }

      .level-title {
        font-size: 1.25rem;
        font-weight: 600;
        color: #2d3748;
        margin-bottom: 0.5rem;
      }

      .xp-progress {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .xp-bar {
        flex: 1;
        height: 8px;
        background: #e2e8f0;
        border-radius: 4px;
        overflow: hidden;
      }

      .xp-fill {
        height: 100%;
        background: linear-gradient(90deg, #667eea, #764ba2);
        transition: width 0.3s ease;
      }

      .stats-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 1rem;
      }

      .stat-card {
        background: white;
        padding: 1.5rem;
        border-radius: 12px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        text-align: center;
      }

      .stat-card h4 {
        font-size: 0.875rem;
        color: #718096;
        margin-bottom: 0.5rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .stat-value {
        font-size: 2rem;
        font-weight: 700;
        color: #2d3748;
      }

      .achievements-list {
        display: grid;
        gap: 1rem;
      }

      .achievement-card {
        background: white;
        padding: 1.5rem;
        border-radius: 12px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        display: flex;
        align-items: center;
        gap: 1rem;
        transition: all 0.2s;
      }

      .achievement-card.unlocked {
        border-left: 4px solid #48bb78;
      }

      .achievement-card.locked {
        opacity: 0.6;
      }

      .achievement-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      }

      .achievement-card .achievement-icon {
        width: 60px;
        height: 60px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 1.5rem;
      }

      .achievement-info {
        flex: 1;
      }

      .achievement-title {
        font-size: 1.125rem;
        font-weight: 600;
        color: #2d3748;
        margin-bottom: 0.25rem;
      }

      .achievement-description {
        color: #718096;
        margin-bottom: 0.75rem;
      }

      .achievement-progress {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 0.5rem;
      }

      .achievement-reward {
        font-size: 0.875rem;
        color: #48bb78;
        font-weight: 600;
      }

      .error-container {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100vh;
        text-align: center;
        padding: 2rem;
      }

      @media (max-width: 768px) {
        .app-navigation {
          padding: 1rem;
          flex-direction: column;
          gap: 1rem;
        }

        .nav-menu {
          flex-wrap: wrap;
          justify-content: center;
        }

        .app-main {
          padding: 1rem;
        }

        .level-info {
          flex-direction: column;
          text-align: center;
        }

        .stats-grid {
          grid-template-columns: 1fr;
        }

        .achievement-card {
          flex-direction: column;
          text-align: center;
        }
      }
    `;
    
    document.head.appendChild(style);
  }
}

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  window.financialApp = new FinancialApp();
});

export default FinancialApp;