// Validation helpers for forms and user inputs
import { CONFIG } from '../constants/config.js';
import { CATEGORIES, TRANSACTION_TYPES, PAYMENT_METHODS } from '../constants/categories.js';

// Validate transaction amount
export const isValidAmount = (amount) => {
  const parsed = parseFloat(amount);
  return !isNaN(parsed) && 
         parsed >= CONFIG.MIN_TRANSACTION_AMOUNT && 
         parsed <= CONFIG.MAX_TRANSACTION_AMOUNT;
};

// Validate date
export const isValidDate = (date) => {
  if (!date) return false;
  const parsed = new Date(date);
  return parsed instanceof Date && !isNaN(parsed) && parsed <= new Date();
};

// Validate future date (for goals)
export const isValidFutureDate = (date) => {
  if (!date) return false;
  const parsed = new Date(date);
  return parsed instanceof Date && !isNaN(parsed) && parsed > new Date();
};

// Validate category
export const isValidCategory = (category) => {
  return Object.values(CATEGORIES).includes(category);
};

// Validate transaction type
export const isValidTransactionType = (type) => {
  return Object.values(TRANSACTION_TYPES).includes(type);
};

// Validate payment method
export const isValidPaymentMethod = (method) => {
  return Object.values(PAYMENT_METHODS).includes(method);
};

// Validate email format
export const isValidEmail = (email) => {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
};

// Validate phone number (Vietnamese format)
export const isValidPhoneNumber = (phone) => {
  // Vietnamese phone: 0xxxxxxxxx or +84xxxxxxxxx
  const regex = /^(\+84|0)[3|5|7|8|9][0-9]{8}$/;
  return regex.test(phone.replace(/\s/g, ''));
};

// Validate description length
export const isValidDescription = (description) => {
  return description && description.trim().length > 0 && description.length <= 500;
};

// Validate goal name
export const isValidGoalName = (name) => {
  return name && name.trim().length >= 3 && name.length <= 100;
};

// Validate budget title
export const isValidBudgetTitle = (title) => {
  return title && title.trim().length >= 3 && title.length <= 100;
};

// Validate password strength
export const isValidPassword = (password) => {
  // At least 8 characters, 1 uppercase, 1 lowercase, 1 number
  const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[A-Za-z\d@$!%*?&]{8,}$/;
  return regex.test(password);
};

// Get password strength score (0-4)
export const getPasswordStrength = (password) => {
  if (!password) return 0;
  
  let score = 0;
  
  // Length
  if (password.length >= 8) score++;
  if (password.length >= 12) score++;
  
  // Character types
  if (/[a-z]/.test(password)) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/[0-9]/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;
  
  return Math.min(score, 4);
};

// Validate file size
export const isValidFileSize = (file, maxSizeMB = 5) => {
  if (!file) return false;
  return file.size <= maxSizeMB * 1024 * 1024;
};

// Validate image file type
export const isValidImageType = (file) => {
  if (!file) return false;
  const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
  return validTypes.includes(file.type);
};

// Validate URL format
export const isValidURL = (url) => {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
};

// Validate Vietnamese ID number
export const isValidIdNumber = (id) => {
  // CMND: 9 or 12 digits, CCCD: 12 digits
  const regex = /^[0-9]{9}$|^[0-9]{12}$/;
  return regex.test(id);
};

// Generic required field validator
export const isRequired = (value) => {
  if (value === null || value === undefined) return false;
  if (typeof value === 'string') return value.trim().length > 0;
  if (Array.isArray(value)) return value.length > 0;
  return Boolean(value);
};

// Validate range
export const isInRange = (value, min, max) => {
  const num = parseFloat(value);
  return !isNaN(num) && num >= min && num <= max;
};

// Validate array length
export const isValidArrayLength = (array, minLength = 0, maxLength = Infinity) => {
  return Array.isArray(array) && array.length >= minLength && array.length <= maxLength;
};

// Form validation helper
export const validateForm = (data, rules) => {
  const errors = {};
  
  for (const [field, validators] of Object.entries(rules)) {
    const value = data[field];
    
    for (const validator of validators) {
      if (typeof validator === 'function') {
        const result = validator(value);
        if (result !== true) {
          errors[field] = result || `Invalid ${field}`;
          break;
        }
      } else if (validator.required && !isRequired(value)) {
        errors[field] = validator.message || `${field} is required`;
        break;
      } else if (validator.validator && !validator.validator(value)) {
        errors[field] = validator.message || `Invalid ${field}`;
        break;
      }
    }
  }
  
  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
};

// Validate transaction data
export const validateTransaction = (transaction) => {
  const errors = {};
  
  if (!isValidAmount(transaction.amount)) {
    errors.amount = 'Invalid amount';
  }
  
  if (!isValidTransactionType(transaction.type)) {
    errors.type = 'Invalid transaction type';
  }
  
  if (!isValidCategory(transaction.category)) {
    errors.category = 'Invalid category';
  }
  
  if (transaction.date && !isValidDate(transaction.date)) {
    errors.date = 'Invalid date';
  }
  
  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
};

// Validate budget data
export const validateBudget = (budget) => {
  const errors = {};
  
  if (!isValidBudgetTitle(budget.title)) {
    errors.title = 'Budget title must be between 3-100 characters';
  }
  
  if (!isValidAmount(budget.amount)) {
    errors.amount = 'Invalid budget amount';
  }
  
  if (!isValidCategory(budget.category)) {
    errors.category = 'Invalid category';
  }
  
  if (budget.startDate && !isValidDate(budget.startDate)) {
    errors.startDate = 'Invalid start date';
  }
  
  if (budget.endDate && !isValidFutureDate(budget.endDate)) {
    errors.endDate = 'End date must be in the future';
  }
  
  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
};