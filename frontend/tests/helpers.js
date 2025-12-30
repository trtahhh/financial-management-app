/**
 * Test Helper Functions
 * Reusable utilities for tests
 */

// Mock data generators
export const generateMockUser = (overrides = {}) => ({
  id: 1,
  email: 'test@example.com',
  fullName: 'Test User',
  ...overrides
});

export const generateMockTransaction = (overrides = {}) => ({
  id: Math.floor(Math.random() * 1000),
  description: 'Test Transaction',
  amount: 100000,
  type: 'EXPENSE',
  categoryId: 1,
  categoryName: 'Ăn uống',
  walletId: 1,
  walletName: 'Ví chính',
  transactionDate: new Date().toISOString().split('T')[0],
  ...overrides
});

export const generateMockBudget = (overrides = {}) => ({
  id: Math.floor(Math.random() * 1000),
  name: 'Test Budget',
  amount: 5000000,
  spent: 2000000,
  categoryId: 1,
  categoryName: 'Ăn uống',
  period: 'MONTHLY',
  ...overrides
});

export const generateMockGoal = (overrides = {}) => ({
  id: Math.floor(Math.random() * 1000),
  name: 'Test Goal',
  targetAmount: 10000000,
  currentAmount: 3000000,
  deadline: '2025-12-31',
  status: 'ACTIVE',
  ...overrides
});

export const generateMockWallet = (overrides = {}) => ({
  id: Math.floor(Math.random() * 1000),
  name: 'Test Wallet',
  balance: 5000000,
  ...overrides
});

// Authentication helpers
export const mockAuthToken = 'test-jwt-token-12345';

export const setAuthToken = (token = mockAuthToken) => {
  localStorage.setItem('accessToken', token);
  localStorage.setItem('authToken', token);
};

export const clearAuthToken = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('authToken');
};

export const getAuthHeaders = (token = mockAuthToken) => ({
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${token}`
});

// API mock helpers
export const mockApiSuccess = (data) => ({
  status: 200,
  contentType: 'application/json',
  body: JSON.stringify({
    success: true,
    data
  })
});

export const mockApiError = (message, statusCode = 400) => ({
  status: statusCode,
  contentType: 'application/json',
  body: JSON.stringify({
    success: false,
    message
  })
});

// Page helpers for Puppeteer
export const waitForLoad = async (page, timeout = 2000) => {
  await page.waitForTimeout(timeout);
};

export const fillForm = async (page, formData) => {
  for (const [name, value] of Object.entries(formData)) {
    const input = await page.$(`input[name="${name}"], select[name="${name}"], textarea[name="${name}"]`);
    if (input) {
      const tagName = await page.evaluate(el => el.tagName.toLowerCase(), input);
      
      if (tagName === 'select') {
        await page.select(`select[name="${name}"]`, value.toString());
      } else {
        await page.type(`input[name="${name}"], textarea[name="${name}"]`, value.toString());
      }
    }
  }
};

export const clickButton = async (page, selector) => {
  const button = await page.$(selector);
  if (button) {
    await button.click();
    await waitForLoad(page, 500);
  }
};

export const expectSuccess = async (page) => {
  const hasSuccess = await page.evaluate(() => {
    const text = document.body.textContent;
    return text.includes('thành công') || text.includes('success');
  });
  return hasSuccess;
};

export const expectError = async (page) => {
  const hasError = await page.evaluate(() => {
    const text = document.body.textContent;
    return text.includes('lỗi') || text.includes('error');
  });
  return hasError;
};

// Dialog handlers
export const acceptDialog = (page) => {
  page.on('dialog', async dialog => {
    await dialog.accept();
  });
};

export const dismissDialog = (page) => {
  page.on('dialog', async dialog => {
    await dialog.dismiss();
  });
};

// Wait for element
export const waitForElement = async (page, selector, timeout = 5000) => {
  try {
    await page.waitForSelector(selector, { timeout });
    return true;
  } catch (error) {
    return false;
  }
};

// Check if element exists
export const elementExists = async (page, selector) => {
  const element = await page.$(selector);
  return element !== null;
};

// Get text content
export const getTextContent = async (page, selector) => {
  const element = await page.$(selector);
  if (element) {
    return await page.evaluate(el => el.textContent, element);
  }
  return null;
};

// Format currency for testing
export const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount);
};

// Date utilities
export const formatDate = (dateStr) => {
  const date = new Date(dateStr);
  return date.toLocaleDateString('vi-VN');
};

export const getCurrentMonth = () => {
  return new Date().getMonth() + 1;
};

export const getCurrentYear = () => {
  return new Date().getFullYear();
};

// Validation helpers
export const isValidEmail = (email) => {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
};

export const isStrongPassword = (password) => {
  return password.length >= 8 &&
         /[A-Z]/.test(password) &&
         /[a-z]/.test(password) &&
         /[0-9]/.test(password);
};

// Request interception helper
export const setupRequestInterception = async (page, handlers) => {
  await page.setRequestInterception(true);
  
  page.on('request', request => {
    const url = request.url();
    let handled = false;
    
    for (const [pattern, response] of Object.entries(handlers)) {
      if (url.includes(pattern)) {
        request.respond(response);
        handled = true;
        break;
      }
    }
    
    if (!handled) {
      request.continue();
    }
  });
};

// Screenshot helper
export const takeScreenshot = async (page, name) => {
  const timestamp = Date.now();
  await page.screenshot({
    path: `screenshots/${name}-${timestamp}.png`,
    fullPage: true
  });
};

// Performance metrics
export const getPerformanceMetrics = async (page) => {
  return await page.evaluate(() => {
    const navigation = performance.getEntriesByType('navigation')[0];
    return {
      domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
      loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
      domInteractive: navigation.domInteractive - navigation.fetchStart,
      firstPaint: performance.getEntriesByType('paint')[0]?.startTime || 0
    };
  });
};

// Console error tracker
export const trackConsoleErrors = (page) => {
  const errors = [];
  
  page.on('console', msg => {
    if (msg.type() === 'error') {
      errors.push(msg.text());
    }
  });
  
  return errors;
};

// Network request tracker
export const trackNetworkRequests = (page) => {
  const requests = [];
  
  page.on('request', request => {
    requests.push({
      url: request.url(),
      method: request.method(),
      timestamp: Date.now()
    });
  });
  
  return requests;
};

// Cleanup helper
export const cleanup = async (page) => {
  if (page) {
    await page.close();
  }
  clearAuthToken();
};

// Batch operations
export const createMultipleTransactions = (count = 10) => {
  return Array.from({ length: count }, (_, i) => 
    generateMockTransaction({ id: i + 1, description: `Transaction ${i + 1}` })
  );
};

export const createMultipleBudgets = (count = 5) => {
  return Array.from({ length: count }, (_, i) => 
    generateMockBudget({ id: i + 1, name: `Budget ${i + 1}` })
  );
};

// Test data cleanup
export const resetTestData = () => {
  localStorage.clear();
  sessionStorage.clear();
};

// Random data generators
export const randomAmount = (min = 10000, max = 10000000) => {
  return Math.floor(Math.random() * (max - min + 1)) + min;
};

export const randomEmail = () => {
  return `test${Date.now()}${Math.floor(Math.random() * 1000)}@example.com`;
};

export const randomDate = (daysFromNow = 30) => {
  const date = new Date();
  date.setDate(date.getDate() + daysFromNow);
  return date.toISOString().split('T')[0];
};

// Assertion helpers
export const assertElementVisible = async (page, selector) => {
  const isVisible = await page.evaluate(sel => {
    const element = document.querySelector(sel);
    if (!element) return false;
    
    const style = window.getComputedStyle(element);
    return style.display !== 'none' && 
           style.visibility !== 'hidden' && 
           style.opacity !== '0';
  }, selector);
  
  return isVisible;
};

export const assertTextContains = async (page, selector, text) => {
  const content = await getTextContent(page, selector);
  return content && content.includes(text);
};

// Wait helpers
export const waitForResponse = async (page, urlPattern, timeout = 5000) => {
  try {
    await page.waitForResponse(
      response => response.url().includes(urlPattern),
      { timeout }
    );
    return true;
  } catch {
    return false;
  }
};

export const waitForNavigation = async (page, timeout = 5000) => {
  try {
    await page.waitForNavigation({ timeout, waitUntil: 'networkidle2' });
    return true;
  } catch {
    return false;
  }
};

// Export all helpers
export default {
  generateMockUser,
  generateMockTransaction,
  generateMockBudget,
  generateMockGoal,
  generateMockWallet,
  mockAuthToken,
  setAuthToken,
  clearAuthToken,
  getAuthHeaders,
  mockApiSuccess,
  mockApiError,
  waitForLoad,
  fillForm,
  clickButton,
  expectSuccess,
  expectError,
  acceptDialog,
  dismissDialog,
  waitForElement,
  elementExists,
  getTextContent,
  formatCurrency,
  formatDate,
  getCurrentMonth,
  getCurrentYear,
  isValidEmail,
  isStrongPassword,
  setupRequestInterception,
  takeScreenshot,
  getPerformanceMetrics,
  trackConsoleErrors,
  trackNetworkRequests,
  cleanup,
  createMultipleTransactions,
  createMultipleBudgets,
  resetTestData,
  randomAmount,
  randomEmail,
  randomDate,
  assertElementVisible,
  assertTextContains,
  waitForResponse,
  waitForNavigation
};
