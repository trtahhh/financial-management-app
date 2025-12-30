/**
 * VISUAL BUTTON INSPECTOR
 * Tool siêu nhanh để inspect và test buttons
 * - Highlight tất cả buttons trên page
 * - Click tự động và verify
 * - Báo cáo trực quan real-time
 */

const puppeteer = require('puppeteer');

class VisualButtonInspector {
  constructor() {
    this.browser = null;
    this.page = null;
    this.baseUrl = 'http://localhost:3000';
    this.results = [];
  }

  async init() {
    this.browser = await puppeteer.launch({
      headless: false,
      devtools: true, // Mở DevTools để xem console
      defaultViewport: { width: 1920, height: 1080 }
    });
    
    this.page = await this.browser.newPage();
    
    // Inject custom CSS để highlight buttons
    await this.page.evaluateOnNewDocument(() => {
      window.addEventListener('load', () => {
        const style = document.createElement('style');
        style.textContent = `
          .copilot-button-highlight {
            outline: 3px solid #00ff00 !important;
            box-shadow: 0 0 10px #00ff00 !important;
            position: relative !important;
          }
          .copilot-button-highlight::after {
            content: attr(data-copilot-info);
            position: absolute;
            top: -25px;
            left: 0;
            background: #000;
            color: #0f0;
            padding: 2px 8px;
            font-size: 11px;
            border-radius: 3px;
            white-space: nowrap;
            z-index: 999999;
          }
          .copilot-button-testing {
            outline: 3px solid #ffff00 !important;
            box-shadow: 0 0 15px #ffff00 !important;
          }
          .copilot-button-success {
            outline: 3px solid #00ff00 !important;
            box-shadow: 0 0 15px #00ff00 !important;
          }
          .copilot-button-failed {
            outline: 3px solid #ff0000 !important;
            box-shadow: 0 0 15px #ff0000 !important;
          }
        `;
        document.head.appendChild(style);
      });
    });
  }

  async login() {
    await this.page.goto(`${this.baseUrl}/login`);
    await this.page.type('input[name="email"]', 'test@example.com');
    await this.page.type('input[name="password"]', 'Test123456');
    await this.page.click('button[type="submit"]');
    await this.page.waitForTimeout(2000);
  }

  async highlightAllButtons() {
    return await this.page.evaluate(() => {
      const selectors = [
        'button:not([disabled])',
        'a[href]:not([disabled])',
        'input[type="submit"]:not([disabled])',
        'input[type="button"]:not([disabled])',
        '[role="button"]:not([disabled])',
        '.btn:not([disabled])'
      ];
      
      const buttons = [];
      let index = 1;
      
      selectors.forEach(selector => {
        document.querySelectorAll(selector).forEach(el => {
          const rect = el.getBoundingClientRect();
          if (rect.width > 0 && rect.height > 0) {
            el.classList.add('copilot-button-highlight');
            const text = el.textContent.trim().substring(0, 30) || el.value || 'Button';
            el.setAttribute('data-copilot-info', `#${index} ${text}`);
            el.setAttribute('data-copilot-id', index);
            
            buttons.push({
              id: index,
              text: text,
              tag: el.tagName,
              selector: `[data-copilot-id="${index}"]`
            });
            
            index++;
          }
        });
      });
      
      return buttons;
    });
  }

  async testButton(buttonId) {
    const selector = `[data-copilot-id="${buttonId}"]`;
    
    console.log(`\n🔍 Testing button #${buttonId}...`);
    
    // Highlight button đang test
    await this.page.evaluate((sel) => {
      const el = document.querySelector(sel);
      if (el) {
        el.classList.remove('copilot-button-highlight');
        el.classList.add('copilot-button-testing');
      }
    }, selector);
    
    const result = {
      id: buttonId,
      status: 'unknown',
      actions: [],
      errors: []
    };
    
    try {
      // Track network
      const requests = [];
      this.page.on('request', req => {
        if (req.url().includes('/api/')) {
          requests.push({ method: req.method(), url: req.url() });
        }
      });
      
      // Track console
      const consoleMessages = [];
      this.page.on('console', msg => {
        consoleMessages.push({ type: msg.type(), text: msg.text() });
      });
      
      // Track dialogs
      this.page.on('dialog', async dialog => {
        result.actions.push(`Dialog: ${dialog.message()}`);
        await dialog.accept();
      });
      
      const urlBefore = this.page.url();
      
      // Click!
      await this.page.click(selector);
      await this.page.waitForTimeout(1500);
      
      const urlAfter = this.page.url();
      
      // Analyze results
      if (urlBefore !== urlAfter) {
        result.actions.push(`Navigation: ${urlBefore} -> ${urlAfter}`);
      }
      
      if (requests.length > 0) {
        result.actions.push(`API Calls: ${requests.length}`);
        requests.forEach(req => {
          result.actions.push(`  - ${req.method} ${req.url}`);
        });
      }
      
      const errors = consoleMessages.filter(m => m.type === 'error');
      if (errors.length > 0) {
        result.errors = errors.map(e => e.text);
        result.status = 'failed';
      } else {
        result.status = result.actions.length > 0 ? 'success' : 'no-action';
      }
      
      // Update visual feedback
      await this.page.evaluate((sel, status) => {
        const el = document.querySelector(sel);
        if (el) {
          el.classList.remove('copilot-button-testing');
          el.classList.add(status === 'failed' ? 'copilot-button-failed' : 'copilot-button-success');
        }
      }, selector, result.status);
      
    } catch (error) {
      result.status = 'failed';
      result.errors.push(error.message);
      
      await this.page.evaluate((sel) => {
        const el = document.querySelector(sel);
        if (el) {
          el.classList.remove('copilot-button-testing');
          el.classList.add('copilot-button-failed');
        }
      }, selector);
    }
    
    this.results.push(result);
    
    // Print result
    const icon = result.status === 'success' ? '✅' : 
                 result.status === 'failed' ? '❌' : '⚠️';
    console.log(`${icon} Button #${buttonId}: ${result.status}`);
    if (result.actions.length > 0) {
      result.actions.forEach(action => console.log(`  📍 ${action}`));
    }
    if (result.errors.length > 0) {
      result.errors.forEach(error => console.log(`  ❌ ${error}`));
    }
    
    return result;
  }

  async inspectPage(url, pageName) {
    console.log(`\n${'='.repeat(60)}`);
    console.log(`🔍 INSPECTING: ${pageName}`);
    console.log(`${'='.repeat(60)}`);
    
    await this.page.goto(url);
    await this.page.waitForTimeout(2000);
    
    const buttons = await this.highlightAllButtons();
    console.log(`\n✨ Found ${buttons.length} buttons on ${pageName}`);
    
    // List all buttons
    console.log('\n📋 Button List:');
    buttons.forEach(btn => {
      console.log(`  #${btn.id} - [${btn.tag}] ${btn.text}`);
    });
    
    // Pause để xem
    console.log('\n⏸️  Page highlighted! Check browser window.');
    console.log('   Green = Found buttons');
    console.log('   Yellow = Testing');
    console.log('   Green glow = Success');
    console.log('   Red glow = Failed');
    
    await this.page.waitForTimeout(3000);
    
    // Test từng button
    console.log('\n🚀 Starting automated testing...\n');
    
    for (const button of buttons) {
      await this.testButton(button.id);
      
      // Return to page after each test
      if (this.page.url() !== url) {
        await this.page.goto(url);
        await this.page.waitForTimeout(1000);
        await this.highlightAllButtons(); // Re-highlight
      }
      
      await this.page.waitForTimeout(800); // Pause giữa các tests
    }
    
    return buttons.length;
  }

  async generateReport() {
    const total = this.results.length;
    const success = this.results.filter(r => r.status === 'success').length;
    const failed = this.results.filter(r => r.status === 'failed').length;
    const noAction = this.results.filter(r => r.status === 'no-action').length;
    
    console.log('\n\n' + '='.repeat(60));
    console.log('📊 FINAL REPORT');
    console.log('='.repeat(60));
    console.log(`Total Buttons Tested: ${total}`);
    console.log(`✅ Success: ${success} (${(success/total*100).toFixed(1)}%)`);
    console.log(`❌ Failed: ${failed} (${(failed/total*100).toFixed(1)}%)`);
    console.log(`⚠️  No Action: ${noAction} (${(noAction/total*100).toFixed(1)}%)`);
    
    if (failed > 0) {
      console.log('\n❌ Failed Buttons:');
      this.results.filter(r => r.status === 'failed').forEach(r => {
        console.log(`  #${r.id}: ${r.errors.join(', ')}`);
      });
    }
    
    // Save report
    const fs = require('fs');
    const reportPath = './reports/visual-button-report.json';
    fs.mkdirSync('./reports', { recursive: true });
    fs.writeFileSync(reportPath, JSON.stringify({
      timestamp: new Date().toISOString(),
      summary: { total, success, failed, noAction },
      details: this.results
    }, null, 2));
    
    console.log(`\n💾 Report saved: ${reportPath}`);
  }

  async close() {
    if (this.browser) {
      await this.browser.close();
    }
  }
}

// MAIN EXECUTION
(async () => {
  const inspector = new VisualButtonInspector();
  
  try {
    await inspector.init();
    await inspector.login();
    
    // Test tất cả pages
    const pages = [
      { url: 'http://localhost:3000/dashboard', name: 'Dashboard' },
      { url: 'http://localhost:3000/transactions', name: 'Transactions' },
      { url: 'http://localhost:3000/budgets', name: 'Budgets' },
      { url: 'http://localhost:3000/goals', name: 'Goals' },
      { url: 'http://localhost:3000/wallets', name: 'Wallets' },
      { url: 'http://localhost:3000/ai-chat', name: 'AI Chat' },
      { url: 'http://localhost:3000/settings', name: 'Settings' }
    ];
    
    for (const page of pages) {
      await inspector.inspectPage(page.url, page.name);
    }
    
    await inspector.generateReport();
    
    console.log('\n✅ Inspection complete!');
    console.log('Check the browser window for visual results.');
    
    // Giữ browser mở 10s để xem kết quả
    console.log('\n⏳ Browser will close in 10 seconds...');
    await new Promise(resolve => setTimeout(resolve, 10000));
    
  } catch (error) {
    console.error('❌ Error:', error);
  } finally {
    await inspector.close();
  }
})();

module.exports = VisualButtonInspector;
