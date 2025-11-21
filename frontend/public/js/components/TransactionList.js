// Transaction List Component
import { formatCurrency } from '../utils/currencyHelpers.js';
import { formatDate, getRelativeTime } from '../utils/dateHelpers.js';
import { CATEGORY_NAMES, CATEGORY_COLORS } from '../constants/categories.js';
import { COLORS } from '../constants/colors.js';

class TransactionList {
  constructor(container, options = {}) {
    this.container = container;
    this.options = {
      itemsPerPage: 20,
      showPagination: true,
      allowEdit: true,
      allowDelete: true,
      showFilters: true,
      groupByDate: true,
      ...options
    };
    
    this.transactions = [];
    this.filteredTransactions = [];
    this.currentPage = 1;
    this.filters = {
      type: 'all',
      category: 'all',
      dateRange: 'all',
      search: ''
    };
    
    this.init();
  }

  init() {
    this.render();
    this.attachEventListeners();
  }

  render() {
    this.container.innerHTML = `
      <div class="transaction-list">
        ${this.options.showFilters ? this.renderFilters() : ''}
        <div class="transaction-content">
          <div class="transaction-summary">
            <div class="summary-item income">
              <span class="label">Thu nhập</span>
              <span class="amount" id="total-income">0 ₫</span>
            </div>
            <div class="summary-item expense">
              <span class="label">Chi tiêu</span>
              <span class="amount" id="total-expense">0 ₫</span>
            </div>
            <div class="summary-item net">
              <span class="label">Còn lại</span>
              <span class="amount" id="net-amount">0 ₫</span>
            </div>
          </div>
          
          <div class="transaction-items" id="transaction-items">
            <div class="loading-state">
              <div class="loading-spinner"></div>
              <span>Đang tải...</span>
            </div>
          </div>
          
          ${this.options.showPagination ? '<div class="pagination" id="pagination"></div>' : ''}
        </div>
      </div>
    `;

    this.renderStyles();
  }

  renderFilters() {
    return `
      <div class="transaction-filters">
        <div class="filter-group">
          <label>Loại:</label>
          <select id="filter-type">
            <option value="all">Tất cả</option>
            <option value="income">Thu nhập</option>
            <option value="expense">Chi tiêu</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label>Danh mục:</label>
          <select id="filter-category">
            <option value="all">Tất cả</option>
            ${Object.entries(CATEGORY_NAMES).map(([key, name]) => 
              `<option value="${key}">${name}</option>`
            ).join('')}
          </select>
        </div>
        
        <div class="filter-group">
          <label>Thời gian:</label>
          <select id="filter-date">
            <option value="all">Tất cả</option>
            <option value="today">Hôm nay</option>
            <option value="yesterday">Hôm qua</option>
            <option value="week">Tuần này</option>
            <option value="month">Tháng này</option>
            <option value="last-month">Tháng trước</option>
          </select>
        </div>
        
        <div class="filter-group search-group">
          <label>Tìm kiếm:</label>
          <input type="text" id="filter-search" placeholder="Tìm kiếm giao dịch...">
        </div>
        
        <button class="btn-clear-filters" id="clear-filters">Xóa bộ lọc</button>
      </div>
    `;
  }

  renderTransactions() {
    const itemsContainer = document.getElementById('transaction-items');
    
    if (this.filteredTransactions.length === 0) {
      itemsContainer.innerHTML = `
        <div class="empty-state">
          <p>Không có giao dịch nào</p>
          <button class="btn-primary" onclick="window.location.href='/transactions/add'">
            Thêm giao dịch đầu tiên
          </button>
        </div>
      `;
      return;
    }

    // Group by date if enabled
    let groupedTransactions = [];
    if (this.options.groupByDate) {
      groupedTransactions = this.groupTransactionsByDate();
    } else {
      groupedTransactions = [{ date: null, transactions: this.getPaginatedTransactions() }];
    }

    itemsContainer.innerHTML = groupedTransactions.map(group => `
      ${group.date ? `<div class="date-header">${group.date}</div>` : ''}
      <div class="transaction-group">
        ${group.transactions.map(transaction => this.renderTransactionItem(transaction)).join('')}
      </div>
    `).join('');

    this.updateSummary();
    if (this.options.showPagination) {
      this.renderPagination();
    }
  }

  renderTransactionItem(transaction) {
    const isIncome = transaction.type === 'income';
    const categoryName = CATEGORY_NAMES[transaction.category] || 'Khác';
    const categoryColor = CATEGORY_COLORS[transaction.category] || COLORS.other;
    
    return `
      <div class="transaction-item" data-id="${transaction.id}">
        <div class="transaction-main">
          <div class="transaction-category">
            <div class="category-indicator" style="background-color: ${categoryColor}"></div>
            <span class="category-name">${categoryName}</span>
          </div>
          
          <div class="transaction-info">
            <div class="transaction-description">${transaction.description || 'Không có mô tả'}</div>
            <div class="transaction-meta">
              <span class="transaction-date">${formatDate(transaction.date)}</span>
              <span class="transaction-time">${formatDate(transaction.date, 'HH:mm')}</span>
              ${transaction.paymentMethod ? `<span class="payment-method">${transaction.paymentMethod}</span>` : ''}
            </div>
          </div>
          
          <div class="transaction-amount ${isIncome ? 'income' : 'expense'}">
            ${isIncome ? '+' : '-'}${formatCurrency(Math.abs(transaction.amount))}
          </div>
        </div>
        
        <div class="transaction-actions">
          ${this.options.allowEdit ? `<button class="btn-edit" data-id="${transaction.id}">Sửa</button>` : ''}
          ${this.options.allowDelete ? `<button class="btn-delete" data-id="${transaction.id}">Xóa</button>` : ''}
          ${transaction.receipt ? '<span class="receipt-indicator">�</span>' : ''}
        </div>
      </div>
    `;
  }

  groupTransactionsByDate() {
    const groups = {};
    
    this.getPaginatedTransactions().forEach(transaction => {
      const date = new Date(transaction.date).toDateString();
      if (!groups[date]) {
        groups[date] = [];
      }
      groups[date].push(transaction);
    });

    return Object.entries(groups).map(([dateStr, transactions]) => ({
      date: this.formatGroupDate(new Date(dateStr)),
      transactions
    }));
  }

  formatGroupDate(date) {
    const today = new Date().toDateString();
    const yesterday = new Date(Date.now() - 24 * 60 * 60 * 1000).toDateString();
    const dateStr = date.toDateString();

    if (dateStr === today) return 'Hôm nay';
    if (dateStr === yesterday) return 'Hôm qua';
    
    return formatDate(date, 'DD/MM/YYYY');
  }

  getPaginatedTransactions() {
    if (!this.options.showPagination) {
      return this.filteredTransactions;
    }

    const start = (this.currentPage - 1) * this.options.itemsPerPage;
    const end = start + this.options.itemsPerPage;
    return this.filteredTransactions.slice(start, end);
  }

  updateSummary() {
    const totalIncome = this.filteredTransactions
      .filter(t => t.type === 'income')
      .reduce((sum, t) => sum + parseFloat(t.amount), 0);
      
    const totalExpense = this.filteredTransactions
      .filter(t => t.type === 'expense')
      .reduce((sum, t) => sum + parseFloat(t.amount), 0);
      
    const netAmount = totalIncome - totalExpense;

    document.getElementById('total-income').textContent = formatCurrency(totalIncome);
    document.getElementById('total-expense').textContent = formatCurrency(totalExpense);
    
    const netElement = document.getElementById('net-amount');
    netElement.textContent = formatCurrency(Math.abs(netAmount));
    netElement.className = `amount ${netAmount >= 0 ? 'positive' : 'negative'}`;
  }

  renderPagination() {
    const totalPages = Math.ceil(this.filteredTransactions.length / this.options.itemsPerPage);
    const paginationContainer = document.getElementById('pagination');
    
    if (totalPages <= 1) {
      paginationContainer.innerHTML = '';
      return;
    }

    const pages = [];
    const maxVisible = 5;
    let start = Math.max(1, this.currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible - 1);
    
    if (end - start + 1 < maxVisible) {
      start = Math.max(1, end - maxVisible + 1);
    }

    // Previous button
    if (this.currentPage > 1) {
      pages.push(`<button class="page-btn" data-page="${this.currentPage - 1}">‹</button>`);
    }

    // First page
    if (start > 1) {
      pages.push(`<button class="page-btn" data-page="1">1</button>`);
      if (start > 2) {
        pages.push(`<span class="page-dots">...</span>`);
      }
    }

    // Page numbers
    for (let i = start; i <= end; i++) {
      pages.push(`
        <button class="page-btn ${i === this.currentPage ? 'active' : ''}" data-page="${i}">
          ${i}
        </button>
      `);
    }

    // Last page
    if (end < totalPages) {
      if (end < totalPages - 1) {
        pages.push(`<span class="page-dots">...</span>`);
      }
      pages.push(`<button class="page-btn" data-page="${totalPages}">${totalPages}</button>`);
    }

    // Next button
    if (this.currentPage < totalPages) {
      pages.push(`<button class="page-btn" data-page="${this.currentPage + 1}">›</button>`);
    }

    paginationContainer.innerHTML = `
      <div class="pagination-info">
        Trang ${this.currentPage} / ${totalPages} 
        (${this.filteredTransactions.length} giao dịch)
      </div>
      <div class="pagination-buttons">
        ${pages.join('')}
      </div>
    `;
  }

  attachEventListeners() {
    // Filter event listeners
    if (this.options.showFilters) {
      document.getElementById('filter-type').addEventListener('change', (e) => {
        this.filters.type = e.target.value;
        this.applyFilters();
      });

      document.getElementById('filter-category').addEventListener('change', (e) => {
        this.filters.category = e.target.value;
        this.applyFilters();
      });

      document.getElementById('filter-date').addEventListener('change', (e) => {
        this.filters.dateRange = e.target.value;
        this.applyFilters();
      });

      document.getElementById('filter-search').addEventListener('input', (e) => {
        this.filters.search = e.target.value;
        this.applyFilters();
      });

      document.getElementById('clear-filters').addEventListener('click', () => {
        this.clearFilters();
      });
    }

    // Pagination event listeners
    this.container.addEventListener('click', (e) => {
      if (e.target.classList.contains('page-btn')) {
        const page = parseInt(e.target.dataset.page);
        this.goToPage(page);
      }

      if (e.target.classList.contains('btn-edit')) {
        const id = e.target.dataset.id;
        this.onEdit?.(id);
      }

      if (e.target.classList.contains('btn-delete')) {
        const id = e.target.dataset.id;
        this.onDelete?.(id);
      }
    });
  }

  // Public methods
  setTransactions(transactions) {
    this.transactions = transactions;
    this.applyFilters();
  }

  applyFilters() {
    this.filteredTransactions = this.transactions.filter(transaction => {
      // Type filter
      if (this.filters.type !== 'all' && transaction.type !== this.filters.type) {
        return false;
      }

      // Category filter
      if (this.filters.category !== 'all' && transaction.category !== this.filters.category) {
        return false;
      }

      // Date range filter
      if (this.filters.dateRange !== 'all') {
        const transactionDate = new Date(transaction.date);
        const now = new Date();
        
        switch (this.filters.dateRange) {
          case 'today':
            if (transactionDate.toDateString() !== now.toDateString()) return false;
            break;
          case 'yesterday':
            const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
            if (transactionDate.toDateString() !== yesterday.toDateString()) return false;
            break;
          case 'week':
            const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
            if (transactionDate < weekAgo) return false;
            break;
          case 'month':
            if (transactionDate.getMonth() !== now.getMonth() || 
                transactionDate.getFullYear() !== now.getFullYear()) return false;
            break;
          case 'last-month':
            const lastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
            const lastMonthEnd = new Date(now.getFullYear(), now.getMonth(), 0);
            if (transactionDate < lastMonth || transactionDate > lastMonthEnd) return false;
            break;
        }
      }

      // Search filter
      if (this.filters.search) {
        const searchTerm = this.filters.search.toLowerCase();
        const description = (transaction.description || '').toLowerCase();
        const category = (CATEGORY_NAMES[transaction.category] || '').toLowerCase();
        const amount = transaction.amount.toString();
        
        if (!description.includes(searchTerm) && 
            !category.includes(searchTerm) && 
            !amount.includes(searchTerm)) {
          return false;
        }
      }

      return true;
    });

    this.currentPage = 1;
    this.renderTransactions();
  }

  clearFilters() {
    this.filters = {
      type: 'all',
      category: 'all',
      dateRange: 'all',
      search: ''
    };

    // Reset form values
    document.getElementById('filter-type').value = 'all';
    document.getElementById('filter-category').value = 'all';
    document.getElementById('filter-date').value = 'all';
    document.getElementById('filter-search').value = '';

    this.applyFilters();
  }

  goToPage(page) {
    this.currentPage = page;
    this.renderTransactions();
  }

  refresh() {
    this.renderTransactions();
  }

  renderStyles() {
    if (document.getElementById('transaction-list-styles')) return;

    const style = document.createElement('style');
    style.id = 'transaction-list-styles';
    style.textContent = `
      .transaction-list {
        max-width: 100%;
        margin: 0 auto;
      }

      .transaction-filters {
        display: flex;
        gap: 1rem;
        margin-bottom: 1.5rem;
        padding: 1rem;
        background: #f9fafb;
        border-radius: 8px;
        flex-wrap: wrap;
      }

      .filter-group {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
      }

      .filter-group label {
        font-size: 0.875rem;
        font-weight: 500;
        color: #374151;
      }

      .filter-group select,
      .filter-group input {
        padding: 0.5rem;
        border: 1px solid #d1d5db;
        border-radius: 6px;
        font-size: 0.875rem;
      }

      .search-group {
        flex: 1;
        min-width: 200px;
      }

      .btn-clear-filters {
        align-self: end;
        padding: 0.5rem 1rem;
        background: #6b7280;
        color: white;
        border: none;
        border-radius: 6px;
        cursor: pointer;
        font-size: 0.875rem;
      }

      .btn-clear-filters:hover {
        background: #4b5563;
      }

      .transaction-summary {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 1rem;
        margin-bottom: 1.5rem;
      }

      .summary-item {
        padding: 1rem;
        background: white;
        border-radius: 8px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        text-align: center;
      }

      .summary-item .label {
        display: block;
        font-size: 0.875rem;
        color: #6b7280;
        margin-bottom: 0.5rem;
      }

      .summary-item .amount {
        display: block;
        font-size: 1.25rem;
        font-weight: 600;
      }

      .summary-item.income .amount {
        color: #10b981;
      }

      .summary-item.expense .amount {
        color: #ef4444;
      }

      .summary-item.net .amount.positive {
        color: #10b981;
      }

      .summary-item.net .amount.negative {
        color: #ef4444;
      }

      .date-header {
        font-weight: 600;
        color: #374151;
        padding: 0.75rem 0 0.5rem 0;
        border-bottom: 1px solid #e5e7eb;
        margin-bottom: 0.5rem;
      }

      .transaction-group {
        margin-bottom: 1.5rem;
      }

      .transaction-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem;
        background: white;
        border-radius: 8px;
        margin-bottom: 0.5rem;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        transition: transform 0.2s, box-shadow 0.2s;
      }

      .transaction-item:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      }

      .transaction-main {
        display: flex;
        align-items: center;
        gap: 1rem;
        flex: 1;
      }

      .transaction-category {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .category-indicator {
        width: 12px;
        height: 12px;
        border-radius: 50%;
      }

      .category-name {
        font-size: 0.875rem;
        color: #6b7280;
        white-space: nowrap;
      }

      .transaction-info {
        flex: 1;
      }

      .transaction-description {
        font-weight: 500;
        color: #111827;
        margin-bottom: 0.25rem;
      }

      .transaction-meta {
        display: flex;
        gap: 0.75rem;
        font-size: 0.75rem;
        color: #6b7280;
      }

      .transaction-amount {
        font-size: 1.125rem;
        font-weight: 600;
      }

      .transaction-amount.income {
        color: #10b981;
      }

      .transaction-amount.expense {
        color: #ef4444;
      }

      .transaction-actions {
        display: flex;
        gap: 0.5rem;
        align-items: center;
      }

      .btn-edit, .btn-delete {
        padding: 0.25rem 0.5rem;
        font-size: 0.75rem;
        border: none;
        border-radius: 4px;
        cursor: pointer;
      }

      .btn-edit {
        background: #3b82f6;
        color: white;
      }

      .btn-delete {
        background: #ef4444;
        color: white;
      }

      .btn-edit:hover {
        background: #2563eb;
      }

      .btn-delete:hover {
        background: #dc2626;
      }

      .receipt-indicator {
        font-size: 1rem;
        color: #6b7280;
      }

      .empty-state {
        text-align: center;
        padding: 3rem 1rem;
        color: #6b7280;
      }

      .empty-state p {
        margin-bottom: 1rem;
        font-size: 1.125rem;
      }

      .btn-primary {
        background: #3b82f6;
        color: white;
        padding: 0.75rem 1.5rem;
        border: none;
        border-radius: 6px;
        cursor: pointer;
        font-size: 0.875rem;
        text-decoration: none;
        display: inline-block;
      }

      .btn-primary:hover {
        background: #2563eb;
      }

      .loading-state {
        text-align: center;
        padding: 2rem;
        color: #6b7280;
      }

      .loading-spinner {
        width: 24px;
        height: 24px;
        border: 2px solid #e5e7eb;
        border-top: 2px solid #3b82f6;
        border-radius: 50%;
        animation: spin 1s linear infinite;
        margin: 0 auto 0.5rem;
      }

      @keyframes spin {
        to { transform: rotate(360deg); }
      }

      .pagination {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 1.5rem;
        padding: 1rem 0;
      }

      .pagination-info {
        font-size: 0.875rem;
        color: #6b7280;
      }

      .pagination-buttons {
        display: flex;
        gap: 0.25rem;
      }

      .page-btn {
        padding: 0.5rem 0.75rem;
        border: 1px solid #d1d5db;
        background: white;
        cursor: pointer;
        font-size: 0.875rem;
        border-radius: 4px;
      }

      .page-btn:hover {
        background: #f3f4f6;
      }

      .page-btn.active {
        background: #3b82f6;
        color: white;
        border-color: #3b82f6;
      }

      .page-dots {
        padding: 0.5rem 0.75rem;
        color: #6b7280;
        font-size: 0.875rem;
      }

      @media (max-width: 768px) {
        .transaction-filters {
          flex-direction: column;
          gap: 0.75rem;
        }

        .filter-group {
          flex-direction: row;
          align-items: center;
          gap: 0.5rem;
        }

        .filter-group label {
          min-width: 80px;
        }

        .search-group {
          flex-direction: column;
          align-items: stretch;
        }

        .transaction-main {
          flex-direction: column;
          align-items: flex-start;
          gap: 0.5rem;
        }

        .transaction-amount {
          align-self: flex-end;
        }

        .pagination {
          flex-direction: column;
          gap: 1rem;
        }
      }
    `;
    
    document.head.appendChild(style);
  }
}

export default TransactionList;