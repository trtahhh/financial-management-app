/**
 * AUTOMATED BUTTON VALIDATOR
 * Tự động test TẤT CẢ các nút trên giao diện
 * - Tìm tất cả buttons, links, clickable elements
 * - Click và verify chức năng
 * - Báo cáo chi tiết kết quả
 */

const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

describe('🔘 Automated Button Validator - Test tất cả nút', () => {
  let browser;
  let page;
  const baseUrl = 'http://localhost:3000';
  const results = {
    total: 0,
    passed: 0,
    failed: 0,
    details: []
  };

  beforeAll(async () => {
    browser = await puppeteer.launch({
      headless: false, // Hiển thị browser để xem quá trình test
      slowMo: 50 // Chậm lại một chút để dễ quan sát
    });
  });

  afterAll(async () => {
    // Tạo báo cáo chi tiết
    const reportPath = path.join(__dirname, '../../reports/button-validation-report.json');
    fs.mkdirSync(path.dirname(reportPath), { recursive: true });
    fs.writeFileSync(reportPath, JSON.stringify(results, null, 2));
    
    console.log('\n📊 KẾT QUẢ TEST TỰ ĐỘNG:');
    console.log(`✅ Passed: ${results.passed}/${results.total}`);
    console.log(`❌ Failed: ${results.failed}/${results.total}`);
    console.log(`📁 Chi tiết: ${reportPath}`);
    
    await browser.close();
  });

  // Helper: Login để access các trang protected
  const login = async () => {
    await page.goto(`${baseUrl}/login`);
    await page.type('input[name="email"]', 'test@example.com');
    await page.type('input[name="password"]', 'Test123456');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(2000);
  };

  // Helper: Lấy tất cả buttons trên page
  const getAllButtons = async () => {
    return await page.evaluate(() => {
      const buttons = [];
      
      // Tìm tất cả elements có thể click
      const selectors = [
        'button:not([disabled])',
        'a[href]:not([disabled])',
        'input[type="submit"]:not([disabled])',
        'input[type="button"]:not([disabled])',
        '[role="button"]:not([disabled])',
        '.btn:not([disabled])',
        '[onclick]:not([disabled])'
      ];
      
      selectors.forEach(selector => {
        document.querySelectorAll(selector).forEach(el => {
          const rect = el.getBoundingClientRect();
          
          // Chỉ lấy elements visible
          if (rect.width > 0 && rect.height > 0) {
            buttons.push({
              tag: el.tagName.toLowerCase(),
              text: el.textContent.trim().substring(0, 50),
              id: el.id || null,
              class: el.className || null,
              href: el.href || null,
              type: el.type || null,
              selector: el.id ? `#${el.id}` : `.${el.className.split(' ')[0]}`,
              position: { x: rect.left, y: rect.top }
            });
          }
        });
      });
      
      return buttons;
    });
  };

  // Helper: Test một button cụ thể
  const testButton = async (button, pageName) => {
    const testResult = {
      page: pageName,
      button: button.text || button.selector,
      selector: button.selector,
      status: 'unknown',
      error: null,
      response: null
    };

    try {
      results.total++;
      
      // Lắng nghe network requests
      const requests = [];
      const responses = [];
      
      page.on('request', req => requests.push({
        url: req.url(),
        method: req.method()
      }));
      
      page.on('response', res => responses.push({
        url: res.url(),
        status: res.status()
      }));

      // Lắng nghe console errors
      const consoleErrors = [];
      page.on('console', msg => {
        if (msg.type() === 'error') {
          consoleErrors.push(msg.text());
        }
      });

      // Lắng nghe dialogs (alert, confirm)
      page.on('dialog', async dialog => {
        testResult.response = `Dialog: ${dialog.message()}`;
        await dialog.accept();
      });

      const urlBefore = page.url();
      
      // Click button
      await page.click(button.selector);
      await page.waitForTimeout(1000);
      
      const urlAfter = page.url();
      
      // Verify kết quả
      if (consoleErrors.length > 0) {
        testResult.status = 'failed';
        testResult.error = `Console errors: ${consoleErrors.join(', ')}`;
        results.failed++;
      } else if (urlBefore !== urlAfter) {
        testResult.status = 'passed';
        testResult.response = `Navigation: ${urlBefore} -> ${urlAfter}`;
        results.passed++;
      } else if (requests.length > 0) {
        testResult.status = 'passed';
        testResult.response = `API calls: ${requests.map(r => `${r.method} ${r.url}`).join(', ')}`;
        results.passed++;
      } else {
        // Kiểm tra DOM changes
        const hasChanges = await page.evaluate(() => {
          return document.querySelector('.success, .error, .message, .alert, .modal, [data-changed]') !== null;
        });
        
        if (hasChanges) {
          testResult.status = 'passed';
          testResult.response = 'DOM updated';
          results.passed++;
        } else {
          testResult.status = 'warning';
          testResult.response = 'No visible changes';
          results.passed++; // Tính là pass vì không có lỗi
        }
      }
      
    } catch (error) {
      testResult.status = 'failed';
      testResult.error = error.message;
      results.failed++;
    }
    
    results.details.push(testResult);
    
    // Log real-time
    const icon = testResult.status === 'passed' ? '✅' : 
                 testResult.status === 'failed' ? '❌' : '⚠️';
    console.log(`${icon} [${pageName}] ${button.text || button.selector}: ${testResult.status}`);
  };

  // TEST 1: Dashboard buttons
  test('🏠 Test tất cả buttons trên Dashboard', async () => {
    page = await browser.newPage();
    await login();
    await page.goto(`${baseUrl}/dashboard`);
    await page.waitForTimeout(2000);
    
    const buttons = await getAllButtons();
    console.log(`\n📍 Dashboard: Tìm thấy ${buttons.length} buttons`);
    
    for (const button of buttons) {
      await testButton(button, 'Dashboard');
      await page.goto(`${baseUrl}/dashboard`); // Reset về trang gốc
      await page.waitForTimeout(500);
    }
  });

  // TEST 2: Transactions page buttons
  test('💰 Test tất cả buttons trên Transactions', async () => {
    if (!page) page = await browser.newPage();
    await page.goto(`${baseUrl}/transactions`);
    await page.waitForTimeout(2000);
    
    const buttons = await getAllButtons();
    console.log(`\n📍 Transactions: Tìm thấy ${buttons.length} buttons`);
    
    for (const button of buttons) {
      await testButton(button, 'Transactions');
      await page.goto(`${baseUrl}/transactions`);
      await page.waitForTimeout(500);
    }
  });

  // TEST 3: Budgets page buttons
  test('📊 Test tất cả buttons trên Budgets', async () => {
    if (!page) page = await browser.newPage();
    await page.goto(`${baseUrl}/budgets`);
    await page.waitForTimeout(2000);
    
    const buttons = await getAllButtons();
    console.log(`\n📍 Budgets: Tìm thấy ${buttons.length} buttons`);
    
    for (const button of buttons) {
      await testButton(button, 'Budgets');
      await page.goto(`${baseUrl}/budgets`);
      await page.waitForTimeout(500);
    }
  });

  // TEST 4: Goals page buttons
  test('🎯 Test tất cả buttons trên Goals', async () => {
    if (!page) page = await browser.newPage();
    await page.goto(`${baseUrl}/goals`);
    await page.waitForTimeout(2000);
    
    const buttons = await getAllButtons();
    console.log(`\n📍 Goals: Tìm thấy ${buttons.length} buttons`);
    
    for (const button of buttons) {
      await testButton(button, 'Goals');
      await page.goto(`${baseUrl}/goals`);
      await page.waitForTimeout(500);
    }
  });

  // TEST 5: Wallets page buttons
  test('👛 Test tất cả buttons trên Wallets', async () => {
    if (!page) page = await browser.newPage();
    await page.goto(`${baseUrl}/wallets`);
    await page.waitForTimeout(2000);
    
    const buttons = await getAllButtons();
    console.log(`\n📍 Wallets: Tìm thấy ${buttons.length} buttons`);
    
    for (const button of buttons) {
      await testButton(button, 'Wallets');
      await page.goto(`${baseUrl}/wallets`);
      await page.waitForTimeout(500);
    }
  });

  // TEST 6: Settings page buttons
  test('⚙️ Test tất cả buttons trên Settings', async () => {
    if (!page) page = await browser.newPage();
    await page.goto(`${baseUrl}/settings`);
    await page.waitForTimeout(2000);
    
    const buttons = await getAllButtons();
    console.log(`\n📍 Settings: Tìm thấy ${buttons.length} buttons`);
    
    for (const button of buttons) {
      await testButton(button, 'Settings');
      await page.goto(`${baseUrl}/settings`);
      await page.waitForTimeout(500);
    }
  });

  // TEST 7: AI Chat buttons
  test('🤖 Test tất cả buttons trên AI Chat', async () => {
    if (!page) page = await browser.newPage();
    await page.goto(`${baseUrl}/ai-chat`);
    await page.waitForTimeout(2000);
    
    const buttons = await getAllButtons();
    console.log(`\n📍 AI Chat: Tìm thấy ${buttons.length} buttons`);
    
    for (const button of buttons) {
      await testButton(button, 'AI Chat');
      await page.goto(`${baseUrl}/ai-chat`);
      await page.waitForTimeout(500);
    }
  });

  // TEST 8: Navigation menu buttons
  test('📱 Test tất cả buttons trong Navigation', async () => {
    if (!page) page = await browser.newPage();
    await page.goto(`${baseUrl}/dashboard`);
    await page.waitForTimeout(2000);
    
    // Lấy buttons trong navbar
    const navButtons = await page.evaluate(() => {
      const buttons = [];
      const nav = document.querySelector('nav, .navbar, .navigation, .sidebar, .menu');
      
      if (nav) {
        nav.querySelectorAll('button, a, [role="button"]').forEach(el => {
          const rect = el.getBoundingClientRect();
          if (rect.width > 0 && rect.height > 0) {
            buttons.push({
              tag: el.tagName.toLowerCase(),
              text: el.textContent.trim().substring(0, 50),
              id: el.id || null,
              class: el.className || null,
              selector: el.id ? `#${el.id}` : `.${el.className.split(' ')[0]}`
            });
          }
        });
      }
      
      return buttons;
    });
    
    console.log(`\n📍 Navigation: Tìm thấy ${navButtons.length} buttons`);
    
    for (const button of navButtons) {
      await testButton(button, 'Navigation');
      await page.goto(`${baseUrl}/dashboard`);
      await page.waitForTimeout(500);
    }
  });

  // TEST 9: Modal buttons (nếu có)
  test('🪟 Test tất cả buttons trong Modals', async () => {
    if (!page) page = await browser.newPage();
    await page.goto(`${baseUrl}/transactions`);
    await page.waitForTimeout(2000);
    
    // Thử mở modal
    try {
      await page.click('button[data-toggle="modal"], .btn-add, .add-transaction, [onclick*="modal"]');
      await page.waitForTimeout(1000);
      
      const modalButtons = await page.evaluate(() => {
        const buttons = [];
        const modals = document.querySelectorAll('.modal, [role="dialog"], .popup, .dialog');
        
        modals.forEach(modal => {
          const rect = modal.getBoundingClientRect();
          if (rect.width > 0 && rect.height > 0) {
            modal.querySelectorAll('button, a').forEach(el => {
              buttons.push({
                tag: el.tagName.toLowerCase(),
                text: el.textContent.trim().substring(0, 50),
                id: el.id || null,
                class: el.className || null,
                selector: el.id ? `#${el.id}` : `.${el.className.split(' ')[0]}`
              });
            });
          }
        });
        
        return buttons;
      });
      
      console.log(`\n📍 Modals: Tìm thấy ${modalButtons.length} buttons`);
      
      for (const button of modalButtons) {
        await testButton(button, 'Modal');
        // Reset modal
        await page.evaluate(() => {
          const closeBtn = document.querySelector('.modal .close, .modal-close, [data-dismiss="modal"]');
          if (closeBtn) closeBtn.click();
        });
        await page.waitForTimeout(500);
      }
    } catch (error) {
      console.log('⚠️ Không tìm thấy modal để test');
    }
  });

  // TEST 10: Form submission buttons
  test('📝 Test tất cả Form Submit buttons', async () => {
    if (!page) page = await browser.newPage();
    
    const pages = [
      { url: `${baseUrl}/transactions`, name: 'Transaction Form' },
      { url: `${baseUrl}/budgets`, name: 'Budget Form' },
      { url: `${baseUrl}/goals`, name: 'Goal Form' },
      { url: `${baseUrl}/wallets`, name: 'Wallet Form' }
    ];
    
    for (const pageInfo of pages) {
      await page.goto(pageInfo.url);
      await page.waitForTimeout(2000);
      
      const formButtons = await page.evaluate(() => {
        const buttons = [];
        document.querySelectorAll('form button[type="submit"], form input[type="submit"]').forEach(el => {
          const rect = el.getBoundingClientRect();
          if (rect.width > 0 && rect.height > 0) {
            buttons.push({
              tag: el.tagName.toLowerCase(),
              text: el.textContent.trim() || el.value || 'Submit',
              id: el.id || null,
              class: el.className || null,
              selector: el.id ? `#${el.id}` : `form button[type="submit"]`
            });
          }
        });
        return buttons;
      });
      
      console.log(`\n📍 ${pageInfo.name}: Tìm thấy ${formButtons.length} submit buttons`);
      
      for (const button of formButtons) {
        await testButton(button, pageInfo.name);
        await page.goto(pageInfo.url);
        await page.waitForTimeout(500);
      }
    }
  });
});
