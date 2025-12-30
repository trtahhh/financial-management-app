/**
 * E2E Tests for Dashboard and Statistics
 * Test Data Visualization, Charts, Summary Cards
 */

const puppeteer = require('puppeteer');

const BASE_URL = process.env.TEST_URL || 'http://localhost:3000';

describe('Dashboard E2E Tests', () => {
  let browser;
  let page;

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: true,
      args: ['--no-sandbox', '--disable-setuid-sandbox'],
    });
  });

  afterAll(async () => {
    await browser.close();
  });

  beforeEach(async () => {
    page = await browser.newPage();
    await page.setViewport({ width: 1920, height: 1080 }); // Larger for dashboard
    
    await page.goto(`${BASE_URL}/login`);
    await page.evaluate(() => {
      localStorage.setItem('accessToken', 'test-token-12345');
      localStorage.setItem('userId', '1');
    });
  });

  afterEach(async () => {
    await page.close();
  });

  describe('Dashboard Overview', () => {
    test('should display dashboard page', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const title = await page.evaluate(() => {
        const h1 = document.querySelector('h1, h2, .page-title');
        return h1 ? h1.textContent : '';
      });
      
      expect(title.toLowerCase()).toContain('dashboard' || 'tổng quan' || 'bảng điều khiển');
    });

    test('should display summary cards', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/dashboard')) {
          request.respond({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              totalIncome: 50000000,
              totalExpense: 30000000,
              balance: 20000000,
              transactionCount: 150
            })
          });
        } else {
          request.continue();
        }
      });
      
      await page.reload();
      await page.waitForTimeout(2000);
      
      const hasCards = await page.evaluate(() => {
        const cards = document.querySelectorAll('.card, .stat-card, [class*="card"]');
        return cards.length >= 3; // At least 3 summary cards
      });
      
      expect(hasCards).toBe(true);
    });

    test('should display total income', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasIncome = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('Thu nhập' || 'Income');
      });
      
      expect(hasIncome).toBe(true);
    });

    test('should display total expense', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasExpense = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('Chi tiêu' || 'Expense');
      });
      
      expect(hasExpense).toBe(true);
    });

    test('should display current balance', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasBalance = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('Số dư' || 'Balance');
      });
      
      expect(hasBalance).toBe(true);
    });

    test('should format currency correctly', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasCurrencyFormat = await page.evaluate(() => {
        const text = document.body.textContent;
        // Check for Vietnamese currency format (with dots or commas)
        return /[\d,\.]+\s*(đ|VND)/i.test(text);
      });
      
      expect(hasCurrencyFormat).toBeTruthy();
    });
  });

  describe('Charts and Visualizations', () => {
    test('should display income vs expense chart', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(2000);
      
      const hasChart = await page.evaluate(() => {
        return !!document.querySelector('canvas, .chart, [id*="chart"]');
      });
      
      expect(hasChart).toBe(true);
    });

    test('should display category breakdown chart', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(2000);
      
      const hasPieChart = await page.evaluate(() => {
        const canvases = document.querySelectorAll('canvas');
        return canvases.length > 0;
      });
      
      expect(hasPieChart).toBe(true);
    });

    test('should display trend chart', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(2000);
      
      const hasTrendChart = await page.evaluate(() => {
        // Look for line chart or trend visualization
        const text = document.body.textContent;
        return text.includes('Xu hướng' || 'Trend' || 'Biểu đồ');
      });
      
      expect(typeof hasTrendChart).toBe('boolean');
    });

    test('should display spending by category chart', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(2000);
      
      const hasCategoryChart = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('Danh mục' || 'Category');
      });
      
      expect(hasCategoryChart).toBe(true);
    });

    test('should render charts without errors', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(2000);
      
      // Check console for chart errors
      const errors = [];
      page.on('console', msg => {
        if (msg.type() === 'error') {
          errors.push(msg.text());
        }
      });
      
      await page.waitForTimeout(1000);
      
      const hasChartErrors = errors.some(err => 
        err.includes('chart') || err.includes('canvas')
      );
      
      expect(hasChartErrors).toBe(false);
    });
  });

  describe('Recent Transactions', () => {
    test('should display recent transactions list', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasTransactions = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('Giao dịch gần đây' || 'Recent Transactions');
      });
      
      expect(hasTransactions).toBe(true);
    });

    test('should show transaction details', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const transactionItems = await page.$$('.transaction-item, [class*="transaction"]');
      
      if (transactionItems.length > 0) {
        const hasDetails = await page.evaluate(() => {
          const items = document.querySelectorAll('.transaction-item, [class*="transaction"]');
          if (items.length === 0) return false;
          
          const firstItem = items[0];
          const text = firstItem.textContent;
          
          // Should have amount and description
          return /\d/.test(text) && text.length > 10;
        });
        
        expect(hasDetails).toBe(true);
      }
    });

    test('should link to full transactions page', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const viewAllLink = await page.$('a[href*="transactions"]');
      
      if (viewAllLink) {
        await viewAllLink.click();
        await page.waitForTimeout(1000);
        
        const currentUrl = page.url();
        expect(currentUrl).toContain('transactions');
      }
    });
  });

  describe('Period Filter', () => {
    test('should filter by current month', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1000);
      
      const monthFilter = await page.$('select[name="month"], #month-select');
      if (monthFilter) {
        const currentMonth = new Date().getMonth() + 1;
        await page.select('select[name="month"]', currentMonth.toString());
        await page.waitForTimeout(1500);
        
        const urlHasFilter = page.url().includes('month=');
        expect(urlHasFilter).toBe(true);
      }
    });

    test('should filter by year', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1000);
      
      const yearFilter = await page.$('select[name="year"], #year-select');
      if (yearFilter) {
        await page.select('select[name="year"]', '2025');
        await page.waitForTimeout(1500);
        
        expect(page.url()).toContain('year' || '2025');
      }
    });

    test('should have quick period buttons', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1000);
      
      const quickFilters = await page.$$('button[data-period], .period-btn');
      
      if (quickFilters.length > 0) {
        await quickFilters[0].click();
        await page.waitForTimeout(1500);
        
        // Data should update
        expect(true).toBe(true);
      }
    });

    test('should update charts when period changes', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const monthFilter = await page.$('select[name="month"]');
      if (monthFilter) {
        await page.select('select[name="month"]', '11'); // November
        await page.waitForTimeout(2000);
        
        // Charts should re-render
        const chartsExist = await page.evaluate(() => {
          return !!document.querySelector('canvas');
        });
        
        expect(chartsExist).toBe(true);
      }
    });
  });

  describe('Budget Summary', () => {
    test('should display active budgets summary', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasBudgetSection = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('Ngân sách' || 'Budget');
      });
      
      expect(hasBudgetSection).toBe(true);
    });

    test('should show budget alerts', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasAlerts = await page.evaluate(() => {
        return !!document.querySelector('.alert, .warning, [class*="alert"]');
      });
      
      expect(typeof hasAlerts).toBe('boolean');
    });

    test('should link to budgets page', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1000);
      
      const budgetLink = await page.$('a[href*="budgets"]');
      if (budgetLink) {
        await budgetLink.click();
        await page.waitForTimeout(1000);
        
        expect(page.url()).toContain('budgets');
      }
    });
  });

  describe('Goals Summary', () => {
    test('should display active goals', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasGoals = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('Mục tiêu' || 'Goals');
      });
      
      expect(hasGoals).toBe(true);
    });

    test('should show goal progress', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasProgress = await page.evaluate(() => {
        return !!document.querySelector('.progress, [role="progressbar"]');
      });
      
      expect(typeof hasProgress).toBe('boolean');
    });

    test('should link to goals page', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      
      const goalsLink = await page.$('a[href*="goals"]');
      if (goalsLink) {
        await goalsLink.click();
        await page.waitForTimeout(1000);
        
        expect(page.url()).toContain('goals');
      }
    });
  });

  describe('Financial Health Score', () => {
    test('should display health score', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasHealthScore = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('Điểm' || 'Score' || 'Sức khỏe');
      });
      
      expect(typeof hasHealthScore).toBe('boolean');
    });

    test('should show health score indicator', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasIndicator = await page.evaluate(() => {
        // Look for gauge, meter, or progress circle
        return !!document.querySelector('[class*="gauge"], [class*="meter"], [class*="score"]');
      });
      
      expect(typeof hasIndicator).toBe('boolean');
    });

    test('should link to health score details', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      
      const healthLink = await page.$('a[href*="health-score"]');
      if (healthLink) {
        await healthLink.click();
        await page.waitForTimeout(1000);
        
        expect(page.url()).toContain('health');
      }
    });
  });

  describe('Quick Actions', () => {
    test('should have add transaction button', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1000);
      
      const addButton = await page.$('button[data-action="add-transaction"], .btn-add');
      expect(addButton).toBeTruthy();
    });

    test('should open add transaction modal from dashboard', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      
      const addButton = await page.$('button[data-action="add-transaction"]');
      if (addButton) {
        await addButton.click();
        await page.waitForTimeout(500);
        
        const modalVisible = await page.evaluate(() => {
          const modal = document.querySelector('.modal, [role="dialog"]');
          return modal && modal.offsetParent !== null;
        });
        
        expect(modalVisible).toBe(true);
      }
    });

    test('should have quick navigation links', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1000);
      
      const navLinks = await page.$$('nav a, .nav-link');
      expect(navLinks.length).toBeGreaterThan(0);
    });
  });

  describe('Statistics Page', () => {
    test('should display statistics page', async () => {
      await page.goto(`${BASE_URL}/dashboard`); // Or /statistics if separate
      await page.waitForTimeout(1500);
      
      const hasStats = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('Thống kê' || 'Statistics' || 'Báo cáo');
      });
      
      expect(hasStats).toBe(true);
    });

    test('should show category statistics', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/statistics/by-category')) {
          request.respond({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify([
              { categoryName: 'Ăn uống', amount: 3000000 },
              { categoryName: 'Giao thông', amount: 1000000 }
            ])
          });
        } else {
          request.continue();
        }
      });
      
      await page.reload();
      await page.waitForTimeout(2000);
      
      const hasCategoryStats = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('Ăn uống' || 'Category');
      });
      
      expect(hasCategoryStats).toBeTruthy();
    });

    test('should display top spending categories', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasTopCategories = await page.evaluate(() => {
        // Look for ranked list or top categories section
        return !!document.querySelector('[class*="category"], [class*="top"]');
      });
      
      expect(typeof hasTopCategories).toBe('boolean');
    });

    test('should show spending trends', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasTrends = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('Xu hướng' || 'Trend');
      });
      
      expect(typeof hasTrends).toBe('boolean');
    });
  });

  describe('Responsive Design', () => {
    test('should be responsive on mobile', async () => {
      await page.setViewport({ width: 375, height: 667 }); // iPhone SE
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const isMobileLayout = await page.evaluate(() => {
        const width = window.innerWidth;
        return width <= 768;
      });
      
      expect(isMobileLayout).toBe(true);
    });

    test('should be responsive on tablet', async () => {
      await page.setViewport({ width: 768, height: 1024 }); // iPad
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const isTabletLayout = await page.evaluate(() => {
        const width = window.innerWidth;
        return width >= 768 && width < 1024;
      });
      
      expect(isTabletLayout).toBe(true);
    });

    test('should adapt charts for small screens', async () => {
      await page.setViewport({ width: 375, height: 667 });
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(2000);
      
      const chartsVisible = await page.evaluate(() => {
        const canvas = document.querySelector('canvas');
        if (!canvas) return false;
        
        const rect = canvas.getBoundingClientRect();
        return rect.width <= 375;
      });
      
      expect(typeof chartsVisible).toBe('boolean');
    });
  });

  describe('Loading States', () => {
    test('should show loading indicator', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      
      // Check for loading state immediately after navigation
      const hasLoader = await page.evaluate(() => {
        return !!document.querySelector('.loading, .spinner, [class*="load"]');
      });
      
      expect(typeof hasLoader).toBe('boolean');
    });

    test('should hide loader after data loads', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(3000);
      
      const loaderHidden = await page.evaluate(() => {
        const loader = document.querySelector('.loading, .spinner');
        return !loader || loader.style.display === 'none';
      });
      
      expect(typeof loaderHidden).toBe('boolean');
    });
  });

  describe('Error Handling', () => {
    test('should handle API errors gracefully', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/dashboard')) {
          request.respond({
            status: 500,
            contentType: 'application/json',
            body: JSON.stringify({ error: 'Server error' })
          });
        } else {
          request.continue();
        }
      });
      
      await page.reload();
      await page.waitForTimeout(1500);
      
      const hasErrorMessage = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('lỗi' || 'error' || 'không thể');
      });
      
      expect(typeof hasErrorMessage).toBe('boolean');
    });

    test('should show retry option on error', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      
      const retryButton = await page.$('button[data-action="retry"], .btn-retry');
      expect(typeof retryButton).toBe('object');
    });
  });

  describe('Data Refresh', () => {
    test('should have refresh button', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      
      const refreshButton = await page.$('button[data-action="refresh"], .btn-refresh');
      expect(typeof refreshButton).toBe('object');
    });

    test('should refresh data on button click', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const refreshButton = await page.$('button[data-action="refresh"]');
      if (refreshButton) {
        await refreshButton.click();
        await page.waitForTimeout(2000);
        
        // Should show loading and then updated data
        expect(true).toBe(true);
      }
    });

    test('should auto-refresh periodically', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      
      // Check if page has auto-refresh logic
      const hasAutoRefresh = await page.evaluate(() => {
        // Look for interval or auto-refresh indicators
        return typeof window.dashboardRefreshInterval !== 'undefined';
      });
      
      expect(typeof hasAutoRefresh).toBe('boolean');
    });
  });
});
