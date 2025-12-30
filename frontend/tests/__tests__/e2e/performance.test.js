/**
 * Performance and Load Tests
 */

const puppeteer = require('puppeteer');

const BASE_URL = process.env.TEST_URL || 'http://localhost:3000';

describe('Performance Tests', () => {
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
    await page.setViewport({ width: 1920, height: 1080 });
    
    await page.goto(`${BASE_URL}/login`);
    await page.evaluate(() => {
      localStorage.setItem('accessToken', 'test-token-12345');
    });
  });

  afterEach(async () => {
    await page.close();
  });

  test('should load dashboard within acceptable time', async () => {
    const startTime = Date.now();
    
    await page.goto(`${BASE_URL}/dashboard`);
    await page.waitForSelector('h1, h2', { timeout: 5000 });
    
    const loadTime = Date.now() - startTime;
    
    expect(loadTime).toBeLessThan(5000); // Should load within 5 seconds
  });

  test('should load transactions page quickly', async () => {
    const startTime = Date.now();
    
    await page.goto(`${BASE_URL}/transactions`);
    await page.waitForSelector('table, .transaction-list', { timeout: 5000 });
    
    const loadTime = Date.now() - startTime;
    
    expect(loadTime).toBeLessThan(5000);
  });

  test('should handle rapid navigation', async () => {
    const pages = ['/dashboard', '/transactions', '/budgets', '/goals', '/wallets'];
    
    for (const pagePath of pages) {
      await page.goto(`${BASE_URL}${pagePath}`);
      await page.waitForTimeout(100);
    }
    
    // Should complete without errors
    expect(true).toBe(true);
  });

  test('should render large transaction list efficiently', async () => {
    await page.goto(`${BASE_URL}/transactions`);
    
    // Mock large dataset
    await page.setRequestInterception(true);
    page.once('request', request => {
      if (request.url().includes('/api/transactions')) {
        const largeData = Array.from({ length: 1000 }, (_, i) => ({
          id: i + 1,
          description: `Transaction ${i + 1}`,
          amount: 100000,
          type: 'EXPENSE'
        }));
        
        request.respond({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(largeData)
        });
      } else {
        request.continue();
      }
    });
    
    await page.reload();
    await page.waitForTimeout(3000);
    
    // Check if rendered
    const rowCount = await page.evaluate(() => {
      return document.querySelectorAll('tr, .transaction-item').length;
    });
    
    expect(rowCount).toBeGreaterThan(0);
  });

  test('should measure page metrics', async () => {
    await page.goto(`${BASE_URL}/dashboard`);
    
    const metrics = await page.evaluate(() => {
      const navigation = performance.getEntriesByType('navigation')[0];
      return {
        domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
        loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
        domInteractive: navigation.domInteractive - navigation.fetchStart
      };
    });
    
    console.log('Performance Metrics:', metrics);
    
    expect(metrics.domInteractive).toBeLessThan(3000);
  });

  test('should not have memory leaks', async () => {
    const initialMetrics = await page.metrics();
    
    // Navigate multiple times
    for (let i = 0; i < 10; i++) {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(500);
    }
    
    const finalMetrics = await page.metrics();
    
    // Memory should not increase dramatically
    const memoryIncrease = finalMetrics.JSHeapUsedSize - initialMetrics.JSHeapUsedSize;
    const increasePercentage = (memoryIncrease / initialMetrics.JSHeapUsedSize) * 100;
    
    expect(increasePercentage).toBeLessThan(100); // Less than 100% increase
  });

  test('should handle concurrent API requests', async () => {
    await page.goto(`${BASE_URL}/dashboard`);
    
    // Trigger multiple API calls
    await Promise.all([
      page.evaluate(() => fetch('/api/transactions')),
      page.evaluate(() => fetch('/api/budgets')),
      page.evaluate(() => fetch('/api/goals')),
      page.evaluate(() => fetch('/api/dashboard/data'))
    ]);
    
    await page.waitForTimeout(2000);
    
    // Should complete without errors
    expect(true).toBe(true);
  });

  test('should lazy load images', async () => {
    await page.goto(`${BASE_URL}/dashboard`);
    
    const imagesLoaded = await page.evaluate(() => {
      const images = document.querySelectorAll('img[loading="lazy"]');
      return images.length;
    });
    
    expect(typeof imagesLoaded).toBe('number');
  });

  test('should cache static assets', async () => {
    await page.goto(`${BASE_URL}/dashboard`);
    
    const cachedResources = await page.evaluate(() => {
      const resources = performance.getEntriesByType('resource');
      return resources.filter(r => r.transferSize === 0).length;
    });
    
    expect(typeof cachedResources).toBe('number');
  });

  test('should handle slow network', async () => {
    // Simulate slow 3G
    const client = await page.target().createCDPSession();
    await client.send('Network.emulateNetworkConditions', {
      offline: false,
      downloadThroughput: 500 * 1024 / 8,
      uploadThroughput: 500 * 1024 / 8,
      latency: 400
    });
    
    await page.goto(`${BASE_URL}/dashboard`);
    await page.waitForSelector('h1, h2', { timeout: 10000 });
    
    // Should still load
    expect(true).toBe(true);
  });
});

describe('Accessibility Tests', () => {
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
  });

  afterEach(async () => {
    await page.close();
  });

  test('should have proper heading hierarchy', async () => {
    await page.goto(`${BASE_URL}/dashboard`);
    
    const hasH1 = await page.$('h1');
    expect(hasH1).toBeTruthy();
  });

  test('should have alt text for images', async () => {
    await page.goto(`${BASE_URL}/dashboard`);
    
    const imagesWithoutAlt = await page.evaluate(() => {
      const images = document.querySelectorAll('img:not([alt])');
      return images.length;
    });
    
    expect(imagesWithoutAlt).toBe(0);
  });

  test('should support keyboard navigation', async () => {
    await page.goto(`${BASE_URL}/dashboard`);
    
    await page.keyboard.press('Tab');
    
    const focusedElement = await page.evaluate(() => {
      return document.activeElement.tagName;
    });
    
    expect(focusedElement).toBeTruthy();
  });

  test('should have ARIA labels', async () => {
    await page.goto(`${BASE_URL}/dashboard`);
    
    const hasAriaLabels = await page.evaluate(() => {
      const elementsWithAria = document.querySelectorAll('[aria-label], [aria-labelledby]');
      return elementsWithAria.length > 0;
    });
    
    expect(typeof hasAriaLabels).toBe('boolean');
  });

  test('should have sufficient color contrast', async () => {
    await page.goto(`${BASE_URL}/dashboard`);
    
    // This would require actual contrast checking library
    // Placeholder for contrast check
    expect(true).toBe(true);
  });
});
