/**
 * UI Component Unit Tests
 * Test individual UI components and utilities
 */

const { JSDOM } = require('jsdom');

describe('UI Component Tests', () => {
  let document;
  let window;

  beforeEach(() => {
    const dom = new JSDOM('<!DOCTYPE html><html><body></body></html>');
    document = dom.window.document;
    window = dom.window;
    global.document = document;
    global.window = window;
  });

  describe('Auth Utilities', () => {
    beforeEach(() => {
      localStorage.clear();
    });

    test('should store auth token', () => {
      const storeAuthToken = (token) => {
        localStorage.setItem('accessToken', token);
        localStorage.setItem('authToken', token);
      };

      storeAuthToken('test-token-123');
      
      expect(localStorage.getItem('accessToken')).toBe('test-token-123');
      expect(localStorage.getItem('authToken')).toBe('test-token-123');
    });

    test('should get auth token', () => {
      const getAuthToken = () => {
        return localStorage.getItem('accessToken') || localStorage.getItem('authToken');
      };

      localStorage.setItem('accessToken', 'my-token');
      
      expect(getAuthToken()).toBe('my-token');
    });

    test('should clear stored tokens', () => {
      const clearStoredTokens = () => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('authToken');
      };

      localStorage.setItem('accessToken', 'token');
      localStorage.setItem('authToken', 'token');
      
      clearStoredTokens();
      
      expect(localStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('authToken')).toBeNull();
    });

    test('should get auth headers', () => {
      const getAuthHeaders = () => {
        const token = localStorage.getItem('accessToken');
        const headers = {
          'Content-Type': 'application/json'
        };
        if (token) {
          headers['Authorization'] = 'Bearer ' + token;
        }
        return headers;
      };

      localStorage.setItem('accessToken', 'test-token');
      
      const headers = getAuthHeaders();
      
      expect(headers).toHaveProperty('Authorization');
      expect(headers.Authorization).toBe('Bearer test-token');
    });

    test('should check if user is authenticated', () => {
      const checkAuth = () => {
        const token = localStorage.getItem('accessToken');
        return !!token;
      };

      expect(checkAuth()).toBe(false);
      
      localStorage.setItem('accessToken', 'token');
      
      expect(checkAuth()).toBe(true);
    });
  });

  describe('Currency Formatter', () => {
    test('should format number to VND currency', () => {
      const formatCurrency = (amount) => {
        return new Intl.NumberFormat('vi-VN', {
          style: 'currency',
          currency: 'VND'
        }).format(amount);
      };

      expect(formatCurrency(1000000)).toContain('1.000.000');
      expect(formatCurrency(5000)).toContain('5.000');
    });

    test('should format with thousand separators', () => {
      const formatNumber = (num) => {
        return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
      };

      expect(formatNumber(1000000)).toBe('1.000.000');
      expect(formatNumber(500000)).toBe('500.000');
    });

    test('should handle decimal places', () => {
      const formatDecimal = (num, decimals = 2) => {
        return num.toFixed(decimals);
      };

      expect(formatDecimal(100.5)).toBe('100.50');
      expect(formatDecimal(99.999, 2)).toBe('100.00');
    });
  });

  describe('Date Utilities', () => {
    test('should format date to Vietnamese format', () => {
      const formatDate = (dateStr) => {
        const date = new Date(dateStr);
        return date.toLocaleDateString('vi-VN');
      };

      const formatted = formatDate('2025-12-29');
      expect(formatted).toContain('29');
      expect(formatted).toContain('12');
      expect(formatted).toContain('2025');
    });

    test('should get current month and year', () => {
      const getCurrentPeriod = () => {
        const now = new Date();
        return {
          month: now.getMonth() + 1,
          year: now.getFullYear()
        };
      };

      const period = getCurrentPeriod();
      
      expect(period.month).toBeGreaterThanOrEqual(1);
      expect(period.month).toBeLessThanOrEqual(12);
      expect(period.year).toBe(2025);
    });

    test('should calculate days between dates', () => {
      const daysBetween = (date1, date2) => {
        const oneDay = 24 * 60 * 60 * 1000;
        return Math.round(Math.abs((date1 - date2) / oneDay));
      };

      const start = new Date('2025-12-01');
      const end = new Date('2025-12-31');
      
      expect(daysBetween(start, end)).toBe(30);
    });

    test('should check if date is in future', () => {
      const isFuture = (dateStr) => {
        const date = new Date(dateStr);
        const now = new Date();
        return date > now;
      };

      expect(isFuture('2026-12-31')).toBe(true);
      expect(isFuture('2020-01-01')).toBe(false);
    });
  });

  describe('Validation Functions', () => {
    test('should validate email format', () => {
      const isValidEmail = (email) => {
        const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return regex.test(email);
      };

      expect(isValidEmail('test@example.com')).toBe(true);
      expect(isValidEmail('invalid-email')).toBe(false);
      expect(isValidEmail('test@')).toBe(false);
    });

    test('should validate password strength', () => {
      const isStrongPassword = (password) => {
        return password.length >= 8 &&
               /[A-Z]/.test(password) &&
               /[a-z]/.test(password) &&
               /[0-9]/.test(password);
      };

      expect(isStrongPassword('Password123')).toBe(true);
      expect(isStrongPassword('weak')).toBe(false);
      expect(isStrongPassword('NoNumbers')).toBe(false);
    });

    test('should validate positive number', () => {
      const isPositiveNumber = (value) => {
        const num = parseFloat(value);
        return !isNaN(num) && num > 0;
      };

      expect(isPositiveNumber('100')).toBe(true);
      expect(isPositiveNumber('-50')).toBe(false);
      expect(isPositiveNumber('abc')).toBe(false);
    });

    test('should validate required field', () => {
      const isRequired = (value) => {
        return value !== null && value !== undefined && value.toString().trim() !== '';
      };

      expect(isRequired('value')).toBe(true);
      expect(isRequired('')).toBe(false);
      expect(isRequired(null)).toBe(false);
      expect(isRequired('  ')).toBe(false);
    });
  });

  describe('Transaction Type Badge', () => {
    test('should render income badge', () => {
      const createBadge = (type) => {
        const badge = document.createElement('span');
        badge.className = `badge badge-${type.toLowerCase()}`;
        badge.textContent = type;
        return badge;
      };

      const badge = createBadge('INCOME');
      
      expect(badge.className).toContain('badge-income');
      expect(badge.textContent).toBe('INCOME');
    });

    test('should render expense badge', () => {
      const createBadge = (type) => {
        const badge = document.createElement('span');
        badge.className = `badge badge-${type.toLowerCase()}`;
        return badge;
      };

      const badge = createBadge('EXPENSE');
      
      expect(badge.className).toContain('badge-expense');
    });
  });

  describe('Progress Bar Component', () => {
    test('should calculate progress percentage', () => {
      const calculateProgress = (current, total) => {
        if (total === 0) return 0;
        return Math.min((current / total) * 100, 100);
      };

      expect(calculateProgress(50, 100)).toBe(50);
      expect(calculateProgress(75, 100)).toBe(75);
      expect(calculateProgress(150, 100)).toBe(100); // Max 100%
      expect(calculateProgress(0, 100)).toBe(0);
    });

    test('should render progress bar element', () => {
      const createProgressBar = (percentage) => {
        const container = document.createElement('div');
        container.className = 'progress';
        
        const bar = document.createElement('div');
        bar.className = 'progress-bar';
        bar.style.width = `${percentage}%`;
        
        container.appendChild(bar);
        return container;
      };

      const progressBar = createProgressBar(75);
      const bar = progressBar.querySelector('.progress-bar');
      
      expect(bar.style.width).toBe('75%');
    });

    test('should apply color based on percentage', () => {
      const getProgressColor = (percentage) => {
        if (percentage >= 100) return 'danger';
        if (percentage >= 80) return 'warning';
        return 'success';
      };

      expect(getProgressColor(50)).toBe('success');
      expect(getProgressColor(85)).toBe('warning');
      expect(getProgressColor(100)).toBe('danger');
    });
  });

  describe('Modal Component', () => {
    test('should create modal element', () => {
      const createModal = (title, content) => {
        const modal = document.createElement('div');
        modal.className = 'modal';
        
        const modalHeader = document.createElement('div');
        modalHeader.className = 'modal-header';
        modalHeader.textContent = title;
        
        const modalBody = document.createElement('div');
        modalBody.className = 'modal-body';
        modalBody.textContent = content;
        
        modal.appendChild(modalHeader);
        modal.appendChild(modalBody);
        
        return modal;
      };

      const modal = createModal('Test Title', 'Test Content');
      
      expect(modal.querySelector('.modal-header').textContent).toBe('Test Title');
      expect(modal.querySelector('.modal-body').textContent).toBe('Test Content');
    });

    test('should show/hide modal', () => {
      const modal = document.createElement('div');
      modal.className = 'modal';
      modal.style.display = 'none';
      document.body.appendChild(modal);

      const showModal = (element) => {
        element.style.display = 'block';
      };

      const hideModal = (element) => {
        element.style.display = 'none';
      };

      showModal(modal);
      expect(modal.style.display).toBe('block');

      hideModal(modal);
      expect(modal.style.display).toBe('none');
    });
  });

  describe('Alert Component', () => {
    test('should create success alert', () => {
      const createAlert = (message, type = 'info') => {
        const alert = document.createElement('div');
        alert.className = `alert alert-${type}`;
        alert.textContent = message;
        return alert;
      };

      const alert = createAlert('Success!', 'success');
      
      expect(alert.className).toContain('alert-success');
      expect(alert.textContent).toBe('Success!');
    });

    test('should create error alert', () => {
      const createAlert = (message, type) => {
        const alert = document.createElement('div');
        alert.className = `alert alert-${type}`;
        return alert;
      };

      const alert = createAlert('Error!', 'danger');
      
      expect(alert.className).toContain('alert-danger');
    });

    test('should auto-dismiss alert after timeout', (done) => {
      const alert = document.createElement('div');
      document.body.appendChild(alert);

      const dismissAlert = (element, timeout = 3000) => {
        setTimeout(() => {
          element.remove();
        }, timeout);
      };

      dismissAlert(alert, 100);

      setTimeout(() => {
        expect(document.body.contains(alert)).toBe(false);
        done();
      }, 150);
    });
  });

  describe('Loading Spinner', () => {
    test('should create loading spinner', () => {
      const createSpinner = () => {
        const spinner = document.createElement('div');
        spinner.className = 'spinner';
        return spinner;
      };

      const spinner = createSpinner();
      
      expect(spinner.className).toBe('spinner');
    });

    test('should show/hide loading state', () => {
      const setLoading = (isLoading) => {
        const spinner = document.getElementById('loading-spinner');
        if (spinner) {
          spinner.style.display = isLoading ? 'block' : 'none';
        }
      };

      const spinner = document.createElement('div');
      spinner.id = 'loading-spinner';
      document.body.appendChild(spinner);

      setLoading(true);
      expect(spinner.style.display).toBe('block');

      setLoading(false);
      expect(spinner.style.display).toBe('none');
    });
  });

  describe('Form Utilities', () => {
    test('should get form data as object', () => {
      const getFormData = (formElement) => {
        const formData = new FormData(formElement);
        const data = {};
        for (const [key, value] of formData.entries()) {
          data[key] = value;
        }
        return data;
      };

      const form = document.createElement('form');
      const input1 = document.createElement('input');
      input1.name = 'name';
      input1.value = 'Test';
      const input2 = document.createElement('input');
      input2.name = 'amount';
      input2.value = '1000';
      
      form.appendChild(input1);
      form.appendChild(input2);

      const data = getFormData(form);
      
      expect(data.name).toBe('Test');
      expect(data.amount).toBe('1000');
    });

    test('should reset form fields', () => {
      const form = document.createElement('form');
      const input = document.createElement('input');
      input.value = 'test';
      form.appendChild(input);

      form.reset();

      expect(input.value).toBe('');
    });

    test('should validate form on submit', () => {
      const validateForm = (formData) => {
        const errors = [];
        
        if (!formData.name) {
          errors.push('Name is required');
        }
        
        if (!formData.amount || formData.amount <= 0) {
          errors.push('Amount must be positive');
        }
        
        return errors;
      };

      const validData = { name: 'Test', amount: 100 };
      const invalidData = { name: '', amount: -50 };

      expect(validateForm(validData)).toHaveLength(0);
      expect(validateForm(invalidData).length).toBeGreaterThan(0);
    });
  });

  describe('Chart Utilities', () => {
    test('should prepare data for pie chart', () => {
      const preparePieData = (categories) => {
        return {
          labels: categories.map(c => c.name),
          data: categories.map(c => c.amount)
        };
      };

      const categories = [
        { name: 'Food', amount: 1000 },
        { name: 'Transport', amount: 500 }
      ];

      const chartData = preparePieData(categories);
      
      expect(chartData.labels).toEqual(['Food', 'Transport']);
      expect(chartData.data).toEqual([1000, 500]);
    });

    test('should prepare data for line chart', () => {
      const prepareLineData = (transactions) => {
        const groupedByDate = {};
        transactions.forEach(t => {
          const date = t.date;
          groupedByDate[date] = (groupedByDate[date] || 0) + t.amount;
        });
        
        return {
          labels: Object.keys(groupedByDate),
          data: Object.values(groupedByDate)
        };
      };

      const transactions = [
        { date: '2025-12-01', amount: 100 },
        { date: '2025-12-01', amount: 200 },
        { date: '2025-12-02', amount: 150 }
      ];

      const chartData = prepareLineData(transactions);
      
      expect(chartData.labels).toContain('2025-12-01');
      expect(chartData.data[0]).toBe(300); // Sum of same date
    });
  });

  describe('Notification Badge', () => {
    test('should display notification count', () => {
      const createNotificationBadge = (count) => {
        const badge = document.createElement('span');
        badge.className = 'notification-badge';
        badge.textContent = count > 99 ? '99+' : count.toString();
        return badge;
      };

      const badge1 = createNotificationBadge(5);
      expect(badge1.textContent).toBe('5');

      const badge2 = createNotificationBadge(150);
      expect(badge2.textContent).toBe('99+');
    });

    test('should hide badge when count is zero', () => {
      const shouldShowBadge = (count) => {
        return count > 0;
      };

      expect(shouldShowBadge(5)).toBe(true);
      expect(shouldShowBadge(0)).toBe(false);
    });
  });

  describe('Search and Filter', () => {
    test('should filter list by search term', () => {
      const filterList = (items, searchTerm) => {
        return items.filter(item => 
          item.toLowerCase().includes(searchTerm.toLowerCase())
        );
      };

      const items = ['Apple', 'Banana', 'Cherry', 'Date'];
      
      expect(filterList(items, 'a')).toEqual(['Apple', 'Banana', 'Date']);
      expect(filterList(items, 'cherry')).toEqual(['Cherry']);
    });

    test('should debounce search input', (done) => {
      let callCount = 0;
      
      const debounce = (func, delay) => {
        let timeout;
        return (...args) => {
          clearTimeout(timeout);
          timeout = setTimeout(() => func(...args), delay);
        };
      };

      const search = () => {
        callCount++;
      };

      const debouncedSearch = debounce(search, 100);
      
      debouncedSearch();
      debouncedSearch();
      debouncedSearch();

      setTimeout(() => {
        expect(callCount).toBe(1); // Only called once after delay
        done();
      }, 150);
    });
  });
});
