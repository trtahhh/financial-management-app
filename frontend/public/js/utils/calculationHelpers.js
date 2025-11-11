// Calculation helpers for financial data
import { CATEGORIES } from '../constants/categories.js';

// Calculate total for transactions
export const calculateTotal = (transactions, type = null) => {
  if (!Array.isArray(transactions)) return 0;
  
  return transactions
    .filter(t => !type || t.type === type)
    .reduce((sum, t) => sum + (parseFloat(t.amount) || 0), 0);
};

// Calculate totals by category
export const calculateCategoryTotals = (transactions) => {
  if (!Array.isArray(transactions)) return {};
  
  return transactions.reduce((acc, t) => {
    const category = t.category || 'other';
    acc[category] = (acc[category] || 0) + (parseFloat(t.amount) || 0);
    return acc;
  }, {});
};

// Calculate budget progress
export const calculateBudgetProgress = (spent, limit) => {
  const spentAmount = parseFloat(spent) || 0;
  const limitAmount = parseFloat(limit) || 1;
  
  const percentage = (spentAmount / limitAmount) * 100;
  const remaining = limitAmount - spentAmount;
  
  let status = 'on-track';
  if (percentage >= 100) status = 'over-budget';
  else if (percentage >= 90) status = 'near-limit';
  else if (percentage >= 80) status = 'warning';
  
  return {
    percentage: Math.min(percentage, 100),
    remaining: Math.max(remaining, 0),
    status,
    isOverBudget: percentage >= 100
  };
};

// Calculate goal progress
export const calculateGoalProgress = (goal) => {
  const current = parseFloat(goal.currentAmount) || 0;
  const target = parseFloat(goal.targetAmount) || 1;
  
  const percentage = (current / target) * 100;
  const remaining = target - current;
  
  // Calculate time progress if deadline exists
  let timeProgress = null;
  let monthsRemaining = null;
  let requiredMonthly = null;
  
  if (goal.deadline) {
    const now = new Date();
    const deadline = new Date(goal.deadline);
    const createdAt = new Date(goal.createdAt || now);
    
    const totalDays = Math.max(1, Math.ceil((deadline - createdAt) / (1000 * 60 * 60 * 24)));
    const elapsedDays = Math.max(0, Math.ceil((now - createdAt) / (1000 * 60 * 60 * 24)));
    
    timeProgress = (elapsedDays / totalDays) * 100;
    monthsRemaining = Math.max(0, Math.ceil((deadline - now) / (1000 * 60 * 60 * 24 * 30)));
    requiredMonthly = monthsRemaining > 0 ? remaining / monthsRemaining : remaining;
  }
  
  // Determine status
  let status = 'on-track';
  if (percentage >= 100) status = 'completed';
  else if (timeProgress && percentage < timeProgress - 10) status = 'behind';
  else if (timeProgress && percentage < timeProgress) status = 'slightly-behind';
  
  return {
    percentage: Math.min(percentage, 100),
    remaining: Math.max(remaining, 0),
    status,
    timeProgress,
    monthsRemaining,
    requiredMonthly: requiredMonthly || 0,
    isCompleted: percentage >= 100,
    isOnTrack: !timeProgress || percentage >= timeProgress
  };
};

// Calculate monthly spending trend
export const calculateMonthlyTrend = (transactions, months = 6) => {
  if (!Array.isArray(transactions)) return [];
  
  const monthlyData = {};
  const now = new Date();
  
  // Initialize last N months
  for (let i = months - 1; i >= 0; i--) {
    const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    monthlyData[key] = {
      month: key,
      date: date,
      income: 0,
      expense: 0,
      transactions: []
    };
  }
  
  // Aggregate transactions by month
  transactions.forEach(t => {
    const date = new Date(t.date);
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    
    if (monthlyData[key]) {
      const amount = parseFloat(t.amount) || 0;
      if (t.type === 'income') {
        monthlyData[key].income += amount;
      } else {
        monthlyData[key].expense += amount;
      }
      monthlyData[key].transactions.push(t);
    }
  });
  
  return Object.values(monthlyData).map(month => ({
    ...month,
    net: month.income - month.expense,
    total: month.income + month.expense
  }));
};

// Calculate weekly spending pattern
export const calculateWeeklyPattern = (transactions) => {
  if (!Array.isArray(transactions)) return {};
  
  const weekdays = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  const pattern = {};
  
  weekdays.forEach(day => {
    pattern[day] = { income: 0, expense: 0, count: 0 };
  });
  
  transactions.forEach(t => {
    const date = new Date(t.date);
    const dayName = weekdays[date.getDay()];
    const amount = parseFloat(t.amount) || 0;
    
    if (t.type === 'income') {
      pattern[dayName].income += amount;
    } else {
      pattern[dayName].expense += amount;
    }
    pattern[dayName].count++;
  });
  
  return pattern;
};

// Calculate average transaction amount
export const calculateAverageAmount = (transactions, type = null) => {
  const filtered = transactions.filter(t => !type || t.type === type);
  if (filtered.length === 0) return 0;
  
  const total = calculateTotal(filtered);
  return total / filtered.length;
};

// Calculate largest transactions
export const findLargestTransactions = (transactions, limit = 5, type = null) => {
  return transactions
    .filter(t => !type || t.type === type)
    .sort((a, b) => parseFloat(b.amount) - parseFloat(a.amount))
    .slice(0, limit);
};

// Calculate spending velocity (amount per day)
export const calculateSpendingVelocity = (transactions, days = 30) => {
  const now = new Date();
  const startDate = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);
  
  const recentTransactions = transactions.filter(t => {
    const date = new Date(t.date);
    return date >= startDate && t.type === 'expense';
  });
  
  const totalSpent = calculateTotal(recentTransactions);
  return totalSpent / days;
};

// Calculate category distribution percentages
export const calculateCategoryDistribution = (transactions) => {
  const totals = calculateCategoryTotals(transactions);
  const grandTotal = Object.values(totals).reduce((sum, amount) => sum + amount, 0);
  
  if (grandTotal === 0) return {};
  
  const distribution = {};
  Object.entries(totals).forEach(([category, amount]) => {
    distribution[category] = {
      amount,
      percentage: (amount / grandTotal) * 100
    };
  });
  
  return distribution;
};

// Calculate balance over time
export const calculateBalanceHistory = (transactions, startBalance = 0) => {
  if (!Array.isArray(transactions)) return [];
  
  const sorted = [...transactions].sort((a, b) => new Date(a.date) - new Date(b.date));
  const history = [];
  let balance = startBalance;
  
  sorted.forEach(t => {
    const amount = parseFloat(t.amount) || 0;
    if (t.type === 'income') {
      balance += amount;
    } else {
      balance -= amount;
    }
    
    history.push({
      date: t.date,
      transaction: t,
      balance,
      change: t.type === 'income' ? amount : -amount
    });
  });
  
  return history;
};

// Calculate savings goal metrics
export const calculateSavingsGoal = (currentAmount, targetAmount, monthsRemaining) => {
  const current = parseFloat(currentAmount) || 0;
  const target = parseFloat(targetAmount) || 1;
  const months = parseInt(monthsRemaining) || 1;
  
  const remaining = Math.max(0, target - current);
  const monthlyRequired = remaining / months;
  const percentage = Math.min((current / target) * 100, 100);
  
  let status = 'on-track';
  if (percentage >= 100) status = 'completed';
  else if (percentage < 50 && months <= 3) status = 'behind';
  else if (percentage < 75 && months <= 1) status = 'critical';
  
  return {
    remaining,
    monthlyRequired,
    percentage,
    status,
    isCompleted: percentage >= 100
  };
};