/**
 * COMPREHENSIVE BUTTON TEST
 * Test TẤT CẢ buttons trên TẤT CẢ pages
 */

const puppeteer = require('puppeteer');
const fs = require('fs');

const PAGES_TO_TEST = [
  { url: 'http://localhost:3000', name: 'Homepage', needsAuth: false },
  { url: 'http://localhost:3000/login', name: 'Login Page', needsAuth: false },
  { url: 'http://localhost:3000/register', name: 'Register Page', needsAuth: false },
];

// Pages cần authentication (sẽ skip nếu không login được)
const AUTH_PAGES = [
  { url: 'http://localhost:3000/dashboard', name: 'Dashboard' },
  { url: 'http://localhost:3000/transactions', name: 'Transactions' },
  { url: 'http://localhost:3000/budgets', name: 'Budgets' },
  { url: 'http://localhost:3000/goals', name: 'Goals' },
  { url: 'http://localhost:3000/wallets', name: 'Wallets' },
  { url: 'http://localhost:3000/settings', name: 'Settings' },
];

async function scanButtons(page) {
  return await page.evaluate(() => {
    const btns = [];
    const selectors = [
      'button:not([disabled])', 
      'a[href]:not([disabled])', 
      'input[type="submit"]:not([disabled])',
      'input[type="button"]:not([disabled])',
      '[role="button"]:not([disabled])',
      '.btn:not([disabled])'
    ];
    
    const seen = new Set();
    
    selectors.forEach(sel => {
      document.querySelectorAll(sel).forEach((el, idx) => {
        const rect = el.getBoundingClientRect();
        if (rect.width > 0 && rect.height > 0) {
          const text = (el.textContent || el.value || el.innerText || 'No text').trim();
          const key = `${el.tagName}-${text}-${rect.left}-${rect.top}`;
          
          if (!seen.has(key)) {
            seen.add(key);
            
            // Highlight
            el.style.outline = '3px solid #00ff00';
            el.style.boxShadow = '0 0 10px #00ff00';
            
            const id = `btn-${Date.now()}-${btns.length}`;
            el.setAttribute('data-test-id', id);
            
            btns.push({
              id: id,
              text: text.substring(0, 50),
              tag: el.tagName,
              href: el.href || null,
              type: el.type || null,
              classes: el.className || ''
            });
          }
        }
      });
    });
    
    return btns;
  });
}

async function testButton(page, btn, pageName, pageUrl, buttonIndex) {
  try {
    // Re-scan và click by index thay vì id
    const clicked = await page.evaluate((idx) => {
      const selectors = [
        'button:not([disabled])', 
        'a[href]:not([disabled])', 
        'input[type="submit"]:not([disabled])',
        'input[type="button"]:not([disabled])',
        '[role="button"]:not([disabled])',
        '.btn:not([disabled])'
      ];
      
      const btns = [];
      const seen = new Set();
      
      selectors.forEach(sel => {
        document.querySelectorAll(sel).forEach((el) => {
          const rect = el.getBoundingClientRect();
          if (rect.width > 0 && rect.height > 0) {
            const text = (el.textContent || el.value || el.innerText || 'No text').trim();
            const key = `${el.tagName}-${text}-${rect.left}-${rect.top}`;
            
            if (!seen.has(key)) {
              seen.add(key);
              btns.push(el);
            }
          }
        });
      });
      
      if (idx < btns.length) {
        btns[idx].click();
        return true;
      }
      return false;
    }, buttonIndex);
    
    if (!clicked) {
      return {
        page: pageName,
        button: btn.text,
        tag: btn.tag,
        status: 'failed',
        action: null,
        error: 'Button no longer exists'
      };
    }
    
    const urlBefore = page.url();
    const errors = [];
    
    // Listen for console errors
    page.once('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text());
      }
    });
    
    await page.waitForTimeout(800);
    
    const urlAfter = page.url();
    
    let result = {
      page: pageName,
      button: btn.text,
      tag: btn.tag,
      status: 'success',
      action: null,
      error: null
    };
    
    if (errors.length > 0) {
      result.status = 'warning';
      result.error = errors[0];
      result.action = 'Console error detected';
    } else if (urlBefore !== urlAfter) {
      result.action = `Navigation: ${urlAfter}`;
    } else {
      result.action = 'Clicked (no navigation)';
    }
    
    // Return to original page if navigated away
    if (urlAfter !== pageUrl && !urlAfter.includes('login')) {
      await page.goto(pageUrl);
      await page.waitForTimeout(1000);
    }
    
    return result;
    
  } catch (error) {
    return {
      page: pageName,
      button: btn.text,
      tag: btn.tag,
      status: 'failed',
      action: null,
      error: error.message
    };
  }
}

(async () => {
  console.log('━'.repeat(60));
  console.log('🔘 COMPREHENSIVE BUTTON TEST');
  console.log('━'.repeat(60));
  console.log('');
  
  const browser = await puppeteer.launch({
    headless: false,
    defaultViewport: { width: 1366, height: 768 }
  });
  
  const page = await browser.newPage();
  
  const allResults = [];
  let totalButtons = 0;
  let totalSuccess = 0;
  let totalFailed = 0;
  let totalWarning = 0;
  
  // Test public pages
  for (const pageInfo of PAGES_TO_TEST) {
    console.log(`\n📍 Testing: ${pageInfo.name}`);
    console.log(`   URL: ${pageInfo.url}`);
    
    await page.goto(pageInfo.url);
    await page.waitForTimeout(2000);
    
    const buttons = await scanButtons(page);
    console.log(`   Found: ${buttons.length} buttons\n`);
    
    totalButtons += buttons.length;
    
    for (let i = 0; i < buttons.length; i++) {
      const btn = buttons[i];
      const result = await testButton(page, btn, pageInfo.name, pageInfo.url);
      
      allResults.push(result);
      
      const icon = result.status === 'success' ? '✅' :
                   result.status === 'warning' ? '⚠️' : '❌';
      
      console.log(`   ${icon} [${result.tag}] ${result.button}`);
      if (result.action) console.log(`      → ${result.action}`);
      if (result.error) console.log(`      ✗ ${result.error}`);
      
      if (result.status === 'success') totalSuccess++;
      else if (result.status === 'warning') totalWarning++;
      else totalFailed++;
    }
  }
  
  // Try to test auth pages (optional)
  console.log(`\n${'─'.repeat(60)}`);
  console.log(`📝 Auth Pages (attempting to access...)`);
  
  for (const pageInfo of AUTH_PAGES) {
    await page.goto(pageInfo.url);
    await page.waitForTimeout(1500);
    
    // Check if redirected to login
    if (page.url().includes('login')) {
      console.log(`   ⚠️  ${pageInfo.name}: Requires authentication (skipped)`);
      continue;
    }
    
    console.log(`\n📍 Testing: ${pageInfo.name}`);
    const buttons = await scanButtons(page);
    console.log(`   Found: ${buttons.length} buttons\n`);
    
    totalButtons += buttons.length;
    
    for (let i = 0; i < buttons.length; i++) {
      const btn = buttons[i];
      const result = await testButton(page, btn, pageInfo.name, pageInfo.url);
      
      allResults.push(result);
      
      const icon = result.status === 'success' ? '✅' :
                   result.status === 'warning' ? '⚠️' : '❌';
      
      console.log(`   ${icon} [${result.tag}] ${result.button}`);
      if (result.action) console.log(`      → ${result.action}`);
      if (result.error) console.log(`      ✗ ${result.error}`);
      
      if (result.status === 'success') totalSuccess++;
      else if (result.status === 'warning') totalWarning++;
      else totalFailed++;
    }
  }
  
  // Summary
  console.log(`\n${'━'.repeat(60)}`);
  console.log(`📊 FINAL SUMMARY`);
  console.log(`${'━'.repeat(60)}`);
  console.log(`Total Buttons Tested: ${totalButtons}`);
  console.log(`✅ Success: ${totalSuccess} (${(totalSuccess/totalButtons*100).toFixed(1)}%)`);
  console.log(`⚠️  Warning: ${totalWarning} (${(totalWarning/totalButtons*100).toFixed(1)}%)`);
  console.log(`❌ Failed: ${totalFailed} (${(totalFailed/totalButtons*100).toFixed(1)}%)`);
  
  // Save report
  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      total: totalButtons,
      success: totalSuccess,
      warning: totalWarning,
      failed: totalFailed,
      successRate: (totalSuccess/totalButtons*100).toFixed(1) + '%'
    },
    results: allResults
  };
  
  fs.mkdirSync('reports', { recursive: true });
  fs.writeFileSync('reports/comprehensive-button-test.json', JSON.stringify(report, null, 2));
  
  console.log(`\n💾 Report saved: reports/comprehensive-button-test.json`);
  
  // Group by page
  console.log(`\n📋 Results by Page:`);
  const byPage = {};
  allResults.forEach(r => {
    if (!byPage[r.page]) byPage[r.page] = { total: 0, success: 0, failed: 0, warning: 0 };
    byPage[r.page].total++;
    if (r.status === 'success') byPage[r.page].success++;
    else if (r.status === 'warning') byPage[r.page].warning++;
    else byPage[r.page].failed++;
  });
  
  Object.entries(byPage).forEach(([page, stats]) => {
    console.log(`\n   ${page}:`);
    console.log(`      Total: ${stats.total}`);
    console.log(`      ✅ ${stats.success}  ⚠️ ${stats.warning}  ❌ ${stats.failed}`);
  });
  
  console.log(`\n${'━'.repeat(60)}`);
  console.log(`Browser will stay open for 10 seconds...`);
  await page.waitForTimeout(10000);
  
  await browser.close();
  
  const exitCode = totalFailed > 0 ? 1 : 0;
  console.log(exitCode === 0 ? '\n✅ All tests passed!' : '\n⚠️  Some tests failed - check report');
  process.exit(exitCode);
  
})().catch(err => {
  console.error('\n❌ Fatal Error:', err.message);
  process.exit(1);
});
