// API utility functions for frontend
class FinanceAPI {
    constructor() {
        this.baseURL = '/api';
        this.token = this.getToken();
    }

    getToken() {
        // Get token from session or localStorage
        return localStorage.getItem('token') || sessionStorage.getItem('token');
    }

    setToken(token) {
        this.token = token;
        localStorage.setItem('token', token);
    }

    getHeaders() {
        return {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.token}`
        };
    }

    async request(endpoint, options = {}) {
        try {
            const url = `${this.baseURL}${endpoint}`;
            const config = {
                headers: this.getHeaders(),
                ...options
            };

            const response = await fetch(url, config);
            
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.error || 'Request failed');
            }

            return await response.json();
        } catch (error) {
            console.error('API Error:', error);
            throw error;
        }
    }

    // Transaction APIs
    async getTransactions(params = {}) {
        const queryString = new URLSearchParams(params).toString();
        return this.request(`/transactions?${queryString}`);
    }

    async createTransaction(data) {
        return this.request('/transactions', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    async updateTransaction(id, data) {
        return this.request(`/transactions/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    async deleteTransaction(id) {
        return this.request(`/transactions/${id}`, {
            method: 'DELETE'
        });
    }

    async getTransactionsByType(type) {
        return this.request(`/transactions/type/${type}`);
    }

    async getTransactionsByCategory(categoryId) {
        return this.request(`/transactions/category/${categoryId}`);
    }

    async getTransactionsByWallet(walletId) {
        return this.request(`/transactions/wallet/${walletId}`);
    }

    async searchTransactions(query) {
        return this.request(`/transactions/search?q=${encodeURIComponent(query)}`);
    }

    async getTransactionStatistics() {
        return this.request('/transactions/statistics');
    }

    async getFinancialSummary(startDate, endDate) {
        return this.request(`/transactions/summary?startDate=${startDate}&endDate=${endDate}`);
    }

    // Budget APIs
    async getBudgets() {
        return this.request('/budgets');
    }

    async createBudget(data) {
        return this.request('/budgets', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    async updateBudget(id, data) {
        return this.request(`/budgets/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    async deleteBudget(id) {
        return this.request(`/budgets/${id}`, {
            method: 'DELETE'
        });
    }

    async getActiveBudgets() {
        return this.request('/budgets/active');
    }

    async getExpiredBudgets() {
        return this.request('/budgets/expired');
    }

    async getUpcomingBudgets() {
        return this.request('/budgets/upcoming');
    }

    async getBudgetSummary() {
        return this.request('/budgets/summary');
    }

    async getBudgetStatistics() {
        return this.request('/budgets/statistics');
    }

    // Wallet APIs
    async getWallets() {
        return this.request('/wallets');
    }

    async createWallet(data) {
        return this.request('/wallets', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    async updateWallet(id, data) {
        return this.request(`/wallets/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    async deleteWallet(id) {
        return this.request(`/wallets/${id}`, {
            method: 'DELETE'
        });
    }

    async getActiveWallets() {
        return this.request('/wallets/active');
    }

    async getWalletsByType(type) {
        return this.request(`/wallets/type/${type}`);
    }

    async getWalletSummary() {
        return this.request('/wallets/summary');
    }

    // Category APIs
    async getCategories() {
        return this.request('/categories');
    }

    async createCategory(data) {
        return this.request('/categories', {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    async updateCategory(id, data) {
        return this.request(`/categories/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    async deleteCategory(id) {
        return this.request(`/categories/${id}`, {
            method: 'DELETE'
        });
    }

    async getCategoriesByType(type) {
        return this.request(`/categories/type/${type}`);
    }

    async getIncomeCategories() {
        return this.request('/categories/income');
    }

    async getExpenseCategories() {
        return this.request('/categories/expense');
    }

    async getDefaultCategories() {
        return this.request('/categories/defaults');
    }

    async getCustomCategories() {
        return this.request('/categories/custom');
    }

    async getCategorySummary() {
        return this.request('/categories/summary');
    }

    // Dashboard APIs
    async getDashboardData() {
        return this.request('/dashboard');
    }

    async getFinancialSummary(startDate, endDate) {
        return this.request(`/dashboard/financial-summary?startDate=${startDate}&endDate=${endDate}`);
    }

    async getMonthlyStatistics(year, month) {
        return this.request(`/dashboard/monthly-statistics?year=${year}&month=${month}`);
    }

    async getYearlyStatistics(year) {
        return this.request(`/dashboard/yearly-statistics?year=${year}`);
    }

    // Utility methods
    formatCurrency(amount) {
        return new Intl.NumberFormat('vi-VN', {
            style: 'currency',
            currency: 'VND'
        }).format(amount);
    }

    formatDate(date) {
        return new Date(date).toLocaleDateString('vi-VN');
    }

    formatDateTime(dateTime) {
        return new Date(dateTime).toLocaleString('vi-VN');
    }

    showNotification(message, type = 'info') {
        // You can implement your own notification system here
        console.log(`${type.toUpperCase()}: ${message}`);
        
        // Example using browser notification
        if (type === 'success') {
            alert(`✅ ${message}`);
        } else if (type === 'error') {
            alert(`❌ ${message}`);
        } else {
            alert(`ℹ️ ${message}`);
        }
    }

    handleError(error) {
        console.error('API Error:', error);
        this.showNotification(error.message || 'Có lỗi xảy ra', 'error');
    }
}

// Create global instance
window.financeAPI = new FinanceAPI(); 