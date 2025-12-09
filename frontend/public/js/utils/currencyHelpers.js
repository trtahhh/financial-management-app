// Currency formatting helpers
export const formatCurrency = (amount, showSymbol = true) => {
  if (amount === null || amount === undefined) return '0';
  
  const numericAmount = parseFloat(amount);
  if (isNaN(numericAmount)) return '0';
  
  // Format with Vietnamese locale
  const formatted = new Intl.NumberFormat('vi-VN', {
    style: showSymbol ? 'currency' : 'decimal',
    currency: 'VND',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(numericAmount);
  
  return formatted;
};

// Short format for large numbers (1.5M, 150K)
export const formatCurrencyShort = (amount) => {
  const numericAmount = parseFloat(amount);
  if (isNaN(numericAmount)) return '0';
  
  if (numericAmount >= 1000000000) {
    return `${(numericAmount / 1000000000).toFixed(1)}B`;
  }
  if (numericAmount >= 1000000) {
    return `${(numericAmount / 1000000).toFixed(1)}M`;
  }
  if (numericAmount >= 1000) {
    return `${(numericAmount / 1000).toFixed(1)}K`;
  }
  return numericAmount.toLocaleString('vi-VN');
};

// Parse currency string to number
export const parseCurrency = (currencyString) => {
  if (!currencyString) return 0;
  
  // Remove all non-digit characters except dots and commas
  const cleaned = currencyString.toString().replace(/[^\d.,]/g, '');
  
  // Handle Vietnamese format: 1.500.000 or 1,500,000
  let normalized = cleaned;
  
  // If has both comma and dot, assume dot is thousands separator
  if (normalized.includes(',') && normalized.includes('.')) {
    // 1.500,50 -> 1500.50
    normalized = normalized.replace(/\./g, '').replace(',', '.');
  } else if (normalized.includes('.')) {
    // Check if it's thousands separator (more than 2 digits after)
    const parts = normalized.split('.');
    if (parts[parts.length - 1].length > 2) {
      // It's thousands separator: 1.500.000
      normalized = normalized.replace(/\./g, '');
    }
  } else if (normalized.includes(',')) {
    // Replace comma with dot for decimal
    normalized = normalized.replace(',', '.');
  }
  
  return parseFloat(normalized) || 0;
};

// Validate amount
export const isValidAmount = (amount) => {
  const parsed = parseCurrency(amount);
  return parsed > 0 && parsed <= 1000000000; // Max 1 billion VND
};

// Calculate percentage change
export const calculatePercentageChange = (current, previous) => {
  if (!previous || previous === 0) return 0;
  return ((current - previous) / previous) * 100;
};

// Format percentage
export const formatPercentage = (percentage, decimals = 1) => {
  return `${percentage.toFixed(decimals)}%`;
};