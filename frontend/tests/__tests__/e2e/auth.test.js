/**
 * E2E Tests for Authentication Flows
 * Test Registration, Login, Logout, Password Reset
 */

const puppeteer = require('puppeteer');

const BASE_URL = process.env.TEST_URL || 'http://localhost:3000';
const API_URL = process.env.API_URL || 'http://localhost:8080';

describe('Authentication E2E Tests', () => {
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
  });

  afterEach(async () => {
    await page.close();
  });

  describe('Registration Flow', () => {
    test('should display registration form', async () => {
      await page.goto(`${BASE_URL}/register`);
      
      const title = await page.$eval('h1, h2', el => el.textContent);
      expect(title).toContain('Đăng ký');
      
      // Check form fields exist
      await expect(page.$('input[name="fullName"]')).resolves.toBeTruthy();
      await expect(page.$('input[name="email"]')).resolves.toBeTruthy();
      await expect(page.$('input[name="password"]')).resolves.toBeTruthy();
      await expect(page.$('button[type="submit"]')).resolves.toBeTruthy();
    });

    test('should show validation errors for empty fields', async () => {
      await page.goto(`${BASE_URL}/register`);
      
      // Click submit without filling form
      await page.click('button[type="submit"]');
      
      // Wait for validation messages
      await page.waitForTimeout(500);
      
      const hasError = await page.evaluate(() => {
        const inputs = document.querySelectorAll('input:invalid');
        return inputs.length > 0;
      });
      
      expect(hasError).toBe(true);
    });

    test('should show error for invalid email format', async () => {
      await page.goto(`${BASE_URL}/register`);
      
      await page.type('input[name="fullName"]', 'Test User');
      await page.type('input[name="email"]', 'invalid-email');
      await page.type('input[name="password"]', 'Password123!');
      
      await page.click('button[type="submit"]');
      await page.waitForTimeout(500);
      
      const emailValid = await page.$eval('input[name="email"]', 
        el => el.validity.valid
      );
      
      expect(emailValid).toBe(false);
    });

    test('should successfully register new user', async () => {
      await page.goto(`${BASE_URL}/register`);
      
      const randomEmail = `test${Date.now()}@example.com`;
      
      await page.type('input[name="fullName"]', 'Test User');
      await page.type('input[name="email"]', randomEmail);
      await page.type('input[name="password"]', 'Password123!');
      
      // Mock API response
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/auth/register')) {
          request.respond({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              message: 'Đăng ký thành công'
            })
          });
        } else {
          request.continue();
        }
      });
      
      await page.click('button[type="submit"]');
      
      // Wait for redirect or success message
      await page.waitForTimeout(2000);
      
      const currentUrl = page.url();
      const hasSuccessMessage = await page.evaluate(() => {
        return document.body.textContent.includes('thành công');
      });
      
      expect(currentUrl.includes('/login') || hasSuccessMessage).toBe(true);
    });

    test('should show error when registering existing email', async () => {
      await page.goto(`${BASE_URL}/register`);
      
      await page.type('input[name="fullName"]', 'Test User');
      await page.type('input[name="email"]', 'existing@example.com');
      await page.type('input[name="password"]', 'Password123!');
      
      // Mock API error response
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/auth/register')) {
          request.respond({
            status: 400,
            contentType: 'application/json',
            body: JSON.stringify({
              success: false,
              message: 'Email đã tồn tại'
            })
          });
        } else {
          request.continue();
        }
      });
      
      await page.click('button[type="submit"]');
      await page.waitForTimeout(1000);
      
      const errorMessage = await page.evaluate(() => {
        const errorEl = document.querySelector('.error, .alert-danger, [class*="error"]');
        return errorEl ? errorEl.textContent : '';
      });
      
      expect(errorMessage).toContain('tồn tại');
    });
  });

  describe('Login Flow', () => {
    test('should display login form', async () => {
      await page.goto(`${BASE_URL}/login`);
      
      const title = await page.$eval('h1, h2', el => el.textContent);
      expect(title).toContain('Đăng nhập');
      
      await expect(page.$('input[name="email"]')).resolves.toBeTruthy();
      await expect(page.$('input[name="password"]')).resolves.toBeTruthy();
      await expect(page.$('button[type="submit"]')).resolves.toBeTruthy();
    });

    test('should show error for invalid credentials', async () => {
      await page.goto(`${BASE_URL}/login`);
      
      await page.type('input[name="email"]', 'wrong@example.com');
      await page.type('input[name="password"]', 'wrongpassword');
      
      // Mock API error
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/auth/login')) {
          request.respond({
            status: 401,
            contentType: 'application/json',
            body: JSON.stringify({
              success: false,
              message: 'Email hoặc mật khẩu không đúng'
            })
          });
        } else {
          request.continue();
        }
      });
      
      await page.click('button[type="submit"]');
      await page.waitForTimeout(1000);
      
      const errorMessage = await page.evaluate(() => {
        const errorEl = document.querySelector('.error, .alert-danger, [class*="error"]');
        return errorEl ? errorEl.textContent : '';
      });
      
      expect(errorMessage.length).toBeGreaterThan(0);
    });

    test('should successfully login and redirect to dashboard', async () => {
      await page.goto(`${BASE_URL}/login`);
      
      await page.type('input[name="email"]', 'test@example.com');
      await page.type('input[name="password"]', 'Password123!');
      
      // Mock successful login
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/auth/login')) {
          request.respond({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              token: 'fake-jwt-token',
              user: {
                id: 1,
                email: 'test@example.com',
                fullName: 'Test User'
              }
            })
          });
        } else {
          request.continue();
        }
      });
      
      await page.click('button[type="submit"]');
      await page.waitForNavigation({ waitUntil: 'networkidle2' });
      
      const currentUrl = page.url();
      expect(currentUrl).toContain('/dashboard');
    });

    test('should store auth token in localStorage after login', async () => {
      await page.goto(`${BASE_URL}/login`);
      
      await page.type('input[name="email"]', 'test@example.com');
      await page.type('input[name="password"]', 'Password123!');
      
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/auth/login')) {
          request.respond({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              token: 'test-jwt-token-12345',
              user: { id: 1, email: 'test@example.com' }
            })
          });
        } else {
          request.continue();
        }
      });
      
      await page.click('button[type="submit"]');
      await page.waitForTimeout(1000);
      
      const token = await page.evaluate(() => {
        return localStorage.getItem('accessToken') || localStorage.getItem('authToken');
      });
      
      expect(token).toBeTruthy();
      expect(token).toContain('test-jwt-token');
    });

    test('should remember me functionality', async () => {
      await page.goto(`${BASE_URL}/login`);
      
      const rememberCheckbox = await page.$('input[type="checkbox"][name*="remember"]');
      if (rememberCheckbox) {
        await page.click('input[type="checkbox"][name*="remember"]');
        
        const isChecked = await page.$eval(
          'input[type="checkbox"][name*="remember"]',
          el => el.checked
        );
        expect(isChecked).toBe(true);
      }
    });
  });

  describe('Logout Flow', () => {
    test('should logout and clear auth data', async () => {
      // First login
      await page.goto(`${BASE_URL}/login`);
      
      // Set fake token
      await page.evaluate(() => {
        localStorage.setItem('accessToken', 'fake-token');
        localStorage.setItem('authToken', 'fake-token');
      });
      
      await page.goto(`${BASE_URL}/dashboard`);
      
      // Find and click logout button
      const logoutButton = await page.$('a[href*="logout"], button[onclick*="logout"]');
      if (logoutButton) {
        await logoutButton.click();
        await page.waitForTimeout(1000);
        
        // Check token is cleared
        const token = await page.evaluate(() => {
          return localStorage.getItem('accessToken');
        });
        
        expect(token).toBeFalsy();
        
        // Should redirect to login
        const currentUrl = page.url();
        expect(currentUrl).toContain('/login');
      }
    });
  });

  describe('Password Reset Flow', () => {
    test('should display forgot password form', async () => {
      await page.goto(`${BASE_URL}/login`);
      
      const forgotLink = await page.$('a[href*="forgot"], a[href*="reset"]');
      if (forgotLink) {
        await forgotLink.click();
        await page.waitForTimeout(500);
        
        const hasEmailInput = await page.$('input[type="email"]');
        expect(hasEmailInput).toBeTruthy();
      }
    });

    test('should send password reset email', async () => {
      await page.goto(`${BASE_URL}/reset-password`);
      
      await page.type('input[type="email"]', 'test@example.com');
      
      await page.setRequestInterception(true);
      page.once('request', request => {
        if (request.url().includes('/api/auth/forgot-password')) {
          request.respond({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              message: 'Email đặt lại mật khẩu đã được gửi'
            })
          });
        } else {
          request.continue();
        }
      });
      
      await page.click('button[type="submit"]');
      await page.waitForTimeout(1000);
      
      const successMessage = await page.evaluate(() => {
        return document.body.textContent;
      });
      
      expect(successMessage).toContain('thành công' || 'gửi');
    });
  });

  describe('Protected Routes', () => {
    test('should redirect to login when accessing protected route without auth', async () => {
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1000);
      
      const currentUrl = page.url();
      expect(currentUrl).toContain('/login');
    });

    test('should allow access to protected route with valid token', async () => {
      // Set valid token
      await page.goto(`${BASE_URL}/login`);
      await page.evaluate(() => {
        localStorage.setItem('accessToken', 'valid-token-12345');
      });
      
      await page.goto(`${BASE_URL}/dashboard`);
      await page.waitForTimeout(1000);
      
      const currentUrl = page.url();
      expect(currentUrl).toContain('/dashboard');
    });
  });
});
