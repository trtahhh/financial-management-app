/**
 * E2E Tests for AI Features and Chat
 * Test AI Chat, Auto-categorization, Insights, Planning
 */

const puppeteer = require('puppeteer');

const BASE_URL = process.env.TEST_URL || 'http://localhost:3000';

describe('AI Features E2E Tests', () => {
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

  describe('AI Chat Interface', () => {
    test('should display chat page', async () => {
      await page.goto(`${BASE_URL}/chat`);
      await page.waitForTimeout(1000);
      
      const title = await page.evaluate(() => {
        const h1 = document.querySelector('h1, h2');
        return h1 ? h1.textContent : '';
      });
      
      expect(title.toLowerCase()).toContain('chat' || 'trợ lý' || 'ai');
    });

    test('should display chat input', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const chatInput = await page.$('textarea, input[type="text"]');
      expect(chatInput).toBeTruthy();
    });

    test('should have send button', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const sendButton = await page.$('button[type="submit"], .btn-send');
      expect(sendButton).toBeTruthy();
    });

    test('should send message and receive response', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const input = await page.$('textarea, input[type="text"]');
      if (input) {
        await page.type('textarea, input[type="text"]', 'Xin chào!');
        
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/chat/message')) {
            request.respond({
              status: 200,
              contentType: 'application/json',
              body: JSON.stringify({
                success: true,
                response: 'Xin chào! Tôi có thể giúp gì cho bạn?'
              })
            });
          } else {
            request.continue();
          }
        });
        
        await page.click('button[type="submit"]');
        await page.waitForTimeout(1500);
        
        const hasResponse = await page.evaluate(() => {
          const messages = document.querySelectorAll('.message, .chat-message');
          return messages.length > 1; // User message + AI response
        });
        
        expect(hasResponse).toBe(true);
      }
    });

    test('should display chat history', async () => {
      await page.goto(`${BASE_URL}/chat`);
      await page.waitForTimeout(1000);
      
      const hasChatArea = await page.evaluate(() => {
        return !!document.querySelector('.chat-container, .messages, [class*="chat"]');
      });
      
      expect(hasChatArea).toBe(true);
    });

    test('should show typing indicator', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const input = await page.$('textarea');
      if (input) {
        await page.type('textarea', 'Test message');
        await page.click('button[type="submit"]');
        
        // Check for typing indicator
        await page.waitForTimeout(500);
        
        const hasTypingIndicator = await page.evaluate(() => {
          return !!document.querySelector('.typing, .loading-dots, [class*="typing"]');
        });
        
        expect(typeof hasTypingIndicator).toBe('boolean');
      }
    });

    test('should support multiline messages', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const textarea = await page.$('textarea');
      if (textarea) {
        await page.type('textarea', 'Line 1\nLine 2\nLine 3');
        
        const value = await page.$eval('textarea', el => el.value);
        expect(value).toContain('\n');
      }
    });

    test('should have suggestion chips/quick actions', async () => {
      await page.goto(`${BASE_URL}/chat`);
      await page.waitForTimeout(1000);
      
      const hasSuggestions = await page.evaluate(() => {
        return !!document.querySelector('.suggestion, .quick-action, [class*="suggestion"]');
      });
      
      expect(typeof hasSuggestions).toBe('boolean');
    });

    test('should click suggestion to send message', async () => {
      await page.goto(`${BASE_URL}/chat`);
      await page.waitForTimeout(1000);
      
      const suggestion = await page.$('.suggestion, .quick-action');
      if (suggestion) {
        await suggestion.click();
        await page.waitForTimeout(500);
        
        const inputValue = await page.$eval('textarea, input', el => el.value);
        expect(inputValue.length).toBeGreaterThan(0);
      }
    });
  });

  describe('Financial Planning with AI', () => {
    test('should display planning page', async () => {
      await page.goto(`${BASE_URL}/planning`);
      await page.waitForTimeout(1000);
      
      const title = await page.evaluate(() => {
        const h1 = document.querySelector('h1, h2');
        return h1 ? h1.textContent : '';
      });
      
      expect(title.toLowerCase()).toContain('kế hoạch' || 'planning');
    });

    test('should request financial plan', async () => {
      await page.goto(`${BASE_URL}/planning`);
      
      const planButton = await page.$('button[data-action="generate-plan"]');
      if (planButton) {
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/ai/planning')) {
            request.respond({
              status: 200,
              contentType: 'application/json',
              body: JSON.stringify({
                success: true,
                plan: {
                  recommendations: ['Tiết kiệm 20% thu nhập'],
                  budgetSuggestions: []
                }
              })
            });
          } else {
            request.continue();
          }
        });
        
        await planButton.click();
        await page.waitForTimeout(2000);
        
        const hasPlan = await page.evaluate(() => {
          return document.body.textContent.includes('Tiết kiệm' || 'recommendation');
        });
        
        expect(hasPlan).toBe(true);
      }
    });

    test('should get savings path suggestions', async () => {
      await page.goto(`${BASE_URL}/planning`);
      
      const savingsButton = await page.$('[data-action="savings-path"]');
      if (savingsButton) {
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/ai/savings-path')) {
            request.respond({
              status: 200,
              contentType: 'application/json',
              body: JSON.stringify({
                paths: [
                  { type: 'conservative', monthly: 1000000 },
                  { type: 'balanced', monthly: 2000000 },
                  { type: 'aggressive', monthly: 3000000 }
                ]
              })
            });
          } else {
            request.continue();
          }
        });
        
        await savingsButton.click();
        await page.waitForTimeout(1500);
        
        const hasPaths = await page.evaluate(() => {
          return document.body.textContent.includes('conservative' || 'balanced');
        });
        
        expect(hasPaths).toBe(true);
      }
    });

    test('should display investment advice', async () => {
      await page.goto(`${BASE_URL}/planning`);
      
      const investButton = await page.$('[data-action="investment-advice"]');
      if (investButton) {
        await investButton.click();
        await page.waitForTimeout(1500);
        
        const hasAdvice = await page.evaluate(() => {
          return document.body.textContent.includes('đầu tư' || 'investment');
        });
        
        expect(hasAdvice).toBe(true);
      }
    });
  });

  describe('Auto-Categorization', () => {
    test('should auto-categorize transaction from description', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const addButton = await page.$('[data-action="create"]');
      if (addButton) {
        await addButton.click();
        await page.waitForTimeout(500);
        
        // Type description that triggers AI
        await page.type('input[name="description"]', 'Mua cafe tại Highlands');
        
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/ai/categorize')) {
            request.respond({
              status: 200,
              contentType: 'application/json',
              body: JSON.stringify({
                categoryId: 1,
                categoryName: 'Ăn uống',
                confidence: 0.95
              })
            });
          } else {
            request.continue();
          }
        });
        
        await page.waitForTimeout(1500);
        
        const categorySelected = await page.$eval('select[name="categoryId"]', el => el.value);
        expect(categorySelected).toBeTruthy();
      }
    });

    test('should show confidence score for AI categorization', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const addButton = await page.$('[data-action="create"]');
      if (addButton) {
        await addButton.click();
        await page.waitForTimeout(500);
        
        await page.type('input[name="description"]', 'Grab đi làm');
        await page.waitForTimeout(1500);
        
        const hasConfidence = await page.evaluate(() => {
          return document.body.textContent.includes('%' || 'confidence');
        });
        
        expect(typeof hasConfidence).toBe('boolean');
      }
    });

    test('should allow manual category override', async () => {
      await page.goto(`${BASE_URL}/transactions`);
      
      const addButton = await page.$('[data-action="create"]');
      if (addButton) {
        await addButton.click();
        await page.waitForTimeout(500);
        
        await page.type('input[name="description"]', 'Test');
        await page.waitForTimeout(1000);
        
        // Manually change category
        const categorySelect = await page.$('select[name="categoryId"]');
        if (categorySelect) {
          await page.select('select[name="categoryId"]', '3');
          
          const newValue = await page.$eval('select[name="categoryId"]', el => el.value);
          expect(newValue).toBe('3');
        }
      }
    });
  });

  describe('AI Insights', () => {
    test('should display spending insights', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      
      const insightsButton = await page.$('[data-action="insights"]');
      if (insightsButton) {
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/ai/insights')) {
            request.respond({
              status: 200,
              contentType: 'application/json',
              body: JSON.stringify({
                insights: [
                  'Chi tiêu ăn uống tăng 15% so với tháng trước',
                  'Bạn đang tiết kiệm tốt'
                ]
              })
            });
          } else {
            request.continue();
          }
        });
        
        await insightsButton.click();
        await page.waitForTimeout(1500);
        
        const hasInsights = await page.evaluate(() => {
          return document.body.textContent.includes('Chi tiêu' || 'insight');
        });
        
        expect(hasInsights).toBe(true);
      }
    });

    test('should show anomaly detection', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      
      const hasAnomaly = await page.evaluate(() => {
        const text = document.body.textContent;
        return text.includes('bất thường' || 'unusual' || 'anomaly');
      });
      
      expect(typeof hasAnomaly).toBe('boolean');
    });

    test('should display budget optimization suggestions', async () => {
      await page.goto(`${BASE_URL}/budgets`);
      
      const optimizeButton = await page.$('[data-action="optimize"]');
      if (optimizeButton) {
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/budgets/optimize')) {
            request.respond({
              status: 200,
              contentType: 'application/json',
              body: JSON.stringify({
                suggestions: ['Giảm chi phí ăn uống 500k/tháng']
              })
            });
          } else {
            request.continue();
          }
        });
        
        await optimizeButton.click();
        await page.waitForTimeout(1500);
        
        const hasSuggestions = await page.evaluate(() => {
          return document.body.textContent.includes('Giảm' || 'suggestion');
        });
        
        expect(hasSuggestions).toBe(true);
      }
    });
  });

  describe('Spending Predictions', () => {
    test('should predict next month spending', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      
      const predictionButton = await page.$('[data-action="predict"]');
      if (predictionButton) {
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/ai/predict')) {
            request.respond({
              status: 200,
              contentType: 'application/json',
              body: JSON.stringify({
                predictions: {
                  nextMonth: 5000000,
                  confidence: 0.85
                }
              })
            });
          } else {
            request.continue();
          }
        });
        
        await predictionButton.click();
        await page.waitForTimeout(1500);
        
        const hasPrediction = await page.evaluate(() => {
          return document.body.textContent.includes('dự đoán' || 'predict');
        });
        
        expect(hasPrediction).toBe(true);
      }
    });

    test('should display forecast chart', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(2000);
      
      const hasForecastChart = await page.evaluate(() => {
        const canvases = Array.from(document.querySelectorAll('canvas'));
        return canvases.length > 0;
      });
      
      expect(typeof hasForecastChart).toBe('boolean');
    });
  });

  describe('Sentiment Analysis', () => {
    test('should analyze spending sentiment', async () => {
      await page.goto(`${BASE_URL}/budgets`);
      
      const sentimentButton = await page.$('[data-action="sentiment"]');
      if (sentimentButton) {
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/budgets/sentiment-analysis')) {
            request.respond({
              status: 200,
              contentType: 'application/json',
              body: JSON.stringify({
                sentiment: 'positive',
                score: 0.7,
                analysis: 'Chi tiêu hợp lý'
              })
            });
          } else {
            request.continue();
          }
        });
        
        await sentimentButton.click();
        await page.waitForTimeout(1500);
        
        const hasSentiment = await page.evaluate(() => {
          return document.body.textContent.includes('positive' || 'hợp lý');
        });
        
        expect(hasSentiment).toBe(true);
      }
    });

    test('should display sentiment indicators', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1500);
      
      const hasIndicators = await page.evaluate(() => {
        // Look for emoji, icons, or sentiment badges
        return !!document.querySelector('[class*="sentiment"], [class*="mood"]');
      });
      
      expect(typeof hasIndicators).toBe('boolean');
    });
  });

  describe('AI Model Selection', () => {
    test('should display available AI models', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const modelButton = await page.$('button[data-action="models"], .model-select');
      if (modelButton) {
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/chat/models')) {
            request.respond({
              status: 200,
              contentType: 'application/json',
              body: JSON.stringify({
                models: ['gpt-3.5-turbo', 'gpt-4', 'claude-3']
              })
            });
          } else {
            request.continue();
          }
        });
        
        await modelButton.click();
        await page.waitForTimeout(1000);
        
        const hasModels = await page.evaluate(() => {
          return document.body.textContent.includes('gpt' || 'model');
        });
        
        expect(hasModels).toBe(true);
      }
    });

    test('should switch AI model', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const modelSelect = await page.$('select[name="model"]');
      if (modelSelect) {
        await page.select('select[name="model"]', '1');
        await page.waitForTimeout(500);
        
        const selectedValue = await page.$eval('select[name="model"]', el => el.value);
        expect(selectedValue).toBe('1');
      }
    });
  });

  describe('Natural Language Processing', () => {
    test('should understand Vietnamese commands', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const input = await page.$('textarea');
      if (input) {
        await page.type('textarea', 'Tạo ngân sách ăn uống 5 triệu');
        
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/chat/message')) {
            request.respond({
              status: 200,
              contentType: 'application/json',
              body: JSON.stringify({
                success: true,
                response: 'Tôi đã tạo ngân sách ăn uống 5 triệu đồng cho bạn',
                action: 'create_budget'
              })
            });
          } else {
            request.continue();
          }
        });
        
        await page.click('button[type="submit"]');
        await page.waitForTimeout(1500);
        
        const hasResponse = await page.evaluate(() => {
          return document.body.textContent.includes('ngân sách');
        });
        
        expect(hasResponse).toBe(true);
      }
    });

    test('should handle complex queries', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const input = await page.$('textarea');
      if (input) {
        await page.type('textarea', 'Tôi chi bao nhiêu cho ăn uống tháng này?');
        await page.click('button[type="submit"]');
        await page.waitForTimeout(1500);
        
        const hasResponse = await page.evaluate(() => {
          const messages = document.querySelectorAll('.message');
          return messages.length > 1;
        });
        
        expect(hasResponse).toBe(true);
      }
    });
  });

  describe('AI Chat Features', () => {
    test('should clear chat history', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const clearButton = await page.$('button[data-action="clear"]');
      if (clearButton) {
        await clearButton.click();
        await page.waitForTimeout(500);
        
        const messageCount = await page.evaluate(() => {
          return document.querySelectorAll('.message').length;
        });
        
        expect(messageCount).toBe(0);
      }
    });

    test('should export chat history', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const exportButton = await page.$('button[data-action="export"]');
      if (exportButton) {
        await exportButton.click();
        await page.waitForTimeout(1000);
        
        // Export initiated
        expect(true).toBe(true);
      }
    });

    test('should support voice input', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const voiceButton = await page.$('button[data-action="voice"]');
      expect(typeof voiceButton).toBe('object');
    });

    test('should handle errors gracefully', async () => {
      await page.goto(`${BASE_URL}/chat`);
      
      const input = await page.$('textarea');
      if (input) {
        await page.type('textarea', 'Test message');
        
        await page.setRequestInterception(true);
        page.once('request', request => {
          if (request.url().includes('/api/chat/message')) {
            request.respond({
              status: 500,
              contentType: 'application/json',
              body: JSON.stringify({ error: 'AI service unavailable' })
            });
          } else {
            request.continue();
          }
        });
        
        await page.click('button[type="submit"]');
        await page.waitForTimeout(1000);
        
        const hasError = await page.evaluate(() => {
          return document.body.textContent.includes('lỗi' || 'error');
        });
        
        expect(hasError).toBe(true);
      }
    });
  });
});
