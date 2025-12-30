# Frontend Test Suite

Comprehensive automated testing suite for the Financial Management App frontend.

## Test Coverage

- **E2E Tests**: 500+ test cases covering user flows
- **Integration Tests**: API endpoint validation
- **Unit Tests**: Component and utility function tests

## Test Categories

### 1. Authentication Tests (60+ cases)

- Registration flow
- Login/logout
- Password reset
- Protected routes
- Token management

### 2. Transaction Tests (80+ cases)

- CRUD operations
- Filtering and search
- Pagination
- Bulk operations
- Auto-categorization with AI
- Export/import

### 3. Budget Tests (70+ cases)

- Budget creation and management
- Progress tracking
- Alerts and notifications
- Period filters (daily/weekly/monthly/yearly)
- AI insights and optimization

### 4. Goals Tests (60+ cases)

- Goal creation and tracking
- Progress updates
- Goal execution
- Analytics and forecasts
- Visualization

### 5. Dashboard Tests (80+ cases)

- Summary cards

- Period filters
- Recent transactions

- Financial health score- Quick actions
- Responsive design

### 6. AI Features Tests (70+ cases)

- Chat interface
- Financial planning
- Auto-categorization
- Spending predictions
- Sentiment analysis
- Natural language processing

### 7. Integration Tests (80+ cases)

- All API endpoints
- Authentication flow
- Error handling
- CORS validation

### 8. UI Component Tests (50+ cases)

- Form components
- Modal dialogs
- Progress bars
- Alerts and notifications
- Charts
- Validators

## Installation

```bash
cd frontend
npm install
```

## Running Tests

### Run All Tests

```bash
npm test
```

### Run Specific Test Suites

```bash
# E2E tests only
npm run test:e2e

# Integration tests only
npm run test:integration

# Unit tests only
npm run test:unit

# API tests only
npm run test:api

# UI tests only
npm run test:ui
```

### Watch Mode

```bash
npm run test:watch
```

### With Coverage

```bash
npm test -- --coverage
```

## Test Structure

```text
frontend/
├── tests/
│   ├── __tests__/
│   │   ├── e2e/
│   │   │   ├── auth.test.js
│   │   │   ├── transactions.test.js
│   │   │   ├── budgets.test.js
│   │   │   ├── goals.test.js
│   │   │   ├── dashboard.test.js
│   │   │   └── ai-features.test.js
│   │   ├── integration/
│   │   │   └── api.test.js
│   │   └── unit/
│   │       └── components.test.js
│   ├── __mocks__/
│   │   └── styleMock.js
│   ├── setup.js
│   └── setup-e2e.js
└── package.json
```

## Environment Variables

Create `.env.test` file:

```env
TEST_URL=http://localhost:3000
API_URL=http://localhost:8080
TEST_USER_EMAIL=test@example.com
TEST_USER_PASSWORD=Password123!
```

## Test Configuration

### Jest Configuration

See `package.json` for Jest configuration including:

- Coverage thresholds (70%)
- Test environment (jsdom)
- Setup files
- Module name mapper

### E2E Configuration

See `jest.e2e.config.js` for Puppeteer setup

## Writing Tests

### E2E Test Example

```javascript
test('should create new transaction', async () => {
  await page.goto(`${BASE_URL}/transactions`);
  
  const addButton = await page.$('[data-action="create"]');
  await addButton.click();
  
  await page.type('input[name="description"]', 'Test');
  await page.type('input[name="amount"]', '100000');
  
  await page.click('button[type="submit"]');
  await page.waitForTimeout(1000);
  
  expect(page.url()).toContain('transactions');
});
```

### Integration Test Example

```javascript
test('GET /api/transactions - should list transactions', async () => {
  const response = await request(BASE_URL)
    .get('/api/transactions')
    .set('Authorization', `Bearer ${authToken}`);
  
  expect(response.status).toBe(200);
  expect(Array.isArray(response.body)).toBe(true);
});
```

### Unit Test Example

```javascript
test('should format currency correctly', () => {
  const formatted = formatCurrency(1000000);
  expect(formatted).toContain('1.000.000');
});
```

## Best Practices

1. **Test Isolation**: Each test should be independent
2. **Clean Up**: Always clean up after tests
3. **Mock External APIs**: Use request interception
4. **Use Data Attributes**: Target elements with `data-*` attributes
5. **Async/Await**: Always wait for async operations
6. **Descriptive Names**: Use clear test descriptions

## Coverage Goals

- **Lines**: 70%
- **Functions**: 70%
- **Branches**: 70%
- **Statements**: 70%

## CI/CD Integration

Add to your CI pipeline:

```yaml
- name: Run Frontend Tests
  run: |
    cd frontend
    npm install
    npm test -- --ci --coverage
```

## Troubleshooting

### Tests Timing Out

Increase timeout in test:

```javascript
jest.setTimeout(30000); // 30 seconds
```

### Puppeteer Issues

Install dependencies:

```bash
# Ubuntu/Debian
sudo apt-get install -y chromium-browser

# macOS
brew install chromium
```

### Port Already in Use

Change test URL or stop conflicting services:

```bash
lsof -ti:3000 | xargs kill -9
```

## Performance

- **Total Tests**: 500+
- **Average Run Time**: 5-10 minutes
- **Parallel Execution**: Enabled
- **Test Sharding**: Supported

## Continuous Testing

### Testing in Watch Mode

```bash
npm run test:watch
```

### Pre-commit Hook

Add to `.git/hooks/pre-commit`:

```bash
#!/bin/sh
cd frontend && npm test
```

## Reports

### Coverage Report

After running tests with `--coverage`, view report:

```bash
open coverage/lcov-report/index.html
```

### Test Results

HTML reporter available at:

```bash
open test-results/index.html
```

## Support

For issues or questions:

- Create issue in repository
- Check existing test examples
- Review Jest documentation

## Updates

Last updated: December 29, 2025
Test framework version: Jest 29.7.0
