module.exports = {
  testEnvironment: 'node',
  testMatch: ['**/__tests__/e2e/**/*.test.js'],
  setupFilesAfterEnv: ['<rootDir>/tests/setup-e2e.js'],
  testTimeout: 30000,
};
