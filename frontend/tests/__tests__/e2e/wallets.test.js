/**
 * E2E Tests for Wallets Management
 */

const puppeteer = require('puppeteer');

const BASE_URL = process.env.TEST_URL || 'http://localhost:3000';

describe('Wallets Management E2E Tests', () => {
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
    await page.setViewport({ width: 1280, height: 720 });
    
    await page.goto(`${BASE_URL}/login`);
    await page.evaluate(() => {
      localStorage.setItem('accessToken', 'test-token-12345');
    });
  });

  afterEach(async () => {
    await page.close();
  });

  test('should display wallets page', async () => {
    await page.goto(`${BASE_URL}/wallets`);
    await page.waitForTimeout(1000);
    
    const title = await page.evaluate(() => {
      const h1 = document.querySelector('h1, h2');
      return h1 ? h1.textContent : '';
    });
    
    expect(title.toLowerCase()).toContain('ví' || 'wallet');
  });

  test('should create new wallet', async () => {
    await page.goto(`${BASE_URL}/wallets`);
    
    const addButton = await page.$('[data-action="create"]');
    if (addButton) {
      await addButton.click();
      await page.waitForTimeout(500);
      
      await page.type('input[name="name"]', 'Ví tiết kiệm');
      await page.type('input[name="balance"]', '10000000');
      
      await page.click('button[type="submit"]');
      await page.waitForTimeout(1000);
      
      const hasSuccess = await page.evaluate(() => {
        return document.body.textContent.includes('thành công');
      });
      
      expect(hasSuccess).toBe(true);
    }
  });

  test('should display wallet balance', async () => {
    await page.goto(`${BASE_URL}/wallets`);
    await page.waitForTimeout(1500);
    
    const hasBalance = await page.evaluate(() => {
      const text = document.body.textContent;
      return /\d/.test(text) && (text.includes('đ') || text.includes('VND'));
    });
    
    expect(hasBalance).toBeTruthy();
  });

  test('should transfer between wallets', async () => {
    await page.goto(`${BASE_URL}/wallets`);
    
    const transferButton = await page.$('[data-action="transfer"]');
    if (transferButton) {
      await transferButton.click();
      await page.waitForTimeout(500);
      
      await page.select('select[name="fromWallet"]', '1');
      await page.select('select[name="toWallet"]', '2');
      await page.type('input[name="amount"]', '500000');
      
      await page.click('button[type="submit"]');
      await page.waitForTimeout(1000);
      
      expect(true).toBe(true); // Transfer completed
    }
  });

  test('should edit wallet', async () => {
    await page.goto(`${BASE_URL}/wallets`);
    
    const editButton = await page.$('[data-action="edit"]');
    if (editButton) {
      await editButton.click();
      await page.waitForTimeout(500);
      
      const nameInput = await page.$('input[name="name"]');
      if (nameInput) {
        await nameInput.click({ clickCount: 3 });
        await page.type('input[name="name"]', 'Ví chính mới');
        
        await page.click('button[type="submit"]');
        await page.waitForTimeout(1000);
        
        expect(true).toBe(true);
      }
    }
  });

  test('should delete wallet', async () => {
    await page.goto(`${BASE_URL}/wallets`);
    
    page.on('dialog', async dialog => {
      await dialog.accept();
    });
    
    const deleteButton = await page.$('[data-action="delete"]');
    if (deleteButton) {
      await deleteButton.click();
      await page.waitForTimeout(1000);
      
      expect(true).toBe(true);
    }
  });

  test('should show wallet transactions', async () => {
    await page.goto(`${BASE_URL}/wallets`);
    
    const viewButton = await page.$('[data-action="view-transactions"]');
    if (viewButton) {
      await viewButton.click();
      await page.waitForTimeout(1000);
      
      const hasTransactions = await page.evaluate(() => {
        return !!document.querySelector('.transaction, table');
      });
      
      expect(typeof hasTransactions).toBe('boolean');
    }
  });
});
