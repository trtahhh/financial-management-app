// Transaction service for API communication
import { apiService } from './apiService.js';
import { validateForm, isValidAmount, isValidCategory, isValidTransactionType } from '../utils/validators.js';

class TransactionService {
  constructor() {
    this.endpoint = '/transactions';
  }

  // Get all transactions with filters
  async getTransactions(filters = {}) {
    try {
      return await apiService.get(this.endpoint, filters);
    } catch (error) {
      console.error('Failed to fetch transactions:', error);
      throw new Error('Không thể tải danh sách giao dịch');
    }
  }

  // Get transaction by ID
  async getTransaction(id) {
    try {
      return await apiService.get(`${this.endpoint}/${id}`);
    } catch (error) {
      console.error('Failed to fetch transaction:', error);
      throw new Error('Không thể tải giao dịch');
    }
  }

  // Create new transaction
  async createTransaction(transactionData) {
    // Validate data
    const validation = this.validateTransactionData(transactionData);
    if (!validation.isValid) {
      throw new Error(`Dữ liệu không hợp lệ: ${Object.values(validation.errors).join(', ')}`);
    }

    try {
      const transaction = {
        ...transactionData,
        amount: parseFloat(transactionData.amount),
        date: transactionData.date || new Date().toISOString()
      };

      return await apiService.post(this.endpoint, transaction);
    } catch (error) {
      console.error('Failed to create transaction:', error);
      throw new Error('Không thể tạo giao dịch mới');
    }
  }

  // Update transaction
  async updateTransaction(id, transactionData) {
    // Validate data
    const validation = this.validateTransactionData(transactionData);
    if (!validation.isValid) {
      throw new Error(`Dữ liệu không hợp lệ: ${Object.values(validation.errors).join(', ')}`);
    }

    try {
      const transaction = {
        ...transactionData,
        amount: parseFloat(transactionData.amount)
      };

      return await apiService.put(`${this.endpoint}/${id}`, transaction);
    } catch (error) {
      console.error('Failed to update transaction:', error);
      throw new Error('Không thể cập nhật giao dịch');
    }
  }

  // Delete transaction
  async deleteTransaction(id) {
    try {
      return await apiService.delete(`${this.endpoint}/${id}`);
    } catch (error) {
      console.error('Failed to delete transaction:', error);
      throw new Error('Không thể xóa giao dịch');
    }
  }

  // Get transactions by date range
  async getTransactionsByDateRange(startDate, endDate, type = null) {
    const params = {
      startDate: startDate.toISOString().split('T')[0],
      endDate: endDate.toISOString().split('T')[0]
    };

    if (type) {
      params.type = type;
    }

    try {
      return await apiService.get(this.endpoint, params);
    } catch (error) {
      console.error('Failed to fetch transactions by date range:', error);
      throw new Error('Không thể tải giao dịch theo khoảng thời gian');
    }
  }

  // Get transactions by category
  async getTransactionsByCategory(categoryId) {
    try {
      return await apiService.get(this.endpoint, { categoryId });
    } catch (error) {
      console.error('Failed to fetch transactions by category:', error);
      throw new Error('Không thể tải giao dịch theo danh mục');
    }
  }

  // Upload receipt
  async uploadReceipt(transactionId, file) {
    try {
      return await apiService.upload(`${this.endpoint}/${transactionId}/receipt`, file);
    } catch (error) {
      console.error('Failed to upload receipt:', error);
      throw new Error('Không thể tải lên hóa đơn');
    }
  }

  // Get transaction statistics
  async getTransactionStats(period = 'month') {
    try {
      return await apiService.get(`${this.endpoint}/stats`, { period });
    } catch (error) {
      console.error('Failed to fetch transaction stats:', error);
      throw new Error('Không thể tải thống kê giao dịch');
    }
  }

  // Search transactions
  async searchTransactions(query) {
    try {
      return await apiService.get(`${this.endpoint}/search`, { q: query });
    } catch (error) {
      console.error('Failed to search transactions:', error);
      throw new Error('Không thể tìm kiếm giao dịch');
    }
  }

  // Validate transaction data
  validateTransactionData(data) {
    const rules = {
      amount: [
        { required: true, message: 'Số tiền là bắt buộc' },
        { validator: isValidAmount, message: 'Số tiền không hợp lệ' }
      ],
      type: [
        { required: true, message: 'Loại giao dịch là bắt buộc' },
        { validator: isValidTransactionType, message: 'Loại giao dịch không hợp lệ' }
      ],
      category: [
        { required: true, message: 'Danh mục là bắt buộc' },
        { validator: isValidCategory, message: 'Danh mục không hợp lệ' }
      ],
      description: [
        { validator: (val) => !val || val.length <= 500, message: 'Mô tả không được quá 500 ký tự' }
      ]
    };

    return validateForm(data, rules);
  }
}

// Create singleton instance
export const transactionService = new TransactionService();
export default TransactionService;