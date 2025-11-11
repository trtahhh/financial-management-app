// Color palette for the application (research-backed)
export const COLORS = {
  // Income & Positive
  income: '#10B981',      // Green - positive reinforcement
  success: '#10B981',
  
  // Expenses & Negative
  expense: '#EF4444',     // Red - but not too harsh
  warning: '#F59E0B',     // Yellow - caution
  danger: '#DC2626',      // Darker red - critical
  
  // Primary & Neutral
  primary: '#3B82F6',     // Blue - trust, stability
  secondary: '#6B7280',   // Gray - neutral info
  
  // Background colors
  background: '#FFFFFF',
  backgroundSecondary: '#F9FAFB',
  surface: '#F5F5F5',
  
  // Text colors
  text: '#111827',
  textSecondary: '#6B7280',
  textLight: '#9CA3AF',
  
  // Border colors
  border: '#E5E7EB',
  borderLight: '#F3F4F6',
  
  // Status colors
  online: '#10B981',
  offline: '#6B7280',
  pending: '#F59E0B',
  
  // Categories (distinct, accessible)
  food_drink: '#FF6B6B',
  transport: '#4ECDC4',
  shopping: '#45B7D1',
  entertainment: '#FFA07A',
  education: '#98D8C8',
  health: '#F7DC6F',
  utilities: '#BB8FCE',
  other: '#95A5A6'
};

// Gradients for modern UI
export const GRADIENTS = {
  primary: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  success: 'linear-gradient(135deg, #10B981 0%, #059669 100%)',
  warning: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)',
  danger: 'linear-gradient(135deg, #EF4444 0%, #DC2626 100%)',
  info: 'linear-gradient(135deg, #3B82F6 0%, #2563EB 100%)'
};

// Dark mode colors
export const DARK_COLORS = {
  ...COLORS,
  background: '#121212',
  backgroundSecondary: '#1E1E1E',
  surface: '#1E1E1E',
  text: '#FFFFFF',
  textSecondary: '#AAAAAA',
  textLight: '#888888',
  border: '#333333',
  borderLight: '#2A2A2A'
};