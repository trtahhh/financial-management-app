/**
 * SIMPLE BUTTON DEMO
 * Demo nhanh không cần login
 */

const puppeteer = require('puppeteer');

(async () => {
  console.log('🔘 BUTTON TESTING DEMO\n');
  console.log('Đang mở browser...\n');
  
  const browser = await puppeteer.launch({
    headless: false,
    defaultViewport: { width: 1366, height: 768 }
  });
  
  const page = await browser.newPage();
  
  console.log('📍 Testing homepage...');
  await page.goto('http://localhost:3000');
  await page.waitForTimeout(2000);
  
  // Find all buttons
  const buttons = await page.evaluate(() => {
    const btns = [];
    const selectors = ['button', 'a[href]', 'input[type="submit"]', '[role="button"]'];
    
    selectors.forEach(sel => {
      document.querySelectorAll(sel).forEach((el, idx) => {
        const rect = el.getBoundingClientRect();
        if (rect.width > 0 && rect.height > 0) {
          // Highlight
          el.style.outline = '3px solid #00ff00';
          el.style.boxShadow = '0 0 10px #00ff00';
          
          const id = `demo-btn-${idx}`;
          el.setAttribute('data-demo-id', id);
          
          btns.push({
            id: id,
            text: (el.textContent || el.value || el.href || 'Button').trim().substring(0, 40),
            tag: el.tagName
          });
        }
      });
    });
    
    return btns;
  });
  
  console.log(`\n✨ Found ${buttons.length} clickable elements:\n`);
  buttons.forEach((btn, i) => {
    console.log(`   ${i + 1}. [${btn.tag}] ${btn.text}`);
  });
  
  console.log(`\n📊 Test Results:\n`);
  
  let passed = 0;
  let failed = 0;
  
  // Test each button
  for (let i = 0; i < buttons.length; i++) {
    try {
      // Re-scan and re-highlight buttons before each click
      const currentButtons = await page.evaluate(() => {
        const btns = [];
        const selectors = ['button', 'a[href]', 'input[type="submit"]', '[role="button"]'];
        
        selectors.forEach(sel => {
          document.querySelectorAll(sel).forEach((el, idx) => {
            const rect = el.getBoundingClientRect();
            if (rect.width > 0 && rect.height > 0) {
              el.style.outline = '3px solid #00ff00';
              el.style.boxShadow = '0 0 10px #00ff00';
              
              const id = `demo-btn-${idx}`;
              el.setAttribute('data-demo-id', id);
              
              btns.push({
                id: id,
                text: (el.textContent || el.value || el.href || 'Button').trim().substring(0, 40),
                tag: el.tagName
              });
            }
          });
        });
        
        return btns;
      });
      
      if (i >= currentButtons.length) {
        console.log(`   ⚠️  Button ${i + 1} no longer exists`);
        continue;
      }
      
      const btn = currentButtons[i];
      const urlBefore = page.url();
      
      // Click
      await page.click(`[data-demo-id="${btn.id}"]`, { timeout: 2000 });
      await page.waitForTimeout(800);
      
      const urlAfter = page.url();
      
      if (urlBefore !== urlAfter) {
        console.log(`   ✅ ${btn.text} → Navigation to ${urlAfter}`);
        passed++;
        
        // Go back
        await page.goto('http://localhost:3000');
        await page.waitForTimeout(1000);
      } else {
        console.log(`   ✅ ${btn.text} → Clicked (no navigation)`);
        passed++;
      }
      
    } catch (error) {
      console.log(`   ❌ ${buttons[i].text} → Error: ${error.message}`);
      failed++;
      
      // Try to recover - go back to homepage
      try {
        await page.goto('http://localhost:3000');
        await page.waitForTimeout(1000);
      } catch (e) {
        // Ignore recovery errors
      }
    }
  }
  
  console.log(`\n${'='.repeat(50)}`);
  console.log(`📊 SUMMARY`);
  console.log(`${'='.repeat(50)}`);
  console.log(`Total: ${buttons.length}`);
  console.log(`✅ Passed: ${passed}`);
  console.log(`❌ Failed: ${failed}`);
  console.log(`\nBrowser will stay open for 10 seconds...`);
  
  await page.waitForTimeout(10000);
  await browser.close();
  
  console.log('\n✅ Demo complete!');
  process.exit(0);
  
})().catch(err => {
  console.error('\n❌ Error:', err.message);
  process.exit(1);
});
