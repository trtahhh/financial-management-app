// Advanced Transaction Features Extension
import { formatCurrency } from '../utils/currencyHelpers.js';
import { formatDate } from '../utils/dateHelpers.js';
import { validateTransaction } from '../utils/validators.js';
import { CATEGORIES, CATEGORY_NAMES } from '../constants/categories.js';

class AdvancedTransactionFeatures {
  constructor(transactionService, options = {}) {
    this.transactionService = transactionService;
    this.options = {
      enableBulkOperations: true,
      enableTemplates: true,
      enableRecurring: true,
      enableAdvancedFilters: true,
      enableExportImport: true,
      enableVoiceInput: false,
      enableReceiptScanning: false,
      ...options
    };
    
    this.templates = [];
    this.recurringTransactions = [];
    this.bulkSelection = new Set();
    
    this.init();
  }

  init() {
    this.loadTemplates();
    this.loadRecurringTransactions();
    this.setupVoiceRecognition();
    this.setupKeyboardShortcuts();
  }

  // Bulk Operations
  enableBulkMode() {
    document.querySelectorAll('.transaction-item').forEach(item => {
      if (!item.querySelector('.bulk-checkbox')) {
        const checkbox = document.createElement('div');
        checkbox.className = 'bulk-checkbox';
        checkbox.innerHTML = '<input type="checkbox" class="bulk-select">';
        item.insertBefore(checkbox, item.firstChild);
        
        checkbox.addEventListener('change', (e) => {
          const transactionId = item.dataset.transactionId;
          if (e.target.checked) {
            this.bulkSelection.add(transactionId);
          } else {
            this.bulkSelection.delete(transactionId);
          }
          this.updateBulkActionsBar();
        });
      }
    });
    
    this.showBulkActionsBar();
  }

  showBulkActionsBar() {
    const existingBar = document.getElementById('bulk-actions-bar');
    if (existingBar) existingBar.remove();

    const bulkBar = document.createElement('div');
    bulkBar.id = 'bulk-actions-bar';
    bulkBar.className = 'bulk-actions-bar';
    bulkBar.innerHTML = `
      <div class="bulk-actions-content">
        <div class="bulk-info">
          <span id="bulk-count">0</span> giao dịch đã chọn
        </div>
        <div class="bulk-actions">
          <button class="btn-bulk-action" onclick="this.bulkDelete()">
            Xóa nhiều
          </button>
          <button class="btn-bulk-action" onclick="this.bulkEdit()">
            Sửa hàng loạt
          </button>
          <button class="btn-bulk-action" onclick="this.bulkExport()">
            Xuất Excel
          </button>
          <button class="btn-bulk-action" onclick="this.bulkDuplicate()">
            Nhân bản
          </button>
          <button class="btn-bulk-action secondary" onclick="this.disableBulkMode()">
            Hủy
          </button>
        </div>
      </div>
    `;
    
    document.body.appendChild(bulkBar);
  }

  updateBulkActionsBar() {
    const countEl = document.getElementById('bulk-count');
    if (countEl) {
      countEl.textContent = this.bulkSelection.size;
    }
  }

  async bulkDelete() {
    if (this.bulkSelection.size === 0) return;
    
    const confirmed = confirm(`Bạn có chắc muốn xóa ${this.bulkSelection.size} giao dịch?`);
    if (!confirmed) return;

    try {
      await Promise.all(
        Array.from(this.bulkSelection).map(id => 
          this.transactionService.deleteTransaction(id)
        )
      );
      
      this.bulkSelection.clear();
      this.disableBulkMode();
      this.onBulkOperationComplete?.('delete', this.bulkSelection.size);
      
      // Show success message
      this.showNotification(`Đã xóa ${this.bulkSelection.size} giao dịch thành công!`, 'success');
    } catch (error) {
      this.showNotification('Có lỗi xảy ra khi xóa giao dịch', 'error');
    }
  }

  async bulkEdit() {
    if (this.bulkSelection.size === 0) return;
    
    this.showBulkEditModal();
  }

  showBulkEditModal() {
    const modal = document.createElement('div');
    modal.className = 'modal-overlay';
    modal.innerHTML = `
      <div class="modal bulk-edit-modal">
        <div class="modal-header">
          <h3>Sửa ${this.bulkSelection.size} giao dịch</h3>
          <button class="modal-close">&times;</button>
        </div>
        
        <div class="modal-content">
          <div class="bulk-edit-field">
            <label>
              <input type="checkbox" id="edit-category"> Thay đổi danh mục
            </label>
            <select id="new-category" disabled>
              ${Object.entries(CATEGORY_NAMES).map(([key, name]) => 
                `<option value="${key}">${name}</option>`
              ).join('')}
            </select>
          </div>
          
          <div class="bulk-edit-field">
            <label>
              <input type="checkbox" id="edit-description"> Thay đổi mô tả
            </label>
            <input type="text" id="new-description" placeholder="Mô tả mới" disabled>
          </div>
          
          <div class="bulk-edit-field">
            <label>
              <input type="checkbox" id="edit-tags"> Thêm tags
            </label>
            <input type="text" id="new-tags" placeholder="tag1, tag2, tag3" disabled>
          </div>
          
          <div class="bulk-edit-field">
            <label>
              <input type="checkbox" id="adjust-amount"> Điều chỉnh số tiền
            </label>
            <div class="adjustment-controls" style="display: none;">
              <select id="adjustment-type">
                <option value="add">Cộng thêm</option>
                <option value="subtract">Trừ bớt</option>
                <option value="multiply">Nhân với</option>
                <option value="divide">Chia cho</option>
              </select>
              <input type="number" id="adjustment-value" placeholder="Giá trị">
            </div>
          </div>
        </div>
        
        <div class="modal-actions">
          <button class="btn-primary" onclick="this.executeBulkEdit()">Áp dụng</button>
          <button class="btn-secondary" onclick="this.closeBulkEditModal()">Hủy</button>
        </div>
      </div>
    `;
    
    document.body.appendChild(modal);
    this.setupBulkEditModal(modal);
  }

  // Transaction Templates
  async saveAsTemplate(transaction) {
    const templateName = prompt('Tên template:');
    if (!templateName) return;
    
    const template = {
      id: Date.now().toString(),
      name: templateName,
      category: transaction.category,
      description: transaction.description,
      amount: transaction.amount,
      tags: transaction.tags || [],
      createdAt: new Date().toISOString()
    };
    
    this.templates.push(template);
    await this.saveTemplates();
    
    this.showNotification(`Đã lưu template "${templateName}"`, 'success');
  }

  showTemplateSelector() {
    if (this.templates.length === 0) {
      this.showNotification('Chưa có template nào. Tạo template từ giao dịch hiện có!', 'info');
      return;
    }

    const modal = document.createElement('div');
    modal.className = 'modal-overlay';
    modal.innerHTML = `
      <div class="modal template-modal">
        <div class="modal-header">
          <h3>Chọn Template</h3>
          <button class="modal-close">&times;</button>
        </div>
        
        <div class="modal-content">
          <div class="template-list">
            ${this.templates.map(template => `
              <div class="template-item" data-template-id="${template.id}">
                <div class="template-info">
                  <h4>${template.name}</h4>
                  <p>${CATEGORY_NAMES[template.category]} - ${formatCurrency(template.amount)}</p>
                  <p class="template-description">${template.description}</p>
                </div>
                <div class="template-actions">
                  <button class="btn-use-template" onclick="this.useTemplate('${template.id}')">
                    Sử dụng
                  </button>
                  <button class="btn-edit-template" onclick="this.editTemplate('${template.id}')">
                    Sửa
                  </button>
                  <button class="btn-delete-template" onclick="this.deleteTemplate('${template.id}')">
                    Xóa
                  </button>
                </div>
              </div>
            `).join('')}
          </div>
        </div>
      </div>
    `;
    
    document.body.appendChild(modal);
  }

  useTemplate(templateId) {
    const template = this.templates.find(t => t.id === templateId);
    if (!template) return;
    
    this.onUseTemplate?.(template);
    document.querySelector('.modal-overlay').remove();
  }

  // Recurring Transactions
  setupRecurringTransaction(transaction, frequency) {
    const recurring = {
      id: Date.now().toString(),
      ...transaction,
      frequency: frequency, // daily, weekly, monthly, yearly
      nextDate: this.calculateNextDate(new Date(), frequency),
      isActive: true,
      createdAt: new Date().toISOString()
    };
    
    this.recurringTransactions.push(recurring);
    this.saveRecurringTransactions();
    
    this.showNotification(`Đã tạo giao dịch định kỳ ${frequency}`, 'success');
  }

  calculateNextDate(lastDate, frequency) {
    const next = new Date(lastDate);
    
    switch (frequency) {
      case 'daily':
        next.setDate(next.getDate() + 1);
        break;
      case 'weekly':
        next.setDate(next.getDate() + 7);
        break;
      case 'monthly':
        next.setMonth(next.getMonth() + 1);
        break;
      case 'yearly':
        next.setFullYear(next.getFullYear() + 1);
        break;
    }
    
    return next.toISOString();
  }

  async processRecurringTransactions() {
    const now = new Date();
    const dueTransactions = this.recurringTransactions.filter(rt => 
      rt.isActive && new Date(rt.nextDate) <= now
    );
    
    for (const recurring of dueTransactions) {
      try {
        // Create new transaction
        const newTransaction = {
          ...recurring,
          id: undefined,
          date: now.toISOString(),
          isRecurring: true,
          recurringId: recurring.id
        };
        
        await this.transactionService.createTransaction(newTransaction);
        
        // Update next date
        recurring.nextDate = this.calculateNextDate(now, recurring.frequency);
        
        this.showNotification(`Đã tạo giao dịch định kỳ: ${recurring.description}`, 'info');
      } catch (error) {
        console.error('Error processing recurring transaction:', error);
      }
    }
    
    if (dueTransactions.length > 0) {
      await this.saveRecurringTransactions();
    }
  }

  // Advanced Filters
  setupAdvancedFilters() {
    return {
      dateRange: {
        start: null,
        end: null,
        preset: 'all' // today, week, month, year, custom
      },
      amountRange: {
        min: null,
        max: null
      },
      categories: [],
      tags: [],
      description: '',
      type: 'all', // income, expense, all
      paymentMethod: [],
      recurring: 'all', // true, false, all
      sortBy: 'date', // date, amount, category, description
      sortOrder: 'desc' // asc, desc
    };
  }

  applyAdvancedFilters(transactions, filters) {
    return transactions.filter(transaction => {
      // Date range filter
      if (filters.dateRange.start && new Date(transaction.date) < new Date(filters.dateRange.start)) {
        return false;
      }
      if (filters.dateRange.end && new Date(transaction.date) > new Date(filters.dateRange.end)) {
        return false;
      }
      
      // Amount range filter
      if (filters.amountRange.min && transaction.amount < filters.amountRange.min) {
        return false;
      }
      if (filters.amountRange.max && transaction.amount > filters.amountRange.max) {
        return false;
      }
      
      // Category filter
      if (filters.categories.length > 0 && !filters.categories.includes(transaction.category)) {
        return false;
      }
      
      // Tags filter
      if (filters.tags.length > 0) {
        const transactionTags = transaction.tags || [];
        if (!filters.tags.some(tag => transactionTags.includes(tag))) {
          return false;
        }
      }
      
      // Description filter
      if (filters.description && !transaction.description.toLowerCase().includes(filters.description.toLowerCase())) {
        return false;
      }
      
      // Type filter
      if (filters.type !== 'all') {
        const isExpense = transaction.amount > 0;
        if (filters.type === 'expense' && !isExpense) return false;
        if (filters.type === 'income' && isExpense) return false;
      }
      
      // Recurring filter
      if (filters.recurring !== 'all') {
        const isRecurring = !!transaction.recurringId;
        if (filters.recurring === 'true' && !isRecurring) return false;
        if (filters.recurring === 'false' && isRecurring) return false;
      }
      
      return true;
    });
  }

  // Export/Import Functions
  async exportToExcel(transactions = null) {
    const data = transactions || await this.transactionService.getAllTransactions();
    
    const csvContent = this.convertToCSV(data);
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    
    link.href = URL.createObjectURL(blob);
    link.download = `transactions_${formatDate(new Date(), 'YYYY-MM-DD')}.csv`;
    link.click();
    
    this.showNotification('Đã xuất file Excel thành công!', 'success');
  }

  convertToCSV(transactions) {
    const headers = ['Ngày', 'Danh mục', 'Mô tả', 'Số tiền', 'Tags', 'Định kỳ'];
    const rows = transactions.map(t => [
      formatDate(new Date(t.date)),
      CATEGORY_NAMES[t.category] || t.category,
      t.description || '',
      t.amount,
      (t.tags || []).join('; '),
      t.recurringId ? 'Có' : 'Không'
    ]);
    
    const csvRows = [headers, ...rows];
    return csvRows.map(row => 
      row.map(field => `"${String(field).replace(/"/g, '""')}"`).join(',')
    ).join('\n');
  }

  async importFromFile(file) {
    try {
      const text = await file.text();
      const transactions = this.parseCSV(text);
      
      let imported = 0;
      let errors = [];
      
      for (const transaction of transactions) {
        try {
          const validation = validateTransaction(transaction);
          if (validation.isValid) {
            await this.transactionService.createTransaction(transaction);
            imported++;
          } else {
            errors.push(`Dòng ${imported + errors.length + 1}: ${validation.errors.join(', ')}`);
          }
        } catch (error) {
          errors.push(`Dòng ${imported + errors.length + 1}: ${error.message}`);
        }
      }
      
      this.showImportResults(imported, errors);
    } catch (error) {
      this.showNotification('Lỗi đọc file: ' + error.message, 'error');
    }
  }

  parseCSV(text) {
    const lines = text.split('\n');
    const headers = lines[0].split(',').map(h => h.replace(/"/g, ''));
    const transactions = [];
    
    for (let i = 1; i < lines.length; i++) {
      if (!lines[i].trim()) continue;
      
      const values = lines[i].split(',').map(v => v.replace(/"/g, ''));
      const transaction = {
        date: new Date(values[0]).toISOString(),
        category: this.findCategoryByName(values[1]),
        description: values[2],
        amount: parseFloat(values[3]) || 0,
        tags: values[4] ? values[4].split('; ') : [],
        id: Date.now().toString() + Math.random()
      };
      
      transactions.push(transaction);
    }
    
    return transactions;
  }

  findCategoryByName(name) {
    const entry = Object.entries(CATEGORY_NAMES).find(([key, value]) => value === name);
    return entry ? entry[0] : 'other';
  }

  // Voice Input (if enabled)
  setupVoiceRecognition() {
    if (!this.options.enableVoiceInput || !('webkitSpeechRecognition' in window)) {
      return;
    }
    
    this.recognition = new webkitSpeechRecognition();
    this.recognition.lang = 'vi-VN';
    this.recognition.continuous = false;
    this.recognition.interimResults = false;
    
    this.recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      this.parseVoiceInput(transcript);
    };
    
    this.recognition.onerror = (event) => {
      this.showNotification('Lỗi nhận dạng giọng nói: ' + event.error, 'error');
    };
  }

  startVoiceInput() {
    if (this.recognition) {
      this.recognition.start();
      this.showNotification('Đang nghe... Hãy nói giao dịch của bạn', 'info');
    }
  }

  parseVoiceInput(text) {
    // Parse voice input like "Chi 50000 đồng mua cafe"
    const amountMatch = text.match(/(\d+(?:\.\d+)?)\s*(?:đồng|vnđ|k)?/i);
    const amount = amountMatch ? parseFloat(amountMatch[1]) : null;
    
    let category = 'other';
    let description = text;
    
    // Simple category detection
    if (text.includes('ăn') || text.includes('uống') || text.includes('cafe') || text.includes('cơm')) {
      category = 'food';
    } else if (text.includes('xe') || text.includes('taxi') || text.includes('grab')) {
      category = 'transport';
    } else if (text.includes('mua') || text.includes('shopping')) {
      category = 'shopping';
    }
    
    if (amount) {
      this.onVoiceTransaction?.({
        amount,
        category,
        description,
        date: new Date().toISOString()
      });
    } else {
      this.showNotification('Không nhận dạng được số tiền. Vui lòng thử lại.', 'warning');
    }
  }

  // Keyboard Shortcuts
  setupKeyboardShortcuts() {
    document.addEventListener('keydown', (e) => {
      // Ctrl/Cmd + N: Quick add transaction
      if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
        e.preventDefault();
        this.onQuickAdd?.();
      }
      
      // Ctrl/Cmd + B: Toggle bulk mode
      if ((e.ctrlKey || e.metaKey) && e.key === 'b') {
        e.preventDefault();
        this.toggleBulkMode();
      }
      
      // Ctrl/Cmd + T: Show templates
      if ((e.ctrlKey || e.metaKey) && e.key === 't') {
        e.preventDefault();
        this.showTemplateSelector();
      }
      
      // Ctrl/Cmd + E: Export
      if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
        e.preventDefault();
        this.exportToExcel();
      }
      
      // V: Start voice input
      if (e.key === 'v' && !e.target.matches('input, textarea')) {
        e.preventDefault();
        this.startVoiceInput();
      }
    });
  }

  toggleBulkMode() {
    const bulkBar = document.getElementById('bulk-actions-bar');
    if (bulkBar) {
      this.disableBulkMode();
    } else {
      this.enableBulkMode();
    }
  }

  disableBulkMode() {
    document.querySelectorAll('.bulk-checkbox').forEach(cb => cb.remove());
    document.getElementById('bulk-actions-bar')?.remove();
    this.bulkSelection.clear();
  }

  // Utility Methods
  async saveTemplates() {
    localStorage.setItem('transaction_templates', JSON.stringify(this.templates));
  }

  async loadTemplates() {
    const saved = localStorage.getItem('transaction_templates');
    this.templates = saved ? JSON.parse(saved) : [];
  }

  async saveRecurringTransactions() {
    localStorage.setItem('recurring_transactions', JSON.stringify(this.recurringTransactions));
  }

  async loadRecurringTransactions() {
    const saved = localStorage.getItem('recurring_transactions');
    this.recurringTransactions = saved ? JSON.parse(saved) : [];
  }

  showNotification(message, type = 'info') {
    // Integration with existing notification system
    if (window.showNotification) {
      window.showNotification(message, type);
    } else {
      console.log(`${type.toUpperCase()}: ${message}`);
    }
  }

  showImportResults(imported, errors) {
    const modal = document.createElement('div');
    modal.className = 'modal-overlay';
    modal.innerHTML = `
      <div class="modal import-results-modal">
        <div class="modal-header">
          <h3>Kết quả Import</h3>
          <button class="modal-close">&times;</button>
        </div>
        
        <div class="modal-content">
          <div class="import-summary">
            <p class="success">✅ Đã import thành công: ${imported} giao dịch</p>
            ${errors.length > 0 ? `<p class="error">❌ Lỗi: ${errors.length} dòng</p>` : ''}
          </div>
          
          ${errors.length > 0 ? `
            <div class="error-details">
              <h4>Chi tiết lỗi:</h4>
              <ul>
                ${errors.map(error => `<li>${error}</li>`).join('')}
              </ul>
            </div>
          ` : ''}
        </div>
        
        <div class="modal-actions">
          <button class="btn-primary" onclick="this.remove()">OK</button>
        </div>
      </div>
    `;
    
    document.body.appendChild(modal);
  }

  // Public API
  getFeatureStatus() {
    return {
      bulkOperations: this.options.enableBulkOperations,
      templates: this.options.enableTemplates,
      recurring: this.options.enableRecurring,
      advancedFilters: this.options.enableAdvancedFilters,
      exportImport: this.options.enableExportImport,
      voiceInput: this.options.enableVoiceInput && 'webkitSpeechRecognition' in window,
      templatesCount: this.templates.length,
      recurringCount: this.recurringTransactions.filter(rt => rt.isActive).length
    };
  }

  // Event handlers (to be set by parent components)
  onBulkOperationComplete = null;
  onUseTemplate = null;
  onVoiceTransaction = null;
  onQuickAdd = null;
}

export default AdvancedTransactionFeatures;