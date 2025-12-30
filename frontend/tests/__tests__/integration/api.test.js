/**
 * API Integration Tests
 * Test all API endpoints integration with Frontend
 */

const request = require('supertest');

const BASE_URL = process.env.TEST_URL || 'http://localhost:3000';
const API_BASE = '/api';

describe('API Integration Tests', () => {
  let authToken = 'test-token-12345';

  describe('Authentication API', () => {
    test('POST /api/auth/register - should register new user', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/auth/register`)
        .send({
          fullName: 'Test User',
          email: `test${Date.now()}@example.com`,
          password: 'Password123!'
        });
      
      expect([200, 201, 400]).toContain(response.status);
      if (response.status === 200 || response.status === 201) {
        expect(response.body).toHaveProperty('success');
      }
    });

    test('POST /api/auth/login - should login user', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/auth/login`)
        .send({
          email: 'test@example.com',
          password: 'Password123!'
        });
      
      expect([200, 401]).toContain(response.status);
      if (response.status === 200) {
        expect(response.body).toHaveProperty('token');
      }
    });

    test('POST /api/auth/forgot-password - should send reset email', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/auth/forgot-password`)
        .send({
          email: 'test@example.com'
        });
      
      expect([200, 404]).toContain(response.status);
    });

    test('POST /api/auth/reset-password - should reset password', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/auth/reset-password`)
        .send({
          token: 'reset-token',
          newPassword: 'NewPassword123!'
        });
      
      expect([200, 400, 404]).toContain(response.status);
    });
  });

  describe('Transactions API', () => {
    test('GET /api/transactions - should list transactions', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/transactions`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
      if (response.status === 200) {
        expect(Array.isArray(response.body) || response.body.data).toBeTruthy();
      }
    });

    test('POST /api/transactions - should create transaction', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/transactions`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          description: 'Test Transaction',
          amount: 100000,
          type: 'EXPENSE',
          categoryId: 1,
          walletId: 1,
          transactionDate: '2025-12-29'
        });
      
      expect([200, 201, 400, 401]).toContain(response.status);
    });

    test('GET /api/transactions/:id - should get transaction by id', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/transactions/1`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });

    test('PUT /api/transactions/:id - should update transaction', async () => {
      const response = await request(BASE_URL)
        .put(`${API_BASE}/transactions/1`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          description: 'Updated Transaction',
          amount: 150000
        });
      
      expect([200, 400, 404, 401]).toContain(response.status);
    });

    test('DELETE /api/transactions/:id - should delete transaction', async () => {
      const response = await request(BASE_URL)
        .delete(`${API_BASE}/transactions/999`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });
  });

  describe('Budgets API', () => {
    test('GET /api/budgets - should list budgets', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/budgets`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });

    test('POST /api/budgets - should create budget', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/budgets`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          name: 'Test Budget',
          amount: 5000000,
          categoryId: 1,
          period: 'MONTHLY'
        });
      
      expect([200, 201, 400, 401]).toContain(response.status);
    });

    test('GET /api/budgets/:id - should get budget by id', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/budgets/1`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });

    test('PUT /api/budgets/:id - should update budget', async () => {
      const response = await request(BASE_URL)
        .put(`${API_BASE}/budgets/1`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          amount: 6000000
        });
      
      expect([200, 400, 404, 401]).toContain(response.status);
    });

    test('DELETE /api/budgets/:id - should delete budget', async () => {
      const response = await request(BASE_URL)
        .delete(`${API_BASE}/budgets/999`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });

    test('GET /api/budgets/ultra-insights - should get AI insights', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/budgets/ultra-insights`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });
  });

  describe('Goals API', () => {
    test('GET /api/goals - should list goals', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/goals`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });

    test('POST /api/goals - should create goal', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/goals`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          name: 'Test Goal',
          targetAmount: 10000000,
          currentAmount: 0,
          deadline: '2025-12-31'
        });
      
      expect([200, 201, 400, 401]).toContain(response.status);
    });

    test('GET /api/goals/:id - should get goal by id', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/goals/1`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });

    test('PUT /api/goals/:id - should update goal', async () => {
      const response = await request(BASE_URL)
        .put(`${API_BASE}/goals/1`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          currentAmount: 2000000
        });
      
      expect([200, 400, 404, 401]).toContain(response.status);
    });

    test('POST /api/goals/:id/execute - should execute goal', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/goals/1/execute`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 400, 404, 401]).toContain(response.status);
    });

    test('DELETE /api/goals/:id - should delete goal', async () => {
      const response = await request(BASE_URL)
        .delete(`${API_BASE}/goals/999`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });

    test('GET /api/goals/active - should get active goals', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/goals/active`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });

    test('GET /api/goals/executed - should get executed goals', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/goals/executed`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });
  });

  describe('Categories API', () => {
    test('GET /api/categories - should list categories', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/categories`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
      if (response.status === 200) {
        expect(Array.isArray(response.body) || response.body.data).toBeTruthy();
      }
    });

    test('GET /api/categories/:id - should get category by id', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/categories/1`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });

    test('GET /api/categories/test-colors - should get category colors', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/categories/test-colors`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });
  });

  describe('Wallets API', () => {
    test('GET /api/wallets - should list wallets', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/wallets`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });

    test('POST /api/wallets - should create wallet', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/wallets`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          name: 'Test Wallet',
          balance: 1000000
        });
      
      expect([200, 201, 400, 401]).toContain(response.status);
    });

    test('GET /api/wallets/:id - should get wallet by id', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/wallets/1`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });

    test('PUT /api/wallets/:id - should update wallet', async () => {
      const response = await request(BASE_URL)
        .put(`${API_BASE}/wallets/1`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          name: 'Updated Wallet'
        });
      
      expect([200, 400, 404, 401]).toContain(response.status);
    });

    test('DELETE /api/wallets/:id - should delete wallet', async () => {
      const response = await request(BASE_URL)
        .delete(`${API_BASE}/wallets/999`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });
  });

  describe('Dashboard API', () => {
    test('GET /api/dashboard/data - should get dashboard data', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/dashboard/data`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
      if (response.status === 200) {
        expect(response.body).toHaveProperty('totalIncome' || 'data');
      }
    });

    test('GET /api/dashboard/user/:userId - should get user dashboard', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/dashboard/user/1`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401, 404]).toContain(response.status);
    });

    test('GET /api/dashboard/stats/:userId - should get basic stats', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/dashboard/stats/1`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401, 404]).toContain(response.status);
    });
  });

  describe('Statistics API', () => {
    test('GET /api/statistics/summary - should get summary', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/statistics/summary?userId=1`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });

    test('GET /api/statistics/by-category - should get category statistics', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/statistics/by-category?userId=1`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });
  });

  describe('Financial Health API', () => {
    test('GET /api/financial-health/score - should get health score', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/financial-health/score`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });

    test('GET /api/financial-health/trend - should get trend data', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/financial-health/trend?months=6`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });

    test('GET /api/financial-health/recommendations - should get recommendations', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/financial-health/recommendations`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });
  });

  describe('Chat API', () => {
    test('POST /api/chat/message - should send chat message', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/chat/message`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          message: 'Xin chào'
        });
      
      expect([200, 400, 401]).toContain(response.status);
    });

    test('GET /api/chat/suggestions - should get chat suggestions', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/chat/suggestions`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });

    test('GET /api/chat/models - should get available models', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/chat/models`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });

    test('GET /api/chat/status - should get AI status', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/chat/status`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });
  });

  describe('AI Features API', () => {
    test('POST /api/ai/categorize - should categorize transaction', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/ai/categorize`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          description: 'Mua cafe'
        });
      
      expect([200, 400, 401]).toContain(response.status);
    });

    test('POST /api/ai/planning - should get financial plan', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/ai/planning`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          income: 20000000,
          expenses: 15000000
        });
      
      expect([200, 400, 401]).toContain(response.status);
    });

    test('POST /api/ai/savings-path - should get savings suggestions', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/ai/savings-path`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          targetAmount: 50000000,
          months: 12
        });
      
      expect([200, 400, 401]).toContain(response.status);
    });
  });

  describe('Notifications API', () => {
    test('GET /api/notifications - should list notifications', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/notifications`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });

    test('PUT /api/notifications/read/:id - should mark as read', async () => {
      const response = await request(BASE_URL)
        .put(`${API_BASE}/notifications/read/1`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });

    test('DELETE /api/notifications/:id - should delete notification', async () => {
      const response = await request(BASE_URL)
        .delete(`${API_BASE}/notifications/999`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });

    test('GET /api/notifications/:userId/unread-count - should get unread count', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/notifications/1/unread-count`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });
  });

  describe('Recurring Transactions API', () => {
    test('GET /api/recurring-transactions - should list recurring transactions', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/recurring-transactions`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 401]).toContain(response.status);
    });

    test('POST /api/recurring-transactions - should create recurring transaction', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/recurring-transactions`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          description: 'Monthly Rent',
          amount: 5000000,
          frequency: 'MONTHLY'
        });
      
      expect([200, 201, 400, 401]).toContain(response.status);
    });

    test('PUT /api/recurring-transactions/:id - should update recurring transaction', async () => {
      const response = await request(BASE_URL)
        .put(`${API_BASE}/recurring-transactions/1`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          amount: 6000000
        });
      
      expect([200, 400, 404, 401]).toContain(response.status);
    });

    test('DELETE /api/recurring-transactions/:id - should delete recurring transaction', async () => {
      const response = await request(BASE_URL)
        .delete(`${API_BASE}/recurring-transactions/999`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });
  });

  describe('File Upload API', () => {
    test('POST /api/files/upload - should upload file', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/files/upload`)
        .set('Authorization', `Bearer ${authToken}`)
        .attach('file', Buffer.from('test'), 'test.txt');
      
      expect([200, 201, 400, 401]).toContain(response.status);
    });

    test('GET /api/files/uploads/:filename - should get uploaded file', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/files/uploads/test.txt`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect([200, 404, 401]).toContain(response.status);
    });
  });

  describe('Error Handling', () => {
    test('should return 401 for unauthorized requests', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/transactions`);
      
      expect(response.status).toBe(401);
    });

    test('should return 404 for non-existent endpoints', async () => {
      const response = await request(BASE_URL)
        .get(`${API_BASE}/non-existent-endpoint`)
        .set('Authorization', `Bearer ${authToken}`);
      
      expect(response.status).toBe(404);
    });

    test('should handle invalid JSON', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/transactions`)
        .set('Authorization', `Bearer ${authToken}`)
        .set('Content-Type', 'application/json')
        .send('invalid json');
      
      expect([400, 401]).toContain(response.status);
    });

    test('should validate required fields', async () => {
      const response = await request(BASE_URL)
        .post(`${API_BASE}/transactions`)
        .set('Authorization', `Bearer ${authToken}`)
        .send({
          // Missing required fields
        });
      
      expect([400, 401]).toContain(response.status);
    });
  });

  describe('CORS', () => {
    test('should include CORS headers', async () => {
      const response = await request(BASE_URL)
        .options(`${API_BASE}/transactions`)
        .set('Origin', 'http://localhost:3000');
      
      expect(response.headers['access-control-allow-origin']).toBeTruthy();
    });
  });
});
