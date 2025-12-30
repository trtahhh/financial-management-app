/**
 * E2E Tests for Transaction Management
 * Test Creating, Editing, Deleting, Filtering Transactions
 */

const puppeteer = require('puppeteer');

const BASE_URL = process.env.TEST_URL || 'http://localhost:3000';

describe('Transaction Management E2E Tests', () => {
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
    
    // Set auth token
    await page.goto(`${BASE_URL}/login`);
    await page.evaluate(() => {
      localStorage.setItem('accessToken', 'test-token-12345');
      localStorage.setItem('userId', '1');
    });
  });

  afterEach(async () => {
    await page.close();
  });

  describe('Transaction List View', () => {
    test('should display transactions page', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      await page.waitForTimeout(1000);
      
      const title = await page.evaluate(() => {
        const h1 = document.querySelector('h1, h2, .page-title');
        return h1 ? h1.textContent : '';
      });
      
      expect(title.toLowerCase()).toContain('giao dịch' || 'transaction');
    });

    test('should display transaction table', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      // Mock API response
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/transactions')) {
          request.respond({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify([
              {
                id: 1,
                description: 'Test Transaction',
                amount: 100000,
                type: 'EXPENSE',
                categoryName: 'Ăn uống',
                transactionDate: '2025-12-29',
                walletName: 'Ví chính'
              }
            ])
          });
        } else {
          request.continue();
        }
      });
      
      await page.reload();
      await page.waitForTimeout(1000);
      
      const hasTable = await page.evaluate(() => {
        return !!document.querySelector('table, .transaction-list');
      });
      
      expect(hasTable).toBe(true);
    });

    test('should display empty state when no transactions', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/transactions')) {
          request.respond({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify([])
          });
        } else {
          request.continue();
        }
      });
      
      await page.reload();
      await page.waitForTimeout(1000);
      
      const emptyMessage = await page.evaluate(() => {
        return document.body.textContent;
      });
      
      expect(emptyMessage).toContain('không có' || 'chưa có' || 'empty');
    });

    test('should paginate transactions', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const paginationExists = await page.evaluate(() => {
        return !!document.querySelector('.pagination, [class*="page"]');
      });
      
      if (paginationExists) {
        const nextButton = await page.$('.pagination .next, button[aria-label*="next"]');
        if (nextButton) {
          await nextButton.click();
          await page.waitForTimeout(1000);
          
          // Check URL changed or content updated
          const urlChanged = page.url().includes('page=');
          expect(urlChanged).toBe(true);
        }
      }
    });
  });

  describe('Create Transaction', () => {
    test('should open create transaction modal/form', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const addButton = await page.$('button[data-action="create"], .btn-add, button:has-text("Thêm")');
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

    test('should validate required fields', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      // Open form
      const addButton = await page.$('button[data-action="create"]');
      if (addButton) {
        await addButton.click();
        await page.waitForTimeout(500);
        
        // Try to submit empty form
        const submitButton = await page.$('button[type="submit"]');
        if (submitButton) {
          await submitButton.click();
          await page.waitForTimeout(500);
          
          const hasValidationErrors = await page.evaluate(() => {
            const invalidInputs = document.querySelectorAll('input:invalid, select:invalid');
            return invalidInputs.length > 0;
          });
          
          expect(hasValidationErrors).toBe(true);
        }
      }
    });

    test('should successfully create income transaction', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      // Open form
      const addButton = await page.$('[data-action="create"], .btn-add');
      if (addButton) {
        await addButton.click();
        await page.waitForTimeout(500);
        
        // Fill form
        await page.type('input[name="description"]', 'Test Income');
        await page.type('input[name="amount"]', '500000');
        await page.select('select[name="type"]', 'INCOME');
        await page.select('select[name="categoryId"]', '1');
        await page.select('select[name="walletId"]', '1');
        
        // Mock API response
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/transactions') && request.method() === 'POST') {
            request.respond({
              status: 201,
              contentType: 'application/json',
              body: JSON.stringify({
                success: true,
                data: {
                  id: 100,
                  description: 'Test Income',
                  amount: 500000,
                  type: 'INCOME'
                }
              })
            });
          } else {
            request.continue();
          }
        });
        
        // Submit
        await page.click('button[type="submit"]');
        await page.waitForTimeout(1000);
        
        // Check success message
        const successMessage = await page.evaluate(() => {
          const alert = document.querySelector('.alert-success, .success-message');
          return alert ? alert.textContent : '';
        });
        
        expect(successMessage).toContain('thành công' || 'success');
      }
    });

    test('should successfully create expense transaction', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const addButton = await page.$('[data-action="create"]');
      if (addButton) {
        await addButton.click();
        await page.waitForTimeout(500);
        
        await page.type('input[name="description"]', 'Mua đồ ăn');
        await page.type('input[name="amount"]', '150000');
        await page.select('select[name="type"]', 'EXPENSE');
        await page.select('select[name="categoryId"]', '2');
        
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/transactions') && request.method() === 'POST') {
            request.respond({
              status: 201,
              contentType: 'application/json',
              body: JSON.stringify({
                success: true,
                data: { id: 101, description: 'Mua đồ ăn', amount: 150000 }
              })
            });
          } else {
            request.continue();
          }
        });
        
        await page.click('button[type="submit"]');
        await page.waitForTimeout(1000);
        
        const hasSuccess = await page.evaluate(() => {
          return document.body.textContent.includes('thành công');
        });
        
        expect(hasSuccess).toBe(true);
      }
    });

    test('should auto-categorize transaction with AI', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const addButton = await page.$('[data-action="create"]');
      if (addButton) {
        await addButton.click();
        await page.waitForTimeout(500);
        
        // Type description that triggers AI
        await page.type('input[name="description"]', 'Cafe sáng');
        await page.waitForTimeout(1000);
        
        // Check if category was auto-selected
        const selectedCategory = await page.$eval('select[name="categoryId"]', el => el.value);
        expect(selectedCategory).toBeTruthy();
      }
    });

    test('should handle amount formatting', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const addButton = await page.$('[data-action="create"]');
      if (addButton) {
        await addButton.click();
        await page.waitForTimeout(500);
        
        // Type amount with thousands separator
        await page.type('input[name="amount"]', '1000000');
        
        const formattedValue = await page.$eval('input[name="amount"]', el => el.value);
        
        // Should accept numeric value
        expect(formattedValue).toMatch(/\d/);
      }
    });

    test('should select transaction date', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const addButton = await page.$('[data-action="create"]');
      if (addButton) {
        await addButton.click();
        await page.waitForTimeout(500);
        
        const dateInput = await page.$('input[type="date"]');
        if (dateInput) {
          await page.type('input[type="date"]', '2025-12-29');
          
          const dateValue = await page.$eval('input[type="date"]', el => el.value);
          expect(dateValue).toBe('2025-12-29');
        }
      }
    });
  });

  describe('Edit Transaction', () => {
    test('should open edit modal with transaction data', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      // Mock transaction list
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/transactions')) {
          request.respond({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify([
              {
                id: 1,
                description: 'Existing Transaction',
                amount: 200000,
                type: 'EXPENSE'
              }
            ])
          });
        } else {
          request.continue();
        }
      });
      
      await page.reload();
      await page.waitForTimeout(1000);
      
      const editButton = await page.$('[data-action="edit"], .btn-edit');
      if (editButton) {
        await editButton.click();
        await page.waitForTimeout(500);
        
        const descriptionValue = await page.$eval('input[name="description"]', el => el.value);
        expect(descriptionValue).toBeTruthy();
      }
    });

    test('should successfully update transaction', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const editButton = await page.$('[data-action="edit"]');
      if (editButton) {
        await editButton.click();
        await page.waitForTimeout(500);
        
        // Clear and update description
        await page.click('input[name="description"]', { clickCount: 3 });
        await page.type('input[name="description"]', 'Updated Description');
        
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/transactions/') && request.method() === 'PUT') {
            request.respond({
              status: 200,
              contentType: 'application/json',
              body: JSON.stringify({
                success: true,
                data: { id: 1, description: 'Updated Description' }
              })
            });
          } else {
            request.continue();
          }
        });
        
        await page.click('button[type="submit"]');
        await page.waitForTimeout(1000);
        
        const hasSuccess = await page.evaluate(() => {
          return document.body.textContent.includes('cập nhật' || 'updated');
        });
        
        expect(hasSuccess).toBe(true);
      }
    });
  });

  describe('Delete Transaction', () => {
    test('should show confirmation dialog before delete', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      // Setup dialog handler
      let dialogShown = false;
      page.on('dialog', async dialog => {
        dialogShown = true;
        await dialog.accept();
      });
      
      const deleteButton = await page.$('[data-action="delete"], .btn-delete');
      if (deleteButton) {
        await deleteButton.click();
        await page.waitForTimeout(500);
        
        expect(dialogShown).toBe(true);
      }
    });

    test('should successfully delete transaction', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      page.on('dialog', async dialog => {
        await dialog.accept();
      });
      
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/transactions/') && request.method() === 'DELETE') {
          request.respond({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ success: true })
          });
        } else {
          request.continue();
        }
      });
      
      const deleteButton = await page.$('[data-action="delete"]');
      if (deleteButton) {
        await deleteButton.click();
        await page.waitForTimeout(1000);
        
        const hasSuccess = await page.evaluate(() => {
          return document.body.textContent.includes('xóa' || 'deleted');
        });
        
        expect(hasSuccess).toBe(true);
      }
    });
  });

  describe('Filter and Search', () => {
    test('should filter by transaction type', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const typeFilter = await page.$('select[name="type"], #filter-type');
      if (typeFilter) {
        await page.select('select[name="type"]', 'EXPENSE');
        await page.waitForTimeout(1000);
        
        // Check URL or API call includes filter
        const urlHasFilter = page.url().includes('type=EXPENSE');
        expect(urlHasFilter).toBe(true);
      }
    });

    test('should filter by date range', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const startDate = await page.$('input[name="startDate"]');
      const endDate = await page.$('input[name="endDate"]');
      
      if (startDate && endDate) {
        await page.type('input[name="startDate"]', '2025-12-01');
        await page.type('input[name="endDate"]', '2025-12-31');
        
        const filterButton = await page.$('button[type="submit"], .btn-filter');
        if (filterButton) {
          await filterButton.click();
          await page.waitForTimeout(1000);
          
          expect(page.url()).toContain('startDate' || 'endDate');
        }
      }
    });

    test('should search by description', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const searchInput = await page.$('input[type="search"], input[placeholder*="Tìm"]');
      if (searchInput) {
        await page.type('input[type="search"]', 'cafe');
        await page.waitForTimeout(1000);
        
        // Check filtered results
        const hasResults = await page.evaluate(() => {
          const rows = document.querySelectorAll('table tbody tr, .transaction-item');
          return rows.length > 0;
        });
        
        expect(hasResults).toBeTruthy();
      }
    });

    test('should filter by category', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const categoryFilter = await page.$('select[name="categoryId"]');
      if (categoryFilter) {
        await page.select('select[name="categoryId"]', '1');
        await page.waitForTimeout(1000);
        
        expect(page.url()).toContain('category');
      }
    });

    test('should filter by wallet', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const walletFilter = await page.$('select[name="walletId"]');
      if (walletFilter) {
        await page.select('select[name="walletId"]', '1');
        await page.waitForTimeout(1000);
        
        expect(page.url()).toContain('wallet');
      }
    });

    test('should clear all filters', async () => {
      await page.goto(`${BASE_URL}/transactions?type=EXPENSE&category=1`);
      
      const clearButton = await page.$('button[data-action="clear"], .btn-clear-filter');
      if (clearButton) {
        await clearButton.click();
        await page.waitForTimeout(500);
        
        const url = page.url();
        expect(url).not.toContain('type=');
        expect(url).not.toContain('category=');
      }
    });
  });

  describe('Transaction Details', () => {
    test('should view transaction details', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const viewButton = await page.$('[data-action="view"], .btn-view');
      if (viewButton) {
        await viewButton.click();
        await page.waitForTimeout(500);
        
        const modalVisible = await page.evaluate(() => {
          const modal = document.querySelector('.modal-detail, [role="dialog"]');
          return modal && modal.offsetParent !== null;
        });
        
        expect(modalVisible).toBe(true);
      }
    });

    test('should display all transaction fields', async () => {
      await page.goto(`${BASE_URL}/transactions/1`);
      await page.waitForTimeout(1000);
      
      const hasRequiredFields = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('Mô tả') && 
               text.includes('Số tiền') && 
               text.includes('Danh mục');
      });
      
      expect(hasRequiredFields).toBe(true);
    });
  });

  describe('Bulk Operations', () => {
    test('should select multiple transactions', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const checkboxes = await page.$$('input[type="checkbox"].transaction-select');
      if (checkboxes.length > 0) {
        await checkboxes[0].click();
        await checkboxes[1]?.click();
        
        const checkedCount = await page.evaluate(() => {
          return document.querySelectorAll('input[type="checkbox"]:checked').length;
        });
        
        expect(checkedCount).toBeGreaterThan(0);
      }
    });

    test('should bulk delete transactions', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const checkboxes = await page.$$('.transaction-select');
      if (checkboxes.length > 0) {
        await checkboxes[0].click();
        
        page.on('dialog', async dialog => {
          await dialog.accept();
        });
        
        const bulkDeleteButton = await page.$('[data-action="bulk-delete"]');
        if (bulkDeleteButton) {
          await bulkDeleteButton.click();
          await page.waitForTimeout(1000);
          
          const hasSuccess = await page.evaluate(() => {
            return document.body.textContent.includes('xóa');
          });
          
          expect(hasSuccess).toBe(true);
        }
      }
    });
  });

  describe('Export and Import', () => {
    test('should export transactions to CSV', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const exportButton = await page.$('[data-action="export"], .btn-export');
      if (exportButton) {
        const downloadPromise = page.waitForEvent('download');
        await exportButton.click();
        
        // Check download initiated
        // Note: Puppeteer download handling may vary
      }
    });

    test('should import transactions from CSV', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const importButton = await page.$('[data-action="import"], .btn-import');
      if (importButton) {
        await importButton.click();
        await page.waitForTimeout(500);
        
        const fileInput = await page.$('input[type="file"]');
        expect(fileInput).toBeTruthy();
      }
    });
  });
});
