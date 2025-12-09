// Constants cho categories và configuration
export const CATEGORIES = {
  FOOD_DRINK: 'food_drink',
  TRANSPORT: 'transport', 
  SHOPPING: 'shopping',
  ENTERTAINMENT: 'entertainment',
  EDUCATION: 'education',
  HEALTH: 'health',
  UTILITIES: 'utilities',
  OTHER: 'other'
};

export const CATEGORY_NAMES = {
  food_drink: 'Ăn uống',
  transport: 'Di chuyển',
  shopping: 'Mua sắm', 
  entertainment: 'Giải trí',
  education: 'Học tập',
  health: 'Sức khỏe',
  utilities: 'Hóa đơn',
  other: 'Khác'
};

// Theo git instruction: Bỏ hết các icon
export const CATEGORY_COLORS = {
  food_drink: '#FF6B6B',
  transport: '#4ECDC4', 
  shopping: '#45B7D1',
  entertainment: '#FFA07A',
  education: '#98D8C8',
  health: '#F7DC6F',
  utilities: '#BB8FCE',
  other: '#95A5A6'
};

export const TRANSACTION_TYPES = {
  INCOME: 'income',
  EXPENSE: 'expense'
};

export const PAYMENT_METHODS = {
  CASH: 'cash',
  CARD: 'card', 
  E_WALLET: 'e-wallet'
};

export const GOAL_STATUSES = {
  ACTIVE: 'active',
  COMPLETED: 'completed',
  PAUSED: 'paused',
  CANCELLED: 'cancelled'
};