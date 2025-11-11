// Configuration constants
export const CONFIG = {
  // Transaction limits
  MAX_TRANSACTION_AMOUNT: 1000000000, // 1 billion VND
  MIN_TRANSACTION_AMOUNT: 1000,       // 1K VND
  
  // Currency and formatting
  DEFAULT_CURRENCY: 'VND',
  DATE_FORMAT: 'DD/MM/YYYY',
  TIME_FORMAT: 'HH:mm',
  
  // UI settings
  ITEMS_PER_PAGE: 20,
  MAX_RECEIPT_SIZE: 5 * 1024 * 1024, // 5MB
  
  // API endpoints
  API_BASE_URL: '/api',
  
  // Local storage keys
  STORAGE_KEYS: {
    USER_PREFERENCES: 'user_preferences',
    CACHED_TRANSACTIONS: 'cached_transactions',
    CACHED_BUDGETS: 'cached_budgets',
    CACHED_GOALS: 'cached_goals'
  },
  
  // Notification settings  
  MAX_NOTIFICATIONS_PER_DAY: 2,
  NOTIFICATION_TIME: '20:00', // 8 PM
  
  // Gamification
  XP_ACTIONS: {
    ADD_TRANSACTION: 5,
    ADD_WITH_RECEIPT: 7,
    CATEGORIZE_TRANSACTION: 2,
    SET_BUDGET: 15,
    COMPLETE_GOAL: 50,
    WEEKLY_BUDGET_SUCCESS: 20,
    MONTHLY_BUDGET_SUCCESS: 100,
    SHARE_ACHIEVEMENT: 10,
    INVITE_FRIEND: 25
  },
  
  // Chart colors (no icons as per instruction)
  CHART_COLORS: [
    '#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A',
    '#98D8C8', '#F7DC6F', '#BB8FCE', '#95A5A6'
  ]
};