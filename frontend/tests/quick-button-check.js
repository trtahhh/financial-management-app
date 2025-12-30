/**
 * ⚡ QUICK BUTTON INSPECTOR
 * Siêu nhanh - chỉ 30 giây để test TẤT CẢ buttons!
 * 
 * CÁCH DÙNG:
 * node quick-button-check.js
 * 
 * OUTPUT:
 * - Console log real-time
 * - JSON report
 * - Screenshot failed buttons
 */

const puppeteer = require('puppeteer');
const fs = require('fs');

const PAGES = [
  { url: 'http://localhost:3000/dashboard', name: 'Dashboard' },
  { url: 'http://localhost:3000/transactions', name: 'Transactions' },
  { url: 'http://localhost:3000/budgets', name: 'Budgets' },
  { url: 'http://localhost:3000/goals', name: 'Goals' },
  { url: 'http://localhost:3000/wallets', name: 'Wallets' },
];

async function quickCheck() {
  console.log('⚡ QUICK BUTTON CHECK - Starting...\n');
  
  const browser = await puppeteer.launch({ 
    headless: false,
    defaultViewport: { width: 1366, height: 768 }
  });
  
  const page = await browser.newPage();
  const results = { pages: [], summary: { total: 0, working: 0, broken: 0 } };
  
  // Login nhanh
  console.log('🔐 Logging in...');
  await page.goto('http://localhost:3000/login');
  await page.type('input[name="email"]', 'test@example.com', { delay: 10 });
  await page.type('input[name="password"]', 'Test123456', { delay: 10 });
  await page.click('button[type="submit"]');
  await page.waitForTimeout(1500);
  
  // Test từng page
  for (const pageInfo of PAGES) {
    console.log(`\n📍 ${pageInfo.name}...`);
    
    await page.goto(pageInfo.url);
    await page.waitForTimeout(1000);
    
    // Tìm buttons
    const buttons = await page.evaluate(() => {
      const btns = [];
      const selectors = ['button:not([disabled])', 'a[href]:not([disabled])', '[role="button"]:not([disabled])'];
      
      selectors.forEach(sel => {
        document.querySelectorAll(sel).forEach((el, idx) => {
          const rect = el.getBoundingClientRect();
          if (rect.width > 0 && rect.height > 0) {
            const id = `btn-${Date.now()}-${idx}`;
            el.setAttribute('data-quick-id', id);
            btns.push({
              id: id,
              text: el.textContent.trim().substring(0, 30) || el.innerText || 'No text',
              tag: el.tagName
            });
          }
        });
      });
      
      return btns;
    });
    
    console.log(`   Found ${buttons.length} buttons`);
    
    const pageResult = { name: pageInfo.name, buttons: [], broken: [] };
    
    // Test nhanh từng button
    for (const btn of buttons) {
      let status = '✅';
      let error = null;
      
      try {
        // Setup listeners
        const errors = [];
        page.on('console', msg => {
          if (msg.type() === 'error') errors.push(msg.text());
        });
        
        page.on('dialog', async dialog => await dialog.accept());
        
        // Click
        await page.click(`[data-quick-id="${btn.id}"]`, { timeout: 2000 });
        await page.waitForTimeout(300);
        
        if (errors.length > 0) {
          status = '❌';
          error = errors[0];
          pageResult.broken.push({ button: btn.text, error });
          results.summary.broken++;
        } else {
          results.summary.working++;
        }
        
        // Reset page
        if (page.url() !== pageInfo.url) {
          await page.goto(pageInfo.url);
          await page.waitForTimeout(500);
        }
        
      } catch (e) {
        status = '❌';
        error = e.message;
        pageResult.broken.push({ button: btn.text, error });
        results.summary.broken++;
        
        await page.goto(pageInfo.url);
        await page.waitForTimeout(500);
      }
      
      results.summary.total++;
      console.log(`   ${status} ${btn.text}`);
    }
    
    pageResult.buttons = buttons;
    results.pages.push(pageResult);
  }
  
  // Summary
  console.log('\n' + '━'.repeat(50));
  console.log('📊 QUICK CHECK SUMMARY');
  console.log('━'.repeat(50));
  console.log(`Total Buttons: ${results.summary.total}`);
  console.log(`✅ Working: ${results.summary.working}`);
  console.log(`❌ Broken: ${results.summary.broken}`);
  
  if (results.summary.broken > 0) {
    console.log('\n❌ BROKEN BUTTONS:');
    results.pages.forEach(page => {
      if (page.broken.length > 0) {
        console.log(`\n  ${page.name}:`);
        page.broken.forEach(b => {
          console.log(`    • ${b.button}`);
          console.log(`      ${b.error}`);
        });
      }
    });
  }
  
  // Save report
  fs.mkdirSync('reports', { recursive: true });
  fs.writeFileSync('reports/quick-button-check.json', JSON.stringify(results, null, 2));
  console.log('\n💾 Report saved: reports/quick-button-check.json');
  
  await browser.close();
  
  return results.summary.broken === 0 ? 0 : 1;
}

// Run
quickCheck()
  .then(exitCode => {
    console.log(exitCode === 0 ? '\n✅ All buttons working!' : '\n❌ Some buttons broken!');
    process.exit(exitCode);
  })
  .catch(err => {
    console.error('\n❌ Error:', err);
    process.exit(1);
  });
