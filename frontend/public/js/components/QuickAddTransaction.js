// Quick Add Transaction Component (< 10 seconds entry as per git instructions)
import { formatCurrency, parseCurrency } from '../utils/currencyHelpers.js';
import { CATEGORIES, CATEGORY_NAMES, CATEGORY_COLORS, TRANSACTION_TYPES } from '../constants/categories.js';
import { transactionService } from '../services/transactionService.js';
import { storageService } from '../services/storageService.js';

class QuickAddTransaction {
  constructor(container, options = {}) {
    this.container = container;
    this.options = {
      autoFocus: true,
      showAdvanced: false,
      defaultType: 'expense',
      onSuccess: null,
      onError: null,
      ...options
    };
    
    this.currentStep = 1;
    this.formData = {
      amount: '',
      type: this.options.defaultType,
      category: '',
      description: '',
      date: new Date().toISOString().split('T')[0],
      paymentMethod: 'cash'
    };
    
    this.init();
  }

  init() {
    this.render();
    this.attachEventListeners();
    
    if (this.options.autoFocus) {
      setTimeout(() => {
        this.focusAmountInput();
      }, 100);
    }
  }

  render() {
    this.container.innerHTML = `
      <div class="quick-add-transaction">
        <div class="quick-add-header">
          <h3>Thêm giao dịch nhanh</h3>
          <div class="step-indicator">
            <span class="step ${this.currentStep >= 1 ? 'active' : ''}" data-step="1">1</span>
            <span class="step ${this.currentStep >= 2 ? 'active' : ''}" data-step="2">2</span>
            <span class="step ${this.currentStep >= 3 ? 'active' : ''}" data-step="3">3</span>
          </div>
        </div>

        <form class="quick-add-form" id="quick-add-form">
          ${this.renderCurrentStep()}
        </form>

        <div class="quick-add-actions">
          ${this.currentStep > 1 ? '<button type="button" class="btn-secondary" id="btn-previous">Quay lại</button>' : ''}
          <button type="button" class="btn-primary" id="btn-next">
            ${this.currentStep === 3 ? 'Lưu giao dịch' : 'Tiếp theo'}
          </button>
        </div>

        ${this.options.showAdvanced ? '<div class="advanced-toggle"><button type="button" id="btn-toggle-advanced">Tùy chọn nâng cao</button></div>' : ''}
      </div>
    `;

    this.renderStyles();
  }

  renderCurrentStep() {
    switch (this.currentStep) {
      case 1:
        return this.renderAmountStep();
      case 2:
        return this.renderCategoryStep();
      case 3:
        return this.renderDetailsStep();
      default:
        return '';
    }
  }

  renderAmountStep() {
    return `
      <div class="step-content step-amount">
        <div class="type-selector">
          <button type="button" class="type-btn ${this.formData.type === 'expense' ? 'active' : ''}" data-type="expense">
            Chi tiêu
          </button>
          <button type="button" class="type-btn ${this.formData.type === 'income' ? 'active' : ''}" data-type="income">
            Thu nhập
          </button>
        </div>

        <div class="amount-input-group">
          <label for="amount-input">Số tiền</label>
          <div class="amount-input-container">
            <input 
              type="text" 
              id="amount-input" 
              class="amount-input" 
              placeholder="0"
              value="${this.formData.amount}"
              inputmode="numeric"
            >
            <span class="currency-symbol">₫</span>
          </div>
          <div class="amount-formatted" id="amount-formatted">
            ${this.formData.amount ? formatCurrency(parseCurrency(this.formData.amount)) : ''}
          </div>
        </div>

        <div class="quick-amounts">
          <button type="button" class="quick-amount-btn" data-amount="50000">50k</button>
          <button type="button" class="quick-amount-btn" data-amount="100000">100k</button>
          <button type="button" class="quick-amount-btn" data-amount="200000">200k</button>
          <button type="button" class="quick-amount-btn" data-amount="500000">500k</button>
        </div>
      </div>
    `;
  }

  renderCategoryStep() {
    return `
      <div class="step-content step-category">
        <label>Chọn danh mục</label>
        <div class="category-grid">
          ${Object.entries(CATEGORY_NAMES).map(([key, name]) => `
            <button 
              type="button" 
              class="category-btn ${this.formData.category === key ? 'active' : ''}" 
              data-category="${key}"
              style="--category-color: ${CATEGORY_COLORS[key]}"
            >
              <div class="category-color" style="background-color: ${CATEGORY_COLORS[key]}"></div>
              <span class="category-name">${name}</span>
            </button>
          `).join('')}
        </div>
      </div>
    `;
  }

  renderDetailsStep() {
    return `
      <div class="step-content step-details">
        <div class="form-group">
          <label for="description-input">Mô tả (tùy chọn)</label>
          <input 
            type="text" 
            id="description-input" 
            placeholder="Ví dụ: Ăn trưa, Lương tháng 11..."
            value="${this.formData.description}"
            maxlength="500"
          >
        </div>

        <div class="form-group">
          <label for="date-input">Ngày</label>
          <input 
            type="date" 
            id="date-input" 
            value="${this.formData.date}"
          >
        </div>

        <div class="form-group">
          <label for="payment-method">Phương thức thanh toán</label>
          <select id="payment-method">
            <option value="cash" ${this.formData.paymentMethod === 'cash' ? 'selected' : ''}>Tiền mặt</option>
            <option value="card" ${this.formData.paymentMethod === 'card' ? 'selected' : ''}>Thẻ ngân hàng</option>
            <option value="e-wallet" ${this.formData.paymentMethod === 'e-wallet' ? 'selected' : ''}>Ví điện tử</option>
          </select>
        </div>

        <div class="transaction-summary">
          <div class="summary-row">
            <span>Loại:</span>
            <span class="value ${this.formData.type}">${this.formData.type === 'income' ? 'Thu nhập' : 'Chi tiêu'}</span>
          </div>
          <div class="summary-row">
            <span>Số tiền:</span>
            <span class="value amount">${formatCurrency(parseCurrency(this.formData.amount))}</span>
          </div>
          <div class="summary-row">
            <span>Danh mục:</span>
            <span class="value">${CATEGORY_NAMES[this.formData.category] || 'Chưa chọn'}</span>
          </div>
        </div>
      </div>
    `;
  }

  attachEventListeners() {
    // Next/Previous buttons
    const btnNext = document.getElementById('btn-next');
    const btnPrevious = document.getElementById('btn-previous');

    btnNext?.addEventListener('click', () => this.handleNext());
    btnPrevious?.addEventListener('click', () => this.handlePrevious());

    // Step-specific listeners
    this.attachStepListeners();

    // Form submission
    document.getElementById('quick-add-form').addEventListener('submit', (e) => {
      e.preventDefault();
      this.handleNext();
    });

    // Keyboard shortcuts
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        this.handleNext();
      } else if (e.key === 'Escape') {
        this.reset();
      }
    });
  }

  attachStepListeners() {
    switch (this.currentStep) {
      case 1:
        this.attachAmountStepListeners();
        break;
      case 2:
        this.attachCategoryStepListeners();
        break;
      case 3:
        this.attachDetailsStepListeners();
        break;
    }
  }

  attachAmountStepListeners() {
    // Type selector
    document.querySelectorAll('.type-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        this.formData.type = btn.dataset.type;
        this.updateTypeButtons();
      });
    });

    // Amount input
    const amountInput = document.getElementById('amount-input');
    amountInput?.addEventListener('input', (e) => {
      this.formData.amount = e.target.value;
      this.updateAmountFormatted();
    });

    // Auto-focus amount input
    setTimeout(() => this.focusAmountInput(), 50);

    // Quick amount buttons
    document.querySelectorAll('.quick-amount-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        this.formData.amount = btn.dataset.amount;
        amountInput.value = this.formData.amount;
        this.updateAmountFormatted();
      });
    });

    // Number formatting on blur
    amountInput?.addEventListener('blur', () => {
      if (this.formData.amount) {
        const parsed = parseCurrency(this.formData.amount);
        this.formData.amount = parsed.toString();
        amountInput.value = parsed.toString();
        this.updateAmountFormatted();
      }
    });
  }

  attachCategoryStepListeners() {
    document.querySelectorAll('.category-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        this.formData.category = btn.dataset.category;
        this.updateCategoryButtons();
        
        // Auto-advance after category selection (< 10 seconds goal)
        setTimeout(() => {
          if (this.formData.category) {
            this.handleNext();
          }
        }, 200);
      });
    });
  }

  attachDetailsStepListeners() {
    // Description input
    const descInput = document.getElementById('description-input');
    descInput?.addEventListener('input', (e) => {
      this.formData.description = e.target.value;
    });

    // Date input
    const dateInput = document.getElementById('date-input');
    dateInput?.addEventListener('change', (e) => {
      this.formData.date = e.target.value;
    });

    // Payment method
    const paymentSelect = document.getElementById('payment-method');
    paymentSelect?.addEventListener('change', (e) => {
      this.formData.paymentMethod = e.target.value;
    });
  }

  updateTypeButtons() {
    document.querySelectorAll('.type-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.type === this.formData.type);
    });
  }

  updateAmountFormatted() {
    const formatted = document.getElementById('amount-formatted');
    if (formatted && this.formData.amount) {
      const parsed = parseCurrency(this.formData.amount);
      formatted.textContent = parsed > 0 ? formatCurrency(parsed) : '';
    } else if (formatted) {
      formatted.textContent = '';
    }
  }

  updateCategoryButtons() {
    document.querySelectorAll('.category-btn').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.category === this.formData.category);
    });
  }

  focusAmountInput() {
    const input = document.getElementById('amount-input');
    if (input) {
      input.focus();
      input.select();
    }
  }

  async handleNext() {
    if (this.currentStep === 1) {
      if (!this.validateAmount()) return;
      this.currentStep = 2;
      this.render();
    } else if (this.currentStep === 2) {
      if (!this.validateCategory()) return;
      this.currentStep = 3;
      this.render();
    } else if (this.currentStep === 3) {
      await this.saveTransaction();
    }
  }

  handlePrevious() {
    if (this.currentStep > 1) {
      this.currentStep--;
      this.render();
    }
  }

  validateAmount() {
    const parsed = parseCurrency(this.formData.amount);
    if (!parsed || parsed <= 0) {
      this.showError('Vui lòng nhập số tiền hợp lệ');
      return false;
    }
    
    if (parsed > 1000000000) {
      this.showError('Số tiền không được vượt quá 1 tỷ VND');
      return false;
    }
    
    return true;
  }

  validateCategory() {
    if (!this.formData.category) {
      this.showError('Vui lòng chọn danh mục');
      return false;
    }
    return true;
  }

  async saveTransaction() {
    try {
      this.showLoading();

      const transactionData = {
        ...this.formData,
        amount: parseCurrency(this.formData.amount)
      };

      // Try to save online first
      let result;
      try {
        result = await transactionService.createTransaction(transactionData);
      } catch (error) {
        // If offline, save to local storage
        if (!navigator.onLine) {
          result = this.saveOffline(transactionData);
        } else {
          throw error;
        }
      }

      this.showSuccess();
      this.options.onSuccess?.(result);
      
      // Auto-reset after success
      setTimeout(() => {
        this.reset();
      }, 1500);

    } catch (error) {
      console.error('Failed to save transaction:', error);
      this.showError(error.message || 'Không thể lưu giao dịch');
      this.options.onError?.(error);
    }
  }

  saveOffline(transactionData) {
    const transaction = {
      id: Date.now().toString(),
      ...transactionData,
      _offline: true,
      createdAt: new Date().toISOString()
    };

    // Save to local storage
    storageService.addCachedTransaction(transaction);
    
    // Add to offline sync queue
    storageService.addToOfflineQueue({
      type: 'CREATE_TRANSACTION',
      data: transactionData
    });

    return transaction;
  }

  showLoading() {
    const btnNext = document.getElementById('btn-next');
    if (btnNext) {
      btnNext.textContent = 'Đang lưu...';
      btnNext.disabled = true;
    }
  }

  showSuccess() {
    this.container.innerHTML = `
      <div class="success-state">
        <div class="success-animation"></div>
        <h3>Đã lưu thành công!</h3>
        <p>${this.formData.type === 'income' ? 'Thu nhập' : 'Chi tiêu'} ${formatCurrency(parseCurrency(this.formData.amount))} đã được thêm</p>
      </div>
    `;
  }

  showError(message) {
    // Create temporary error message
    let errorEl = document.querySelector('.error-message');
    if (!errorEl) {
      errorEl = document.createElement('div');
      errorEl.className = 'error-message';
      this.container.appendChild(errorEl);
    }
    
    errorEl.textContent = message;
    errorEl.style.display = 'block';
    
    // Auto-hide after 3 seconds
    setTimeout(() => {
      if (errorEl) {
        errorEl.style.display = 'none';
      }
    }, 3000);
  }

  reset() {
    this.currentStep = 1;
    this.formData = {
      amount: '',
      type: this.options.defaultType,
      category: '',
      description: '',
      date: new Date().toISOString().split('T')[0],
      paymentMethod: 'cash'
    };
    
    this.render();
  }

  // Public methods
  setType(type) {
    this.formData.type = type;
    if (this.currentStep === 1) {
      this.updateTypeButtons();
    }
  }

  setAmount(amount) {
    this.formData.amount = amount.toString();
    const input = document.getElementById('amount-input');
    if (input) {
      input.value = this.formData.amount;
      this.updateAmountFormatted();
    }
  }

  setCategory(category) {
    this.formData.category = category;
    if (this.currentStep === 2) {
      this.updateCategoryButtons();
    }
  }

  renderStyles() {
    if (document.getElementById('quick-add-transaction-styles')) return;

    const style = document.createElement('style');
    style.id = 'quick-add-transaction-styles';
    style.textContent = `
      .quick-add-transaction {
        max-width: 400px;
        margin: 0 auto;
        background: white;
        border-radius: 12px;
        padding: 1.5rem;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      }

      .quick-add-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1.5rem;
      }

      .quick-add-header h3 {
        margin: 0;
        color: #111827;
        font-size: 1.25rem;
      }

      .step-indicator {
        display: flex;
        gap: 0.5rem;
      }

      .step {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        background: #e5e7eb;
        color: #6b7280;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 0.75rem;
        font-weight: 600;
        transition: all 0.2s;
      }

      .step.active {
        background: #3b82f6;
        color: white;
      }

      .step-content {
        min-height: 200px;
      }

      .type-selector {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 0.5rem;
        margin-bottom: 1.5rem;
      }

      .type-btn {
        padding: 0.75rem;
        border: 2px solid #e5e7eb;
        background: white;
        border-radius: 8px;
        cursor: pointer;
        font-weight: 500;
        transition: all 0.2s;
      }

      .type-btn:hover {
        border-color: #3b82f6;
      }

      .type-btn.active {
        border-color: #3b82f6;
        background: #eff6ff;
        color: #3b82f6;
      }

      .amount-input-group label {
        display: block;
        margin-bottom: 0.5rem;
        font-weight: 500;
        color: #374151;
      }

      .amount-input-container {
        position: relative;
        margin-bottom: 0.5rem;
      }

      .amount-input {
        width: 100%;
        padding: 1rem 2rem 1rem 1rem;
        font-size: 1.5rem;
        font-weight: 600;
        border: 2px solid #e5e7eb;
        border-radius: 8px;
        text-align: center;
        box-sizing: border-box;
      }

      .amount-input:focus {
        outline: none;
        border-color: #3b82f6;
        box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
      }

      .currency-symbol {
        position: absolute;
        right: 1rem;
        top: 50%;
        transform: translateY(-50%);
        font-size: 1.25rem;
        color: #6b7280;
        font-weight: 600;
      }

      .amount-formatted {
        text-align: center;
        color: #6b7280;
        font-size: 0.875rem;
        min-height: 1.25rem;
      }

      .quick-amounts {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 0.5rem;
        margin-top: 1rem;
      }

      .quick-amount-btn {
        padding: 0.5rem;
        border: 1px solid #d1d5db;
        background: white;
        border-radius: 6px;
        cursor: pointer;
        font-size: 0.875rem;
        transition: all 0.2s;
      }

      .quick-amount-btn:hover {
        background: #f3f4f6;
        border-color: #9ca3af;
      }

      .category-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 0.75rem;
        margin-top: 1rem;
      }

      .category-btn {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 1rem;
        border: 2px solid #e5e7eb;
        background: white;
        border-radius: 8px;
        cursor: pointer;
        text-align: left;
        transition: all 0.2s;
      }

      .category-btn:hover {
        border-color: var(--category-color, #3b82f6);
        transform: translateY(-1px);
      }

      .category-btn.active {
        border-color: var(--category-color, #3b82f6);
        background: color-mix(in srgb, var(--category-color, #3b82f6) 10%, white);
      }

      .category-color {
        width: 16px;
        height: 16px;
        border-radius: 50%;
        flex-shrink: 0;
      }

      .category-name {
        font-weight: 500;
        color: #374151;
        font-size: 0.875rem;
      }

      .form-group {
        margin-bottom: 1rem;
      }

      .form-group label {
        display: block;
        margin-bottom: 0.5rem;
        font-weight: 500;
        color: #374151;
        font-size: 0.875rem;
      }

      .form-group input,
      .form-group select {
        width: 100%;
        padding: 0.75rem;
        border: 1px solid #d1d5db;
        border-radius: 6px;
        font-size: 0.875rem;
        box-sizing: border-box;
      }

      .form-group input:focus,
      .form-group select:focus {
        outline: none;
        border-color: #3b82f6;
        box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
      }

      .transaction-summary {
        margin-top: 1.5rem;
        padding: 1rem;
        background: #f9fafb;
        border-radius: 8px;
        border: 1px solid #e5e7eb;
      }

      .summary-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.5rem;
      }

      .summary-row:last-child {
        margin-bottom: 0;
      }

      .summary-row .value {
        font-weight: 600;
      }

      .summary-row .value.income {
        color: #10b981;
      }

      .summary-row .value.expense {
        color: #ef4444;
      }

      .summary-row .value.amount {
        font-size: 1.125rem;
      }

      .quick-add-actions {
        display: flex;
        gap: 0.75rem;
        margin-top: 1.5rem;
      }

      .btn-secondary {
        flex: 1;
        padding: 0.75rem 1.5rem;
        border: 1px solid #d1d5db;
        background: white;
        color: #374151;
        border-radius: 6px;
        cursor: pointer;
        font-weight: 500;
        transition: all 0.2s;
      }

      .btn-secondary:hover {
        background: #f3f4f6;
      }

      .btn-primary {
        flex: 2;
        padding: 0.75rem 1.5rem;
        background: #3b82f6;
        color: white;
        border: none;
        border-radius: 6px;
        cursor: pointer;
        font-weight: 500;
        transition: all 0.2s;
      }

      .btn-primary:hover {
        background: #2563eb;
      }

      .btn-primary:disabled {
        background: #9ca3af;
        cursor: not-allowed;
      }

      .success-state {
        text-align: center;
        padding: 2rem 1rem;
      }

      .success-animation {
        font-size: 3rem;
        margin-bottom: 1rem;
        animation: success-bounce 0.6s ease-out;
      }

      @keyframes success-bounce {
        0% { transform: scale(0); }
        50% { transform: scale(1.2); }
        100% { transform: scale(1); }
      }

      .success-state h3 {
        color: #10b981;
        margin: 0 0 0.5rem 0;
      }

      .success-state p {
        color: #6b7280;
        margin: 0;
      }

      .error-message {
        display: none;
        background: #fef2f2;
        border: 1px solid #fecaca;
        color: #dc2626;
        padding: 0.75rem;
        border-radius: 6px;
        margin-top: 1rem;
        font-size: 0.875rem;
      }

      .advanced-toggle {
        margin-top: 1rem;
        text-align: center;
      }

      .advanced-toggle button {
        background: none;
        border: none;
        color: #6b7280;
        font-size: 0.875rem;
        cursor: pointer;
        text-decoration: underline;
      }

      @media (max-width: 480px) {
        .quick-add-transaction {
          margin: 1rem;
          padding: 1rem;
        }

        .category-grid {
          grid-template-columns: 1fr;
        }

        .quick-amounts {
          grid-template-columns: repeat(2, 1fr);
        }

        .amount-input {
          font-size: 1.25rem;
        }

        .quick-add-actions {
          flex-direction: column;
        }
      }
    `;
    
    document.head.appendChild(style);
  }
}

export default QuickAddTransaction;